package mango.interpreter.symbols

object BuiltinFunctions {

    val print = FunctionSymbol(
        "print",
        arrayOf(ParameterSymbol("text", TypeSymbol.string)),
        TypeSymbol.unit, "mango.io.print", null, FunctionSymbol.MetaData().apply { isExtern = true })

    val println = FunctionSymbol(
        "println",
        arrayOf(ParameterSymbol("text", TypeSymbol.string)),
        TypeSymbol.unit, "mango.io.println", null, FunctionSymbol.MetaData().apply { isExtern = true })

    val readln = FunctionSymbol(
        "readln",
        arrayOf(),
        TypeSymbol.string, "mango.io.readln", null, FunctionSymbol.MetaData().apply { isExtern = true })

    val typeOf = FunctionSymbol(
        "typeof",
        arrayOf(ParameterSymbol("object", TypeSymbol.any)),
        TypeSymbol.string,"mango.util.typeof", null, FunctionSymbol.MetaData().apply { isExtern = true })

    val random = FunctionSymbol(
        "random",
        arrayOf(ParameterSymbol("max", TypeSymbol.int)),
        TypeSymbol.int, "mango.util.random", null, FunctionSymbol.MetaData().apply { isExtern = true })

    fun getAll() = listOf(print, println, readln, typeOf, random)
}