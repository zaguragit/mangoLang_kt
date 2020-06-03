package mango

import mango.console.MangoRepl
import mango.compilation.Compilation
import mango.compilation.EmissionType
import mango.console.Console
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
                exitAndPrintCompileHelp()
            }
            val inFileName = args[1]
            var outName: String? = null
            var target: String? = null
            var emissionType: EmissionType = EmissionType.Binary
            var doSuggestions = true
            var i = 2
            while (i < args.size) {
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
                                else -> exitAndPrintCompileHelp()
                            }
                        }
                        "-nosuggest" -> doSuggestions = false
                        else -> exitAndPrintCompileHelp()
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
            val result = compilation.evaluate(HashMap())
            val errors = result.errors
            val nonErrors = result.nonErrors

            if (errors.isEmpty()) {
                compilation.emit(moduleName, arrayOf(), outName, target, emissionType)
                if (doSuggestions) {
                    for (nonError in nonErrors) {
                        nonError.printAsSuggestion()
                        println()
                    }
                }
            } else {
                println()
                for (error in errors) {
                    error.printAsError()
                    println()
                }
                if (doSuggestions) {
                    for (nonError in nonErrors) {
                        nonError.printAsSuggestion()
                        println()
                    }
                }
                ExitCodes.ERROR()
            }
        }
        else -> exitAndPrintHelp()
    }
}

private fun exitAndPrintHelp() {
    val p = Console.GREEN_BOLD_BRIGHT
    val d = Console.GRAY
    val r = Console.RESET
    println("${Console.BOLD}Usage of mango:${Console.RESET}")
    println("${p}compile $d<${r}file$d>$r  $d│$r Compile one file")
    //println("${p}build           $d│$r Compile the project")
    //println("${p}run             $d│$r Build and run project")
    println("${p}repl            $d│$r Use the repl")
    ExitCodes.ERROR()
}

private fun exitAndPrintCompileHelp() {
    val p = Console.CYAN_BOLD_BRIGHT
    val d = Console.GRAY
    val r = Console.RESET
    println("usage: ${Console.GREEN_BOLD_BRIGHT}compile $d<${r}file$d>$r $d[${r}parameters$d]$r")
    println("$p-out           $d│$r Path of the output file")
    println("$p-target        $d│$r Path of the output file")
    println("$p-type          $d│$r Type of the output")
    println("  asm / assembly")
    println("  ir / llvm")
    println("  obj / object")
    println("  bin / binary")
    println("$p-nosuggest     $d│$r Disable warnings and style suggestions")
    ExitCodes.ERROR()
}

enum class ExitCodes {
    SUCCESS,
    ERROR;

    operator fun invoke(): Nothing = exitProcess(ordinal)
}