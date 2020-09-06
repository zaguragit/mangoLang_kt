package mango.compilation.llvm

import mango.compilation.Emitter
import mango.compilation.llvm.LLVMValue.*
import mango.compilation.llvm.LLVMValue.Float
import mango.compilation.llvm.LLVMValue.Int
import mango.interpreter.binding.Program
import mango.interpreter.binding.nodes.BiOperator
import mango.interpreter.binding.nodes.BoundNode
import mango.interpreter.binding.nodes.UnOperator
import mango.interpreter.binding.nodes.expressions.*
import mango.interpreter.binding.nodes.statements.*
import mango.interpreter.symbols.Symbol
import mango.interpreter.symbols.TypeSymbol
import mango.interpreter.symbols.VisibleSymbol
import mango.util.EmitterError

object LLVMEmitter : Emitter {

    override fun emit(
        program: Program,
        moduleName: String
    ): String {

        val builder = ModuleBuilder()
        lateinit var initBlock: BlockBuilder

        for (struct in TypeSymbol.map.values) {
            if (struct is TypeSymbol.StructTypeSymbol) {
                builder.declareStruct(struct.name, struct.fields.map {
                    LLVMType[it.type]
                }.toTypedArray())
            }
        }

        for (f in program.functions) {
            val symbol = f.key
            val body = f.value
            if (symbol.meta.isExtern) {
                builder.addImportedDeclaration(symbol)
                continue
            }
            val function = builder.createFunction(symbol)
            if (symbol.meta.isInline) {
                function.addAttribute("alwaysinline")
            }
            var currentBlock = if (symbol.meta.isEntry) {
                initBlock = function.entryBlock()
                function.createBlock(null)
            } else function.entryBlock()
            for (statement in body!!.statements) {
                when (statement.kind) {
                    BoundNode.Kind.ExpressionStatement -> {
                        val expression = (statement as ExpressionStatement).expression
                        emitValue(currentBlock, expression)
                    }
                    BoundNode.Kind.VariableDeclaration -> {
                        statement as VariableDeclaration
                        if (statement.initializer.kind == BoundNode.Kind.LiteralExpression ||
                            statement.initializer.kind == BoundNode.Kind.VariableExpression ||
                            statement.initializer.kind == BoundNode.Kind.ReferenceExpression) {
                            val value = emitValue(currentBlock, statement.initializer)!!
                            val alloc = currentBlock.alloc(statement.variable.realName, value.type)
                            currentBlock.store(alloc, value)
                        } else {
                            val initializer = emitInstruction(currentBlock, statement.initializer)!!
                            val alloc = currentBlock.alloc(statement.variable.realName, initializer.type)
                            currentBlock.store(alloc, currentBlock.tmpVal(initializer).ref)
                        }
                    }
                    BoundNode.Kind.LabelStatement -> {
                        statement as LabelStatement
                        val lastInstruction = currentBlock.lastOrNull()
                        if (lastInstruction.isJump) {
                            currentBlock.addInstruction(Jmp(statement.symbol.name))
                        }
                        currentBlock = currentBlock.functionBuilder.createBlock(statement.symbol.name)
                    }
                    BoundNode.Kind.GotoStatement -> currentBlock.jump((statement as GotoStatement).label.name)
                    BoundNode.Kind.ConditionalGotoStatement -> {
                        statement as ConditionalGotoStatement
                        val condition = emitValue(currentBlock, statement.condition)!!
                        val antiName = statement.label.name + "_anti"
                        if (statement.jumpIfTrue) {
                            currentBlock.conditionalJump(condition, statement.label.name, antiName)
                        } else {
                            currentBlock.conditionalJump(condition, antiName, statement.label.name)
                        }
                        currentBlock = currentBlock.functionBuilder.createBlock(antiName)
                    }
                    BoundNode.Kind.ReturnStatement -> {
                        statement as ReturnStatement
                        if (statement.expression == null) {
                            currentBlock.ret()
                        } else {
                            currentBlock.ret(emitValue(currentBlock, statement.expression)!!)
                        }
                    }
                    BoundNode.Kind.NopStatement -> {}
                    else -> throw EmitterError("internal error: Unknown statement to LLVM")
                }
            }
            if (symbol.returnType == TypeSymbol.Unit && body.statements.lastOrNull()?.kind != BoundNode.Kind.ReturnStatement) {
                currentBlock.ret()
            }
        }
        for (v in program.statement.statements) {
            v as VariableDeclaration
            val expression = v.initializer
            val value: LLVMValue? = when (expression.kind) {
                BoundNode.Kind.AssignmentExpression -> emitAssignment(initBlock, expression as AssignmentExpression)
                BoundNode.Kind.CastExpression -> emitValue(initBlock, (expression as CastExpression).expression)
                BoundNode.Kind.LiteralExpression -> emitLiteral(initBlock, expression as LiteralExpression)
                BoundNode.Kind.VariableExpression -> emitVariableExpression(initBlock, expression as NameExpression)
                else -> {
                    val instruction = emitInstruction(initBlock, expression)!!
                    val type = instruction.type
                    if (type != LLVMType.Void) {
                        initBlock.tmpVal(instruction).ref
                    }
                    else {
                        initBlock.addInstruction(instruction)
                        null
                    }
                }
            }
            var type = LLVMType[v.variable.type]
            if (type is LLVMType.Ptr) type = type.element
            builder.globalVariable((v.variable as VisibleSymbol).mangledName(), value!!)
        }
        return builder.code()
    }

