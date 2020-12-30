package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxTree
import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token
import java.util.*

class TypeDeclarationNode(
    syntaxTree: SyntaxTree,
    val keyword: Token,
    val identifier: Token,
    val parent: TypeClauseNode?,
    val fields: ArrayList<ValVarDeclarationNode>?
) : TopLevelNode(syntaxTree) {

    override val kind = SyntaxType.TypeDeclaration
    override val children get() = arrayListOf<Node>(keyword, identifier).apply {
        parent?.let { add(it) }
        fields?.let { addAll(it) }
    }
}
