package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxTree
import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token
import java.util.*

class StructDeclarationNode(
    syntaxTree: SyntaxTree,
    val keyword: Token,
    val identifier: Token,
    val openBrace: Token,
    val fields: ArrayList<VariableDeclarationNode>,
    val closedBrace: Token
) : TopLevelNode(syntaxTree) {

    override val kind = SyntaxType.StructDeclaration
    override val children get() = arrayListOf<Node>(keyword, identifier, openBrace).apply {
        addAll(fields)
        add(closedBrace)
    }
}
