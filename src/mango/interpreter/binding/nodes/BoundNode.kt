package mango.interpreter.binding.nodes

import mango.interpreter.binding.nodes.expressions.*
import mango.interpreter.binding.nodes.statements.*
import mango.interpreter.symbols.TypeSymbol

abstract class BoundNode {

    abstract val kind: BoundNodeType

    private fun writeNestedExpression(builder: StringBuilder, indent: Int, parentPrecedence: Int, expression: BoundExpression) {
        when (expression) {
            is BoundUnaryExpression -> {
                writeNestedExpression(builder, indent + 1, parentPrecedence, expression.operator.syntaxType.getUnaryOperatorPrecedence(), expression)
            }
            is BoundBinaryExpression -> {
                writeNestedExpression(builder, indent + 1, parentPrecedence, expression.operator.syntaxType.getBinaryOperatorPrecedence(), expression)
            }
            else -> {
                builder.append(expression.structureString(indent, true))
            }
        }
    }

    private fun writeNestedExpression(builder: StringBuilder, indent: Int, parentPrecedence: Int, currentPrecedence: Int, expression: BoundExpression) {
        val needsParentheses = parentPrecedence >= currentPrecedence
        if (needsParentheses) { builder.append('(') }
        builder.append(expression.structureString(indent, true))
        if (needsParentheses) { builder.append(')') }
    }

