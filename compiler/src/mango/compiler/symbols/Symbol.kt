package mango.compiler.symbols

abstract class Symbol {

    abstract val name: String
    abstract val kind: Kind
    abstract val type: TypeSymbol

    var useCounter = 0
    open val meta = MetaData()

    enum class Kind {
        Variable,
        Field,
        VisibleVariable,
        Parameter,
        Function,
        Type,
        StructType,
        FunctionType
    }

    override fun toString() = name

    companion object {
        private var fnUIDCounter = 0
        fun genFnUID() = "0fn${fnUIDCounter++}"
    }

    class MetaData {

        //// FUNCTIONS /////////////////////////////////////////////////////////////////////////////////////////////////
        var isInline = false
        var isExtern = false
        var isEntry = false
        var cname: String? = null
        var isExtension = false
        var isOperator = false

        //// STRUCT FIELDS /////////////////////////////////////////////////////////////////////////////////////////////
        //var init = false


        //// ANY VISIBLE FIELD /////////////////////////////////////////////////////////////////////////////////////////
        var isInternal = false
        var isPrivate = false

        companion object {
            var entryExists = false
        }
    }
}