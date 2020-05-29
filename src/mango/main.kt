package mango

import mango.console.MangoRepl
import mango.compilation.Compilation
import mango.interpreter.syntax.parser.SyntaxTree
import java.io.File

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        printHelp()
        return
    }
    when (args[0]) {
        "repl" -> {
            val repl = MangoRepl()
            repl.run()
        }
        "compile" -> {
            if (args.size != 2) {
                printHelp()
                return
            }
            val path = System.getProperty("user.dir") + File.separatorChar + args[1]
            val text = File(path).readText()
            val syntaxTree = SyntaxTree.parse(text)
            val compilation = Compilation(syntaxTree, null)
            val errors = compilation.evaluate(HashMap()).errors

            if (errors.isNotEmpty()) {
                println()
                for (error in errors) {
                    error.print(syntaxTree)
                    println()
                }
            }
        }
        else -> printHelp()
    }
}

private fun printHelp() {
    println("compile <file>  | Compile one file")
    //println("build           | Compile the project")
    //println("run             | Build and run project")
    println("repl            | Use the repl")
}