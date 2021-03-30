package mango.cli

import mango.cli.console.Highlighter
import mango.compiler.binding.nodes.BiOperator
import mango.compiler.binding.nodes.BoundNode
import mango.compiler.binding.nodes.UnOperator
import mango.compiler.binding.nodes.expressions.*
import mango.compiler.binding.nodes.statements.*
import mango.compiler.ir.instructions.ConditionalGotoStatement
import mango.compiler.ir.instructions.GotoStatement
import mango.compiler.ir.instructions.LabelStatement
import mango.compiler.symbols.CallableSymbol
import mango.compiler.symbols.TypeSymbol

private fun BoundNode.writeNestedExpression(builder: StringBuilder, indent: Int, functionBodies: HashMap<CallableSymbol, Statement?>?, parentPrecedence: Int, expression: Expression) {
    when (expression) {
        is UnaryExpression -> writeNestedExpression(builder, indent + 1, functionBodies, parentPrecedence, expression.operator.syntaxType.getUnaryOperatorPrecedence(), expression)
        is BinaryExpression -> writeNestedExpression(builder, indent + 1, functionBodies, parentPrecedence, expression.operator.syntaxType.getBinaryOperatorPrecedence(), expression)
        else -> builder.append(expression.structureString(functionBodies, indent, true))
    }
}

private fun BoundNode.writeNestedExpression(builder: StringBuilder, indent: Int, functionBodies: HashMap<CallableSymbol, Statement?>?, parentPrecedence: Int, currentPrecedence: Int, expression: Expression) {
    val needsParentheses = parentPrecedence >= currentPrecedence
    if (needsParentheses) { builder.append('(') }
    builder.append(expression.structureString(functionBodies, indent, true))
    if (needsParentheses) { builder.append(')') }
}

