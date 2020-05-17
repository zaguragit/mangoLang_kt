package mango.binding

import mango.console.Console
import mango.symbols.TypeSymbol

abstract class BoundNode {
    abstract val boundType: BoundNodeType
    abstract val children: Collection<BoundNode>

    fun printTree(indent: String = "", isLast: Boolean = true) {
        print(Console.CYAN)
        print(indent)
        print(if (isLast) "└──" else "├──")

        print(Console.RESET)
        print(boundType.name)
        print(' ')
        print(Console.BLUE)
        println(getDataString())

        val newIndent = indent + if (isLast) "    " else "│   "

        val lastChild = children.lastOrNull()

        for (child in children) {
            child.printTree(newIndent, child === lastChild)
        }

        print(Console.RESET)
    }

    protected open fun getDataString() = ""
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

    BlockStatement,
    ExpressionStatement,
    VariableDeclaration,
    IfStatement,
    WhileStatement,
    ForStatement,
    LabelStatement,
    GotoStatement,
    ConditionalGotoStatement
}