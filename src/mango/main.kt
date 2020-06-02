package mango

import mango.console.MangoRepl
import mango.compilation.Compilation
import mango.compilation.EmissionType
import mango.interpreter.syntax.parser.SyntaxTree
import java.io.File
import kotlin.system.exitProcess

var isRepl = false; private set

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        exitAndPrintHelp()
    }
    when (args[0]) {
        "repl" -> {
            isRepl = true
            val repl = MangoRepl()
            repl.run()
        }
        "compile" -> {
            if (args.size < 2) {
                exitAndPrintHelp()
            }
            val inFileName = args[1]
            var outName: String? = null
            var target: String? = null
            var emissionType: EmissionType = EmissionType.Binary
            var i = 2
            while (i < args.lastIndex) {
                if (args[i].startsWith('-')) {
                    when (args[i]) {
                        "-out" -> {
                            if (++i < args.size) {
                                outName = args[i]
                            }
                        }
                        "-target" -> {
                            if (++i < args.size) {
                                target = args[i].toLowerCase()
                            }
                        }
                        "-type" -> if (++i < args.size) {
                            when(args[i].toLowerCase()) {
                                "asm", "assembly" -> emissionType = EmissionType.Assembly
                                "ir", "llvm" -> emissionType = EmissionType.IR
                                "obj", "object" -> emissionType = EmissionType.Object
                                "bin", "binary" -> emissionType = EmissionType.Binary
                            }
                        }
                    }
                }
                i++
            }
            if (outName == null) {
                outName = when (emissionType) {
                    EmissionType.Binary -> inFileName.substringBeforeLast('.')
                    EmissionType.Object -> inFileName.substringBeforeLast('.') + ".o"
                    EmissionType.Assembly -> inFileName.substringBeforeLast('.') + ".asm"
                    EmissionType.IR -> inFileName.substringBeforeLast('.') + ".ll"
                }
            }
            if (target == null) {
                target = System.getProperty("os.name").substringBefore(' ').toLowerCase()
            }
            val moduleName = inFileName.substringAfterLast(File.separatorChar).substringBefore('.')
            val syntaxTree = SyntaxTree.load(inFileName)
            val compilation = Compilation(null, syntaxTree)
            compilation.emit(moduleName, arrayOf(), outName, target, emissionType)
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

private fun exitAndPrintCompileHelp() {
    println("usage: compile <file> [parameters]")
    println("-out       | Path of the output file")
    println("-target    | Path of the output file")
    println("-type      | Type of the output")
    println("  asm / assembly")
    println("  ir / llvm")
    println("  obj / object")
    println("  bin / binary")
    ExitCodes.ERROR()
}

enum class ExitCodes {
    SUCCESS,
    ERROR;

    operator fun invoke(): Nothing = exitProcess(ordinal)
}