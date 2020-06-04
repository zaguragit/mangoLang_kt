package mango.interpreter.symbols

abstract class Symbol {
    abstract val name: String
    abstract val kind: Kind

    enum class Kind {
        GlobalVariable,
        LocalVariable,
        Type,
        Function,
        Parameter
    }

    override fun toString() = name

    fun printStructure() = when (kind) {
        Kind.GlobalVariable -> printGlobalVariable()
        Kind.LocalVariable -> printLocalVariable()
        Kind.Type -> printType()
        Kind.Function -> printFunction()
        Kind.Parameter -> printParameter()
    }

    private fun printGlobalVariable() {
        this as GlobalVariableSymbol
        print(if (isReadOnly) { if (constant == null) "val " else "const " } else "var ")
        print(name)
        print(' ')
        type.printStructure()
    }

    private fun printLocalVariable() {
        this as LocalVariableSymbol
        print(if (isReadOnly) { if (constant == null) "val " else "const " } else "var ")
        print(name)
        print(' ')
        type.printStructure()
    }

    private fun printType() {
        this as TypeSymbol
        print(name)
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

    private fun printParameter() {
        this as ParameterSymbol
        print(name)
        print(' ')
        type.printStructure()
    }
}