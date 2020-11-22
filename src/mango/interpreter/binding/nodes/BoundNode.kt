package mango.interpreter.binding.nodes

import mango.interpreter.binding.nodes.expressions.*
import mango.interpreter.binding.nodes.statements.*
import mango.interpreter.symbols.TypeSymbol

abstract class BoundNode {

    abstract val kind: Kind

    private fun writeNestedExpression(builder: StringBuilder, indent: Int, parentPrecedence: Int, expression: Expression) {
        when (expression) {
            is UnaryExpression -> {
                writeNestedExpression(builder, indent + 1, parentPrecedence, expression.operator.syntaxType.getUnaryOperatorPrecedence(), expression)
            }
            is BinaryExpression -> {
                writeNestedExpression(builder, indent + 1, parentPrecedence, expression.operator.syntaxType.getBinaryOperatorPrecedence(), expression)
            }
            else -> {
                builder.append(expression.structureString(indent, true))
            }
        }
    }

    private fun writeNestedExpression(builder: StringBuilder, indent: Int, parentPrecedence: Int, currentPrecedence: Int, expression: Expression) {
        val needsParentheses = parentPrecedence >= currentPrecedence
        if (needsParentheses) { builder.append('(') }
        builder.append(expression.structureString(indent, true))
        if (needsParentheses) { builder.append(')') }
    }

