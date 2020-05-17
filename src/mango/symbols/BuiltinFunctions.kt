package mango.symbols

object BuiltinFunctions {

    val print = FunctionSymbol(
            "print",
            arrayOf(ParameterSymbol("text", TypeSymbol.string)),
            TypeSymbol.unit)

    val println = FunctionSymbol(
            "println",
            arrayOf(ParameterSymbol("text", TypeSymbol.string)),
            TypeSymbol.unit)

    val readln = FunctionSymbol(
            "readln",
            arrayOf(),
            TypeSymbol.string)

    val typeOf = FunctionSymbol(
            "typeof",
            arrayOf(ParameterSymbol("object", TypeSymbol.any)),
            TypeSymbol.string)

    val random = FunctionSymbol(
            "random",
            arrayOf(ParameterSymbol("max", TypeSymbol.int)),
            TypeSymbol.int)

    fun getAll() = listOf(print, println, readln, typeOf, random)
}