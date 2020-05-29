package mango.interpreter.binding

import mango.console.Console
import mango.interpreter.symbols.TypeSymbol

abstract class BoundNode {
    abstract val boundType: BoundNodeType

    private fun printNestedExpression(indent: Int,parentPrecedence: Int, expression: BoundExpression) {
        when (expression) {
            is BoundUnaryExpression -> {
                printNestedExpression(indent + 1, parentPrecedence, expression.operator.syntaxType.getUnaryOperatorPrecedence(), expression)
            }
            is BoundBinaryExpression -> {
                printNestedExpression(indent + 1, parentPrecedence, expression.operator.syntaxType.getBinaryOperatorPrecedence(), expression)
            }
            else -> {
                expression.printStructure(indent, true)
            }
        }
    }

    private fun printNestedExpression(indent: Int, parentPrecedence: Int, currentPrecedence: Int, expression: BoundExpression) {
        val needsParentheses = parentPrecedence >= currentPrecedence
        if (needsParentheses) { print('(') }
        expression.printStructure(indent, true)
        if (needsParentheses) { print(')') }
    }

    fun printStructure(indent: Int = 0, sameLine: Boolean = false) {
        if (!sameLine && boundType != BoundNodeType.LabelStatement) {
            for (t in 0 until indent) {
                print("    ")
            }
        }
        when (boundType) {
            BoundNodeType.UnaryExpression -> {
                this as BoundUnaryExpression
                val precedence = operator.syntaxType.getUnaryOperatorPrecedence()
                print(BoundUnaryOperator.getString(operator.type))
                printNestedExpression(indent + 1, precedence, operand)
            }
            BoundNodeType.BinaryExpression -> {
                this as BoundBinaryExpression
                val precedence = operator.syntaxType.getBinaryOperatorPrecedence()
                printNestedExpression(indent + 1, precedence, left)
                print(' ')
                print(BoundBinaryOperator.getString(operator.type))
                print(' ')
                printNestedExpression(indent + 1, precedence, right)
            }
            BoundNodeType.LiteralExpression -> {
                this as BoundLiteralExpression
                if (type == TypeSymbol.string) {
                    val value = value as String
                    print('"' + value
                            .replace("\\", "\\\\")
                            .replace("\"", "\\\"")
                            .replace("\t", "\\t")
                            .replace("\n", "\\n")
                            .replace("\r", "\\r") + '"')
                } else {
                    print(value.toString())
                }
            }
            BoundNodeType.VariableExpression -> {
                this as BoundVariableExpression
                print(variable.name)
            }
            BoundNodeType.AssignmentExpression -> {
                this as BoundAssignmentExpression
                print(variable.name)
                print(" = ")
                expression.printStructure(indent + 1, true)
            }
            BoundNodeType.CallExpression -> {
                this as BoundCallExpression
                print(function.name)
                print('(')
                var isFirst = true
                for (arg in arguments) {
                    if (isFirst) {
                        isFirst = false
                    }
                    else {
                        print(", ")
                    }
                    arg.printStructure(indent + 1, true)
                }
                print(')')
            }
            BoundNodeType.ErrorExpression -> {
                this as BoundErrorExpression
                print(Console.RED_BOLD_BRIGHT)
                print("ERROR")
                println(Console.RESET)
            }
            BoundNodeType.CastExpression -> {
                this as BoundCastExpression
                print(type.name)
                print('(')
                expression.printStructure(indent + 1, true)
                print(')')
            }
            BoundNodeType.BlockStatement -> {
                this as BoundBlockStatement
                println('{')
                for (statement in statements) {
                    statement.printStructure(indent + 1)
                }
                for (t in 0 until indent) {
                    print("    ")
                }
                println('}')
            }
            BoundNodeType.ExpressionStatement -> {
                this as BoundExpressionStatement
                expression.printStructure(indent, true)
                println()
            }
            BoundNodeType.VariableDeclaration -> {
                this as BoundVariableDeclaration
                print(if (variable.isReadOnly) "val " else "var ")
                print(variable.name)
                print(" = ")
                initializer.printStructure(indent + 1, true)
                println()
            }
            BoundNodeType.IfStatement -> {
                this as BoundIfStatement
                println("IF STATEMENT GOT HERE WTF")
            }
            BoundNodeType.WhileStatement -> {
                this as BoundWhileStatement
                println("WHILE STATEMENT GOT HERE WTF")
            }
            BoundNodeType.ForStatement -> {
                this as BoundForStatement
                println("FOR STATEMENT GOT HERE WTF")
            }
            BoundNodeType.LabelStatement -> {
                this as BoundLabelStatement
                print(symbol.name)
                println(':')
            }
            BoundNodeType.GotoStatement -> {
                this as BoundGotoStatement
                print("jmp ")
                println(label)
            }
            BoundNodeType.ConditionalGotoStatement -> {
                this as BoundConditionalGotoStatement
                print("jmp ")
                print(label)
                print(if (jumpIfTrue) " if " else " unless ")
                condition.printStructure(indent + 1, true)
                println()
            }
            BoundNodeType.ReturnStatement -> {
                this as BoundReturnStatement
                print("return ")
                expression?.printStructure(indent + 1, true)
                println()
            }
        }
    }
}

abstract class BoundExpression : BoundNode() {
    abstract val type: TypeSymbol
}

abstract class BoundStatement : BoundNode()

enum class BoundNodeType {
    UnaryExpression,
    BinaryExpression,
    LiteralExpression,
    VariableExpression,
    AssignmentExpression,
    CallExpression,
    ErrorExpression,
    CastExpression,

    BlockStatement,
    ExpressionStatement,
    VariableDeclaration,
    IfStatement,
    WhileStatement,
    ForStatement,
    LabelStatement,
    GotoStatement,
    ConditionalGotoStatement,
    ReturnStatement
}