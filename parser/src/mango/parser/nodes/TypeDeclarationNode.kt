package mango.parser.nodes

import mango.parser.SyntaxType
import mango.parser.TextFile
import mango.parser.Token
import java.util.*

class TypeDeclarationNode(
        textFile: TextFile,
        val keyword: Token,
        val identifier: Token,
        val parent: TypeClauseNode?,
        val fields: ArrayList<ValVarDeclarationNode>?,
        val annotations: Collection<AnnotationNode>
) : TopLevelNode(textFile) {

    override val kind = SyntaxType.TypeDeclaration
    override val children get() = arrayListOf<Node>(keyword, identifier).apply {
        parent?.let { add(it) }
        fields?.let { addAll(it) }
    }
}