    fun structureString(indent: Int = 0, sameLine: Boolean = false): String {
        val builder = StringBuilder()
        if (!sameLine && kind != Kind.LabelStatement) {
            for (t in 0 until indent) {
                builder.append("    ")
            }
        }
        when (kind) {
            Kind.UnaryExpression -> {
                this as UnaryExpression
                val precedence = operator.syntaxType.getUnaryOperatorPrecedence()
                builder.append(UnOperator.getString(operator.type))
                writeNestedExpression(builder, indent + 1, precedence, operand)
            }
            Kind.BinaryExpression -> {
                this as BinaryExpression
                val precedence = operator.syntaxType.getBinaryOperatorPrecedence()
                writeNestedExpression(builder, indent + 1, precedence, left)
                builder.append(' ')
                builder.append(BiOperator.getString(operator.type))
                builder.append(' ')
                writeNestedExpression(builder, indent + 1, precedence, right)
            }
            Kind.BlockExpression -> {
                this as BlockExpression
                if (isUnsafe) {
                    builder.append("unsafe ")
                }
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
            Kind.LiteralExpression -> {
                this as LiteralExpression
                if (type == TypeSymbol["String"]!!/*TypeSymbol.String*/) {
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
            Kind.NameExpression -> {
                this as NameExpression
                builder.append(symbol.name)
            }
            Kind.CallExpression -> {
                this as CallExpression
                val args = if (isExtension) {
                    val a = ArrayList(arguments)
                    builder.append(a.removeAt(0).structureString(indent + 1, true)).append('.')
                    a
                } else arguments
                builder.append(expression.structureString(indent + 1, true))
                builder.append('(')
                var isFirst = true
                for (arg in args) {
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
            Kind.ErrorExpression -> {
                this as ErrorExpression
                builder.append("// ERROR")
            }
            Kind.CastExpression -> {
                this as CastExpression
                builder.append(expression.structureString(indent + 1, true))
                builder.append(" as ")
                builder.append(type.name)
            }
            Kind.NamespaceFieldAccess -> {
                this as NamespaceFieldAccess
                builder.append("// ERROR")
            }
            Kind.StructFieldAccess -> {
                this as StructFieldAccess
                builder.append(struct.structureString(indent + 1, true))
                builder.append('.')
                builder.append(field.name)
            }
            Kind.ReferenceExpression -> {
                this as Reference
                builder.append('&')
                builder.append(expression.structureString(indent + 1, true))
            }
            Kind.PointerAccessExpression -> {
                this as PointerAccess
                builder.append(expression.structureString(indent + 1, true))
                builder.append('[')
                builder.append(i.structureString(indent + 1, true))
                builder.append(']')
            }
            Kind.StructInitialization -> {
                this as StructInitialization
                builder.append(type.toString())
                builder.append(" {\n")
                builder.append(fields.entries.joinToString("\n") {
                    "${it.key.name}: ${it.value.structureString(indent + 1, true)}"
                })
                builder.append("\n")
                for (t in 0 until indent) {
                    builder.append("    ")
                }
                builder.append("}")
            }
            Kind.PointerArrayInitialization -> {
                this as PointerArrayInitialization
                builder.append(type.toString())
                builder.append(" { ")
                if (expressions != null) {
                    builder.append(expressions.joinToString(", ") {
                        it.structureString(indent + 1, true)
                    })
                }
                if (length != null) {
                    builder.append("length: ${length.structureString(indent + 1, true)}")
                }
                builder.append(" }")
            }


            Kind.ExpressionStatement -> {
                this as ExpressionStatement
                builder.append(expression.structureString(indent, true))
                builder.append('\n')
            }
            Kind.VariableDeclaration -> {
                this as VariableDeclaration
                builder.append(if (variable.isReadOnly) "val " else "var ")
                builder.append(variable.name)
                builder.append(" = ")
                builder.append(initializer.structureString(indent + 1, true))
                builder.append('\n')
            }
            Kind.IfStatement -> {
                this as IfStatement
                builder.append("if ")
                builder.append(condition.structureString(indent + 1, true))
                builder.append(' ')
                builder.append(statement.structureString(indent, true))
                if (elseStatement != null) {
                    builder.append("\t: ")
                    builder.append(elseStatement.structureString(indent, true))
                }
            }
            Kind.WhileStatement -> {
                this as LoopStatement
                builder.append("loop ")
                builder.append(body.structureString(indent, true))
            }
            Kind.ForStatement -> {
                this as ForStatement
                builder.append("loop ")
                builder.append(variable.name)
                builder.append(" : ")
                builder.append(lowerBound.structureString(indent + 1, true))
                builder.append("..")
                builder.append(upperBound.structureString(indent + 1, true))
                builder.append(' ')
                builder.append(body.structureString(indent, true))
            }
            Kind.LabelStatement -> {
                this as LabelStatement
                builder.append(symbol.name)
                builder.append(':')
                builder.append('\n')
            }
            Kind.GotoStatement -> {
                this as GotoStatement
                builder.append("br ")
                builder.append(label)
                builder.append('\n')
            }
            Kind.ConditionalGotoStatement -> {
                this as ConditionalGotoStatement
                builder.append("br ")
                builder.append(label)
                builder.append(if (jumpIfTrue) " if " else " unless ")
                builder.append(condition.structureString(indent + 1, true))
                builder.append('\n')
            }
            Kind.ReturnStatement -> {
                this as ReturnStatement
                builder.append("return ")
                builder.append(expression?.structureString(indent + 1, true))
                builder.append('\n')
            }
            Kind.AssignmentStatement -> {
                this as Assignment
                builder.append(assignee.structureString(indent + 1, true))
                builder.append(" = ")
                builder.append(expression.structureString(indent + 1, true))
                builder.append('\n')
            }
            Kind.PointerAccessAssignment -> {
                this as PointerAccessAssignment
                builder.append(expression.structureString(indent + 1, true))
                builder.append('[')
                builder.append(i.structureString(indent + 1, true))
                builder.append(']')
                builder.append(" = ")
                builder.append(value.structureString(indent + 1, true))
                builder.append('\n')
            }
            Kind.NopStatement -> {
                builder.append("nop")
                builder.append('\n')
            }
        }
        return builder.toString()
    }

    enum class Kind {
        UnaryExpression,
        BinaryExpression,
        LiteralExpression,
        NameExpression,
        CallExpression,
        ErrorExpression,
        CastExpression,
        NamespaceFieldAccess,
        StructFieldAccess,
        BlockExpression,
        ReferenceExpression,
        PointerAccessExpression,
        StructInitialization,
        PointerArrayInitialization,

        ExpressionStatement,
        VariableDeclaration,
        IfStatement,
        WhileStatement,
        ForStatement,
        LabelStatement,
        GotoStatement,
        ConditionalGotoStatement,
        ReturnStatement,
        AssignmentStatement,
        PointerAccessAssignment,
        NopStatement
    }
}