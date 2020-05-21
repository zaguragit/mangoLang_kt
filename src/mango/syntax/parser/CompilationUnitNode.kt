package mango.syntax.parser

import mango.syntax.SyntaxType
import mango.syntax.lex.Token

class CompilationUnitNode(
    val members: Collection<MemberNode>,
    val endOfFileToken: Token
) : Node() {

    override val kind
        get() = SyntaxType.CompilationUnit
    override val children
        get() = members.toList()
}