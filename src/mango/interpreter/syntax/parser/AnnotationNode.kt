package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Token

class AnnotationNode(
    syntaxTree: SyntaxTree,
    val left: Token,
    val identifier: Token,
    val colon: Token?,
    val value: ExpressionNode?,
    val right: Token
) : Node(syntaxTree) {

    override val kind = SyntaxType.Annotation
    override val children: Collection<Node> get() = arrayListOf<Node>(left, identifier).apply {
        colon?.let { add(it) }
        value?.let { add(it) }
        add(right)
    }
}