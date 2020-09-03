package mango.interpreter.symbols

import mango.interpreter.syntax.nodes.FunctionDeclarationNode

class FunctionSymbol(
    name: String,
    parameters: Array<VariableSymbol>,
    returnType: TypeSymbol,
    path: String,
    declarationNode: FunctionDeclarationNode?,
    meta: MetaData
) : CallableSymbol(name, parameters, returnType, path, declarationNode, meta), VisibleSymbol {

    override val type = TypeSymbol.Fn(returnType, parameters.map { it.type })
}