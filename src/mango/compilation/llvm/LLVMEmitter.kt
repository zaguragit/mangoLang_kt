package mango.compilation.llvm

import mango.compilation.Emitter
import mango.compilation.llvm.LLVMValue.*
import mango.interpreter.binding.*
import mango.interpreter.symbols.*

object LLVMEmitter : Emitter {

    override fun emit(
        program: BoundProgram,
        moduleName: String,
        references: Array<String>,
        outputPath: String
    ): String {
        val builder = ModuleBuilder()
        lateinit var initBlock: BlockBuilder
        for (f in program.functions) {
            val symbol = f.key
            val body = f.value
            if (symbol.meta.isExtern) {
                builder.addImportedDeclaration("declare ${LLVMType.valueOf(symbol.type).code} @${symbol.meta.cName ?: symbol.name}(${symbol.parameters.joinToString(", ") { LLVMType.valueOf(it.type).code }})")
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
                        instruction as BoundExpressionStatement
                        val expression = instruction.expression
                        emitValue(currentBlock, expression)
                    }
                    BoundNodeType.VariableDeclaration -> {
                        instruction as BoundVariableDeclaration
                        currentBlock.addInstruction(TempValue(
                            instruction.variable.name,
                            emitInstruction(currentBlock, instruction.initializer)!!
                        ))
                    }
                    BoundNodeType.LabelStatement -> {
                        instruction as BoundLabelStatement
                        val lastInstruction = currentBlock.lastOrNull()
                        if (lastInstruction !is Return &&
                            lastInstruction !is ReturnVoid &&
                            lastInstruction !is ReturnInt &&
                            lastInstruction !is JumpInstruction &&
                            lastInstruction !is IfInstruction) {
                            currentBlock.addInstruction(JumpInstruction(instruction.symbol.name))
                        }
                        currentBlock = currentBlock.functionBuilder.createBlock(instruction.symbol.name)
                    }
                    BoundNodeType.GotoStatement -> {
                        instruction as BoundGotoStatement
                        currentBlock.addInstruction(JumpInstruction(instruction.label.name))
                    }
                    BoundNodeType.ConditionalGotoStatement -> {
                        instruction as BoundConditionalGotoStatement
                        val condition = emitValue(currentBlock, instruction.condition)!!
                        val antiName = instruction.label.name + "_anti"
                        if (instruction.jumpIfTrue) {
                            currentBlock.addInstruction(IfInstruction(condition, instruction.label.name, antiName))
                        } else {
                            currentBlock.addInstruction(IfInstruction(condition, antiName, instruction.label.name))
                        }
                        currentBlock = currentBlock.functionBuilder.createBlock(antiName)
                    }
                    BoundNodeType.ReturnStatement -> {
                        instruction as BoundReturnStatement
                        if (instruction.expression == null) {
                            currentBlock.addInstruction(ReturnVoid())
                        } else {
                            currentBlock.addInstruction(Return(emitValue(currentBlock, instruction.expression)!!))
                        }
                    }
                    BoundNodeType.NopStatement -> {}
                    else -> throw Exception("internal error: Unknown statement to LLVM")
                }
            }
            if (symbol.type == TypeSymbol.unit && body.statements.lastOrNull()?.boundType != BoundNodeType.ReturnStatement) {
                currentBlock.addInstruction(ReturnVoid())
            }
        }
        for (v in program.statement.statements) {
            v as BoundVariableDeclaration
            val expression = v.initializer
            val value: LLVMValue? = when (expression.boundType) {
                BoundNodeType.AssignmentExpression -> {
                    expression as BoundAssignmentExpression
                    val value = emitValue(initBlock, expression.expression)!!
                    val name = emitValue(initBlock, BoundVariableExpression(expression.variable))!!
                    initBlock.addInstruction(Store(value, name))
                    value
                }
                BoundNodeType.CastExpression -> {
                    expression as BoundCastExpression
                    null
                }
                BoundNodeType.LiteralExpression -> {
                    expression as BoundLiteralExpression
                    when (expression.type) {
                        TypeSymbol.string -> {
                            StringRef(initBlock.stringConstForContent(expression.value as String))
                        }
                        TypeSymbol.int -> IntConst(expression.value as Int, LLVMType.I32)
                        TypeSymbol.bool -> BoolConst(expression.value as Boolean)
                        else -> null
                    }
                }
                BoundNodeType.VariableExpression -> {
                    expression as BoundVariableExpression
                    LocalValRef(expression.variable.name, LLVMType.valueOf(expression.variable.type))
                }
                else -> {
                    val instruction = emitInstruction(initBlock, expression)!!
                    val type = instruction.type
                    if (type != null && type != LLVMType.Void) {
                        val uid = newUID()
                        initBlock.addInstruction(TempValue(uid, instruction))
                        LocalValRef(uid, type)
                    }
                    else {
                        initBlock.addInstruction(instruction)
                        null
                    }
                }
            }
            builder.globalVariable((v.variable as VisibleVariableSymbol).path, LLVMType.valueOf(v.variable.type), value!!.code)
        }
        return builder.code()
    }

    private fun emitValue(block: BlockBuilder, expression: BoundExpression): LLVMValue? {
        return when (expression.boundType) {
            BoundNodeType.AssignmentExpression -> {
                expression as BoundAssignmentExpression
                val value = emitValue(block, expression.expression)!!
                val name = emitValue(block, BoundVariableExpression(expression.variable))!!
                block.addInstruction(Store(value, name))
                value
            }
            BoundNodeType.CastExpression -> {
                expression as BoundCastExpression
                null
            }
            BoundNodeType.LiteralExpression -> {
                expression as BoundLiteralExpression
                when (expression.type) {
                    TypeSymbol.string -> {
                        StringRef(block.stringConstForContent(expression.value as String))
                    }
                    TypeSymbol.int -> IntConst(expression.value as Int, LLVMType.I32)
                    TypeSymbol.bool -> BoolConst(expression.value as Boolean)
                    else -> null
                }
            }
            BoundNodeType.VariableExpression -> {
                expression as BoundVariableExpression
                when (expression.variable.kind) {
                    Symbol.Kind.Parameter -> {
                        val i = block.functionBuilder.symbol.parameters.indexOfFirst {
                            it.name == expression.variable.name
                        }
                        block.functionBuilder.paramReference(i)
                    }
                    Symbol.Kind.GlobalVariable -> {
                        val variable = expression.variable as VisibleVariableSymbol
                        val uid = newUID()
                        val tmp = TempValue(uid, Load(GlobalValRef(variable.path, LLVMType.valueOf(expression.variable.type))))
                        block.addInstruction(tmp)
                        tmp.reference()
                    }
                    else -> {
                        LocalValRef(expression.variable.name, LLVMType.valueOf(expression.variable.type))
                    }
                }
            }
            BoundNodeType.ErrorExpression -> return null
            else -> {
                val instruction = emitInstruction(block, expression)!!
                val type = instruction.type
                if (type == null || type == LLVMType.Void) {
                    block.addInstruction(instruction)
                    null
                } else {
                    val uid = newUID()
                    block.addInstruction(TempValue(uid, instruction))
                    LocalValRef(uid, type)
                }
            }
        }
    }


    private fun emitInstruction(block: BlockBuilder, expression: BoundExpression): LLVMInstruction? {
        when (expression.boundType) {
            BoundNodeType.CallExpression -> {
                expression as BoundCallExpression
                val type = LLVMType.valueOf(expression.type)
                val function = expression.function
                when {
                    expression.function === BuiltinFunctions.readln -> {
                        block.functionBuilder.moduleBuilder.include("readln.ll")
                    }
                }
                return Call(type, function, *Array(expression.arguments.size) {
                    emitValue(block, expression.arguments.elementAt(it))!!
                })
            }
            BoundNodeType.UnaryExpression -> {
                expression as BoundUnaryExpression
                val operand = emitValue(block, expression.operand)!!
                return when (expression.operator.type) {
                    BoundUnaryOperatorType.Identity -> IntAddition(IntConst(0, operand.type), operand)
                    BoundUnaryOperatorType.Negation -> IntSubtraction(IntConst(0, operand.type), operand)
                    BoundUnaryOperatorType.Not -> IntSubtraction(IntConst(1, LLVMType.Bool), operand)
                }
            }
            BoundNodeType.BinaryExpression -> {
                expression as BoundBinaryExpression
                val left = emitValue(block, expression.left)!!
                val right = emitValue(block, expression.right)!!
                when (expression.operator.type) {
                    BoundBinaryOperatorType.Add -> {
                        if (expression.right.type == TypeSymbol.int) {
                            return IntAddition(left, right)
                        }
                    }
                    BoundBinaryOperatorType.Sub -> {
                        if (expression.right.type == TypeSymbol.int) {
                            return IntSubtraction(left, right)
                        }
                    }
                    BoundBinaryOperatorType.Mul -> {
                        if (expression.right.type == TypeSymbol.int) {
                            return IntMultiplication(left, right)
                        }
                    }
                    BoundBinaryOperatorType.Div -> {
                        if (expression.right.type == TypeSymbol.int) {
                            return SignedIntDivision(left, right)
                        }
                    }
                    BoundBinaryOperatorType.Rem -> {}
                    BoundBinaryOperatorType.BitAnd -> {}
                    BoundBinaryOperatorType.BitOr -> {}
                    BoundBinaryOperatorType.LogicAnd -> {}
                    BoundBinaryOperatorType.LogicOr -> {}
                    BoundBinaryOperatorType.LessThan -> return Comparison(ComparisonType.LessThan, left, right)
                    BoundBinaryOperatorType.MoreThan -> return Comparison(ComparisonType.MoreThan, left, right)
                    BoundBinaryOperatorType.IsEqual -> {
                        return Comparison(ComparisonType.IsEqual, left, right)
                    }
                    BoundBinaryOperatorType.IsEqualOrMore -> return Comparison(ComparisonType.IsEqualOrMore, left, right)
                    BoundBinaryOperatorType.IsEqualOrLess -> return Comparison(ComparisonType.IsEqualOrLess, left, right)
                    BoundBinaryOperatorType.IsNotEqual -> {
                        return Comparison(ComparisonType.IsNotEqual, left, right)
                    }
                    BoundBinaryOperatorType.IsIdentityEqual -> return Comparison(ComparisonType.IsEqual, left, right)
                    BoundBinaryOperatorType.IsNotIdentityEqual -> return Comparison(ComparisonType.IsNotEqual, left, right)
                }
            }
            BoundNodeType.ErrorExpression -> return null
            else -> throw Exception("internal error: Unknown expression to LLVM (${expression.boundType})")
        }
        return null
    }
}