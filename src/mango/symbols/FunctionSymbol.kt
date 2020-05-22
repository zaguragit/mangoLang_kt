package mango.symbols

import mango.syntax.parser.FunctionDeclarationNode

class FunctionSymbol(
    override val name: String,
    val parameters: Array<ParameterSymbol>,
    val type: TypeSymbol,
    val declarationNode: FunctionDeclarationNode? = null
) : Symbol() {

    override val kind = Kind.Function
}