package mango.interpreter.syntax.parser

import mango.console.Console
import mango.interpreter.text.TextSpan
import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Token
import mango.interpreter.text.TextLocation

abstract class Node(val syntaxTree: SyntaxTree) {

    abstract val kind: SyntaxType
    abstract val children: Collection<Node>
    open val span: TextSpan get() = TextSpan.fromBounds(children.first().span.start, children.last().span.end)

    val location get() = TextLocation(syntaxTree.sourceText, span)

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