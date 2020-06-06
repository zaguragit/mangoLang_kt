package mango

import conf.ConfParser
import mango.console.MangoRepl
import mango.compilation.Compilation
import mango.compilation.EmissionType
import mango.console.Console
import mango.interpreter.syntax.parser.SyntaxTree
import java.io.File
import kotlin.system.exitProcess

var isRepl = false; private set
var isProject = false; private set

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
                outName = deduceOutputName(inFileName, emissionType)
            }
            if (target == null) {
                target = System.getProperty("os.name").substringBefore(' ').toLowerCase()
            }
            val moduleName = inFileName.substringAfterLast(File.separatorChar).substringBefore('.')
            val syntaxTree = SyntaxTree.load(inFileName)
            compile(moduleName, outName, target, emissionType, doSuggestions, listOf(syntaxTree))
        }
        "build" -> {
            if (args.size != 1) {
                exitAndPrintHelp()
            }
            val conf = File("project.conf")
            if (!conf.exists() || !conf.isFile) {
                println("This isn't a project directory (no project.conf file found)")
                ExitCodes.ERROR()
            }
            val src = File("src")
            if (!src.exists() || !src.isDirectory) {
                println("This isn't a project directory (no src directory found)")
                ExitCodes.ERROR()
            }

            val (confData, errors) = ConfParser.parse(conf)

            val moduleName = confData["name"]
            if (moduleName == null) {
                errors.reportConfMissingMandatoryField("name")
            }

            if (errors.errorList.isNotEmpty()) {
                for (error in errors.errorList) {
                    error.printAsError()
                    println()
                }
                ExitCodes.ERROR()
            }

            val outName = "out/" + (confData["outFileName"] ?: "binary")

            isProject = true
            val target = System.getProperty("os.name").substringBefore(' ').toLowerCase()

            val syntaxTrees = SyntaxTree.loadProject()
            compile(moduleName!!, outName, target, EmissionType.Binary, true, syntaxTrees)
        }
        else -> exitAndPrintHelp()
    }
}

private fun compile(
    moduleName: String,
    outName: String,
    target: String,
    emissionType: EmissionType,
    doSuggestions: Boolean,
    syntaxTrees: Collection<SyntaxTree>
) {
    val compilation = Compilation(null, syntaxTrees)
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

private fun deduceOutputName(
    inFileName: String,
    emissionType: EmissionType
) = when (emissionType) {
    EmissionType.Binary -> inFileName.substringBeforeLast('.')
    EmissionType.Object -> inFileName.substringBeforeLast('.') + ".o"
    EmissionType.Assembly -> inFileName.substringBeforeLast('.') + ".asm"
    EmissionType.IR -> inFileName.substringBeforeLast('.') + ".ll"
}

private fun exitAndPrintHelp() {
    val p = Console.GREEN_BOLD_BRIGHT
    val d = Console.GRAY
    val r = Console.RESET
    println("${Console.BOLD}Usage of mango:${Console.RESET}")
    println("${p}compile $d<${r}file$d>$r  $d│$r Compile one file")
    println("${p}build           $d│$r Compile the project")
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