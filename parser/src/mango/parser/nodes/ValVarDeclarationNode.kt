package mango.parser.nodes

import mango.parser.SyntaxType
import mango.parser.TextFile
import mango.parser.Token

class ValVarDeclarationNode(
        textFile: TextFile,
        val keyword: Token,
        val identifier: Token,
        val typeClauseNode: TypeClauseNode?,
        val equals: Token?,
        val initializer: Node?,
        val extensionParam: ParameterNode?,
        val annotations: Collection<AnnotationNode>
) : TopLevelNode(textFile) {

    override val kind = SyntaxType.ValVarDeclaration
    override val children: Collection<Node> get() = arrayListOf<Node>(keyword, identifier).apply {
        typeClauseNode?.let { add(it) }
        equals?.let { add(it) }
        initializer?.let { add(it) }
    }
}