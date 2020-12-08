package mango.interpreter.symbols

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

    fun printStructure() = when (kind) {
        Kind.Variable, Kind.VisibleVariable, Kind.Field -> printVariable()
        Kind.Parameter -> printParameter()
        Kind.Function -> printFunction()
        Kind.Type -> print(this)
        Kind.StructType -> printStruct()
        Kind.FunctionType -> printFnType()
    }

    private fun printVariable() {
        this as VariableSymbol
        print(if (isReadOnly) { if (constant == null) "val " else "const " } else "var ")
        print(name)
        print(' ')
        type.printStructure()
    }

    private fun printParameter() {
        this as VariableSymbol
        print(name)
        print(' ')
        type.printStructure()
    }

    private fun printFunction() {
        this as CallableSymbol
        print("fn ")
        print(name)
        if (parameters.isNotEmpty()) {
            print(" (")
            for (i in parameters.indices) {
                if (i != 0) {
                    print(',')
                    print(' ')
                }
                parameters[i].printStructure()
            }
            print(')')
        }
        print(' ')
        returnType.printStructure()
    }

    private fun printStruct() {
        this as TypeSymbol.StructTypeSymbol
        print("type ")
        print(name)
        println(" {")
        for (field in fields) {
            print('\t')
            print("val")
            print(field.name)
            print(' ')
            field.type.printStructure()
            println()
        }
        print('}')
    }

    private fun printFnType() {
        this as TypeSymbol.Fn
        print("(")

        print(")")
    }

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
        var isInternal: Boolean = false

        companion object {
            var entryExists = false
        }
    }
}