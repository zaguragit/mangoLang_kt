package mango.syntax.parser

import mango.console.Console
import mango.compilation.TextSpan
import mango.syntax.SyntaxType
import mango.syntax.lex.Token

abstract class Node {

    abstract val kind: SyntaxType
    abstract val children: Collection<Node>
    open val span: TextSpan
        get() = TextSpan(children.first().span.start, children.last().span.end)

    var isMissing = false

    fun getLastToken(): Token {
        if (this is Token) {
            return this
        }
        return children.last().getLastToken()
    }

    fun printTree(indent: String = "", isLast: Boolean = true) {
        print(Console.CYAN)
        print(indent)
        print(if (isLast) "└──" else "├──")

        if (this is Token) {
            print(Console.PURPLE)
            print(kind.name)
            if (value != null) {
                print(" ")
                print(Console.GREEN_BOLD_BRIGHT)
                print(value)
            }
        } else {
            print(Console.RESET)
            print(kind.name)
        }

        println()

        val newIndent = indent + if (isLast) "    " else "│   "

        val lastChild = children.lastOrNull()

        for (child in children) {
            child.printTree(newIndent, child === lastChild)
        }

        print(Console.RESET)
    }
}