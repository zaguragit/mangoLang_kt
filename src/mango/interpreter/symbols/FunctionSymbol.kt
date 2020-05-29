package mango.interpreter.symbols

import mango.interpreter.syntax.parser.FunctionDeclarationNode

class FunctionSymbol(
    override val name: String,
    val parameters: Array<ParameterSymbol>,
    val type: TypeSymbol,
    val declarationNode: FunctionDeclarationNode? = null
) : Symbol() {

    override val kind = Kind.Function
}