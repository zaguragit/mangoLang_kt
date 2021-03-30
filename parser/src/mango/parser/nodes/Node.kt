package mango.parser.nodes

import mango.parser.SyntaxType
import mango.parser.TextFile
import mango.parser.Token
import shared.text.TextLocation
import shared.text.TextSpan

abstract class Node(val textFile: TextFile) {

    abstract val kind: SyntaxType
    abstract val children: Collection<Node>
    open val span: TextSpan get() = TextSpan.fromBounds(children.first().span.start, children.last().span.end)

    val location get() = TextLocation(textFile.sourceText, span)

    var isMissing = false

    fun getLastToken(): Token {
        if (this is Token) {
            return this
        }
        return children.last().getLastToken()
    }
/*
    fun printTree(indent: String = "", isLast: Boolean = true) {
        print(Console.GRAY)
        print(indent)
        print(if (isLast) "└──" else "├──")

        if (this is Token) {
            print(Console.CYAN_BRIGHT)
            print(kind.name)
            if (string != null) {
                print(" ")
                print(Console.GREEN_BOLD_BRIGHT)
                print(string)
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
    }*/
}