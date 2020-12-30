package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxTree
import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token

class ValVarDeclarationNode(
    syntaxTree: SyntaxTree,
    val keyword: Token,
    val identifier: Token,
    val typeClauseNode: TypeClauseNode?,
    val equals: Token?,
    val initializer: Node?,
    val extensionType: TypeClauseNode?,
    val annotations: Collection<AnnotationNode>
) : TopLevelNode(syntaxTree) {

    override val kind = SyntaxType.ValVarDeclaration
    override val children: Collection<Node> get() = arrayListOf<Node>(keyword, identifier).apply {
        typeClauseNode?.let { add(it) }
        equals?.let { add(it) }
        initializer?.let { add(it) }
    }
}