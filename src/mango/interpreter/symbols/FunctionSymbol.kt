package mango.interpreter.symbols

import mango.interpreter.syntax.parser.FunctionDeclarationNode

class FunctionSymbol(
    override val name: String,
    val parameters: Array<ParameterSymbol>,
    val type: TypeSymbol,
    val declarationNode: FunctionDeclarationNode?,
    val meta: MetaData
) : Symbol() {

    override val kind = Kind.Function

    class MetaData {
        var isInline = false
        var isExtern = false
        var cName: String? = null
    }
}