    fun structureString(indent: Int = 0, sameLine: Boolean = false): String {
        val builder = StringBuilder()
        if (!sameLine && kind != BoundNodeType.LabelStatement) {
            for (t in 0 until indent) {
                builder.append("    ")
            }
        }
        when (kind) {
            BoundNodeType.UnaryExpression -> {
                this as BoundUnaryExpression
                val precedence = operator.syntaxType.getUnaryOperatorPrecedence()
                builder.append(BoundUnOperator.getString(operator.type))
                writeNestedExpression(builder, indent + 1, precedence, operand)
            }
            BoundNodeType.BinaryExpression -> {
                this as BoundBinaryExpression
                val precedence = operator.syntaxType.getBinaryOperatorPrecedence()
                writeNestedExpression(builder, indent + 1, precedence, left)
                builder.append(' ')
                builder.append(BoundBiOperator.getString(operator.type))
                builder.append(' ')
                writeNestedExpression(builder, indent + 1, precedence, right)
            }
            BoundNodeType.BlockExpression -> {
                this as BoundBlockExpression
                builder.append('{')
                builder.append('\n')
                for (statement in statements) {
                    builder.append(statement.structureString(indent + 1))
                }
                for (t in 0 until indent) {
                    builder.append("    ")
                }
                builder.append('}')
            }
            BoundNodeType.LiteralExpression -> {
                this as BoundLiteralExpression
                if (type == TypeSymbol.String) {
                    val value = value as String
                    builder.append('"' + value
                            .replace("\\", "\\\\")
                            .replace("\"", "\\\"")
                            .replace("\t", "\\t")
                            .replace("\n", "\\n")
                            .replace("\r", "\\r") + '"')
                } else {
                    builder.append(value.toString())
                }
            }
            BoundNodeType.VariableExpression -> {
                this as BoundVariableExpression
                builder.append(symbol.name)
            }
            BoundNodeType.AssignmentExpression -> {
                this as BoundAssignmentExpression
                builder.append(variable.name)
                builder.append(" = ")
                builder.append(expression.structureString(indent + 1, true))
            }
            BoundNodeType.CallExpression -> {
                this as BoundCallExpression
                builder.append(symbol.name)
                builder.append('(')
                var isFirst = true
                for (arg in arguments) {
                    if (isFirst) {
                        isFirst = false
                    }
                    else {
                        builder.append(", ")
                    }
                    builder.append(arg.structureString(indent + 1, true))
                }
                builder.append(')')
            }
            BoundNodeType.ErrorExpression -> {
                this as BoundErrorExpression
                builder.append("// ERROR")
            }
            BoundNodeType.CastExpression -> {
                this as BoundCastExpression
                builder.append(type.name)
                builder.append('(')
                builder.append(expression.structureString(indent + 1, true))
                builder.append(')')
            }
            BoundNodeType.NamespaceFieldAccess -> {
                this as BoundNamespaceFieldAccess
                builder.append("// ERROR")
            }
            BoundNodeType.StructFieldAccess -> {
                this as BoundStructFieldAccess
                builder.append(struct.structureString(indent + 1, true))
                builder.append('.')
                builder.append(field.name)
            }
            BoundNodeType.ReferenceExpression -> {
                this as BoundReference
                builder.append('&')
                builder.append(expression.structureString(indent + 1, true))
            }
            BoundNodeType.PointerAccessExpression -> {
                this as BoundPointerAccess
                builder.append(expression.structureString(indent + 1, true))
                builder.append('[')
                builder.append(i.structureString(indent + 1, true))
                builder.append(']')
            }


            BoundNodeType.BlockStatement -> {
                this as BoundBlockStatement
                builder.append('{')
                builder.append('\n')
                for (statement in statements) {
                    builder.append(statement.structureString(indent + 1))
                }
                for (t in 0 until indent) {
                    builder.append("    ")
                }
                builder.append('}')
                builder.append('\n')
            }
            BoundNodeType.ExpressionStatement -> {
                this as BoundExpressionStatement
                builder.append(expression.structureString(indent, true))
                builder.append('\n')
            }
            BoundNodeType.VariableDeclaration -> {
                this as BoundVariableDeclaration
                builder.append(if (variable.isReadOnly) "val " else "var ")
                builder.append(variable.name)
                builder.append(" = ")
                builder.append(initializer.structureString(indent + 1, true))
                builder.append('\n')
            }
            BoundNodeType.IfStatement -> {
                this as BoundIfStatement
                builder.append("if ")
                builder.append(condition.structureString(indent + 1, true))
                builder.append(' ')
                builder.append(statement.structureString(indent, true))
                if (elseStatement != null) {
                    builder.append("\telse ")
                    builder.append(elseStatement.structureString(indent, true))
                }
            }
            BoundNodeType.WhileStatement -> {
                this as BoundWhileStatement
                builder.append("while ")
                builder.append(condition.structureString(indent + 1, true))
                builder.append(' ')
                builder.append(body.structureString(indent, true))
            }
            BoundNodeType.ForStatement -> {
                this as BoundForStatement
                builder.append("for ")
                builder.append(variable.name)
                builder.append(" in ")
                builder.append(lowerBound.structureString(indent + 1, true))
                builder.append("..")
                builder.append(upperBound.structureString(indent + 1, true))
                builder.append(' ')
                builder.append(body.structureString(indent, true))
            }
            BoundNodeType.LabelStatement -> {
                this as BoundLabelStatement
                builder.append(symbol.name)
                builder.append(':')
                builder.append('\n')
            }
            BoundNodeType.GotoStatement -> {
                this as BoundGotoStatement
                builder.append("br ")
                builder.append(label)
                builder.append('\n')
            }
            BoundNodeType.ConditionalGotoStatement -> {
                this as BoundConditionalGotoStatement
                builder.append("br ")
                builder.append(label)
                builder.append(if (jumpIfTrue) " if " else " unless ")
                builder.append(condition.structureString(indent + 1, true))
                builder.append('\n')
            }
            BoundNodeType.ReturnStatement -> {
                this as BoundReturnStatement
                builder.append("return ")
                builder.append(expression?.structureString(indent + 1, true))
                builder.append('\n')
            }
            BoundNodeType.NopStatement -> {
                builder.append("nop")
                builder.append('\n')
            }
        }
        return builder.toString()
    }
}

enum class BoundNodeType {
    UnaryExpression,
    BinaryExpression,
    LiteralExpression,
    VariableExpression,
    AssignmentExpression,
    CallExpression,
    ErrorExpression,
    CastExpression,
    NamespaceFieldAccess,
    StructFieldAccess,
    BlockExpression,
    ReferenceExpression,
    PointerAccessExpression,

    BlockStatement,
    ExpressionStatement,
    VariableDeclaration,
    IfStatement,
    WhileStatement,
    ForStatement,
    LabelStatement,
    GotoStatement,
    ConditionalGotoStatement,
    ReturnStatement,
    NopStatement
}