    private fun emitValue(
        block: BlockBuilder,
        expression: BoundExpression
    ): LLVMValue? = when (expression.kind) {
        BoundNode.Kind.AssignmentExpression -> emitAssignment(block, expression as AssignmentExpression)
        BoundNode.Kind.CastExpression -> emitValue(block, (expression as CastExpression).expression)
        BoundNode.Kind.LiteralExpression -> emitLiteral(block, expression as LiteralExpression)
        BoundNode.Kind.VariableExpression -> emitVariableExpression(block, expression as NameExpression)
        BoundNode.Kind.ErrorExpression -> throw EmitterError("Error expression got to the emission stage")
        BoundNode.Kind.ReferenceExpression -> emitReference(block, (expression as Reference).expression)
        else -> {
            val instruction = emitInstruction(block, expression)!!
            val type = instruction.type
            if (type == LLVMType.Void) {
                block.addInstruction(instruction)
                null
            } else {
                block.tmpVal(instruction).ref
            }
        }
    }

    private fun emitAssignment(
        block: BlockBuilder,
        expression: AssignmentExpression
    ): LLVMValue {
        val value = emitValue(block, expression.expression)!!
        val variable = expression.variable
        block.store(if (variable is VisibleSymbol) {
            GlobalRef(expression.variable.realName, LLVMType.Ptr(LLVMType[expression.variable.type]))
        } else {
            LocalRef(expression.variable.realName, LLVMType.Ptr(LLVMType[expression.variable.type]))
        }, value)
        return value
    }

    private fun emitLiteral(
        block: BlockBuilder,
        expression: LiteralExpression
    ): LLVMValue = when {
        expression.value == null -> Null(LLVMType[expression.type])
        expression.type.isOfType(TypeSymbol["String"]!!/*TypeSymbol.String*/) -> block.stringConstForContent(expression.value as String).ref
        expression.type.isOfType(TypeSymbol.Integer) -> Int((expression.value as Number).toInt(), LLVMType[expression.type])
        expression.type.isOfType(TypeSymbol.UInteger) -> Int((expression.value as Number).toInt(), LLVMType[expression.type])
        expression.type.isOfType(TypeSymbol.Float) -> Float((expression.value as Number).toFloat(), LLVMType.Float)
        expression.type.isOfType(TypeSymbol.Double) -> Float((expression.value as Number).toFloat(), LLVMType.Double)
        expression.type.isOfType(TypeSymbol.Bool) -> Bool(expression.value as Boolean)
        else -> throw EmitterError("Unknown literal type")
    }

    private fun emitVariableExpression(
        block: BlockBuilder,
        expression: NameExpression
    ): LLVMValue = when (expression.symbol.kind) {
        Symbol.Kind.Parameter -> {
            val i = block.functionBuilder.symbol.parameters.indexOfFirst {
                it.realName == expression.symbol.realName
            }
            block.functionBuilder.paramReference(i)
        }
        else -> {
            var value = emitReference(block, expression)
            var type = value.type as LLVMType.Ptr
            if (value is LocalRef && type.element is LLVMType.Fn) {
                type = LLVMType.Ptr(type)
                value = LocalRef(value.name, type)
            }
            if (type.element is LLVMType.Fn) { value } else block.load(value)
        }
    }

    private fun emitStructFieldAccess(
        block: BlockBuilder,
        expression: StructFieldAccess
    ) = block.getStructField(emitValue(block, expression.struct)!!, expression.i, expression.field)

    private fun emitReference(
        block: BlockBuilder,
        expression: NameExpression
    ) = if (expression.symbol is VisibleSymbol) {
        val variable = expression.symbol
        GlobalRef(variable.mangledName(), LLVMType[variable.type])
    } else {
        val variable = expression.symbol
        LocalRef(variable.realName, LLVMType.Ptr(LLVMType[variable.type]))
    }

