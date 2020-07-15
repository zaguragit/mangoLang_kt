package mango.interpreter.symbols

import mango.interpreter.syntax.nodes.FunctionDeclarationNode

class FunctionSymbol(
    override val name: String,
    override val parameters: Array<VariableSymbol>,
    override val type: TypeSymbol,
    override val path: String,
    val declarationNode: FunctionDeclarationNode?,
    override val meta: MetaData
) : CallableSymbol(), VisibleSymbol {

    override val kind = Kind.Function
}