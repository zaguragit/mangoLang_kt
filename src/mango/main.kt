package mango

import mango.console.MangoRepl
import mango.compilation.Compilation
import mango.interpreter.syntax.parser.SyntaxTree
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        exitAndPrintHelp()
    }
    when (args[0]) {
        "repl" -> {
            val repl = MangoRepl()
            repl.run()
        }
        "compile" -> {
            if (args.size != 2) {
                exitAndPrintHelp()
            }
            val syntaxTree = SyntaxTree.load(args[1])
            val compilation = Compilation(null, syntaxTree)
            val errors = compilation.evaluate(HashMap()).errors

            if (errors.isNotEmpty()) {
                println()
                for (error in errors) {
                    error.print()
                    println()
                }
                ExitCodes.ERROR()
            }
        }
        else -> exitAndPrintHelp()
    }
}

private fun exitAndPrintHelp() {
    println("compile <file>  | Compile one file")
    //println("build           | Compile the project")
    //println("run             | Build and run project")
    println("repl            | Use the repl")
    ExitCodes.ERROR()
}

enum class ExitCodes {
    SUCCESS,
    ERROR;

    operator fun invoke(): Nothing = exitProcess(ordinal)
}