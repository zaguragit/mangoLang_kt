package mango.interpreter.symbols

object BuiltinFunctions {

    val print = FunctionSymbol(
            "print",
            arrayOf(ParameterSymbol("text", TypeSymbol.string)),
            TypeSymbol.unit, null, FunctionSymbol.MetaData())

    val println = FunctionSymbol(
            "println",
            arrayOf(ParameterSymbol("text", TypeSymbol.string)),
            TypeSymbol.unit, null, FunctionSymbol.MetaData())

    val readln = FunctionSymbol(
            "readln",
            arrayOf(),
            TypeSymbol.string, null, FunctionSymbol.MetaData())

    val typeOf = FunctionSymbol(
            "typeof",
            arrayOf(ParameterSymbol("object", TypeSymbol.any)),
            TypeSymbol.string, null, FunctionSymbol.MetaData())

    val random = FunctionSymbol(
            "random",
            arrayOf(ParameterSymbol("max", TypeSymbol.int)),
            TypeSymbol.int, null, FunctionSymbol.MetaData())

    fun getAll() = listOf(print, println, readln, typeOf, random)
}