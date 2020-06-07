package mango.interpreter.symbols

import mango.interpreter.syntax.parser.FunctionDeclarationNode

class FunctionSymbol(
    override val name: String,
    val parameters: Array<ParameterSymbol>,
    val type: TypeSymbol,
    override val path: String,
    val declarationNode: FunctionDeclarationNode?,
    val meta: MetaData
) : Symbol(), VisibleSymbol {

    override val kind = Kind.Function

    class MetaData {
        var isInline = false
        var isExtern = false
        var isEntry = false
        var cName: String? = null

        companion object {
            var entryExists = false
        }
    }
}