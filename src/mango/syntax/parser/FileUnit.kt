package mango.syntax.parser

import mango.syntax.SyntaxType
import mango.syntax.lex.Token

class FileUnit(val statementNode: StatementNode, val endOfFileToken: Token) : Node() {
    override val kind
        get() = SyntaxType.FileUnit
    override val children
        get() = listOf(statementNode)

}