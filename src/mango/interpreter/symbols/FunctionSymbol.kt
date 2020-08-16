package mango.interpreter.symbols

import mango.interpreter.syntax.nodes.FunctionDeclarationNode

class FunctionSymbol(
    override val name: String,
    override val parameters: Array<VariableSymbol>,
    returnType: TypeSymbol,
    override val path: String,
    val declarationNode: FunctionDeclarationNode?,
    override val meta: MetaData
) : CallableSymbol(), VisibleSymbol {

    override val kind = Kind.Function
    override val type = TypeSymbol.Fn(returnType, parameters.map { it.type })
}