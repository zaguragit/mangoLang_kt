package mango.interpreter.symbols

abstract class Symbol {
    abstract val name: String
    abstract val kind: Kind
    var useCounter = 0

    enum class Kind {
        VisibleVariable,
        Variable,
        Parameter,
        Struct,
        Function,
        Type
    }

    override fun toString() = name

    fun printStructure() = when (kind) {
        Kind.Variable, Kind.VisibleVariable -> printVariable()
        Kind.Parameter -> printParameter()
        Kind.Function -> printFunction()
        Kind.Type -> printType()
        Kind.Struct -> printStruct()
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
        this as FunctionSymbol
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
        type.printStructure()
    }

    private fun printType() {
        this as TypeSymbol
        print(name)
    }

    private fun printStruct() {
        this as TypeSymbol.StructTypeSymbol
        print("struct ")
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

    companion object {
        private var fnUIDCounter = 0
        fun genFnUID() = "0fn${fnUIDCounter++}"
    }

    /*class MetaData {

        //// FUNCTIONS /////////////////////////////////////////////////////////////////////////////////////////////////
        var isInline = false
        var isExtern = false
        var isEntry = false
        var cName: String? = null

        //// STRUCT FIELDS /////////////////////////////////////////////////////////////////////////////////////////////
        var init = false
    }*/
}