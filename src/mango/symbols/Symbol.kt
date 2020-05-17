package mango.symbols

abstract class Symbol {
    abstract val name: String
    abstract val kind: Kind

    enum class Kind {
        Variable,
        Type,
        Function,
        Parameter
    }

    override fun toString() = name
}