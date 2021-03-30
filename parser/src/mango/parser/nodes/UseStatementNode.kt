package mango.parser.nodes

import mango.parser.SyntaxType
import mango.parser.TextFile
import mango.parser.Token

class UseStatementNode(
        textFile: TextFile,
        val keyword: Token,
        val directories: Collection<Token>,
        val isInclude: Boolean
) : TopLevelNode(textFile) {

    override val kind = SyntaxType.UseStatement
    override val children: Collection<Node> get() = arrayListOf(keyword).apply { addAll(directories) }
}