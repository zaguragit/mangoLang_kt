package mango.compilation.llvm

import mango.compilation.Emitter
import mango.compilation.llvm.LLVMValue.*
import mango.interpreter.binding.*
import mango.interpreter.binding.nodes.BoundBinaryOperatorType
import mango.interpreter.binding.nodes.BoundNodeType
import mango.interpreter.binding.nodes.BoundUnaryOperatorType
import mango.interpreter.binding.nodes.expressions.*
import mango.interpreter.binding.nodes.statements.*
import mango.interpreter.symbols.*

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
                    LLVMType.valueOf(it.type)
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
            for (instruction in body!!.statements) {
                when (instruction.boundType) {
                    BoundNodeType.ExpressionStatement -> {
                        val expression = (instruction as BoundExpressionStatement).expression
                        emitValue(currentBlock, expression)
                    }
                    BoundNodeType.VariableDeclaration -> {
                        instruction as BoundVariableDeclaration

                        currentBlock.tmpVal(
                            instruction.variable.name,
                            emitInstruction(currentBlock, instruction.initializer)!!)
                        /*
                        if (instruction.initializer.type.kind == Symbol.Kind.Struct) {
                            val structType = LLVMType.valueOf(instruction.initializer.type)
                            currentBlock.alloc(instruction.variable.name, structType)
                        } else {
                            currentBlock.tmpVal(
                                instruction.variable.name,
                                emitInstruction(currentBlock, instruction.initializer)!!)
                        }*/
                    }
                    BoundNodeType.LabelStatement -> {
                        instruction as BoundLabelStatement
                        val lastInstruction = currentBlock.lastOrNull()
                        if (lastInstruction.isJump) {
                            currentBlock.addInstruction(Jmp(instruction.symbol.name))
                        }
                        currentBlock = currentBlock.functionBuilder.createBlock(instruction.symbol.name)
                    }
                    BoundNodeType.GotoStatement -> currentBlock.jump((instruction as BoundGotoStatement).label.name)
                    BoundNodeType.ConditionalGotoStatement -> {
                        instruction as BoundConditionalGotoStatement
                        val condition = emitValue(currentBlock, instruction.condition)!!
                        val antiName = instruction.label.name + "_anti"
                        if (instruction.jumpIfTrue) {
                            currentBlock.conditionalJump(condition, instruction.label.name, antiName)
                        } else {
                            currentBlock.conditionalJump(condition, antiName, instruction.label.name)
                        }
                        currentBlock = currentBlock.functionBuilder.createBlock(antiName)
                    }
                    BoundNodeType.ReturnStatement -> {
                        instruction as BoundReturnStatement
                        if (instruction.expression == null) {
                            currentBlock.ret()
                        } else {
                            currentBlock.ret(emitValue(currentBlock, instruction.expression)!!)
                        }
                    }
                    BoundNodeType.NopStatement -> {}
                    else -> throw Exception("internal error: Unknown statement to LLVM")
                }
            }
            if (symbol.type == TypeSymbol.Unit && body.statements.lastOrNull()?.boundType != BoundNodeType.ReturnStatement) {
                currentBlock.ret()
            }
        }
        for (v in program.statement.statements) {
            v as BoundVariableDeclaration
            val expression = v.initializer
            val value: LLVMValue? = when (expression.boundType) {
                BoundNodeType.AssignmentExpression -> emitAssignment(initBlock, expression as BoundAssignmentExpression)
                BoundNodeType.CastExpression -> {
                    expression as BoundCastExpression
                    null
                }
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
            builder.globalVariable((v.variable as VisibleSymbol).mangledName(), LLVMType.valueOf(v.variable.type), value!!.code)
        }
        return builder.code()
    }

    private fun emitValue(
        block: BlockBuilder,
        expression: BoundExpression
    ): LLVMValue? = when (expression.boundType) {
        BoundNodeType.AssignmentExpression -> emitAssignment(block, expression as BoundAssignmentExpression)
        BoundNodeType.CastExpression -> {
            expression as BoundCastExpression
            null
        }
        BoundNodeType.LiteralExpression -> emitLiteral(block, expression as BoundLiteralExpression, true)
        BoundNodeType.VariableExpression -> emitVariableExpression(block, expression as BoundVariableExpression)
        BoundNodeType.StructFieldAccess -> emitStructFieldAccess(block, expression as BoundStructFieldAccess)
        BoundNodeType.ErrorExpression -> throw Exception("Error expression got to the emission stage")
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
        val name = emitValue(block, BoundVariableExpression(expression.variable))!!
        block.addInstruction(Store(value, name))
        return value
    }

    private fun emitLiteral(
        block: BlockBuilder,
        expression: BoundLiteralExpression,
        isLocal: Boolean
    ): LLVMValue? = when {
        expression.type.isOfType(TypeSymbol.String) -> {
            if (isLocal) {
                block.stringConstForContent(expression.value as String).ref
            } else {
                val content = expression.value as String
                val chars = block.cStringConstForContent(content)
                val length = Int(content.length, LLVMType.I32)
                val type = LLVMType.valueOf(TypeSymbol.String)
                Struct(type, arrayOf(length, chars.ref))
            }
        }
        expression.type.isOfType(TypeSymbol.AnyI) -> Int((expression.value as Number).toInt(), LLVMType.valueOf(expression.type))
        expression.type.isOfType(TypeSymbol.AnyU) -> Int((expression.value as Number).toInt(), LLVMType.valueOf(expression.type))
        expression.type.isOfType(TypeSymbol.Float) -> Float((expression.value as Number).toFloat(), LLVMType.Float)
        expression.type.isOfType(TypeSymbol.Double) -> Float((expression.value as Number).toFloat(), LLVMType.Double)
        expression.type.isOfType(TypeSymbol.Bool) -> Bool(expression.value as Boolean)
        else -> throw Exception("Unknown literal type")
    }

    private fun emitVariableExpression(
        block: BlockBuilder,
        expression: BoundVariableExpression
    ): LLVMValue = when (expression.symbol.kind) {
        Symbol.Kind.Parameter -> {
            val i = block.functionBuilder.symbol.parameters.indexOfFirst {
                it.name == expression.symbol.name
            }
            block.functionBuilder.paramReference(i)
        }
        Symbol.Kind.VisibleVariable -> {
            val variable = expression.symbol; variable as VisibleSymbol
            if (expression.type.kind == Symbol.Kind.Struct) {
                GlobalRef(variable.mangledName(), LLVMType.valueOf(variable.type))
            } else {
                block.load(GlobalRef(variable.mangledName(), LLVMType.valueOf(variable.type)))
            }
        }
        else -> {
            val variable = expression.symbol
            LocalRef(variable.name, if (variable.type.kind == Symbol.Kind.Struct) LLVMType.Ptr(LLVMType.valueOf(variable.type)) else LLVMType.valueOf(variable.type))
        }
    }

    private fun emitInstruction(
        block: BlockBuilder,
        expression: BoundExpression
    ): LLVMInstruction? = when (expression.boundType) {
        BoundNodeType.CallExpression -> {
            expression as BoundCallExpression
            val type = if (expression.type.kind == Symbol.Kind.Struct) LLVMType.Ptr(LLVMType.valueOf(expression.type)) else LLVMType.valueOf(expression.type)
            val function = expression.symbol
            Call(type, function, *Array(expression.arguments.size) {
                emitValue(block, expression.arguments.elementAt(it))!!
            })
        }
        BoundNodeType.UnaryExpression -> emitUnaryExpression(block, expression as BoundUnaryExpression)
        BoundNodeType.BinaryExpression -> emitBinaryExpression(block, expression as BoundBinaryExpression)
        BoundNodeType.ErrorExpression -> throw Exception("Error expression got to the emission stage")
        else -> throw Exception("internal error: Unknown expression to LLVM (${expression.boundType})")
    }

    private fun emitUnaryExpression(
        block: BlockBuilder,
        expression: BoundUnaryExpression
    ): LLVMInstruction {
        val operand = emitValue(block, expression.operand)!!
        return when (expression.operator.type) {
            BoundUnaryOperatorType.Identity -> IntAdd(Int(0, operand.type), operand)
            BoundUnaryOperatorType.Negation -> IntSub(Int(0, operand.type), operand)
            BoundUnaryOperatorType.Not -> IntSub(Int(1, LLVMType.Bool), operand)
        }
    }

    private fun emitBinaryExpression(
        block: BlockBuilder,
        expression: BoundBinaryExpression
    ): LLVMInstruction? {
        val left = emitValue(block, expression.left)!!
        val right = emitValue(block, expression.right)!!
        return when (expression.operator.type) {
            BoundBinaryOperatorType.Add -> if (expression.right.type == TypeSymbol.AnyI) {
                IntAdd(left, right)
            } else null
            BoundBinaryOperatorType.Sub -> if (expression.right.type == TypeSymbol.AnyI) {
                IntSub(left, right)
            } else null
            BoundBinaryOperatorType.Mul -> if (expression.right.type == TypeSymbol.AnyI) {
                IntMul(left, right)
            } else null
            BoundBinaryOperatorType.Div -> when (expression.right.type) {
                TypeSymbol.AnyI -> IntDiv(left, right)
                TypeSymbol.AnyU -> UIntDiv(left, right)
                else -> null
            }
            BoundBinaryOperatorType.Rem -> null
            BoundBinaryOperatorType.BitAnd -> null
            BoundBinaryOperatorType.BitOr -> null
            BoundBinaryOperatorType.LogicAnd -> null
            BoundBinaryOperatorType.LogicOr -> null
            BoundBinaryOperatorType.LessThan -> Icmp(Icmp.Type.LessThan, left, right)
            BoundBinaryOperatorType.MoreThan -> Icmp(Icmp.Type.MoreThan, left, right)
            BoundBinaryOperatorType.IsEqual -> Icmp(Icmp.Type.IsEqual, left, right)
            BoundBinaryOperatorType.IsEqualOrMore -> Icmp(Icmp.Type.IsEqualOrMore, left, right)
            BoundBinaryOperatorType.IsEqualOrLess -> Icmp(Icmp.Type.IsEqualOrLess, left, right)
            BoundBinaryOperatorType.IsNotEqual -> Icmp(Icmp.Type.IsNotEqual, left, right)
            BoundBinaryOperatorType.IsIdentityEqual -> Icmp(Icmp.Type.IsEqual, left, right)
            BoundBinaryOperatorType.IsNotIdentityEqual -> Icmp(Icmp.Type.IsNotEqual, left, right)
        }
    }

    private fun emitStructFieldAccess(
        block: BlockBuilder,
        expression: BoundStructFieldAccess
    ): LLVMValue = block.getStructField(emitValue(block, expression.struct)!!, expression.i, expression.field)
}