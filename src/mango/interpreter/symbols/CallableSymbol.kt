package mango.interpreter.symbols

abstract class CallableSymbol : Symbol() {
    abstract val parameters: Array<VariableSymbol>
}