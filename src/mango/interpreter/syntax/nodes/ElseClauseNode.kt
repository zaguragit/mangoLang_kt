package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxTree
import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token

class ElseClauseNode(
    syntaxTree: SyntaxTree,
    val colon: Token,
    val expression: Node
) : Node(syntaxTree) {

    override val kind = SyntaxType.ElseClause

    override val children get() = listOf(colon, expression)
}