fun BoundNode.structureString(functionBodies: HashMap<CallableSymbol, Statement?>? = null, indent: Int = 0, sameLine: Boolean = false): String {
    val builder = StringBuilder()
    if (!sameLine && kind != BoundNode.Kind.LabelStatement) {
        for (t in 0 until indent) {
            builder.append("    ")
        }
    }
    when (kind) {
        BoundNode.Kind.UnaryExpression -> {
            this as UnaryExpression
            val precedence = operator.syntaxType.getUnaryOperatorPrecedence()
            builder.append(UnOperator.getString(operator.type))
            writeNestedExpression(builder, indent + 1, functionBodies, precedence, operand)
        }
        BoundNode.Kind.BinaryExpression -> {
            this as BinaryExpression
            val precedence = operator.syntaxType.getBinaryOperatorPrecedence()
            writeNestedExpression(builder, indent + 1, functionBodies, precedence, left)
            builder.append(' ')
            builder.append(BiOperator.getString(operator.type))
            builder.append(' ')
            writeNestedExpression(builder, indent + 1, functionBodies, precedence, right)
        }
        BoundNode.Kind.BlockExpression -> {
            this as BlockExpression
            if (isUnsafe) {
                builder.append("unsafe ")
            }
            builder.append('{')
            builder.append('\n')
            for (statement in statements) {
                builder.append(statement.structureString(functionBodies, indent + 1))
            }
            for (t in 0 until indent) {
                builder.append("    ")
            }
            builder.append('}')
        }
        BoundNode.Kind.LiteralExpression -> {
            this as LiteralExpression
            when (type) {
                TypeSymbol.String -> {
                    val value = value as String?
                    if (value == null)
                        builder.append("\"null\"")
                    else builder.append('"' + value
                            .replace("\\", "\\\\")
                            .replace("\"", "\\\"")
                            .replace("\t", "\\t")
                            .replace("\n", "\\n")
                            .replace("\r", "\\r") + '"')
                }
                TypeSymbol.Char -> {
                    builder.append('\'' + (value as Short).toChar().toString()
                            .replace("\\", "\\\\")
                            .replace("\"", "\\\"")
                            .replace("\t", "\\t")
                            .replace("\n", "\\n")
                            .replace("\r", "\\r") + '\'')
                }
                else -> {
                    builder.append(value.toString())
                }
            }
        }
        BoundNode.Kind.NameExpression -> {
            this as NameExpression
            builder.append(symbol.name)
        }
        BoundNode.Kind.CallExpression -> {
            this as CallExpression
            val args = if (isExtension) {
                val a = ArrayList(arguments)
                builder.append(a.removeAt(0).structureString(functionBodies, indent + 1, true)).append('.')
                a
            } else arguments
            builder.append(expression.structureString(functionBodies, indent + 1, true))
            builder.append('(')
            var isFirst = true
            for (arg in args) {
                if (isFirst) {
                    isFirst = false
                }
                else {
                    builder.append(", ")
                }
                builder.append(arg.structureString(functionBodies, indent + 1, true))
            }
            builder.append(')')
        }
        BoundNode.Kind.ErrorExpression -> {
            this as ErrorExpression
            builder.append("// ERROR")
        }
        BoundNode.Kind.CastExpression -> {
            this as CastExpression
            builder.append(expression.structureString(functionBodies, indent + 1, true))
            builder.append(" as ")
            builder.append(type.path)
        }
        BoundNode.Kind.NamespaceFieldAccess -> {
            this as NamespaceFieldAccess
            builder.append("// ERROR")
        }
        BoundNode.Kind.StructFieldAccess -> {
            this as StructFieldAccess
            builder.append(struct.structureString(functionBodies, indent + 1, true))
            builder.append('.')
            builder.append(field.name)
        }
        BoundNode.Kind.ReferenceExpression -> {
            this as Reference
            builder.append('&')
            builder.append(expression.structureString(functionBodies, indent + 1, true))
        }
        BoundNode.Kind.PointerAccessExpression -> {
            this as PointerAccess
            builder.append(expression.structureString(functionBodies, indent + 1, true))
            builder.append('[')
            builder.append(i.structureString(functionBodies, indent + 1, true))
            builder.append(']')
        }
        BoundNode.Kind.StructInitialization -> {
            this as StructInitialization
            builder.append(type.toString())
            builder.append(" {\n")
            builder.append(fields.entries.joinToString("\n") {
                "    ".repeat(indent) + "${it.key.name}: ${it.value.structureString(functionBodies, indent + 1, true)}"
            })
            builder.append("\n")
            for (t in 0 until indent - 1) {
                builder.append("    ")
            }
            builder.append("}")
        }
        BoundNode.Kind.PointerArrayInitialization -> {
            this as PointerArrayInitialization
            builder.append(type.toString())
            builder.append(" { ")
            if (expressions != null) {
                builder.append(expressions!!.joinToString(", ") {
                    it.structureString(functionBodies, indent + 1, true)
                })
            }
            if (length != null) {
                builder.append("length: ${length!!.structureString(functionBodies, indent + 1, true)}")
            }
            builder.append(" }")
        }
        BoundNode.Kind.IfExpression -> {
            this as IfExpression
            builder.append(condition.structureString(functionBodies, indent + 1, true))
            builder.append(" ? ")
            builder.append(thenExpression.structureString(functionBodies, indent, true))
            if (elseExpression != null) {
                builder.append("\t: ")
                builder.append(elseExpression!!.structureString(functionBodies, indent, true))
            }
        }
        BoundNode.Kind.Lambda -> {
            this as Lambda
            builder.append('(')
            builder.append(symbol.parameters.joinToString(", ") { "${it.name} ${it.type}" })
            builder.append(')')
            if (symbol.returnType != TypeSymbol.Void) {
                builder.append(' ')
                builder.append(symbol.returnType)
            }
            if (!symbol.meta.isExtern) {
                builder.append(" -> ")
                if (functionBodies != null) {
                    builder.append(functionBodies[symbol]!!.structureString(functionBodies, indent + 1))
                }
            }
        }


        BoundNode.Kind.ExpressionStatement -> {
            this as ExpressionStatement
            builder.append(expression.structureString(functionBodies, indent, true))
            builder.append('\n')
        }
        BoundNode.Kind.ValVarDeclaration -> {
            this as VariableDeclaration
            if (variable.meta.isExtern) {
                builder.append("[cname: \"${variable.meta.cname}\"]\n")
                for (t in 0 until indent) {
                    builder.append("    ")
                }
            }
            builder.append(if (variable.isReadOnly) "val " else "var ")
            builder.append(variable.name)
            builder.append(" = ")
            builder.append(initializer.structureString(functionBodies, indent + 1, true))
            builder.append('\n')
        }
        BoundNode.Kind.LoopStatement -> {
            this as LoopStatement
            builder.append("loop ")
            builder.append(body.structureString(functionBodies, indent, true))
        }
        BoundNode.Kind.ForLoopStatement -> {
            this as ForStatement
            builder.append("loop ")
            builder.append(variable.name)
            builder.append(" : ")
            builder.append(lowerBound.structureString(functionBodies, indent + 1, true))
            builder.append("..")
            builder.append(upperBound.structureString(functionBodies, indent + 1, true))
            builder.append(' ')
            builder.append(body.structureString(functionBodies, indent, true))
        }
        BoundNode.Kind.LabelStatement -> {
            this as LabelStatement
            builder.append(Highlighter.label(symbol.name))
            builder.append(':')
            builder.append('\n')
        }
        BoundNode.Kind.GotoStatement -> {
            this as GotoStatement
            builder.append("br ")
            builder.append(label)
            builder.append('\n')
        }
        BoundNode.Kind.ConditionalGotoStatement -> {
            this as ConditionalGotoStatement
            builder.append("br ")
            builder.append(label)
            builder.append(if (jumpIfTrue) " if " else " unless ")
            builder.append(condition.structureString(functionBodies, indent + 1, true))
            builder.append('\n')
        }
        BoundNode.Kind.ReturnStatement -> {
            this as ReturnStatement
            builder.append("return ")
            builder.append(expression?.structureString(functionBodies, indent + 1, true))
            builder.append('\n')
        }
        BoundNode.Kind.AssignmentStatement -> {
            this as Assignment
            builder.append(assignee.structureString(functionBodies, indent + 1, true))
            builder.append(" = ")
            builder.append(expression.structureString(functionBodies, indent + 1, true))
            builder.append('\n')
        }
        BoundNode.Kind.PointerAccessAssignment -> {
            this as PointerAccessAssignment
            builder.append(expression.structureString(functionBodies, indent + 1, true))
            builder.append('[')
            builder.append(i.structureString(functionBodies, indent + 1, true))
            builder.append(']')
            builder.append(" = ")
            builder.append(value.structureString(functionBodies, indent + 1, true))
            builder.append('\n')
        }
        BoundNode.Kind.NopStatement -> {
            builder.append("nop")
            builder.append('\n')
        }
    }
    return builder.toString()
}