    private fun emitInstruction(
        block: BlockBuilder,
        expression: BoundExpression
    ): LLVMInstruction? = when (expression.kind) {
        BoundNode.Kind.CallExpression -> {
            expression as CallExpression
            val type = /*if (expression.type.kind == Symbol.Kind.Struct) LLVMType.Ptr(LLVMType[expression.type]) else*/ LLVMType[expression.type]
            val function = emitValue(block, expression.expression)!!
            Call(type, function, *Array(expression.arguments.size) {
                emitValue(block, expression.arguments.elementAt(it))!!
            })
        }
        BoundNode.Kind.UnaryExpression -> emitUnaryExpression(block, expression as UnaryExpression)
        BoundNode.Kind.BinaryExpression -> emitBinaryExpression(block, expression as BinaryExpression)
        BoundNode.Kind.PointerAccessExpression -> emitPointerAccessExpression(block, expression as PointerAccess)
        BoundNode.Kind.ReferenceExpression -> emitInstruction(block, (expression as Reference).expression)
        BoundNode.Kind.StructFieldAccess -> emitStructFieldAccess(block, expression as StructFieldAccess)
        BoundNode.Kind.ErrorExpression -> throw EmitterError("Error expression got to the emission stage")
        else -> throw EmitterError("internal error: Unknown expression to LLVM (${expression.kind})")
    }

    private fun emitUnaryExpression(
        block: BlockBuilder,
        expression: UnaryExpression
    ): LLVMInstruction {
        val operand = emitValue(block, expression.operand)!!
        return when (expression.operator.type) {
            UnOperator.Type.Identity -> IntAdd(Int(0, operand.type), operand)
            UnOperator.Type.Negation -> IntSub(Int(0, operand.type), operand)
            UnOperator.Type.Not -> IntSub(Int(1, LLVMType.Bool), operand)
        }
    }

    private fun emitBinaryExpression(
        block: BlockBuilder,
        expression: BinaryExpression
    ): LLVMInstruction? {
        val left = emitValue(block, expression.left)!!
        val right = emitValue(block, expression.right)!!
        return when (expression.operator.type) {
            BiOperator.Type.Add -> if (expression.right.type.isOfType(TypeSymbol.Integer)) {
                IntAdd(left, right)
            } else null
            BiOperator.Type.Sub -> if (expression.right.type.isOfType(TypeSymbol.Integer)) {
                IntSub(left, right)
            } else null
            BiOperator.Type.Mul -> if (expression.right.type.isOfType(TypeSymbol.Integer)) {
                IntMul(left, right)
            } else null
            BiOperator.Type.Div -> when {
                expression.right.type.isOfType(TypeSymbol.Integer) -> IntDiv(left, right)
                expression.right.type.isOfType(TypeSymbol.UInteger) -> UIntDiv(left, right)
                else -> null
            }
            BiOperator.Type.Rem -> null
            BiOperator.Type.BitAnd -> null
            BiOperator.Type.BitOr -> null
            BiOperator.Type.LogicAnd -> null
            BiOperator.Type.LogicOr -> null
            BiOperator.Type.LessThan -> Icmp(Icmp.Type.LessThan, left, right)
            BiOperator.Type.MoreThan -> Icmp(Icmp.Type.MoreThan, left, right)
            BiOperator.Type.IsEqual -> Icmp(Icmp.Type.IsEqual, left, right)
            BiOperator.Type.IsEqualOrMore -> Icmp(Icmp.Type.IsEqualOrMore, left, right)
            BiOperator.Type.IsEqualOrLess -> Icmp(Icmp.Type.IsEqualOrLess, left, right)
            BiOperator.Type.IsNotEqual -> Icmp(Icmp.Type.IsNotEqual, left, right)
            BiOperator.Type.IsIdentityEqual -> Icmp(Icmp.Type.IsEqual, left, right)
            BiOperator.Type.IsNotIdentityEqual -> Icmp(Icmp.Type.IsNotEqual, left, right)
        }
    }

    private fun emitPointerAccessExpression(
        block: BlockBuilder,
        expression: PointerAccess
    ): LLVMInstruction {
        val pointer = emitValue(block, expression.expression)!!
        val i = emitValue(block, expression.i)!!
        val ptr = block.tmpVal(GetPtr((pointer.type as LLVMType.Ptr).element, pointer, i))
        return Load(ptr.ref, (ptr.type as LLVMType.Ptr).element)
    }
}