package mango.symbols

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
}