package mango.compilation.llvm

import mango.compilation.Emitter
import mango.compilation.llvm.LLVMValue.*
import mango.interpreter.binding.*
import mango.interpreter.binding.nodes.BoundBiOperator
import mango.interpreter.binding.nodes.BoundNodeType
import mango.interpreter.binding.nodes.expressions.BoundPointerAccess
import mango.interpreter.binding.nodes.BoundUnOperator
import mango.interpreter.binding.nodes.expressions.*
import mango.interpreter.binding.nodes.statements.*
import mango.interpreter.symbols.*
import mango.util.EmitterError

object LLVMEmitter : Emitter {

    override fun emit(
        program: BoundProgram,
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
                    BoundNodeType.ExpressionStatement -> {
                        val expression = (statement as BoundExpressionStatement).expression
                        emitValue(currentBlock, expression)
                    }
                    BoundNodeType.VariableDeclaration -> {
                        statement as BoundVariableDeclaration
                        if (statement.initializer.kind == BoundNodeType.LiteralExpression ||
                            statement.initializer.kind == BoundNodeType.VariableExpression ||
                            statement.initializer.kind == BoundNodeType.ReferenceExpression) {
                            val value = emitValue(currentBlock, statement.initializer)!!
                            val alloc = currentBlock.alloc(statement.variable.realName, value.type)
                            currentBlock.store(alloc, value)
                        } else {
                            val initializer = emitInstruction(currentBlock, statement.initializer)!!
                            if (initializer.type is LLVMType.Ptr) {
                                currentBlock.tmpVal(statement.variable.realName, initializer)
                            } else {
                                val alloc = currentBlock.alloc(statement.variable.realName, initializer.type)
                                currentBlock.store(alloc, currentBlock.tmpVal(initializer).ref)
                            }
                        }
                    }
                    BoundNodeType.LabelStatement -> {
                        statement as BoundLabelStatement
                        val lastInstruction = currentBlock.lastOrNull()
                        if (lastInstruction.isJump) {
                            currentBlock.addInstruction(Jmp(statement.symbol.name))
                        }
                        currentBlock = currentBlock.functionBuilder.createBlock(statement.symbol.name)
                    }
                    BoundNodeType.GotoStatement -> currentBlock.jump((statement as BoundGotoStatement).label.name)
                    BoundNodeType.ConditionalGotoStatement -> {
                        statement as BoundConditionalGotoStatement
                        val condition = emitValue(currentBlock, statement.condition)!!
                        val antiName = statement.label.name + "_anti"
                        if (statement.jumpIfTrue) {
                            currentBlock.conditionalJump(condition, statement.label.name, antiName)
                        } else {
                            currentBlock.conditionalJump(condition, antiName, statement.label.name)
                        }
                        currentBlock = currentBlock.functionBuilder.createBlock(antiName)
                    }
                    BoundNodeType.ReturnStatement -> {
                        statement as BoundReturnStatement
                        if (statement.expression == null) {
                            currentBlock.ret()
                        } else {
                            currentBlock.ret(emitValue(currentBlock, statement.expression)!!)
                        }
                    }
                    BoundNodeType.NopStatement -> {}
                    else -> throw EmitterError("internal error: Unknown statement to LLVM")
                }
            }
            if (symbol.returnType == TypeSymbol.Unit && body.statements.lastOrNull()?.kind != BoundNodeType.ReturnStatement) {
                currentBlock.ret()
            }
        }
        for (v in program.statement.statements) {
            v as BoundVariableDeclaration
            val expression = v.initializer
            val value: LLVMValue? = when (expression.kind) {
                BoundNodeType.AssignmentExpression -> emitAssignment(initBlock, expression as BoundAssignmentExpression)
                BoundNodeType.CastExpression -> emitValue(initBlock, (expression as BoundCastExpression).expression)
                BoundNodeType.LiteralExpression -> emitLiteral(initBlock, expression as BoundLiteralExpression, false)
                BoundNodeType.VariableExpression -> emitVariableExpression(initBlock, expression as BoundVariableExpression)
                BoundNodeType.StructFieldAccess -> emitStructFieldAccess(initBlock, expression as BoundStructFieldAccess)
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
            builder.globalVariable((v.variable as VisibleSymbol).mangledName(), LLVMType.get(v.variable.type), value!!.code)
        }
        return builder.code()
    }

    private fun emitValue(
        block: BlockBuilder,
        expression: BoundExpression
    ): LLVMValue? = when (expression.kind) {
        BoundNodeType.AssignmentExpression -> emitAssignment(block, expression as BoundAssignmentExpression)
        BoundNodeType.CastExpression -> emitValue(block, (expression as BoundCastExpression).expression)
        BoundNodeType.LiteralExpression -> emitLiteral(block, expression as BoundLiteralExpression, true)
        BoundNodeType.VariableExpression -> emitVariableExpression(block, expression as BoundVariableExpression)
        BoundNodeType.StructFieldAccess -> emitStructFieldAccess(block, expression as BoundStructFieldAccess)
        BoundNodeType.ErrorExpression -> throw EmitterError("Error expression got to the emission stage")
        BoundNodeType.ReferenceExpression -> emitReference(block, (expression as BoundReference).expression)
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
        expression: BoundAssignmentExpression
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
        expression: BoundLiteralExpression,
        isLocal: Boolean
    ): LLVMValue = when {
        expression.type.isOfType(TypeSymbol.String) -> {
            if (isLocal) {
                block.stringConstForContent(expression.value as String).ref
            } else {
                val content = expression.value as String
                val chars = block.cStringConstForContent(content)
                val length = Int(content.length, LLVMType.I32)
                val type = LLVMType[TypeSymbol.String]
                Struct(type, arrayOf(length, chars.ref))
            }
        }
        expression.type.isOfType(TypeSymbol.Integer) -> Int((expression.value as Number).toInt(), LLVMType[expression.type])
        expression.type.isOfType(TypeSymbol.UInteger) -> Int((expression.value as Number).toInt(), LLVMType[expression.type])
        expression.type.isOfType(TypeSymbol.Float) -> Float((expression.value as Number).toFloat(), LLVMType.Float)
        expression.type.isOfType(TypeSymbol.Double) -> Float((expression.value as Number).toFloat(), LLVMType.Double)
        expression.type.isOfType(TypeSymbol.Bool) -> Bool(expression.value as Boolean)
        else -> throw EmitterError("Unknown literal type")
    }

    private fun emitVariableExpression(
        block: BlockBuilder,
        expression: BoundVariableExpression
    ): LLVMValue = when (expression.symbol.kind) {
        Symbol.Kind.Parameter -> {
            val i = block.functionBuilder.symbol.parameters.indexOfFirst {
                it.realName == expression.symbol.realName
            }
            block.functionBuilder.paramReference(i)
        }
        else -> {
            val value = emitReference(block, expression)
            if ((value.type as LLVMType.Ptr).element is LLVMType.Struct) { value } else block.load(value)
        }
    }

    private fun emitStructFieldAccess(
        block: BlockBuilder,
        expression: BoundStructFieldAccess
    ): LLVMValue = block.getStructField(emitValue(block, expression.struct)!!, expression.i, expression.field)

    private fun emitReference(
        block: BlockBuilder,
        expression: BoundVariableExpression
    ) = if (expression.symbol.kind == Symbol.Kind.VisibleVariable) {
        val variable = expression.symbol
        variable as VisibleSymbol
        GlobalRef(variable.mangledName(), LLVMType[variable.type])
    } else {
        val variable = expression.symbol
        LocalRef(variable.realName, LLVMType.Ptr(LLVMType[variable.type]))
    }

    private fun emitInstruction(
        block: BlockBuilder,
        expression: BoundExpression
    ): LLVMInstruction? = when (expression.kind) {
        BoundNodeType.CallExpression -> {
            expression as BoundCallExpression
            val type = if (expression.type.kind == Symbol.Kind.Struct) LLVMType.Ptr(LLVMType[expression.type]) else LLVMType.get(expression.type)
            val function = expression.symbol
            Call(type, function, *Array(expression.arguments.size) {
                emitValue(block, expression.arguments.elementAt(it))!!
            })
        }
        BoundNodeType.UnaryExpression -> emitUnaryExpression(block, expression as BoundUnaryExpression)
        BoundNodeType.BinaryExpression -> emitBinaryExpression(block, expression as BoundBinaryExpression)
        BoundNodeType.PointerAccessExpression -> emitPointerAccessExpression(block, expression as BoundPointerAccess)
        BoundNodeType.ReferenceExpression -> emitInstruction(block, (expression as BoundReference).expression)
        BoundNodeType.ErrorExpression -> throw EmitterError("Error expression got to the emission stage")
        else -> throw EmitterError("internal error: Unknown expression to LLVM (${expression.kind})")
    }

    private fun emitUnaryExpression(
        block: BlockBuilder,
        expression: BoundUnaryExpression
    ): LLVMInstruction {
        val operand = emitValue(block, expression.operand)!!
        return when (expression.operator.type) {
            BoundUnOperator.Type.Identity -> IntAdd(Int(0, operand.type), operand)
            BoundUnOperator.Type.Negation -> IntSub(Int(0, operand.type), operand)
            BoundUnOperator.Type.Not -> IntSub(Int(1, LLVMType.Bool), operand)
        }
    }

    private fun emitBinaryExpression(
        block: BlockBuilder,
        expression: BoundBinaryExpression
    ): LLVMInstruction? {
        val left = emitValue(block, expression.left)!!
        val right = emitValue(block, expression.right)!!
        return when (expression.operator.type) {
            BoundBiOperator.Type.Add -> if (expression.right.type.isOfType(TypeSymbol.Integer)) {
                IntAdd(left, right)
            } else null
            BoundBiOperator.Type.Sub -> if (expression.right.type.isOfType(TypeSymbol.Integer)) {
                IntSub(left, right)
            } else null
            BoundBiOperator.Type.Mul -> if (expression.right.type.isOfType(TypeSymbol.Integer)) {
                IntMul(left, right)
            } else null
            BoundBiOperator.Type.Div -> when {
                expression.right.type.isOfType(TypeSymbol.Integer) -> IntDiv(left, right)
                expression.right.type.isOfType(TypeSymbol.UInteger) -> UIntDiv(left, right)
                else -> null
            }
            BoundBiOperator.Type.Rem -> null
            BoundBiOperator.Type.BitAnd -> null
            BoundBiOperator.Type.BitOr -> null
            BoundBiOperator.Type.LogicAnd -> null
            BoundBiOperator.Type.LogicOr -> null
            BoundBiOperator.Type.LessThan -> Icmp(Icmp.Type.LessThan, left, right)
            BoundBiOperator.Type.MoreThan -> Icmp(Icmp.Type.MoreThan, left, right)
            BoundBiOperator.Type.IsEqual -> Icmp(Icmp.Type.IsEqual, left, right)
            BoundBiOperator.Type.IsEqualOrMore -> Icmp(Icmp.Type.IsEqualOrMore, left, right)
            BoundBiOperator.Type.IsEqualOrLess -> Icmp(Icmp.Type.IsEqualOrLess, left, right)
            BoundBiOperator.Type.IsNotEqual -> Icmp(Icmp.Type.IsNotEqual, left, right)
            BoundBiOperator.Type.IsIdentityEqual -> Icmp(Icmp.Type.IsEqual, left, right)
            BoundBiOperator.Type.IsNotIdentityEqual -> Icmp(Icmp.Type.IsNotEqual, left, right)
        }
    }

    private fun emitPointerAccessExpression(
        block: BlockBuilder,
        expression: BoundPointerAccess
    ): LLVMInstruction {
        val pointer = emitValue(block, expression.expression)!!
        val i = emitValue(block, expression.i)!!
        val ptr = block.tmpVal(GetPtr((pointer.type as LLVMType.Ptr).element, pointer, i))
        return Load(ptr.ref, (ptr.type as LLVMType.Ptr).element)
    }
}