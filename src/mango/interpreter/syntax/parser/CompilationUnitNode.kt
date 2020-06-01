package mango.interpreter.syntax.parser

import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Token

class CompilationUnitNode(
    syntaxTree: SyntaxTree,
    val members: Collection<TopLevelNode>,
    val endOfFileToken: Token
) : Node(syntaxTree) {

    override val kind = SyntaxType.CompilationUnit
    override val children = members
}