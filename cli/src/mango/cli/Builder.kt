package mango.cli

import mango.cli.conf.ConfParser
import mango.cli.conf.reportConfMissingMandatoryField
import mango.cli.console.Console
import mango.cli.emission.EmissionType
import mango.cli.emission.Emitter
import mango.compiler.Compilation
import mango.compiler.SyntaxTree
import java.io.File

var isProject = false; private set
var isSharedLib = false; private set
var isExecutable = true; private set
var useStd = true; private set
var DEBUG = false; private set

object Builder {

    fun build(args: Array<String>): String {
        val conf = File("project.conf")
        if (!conf.exists() || !conf.isFile) {
            print(Console.RED)
            print("This isn't a project directory (no project.conf file found)")
            println(Console.RESET)
            ExitCodes.ERROR()
        }
        val src = File("src")
        if (!src.exists() || !src.isDirectory) {
            print(Console.RED)
            print("This isn't a project directory (no src directory found)")
            println(Console.RESET)
            ExitCodes.ERROR()
        }

        val (confData, moduleName) = run {
            val (confData, errors) = ConfParser.parse(conf)

            val moduleName = confData["name"]
            if (moduleName.isNullOrBlank()) {
                errors.reportConfMissingMandatoryField("name")
            }

            if (errors.errorList.isNotEmpty()) {
                for (error in errors.errorList) {
                    printError(error)
                    println()
                }
                ExitCodes.ERROR()
            }

            confData to moduleName
        }

        when (confData["isLibrary"]) {
            "true" -> {
                isExecutable = false
                isSharedLib = true
            }
            "false" -> isSharedLib = false
        }
        when (confData["useStd"]) {
            "true" -> useStd = true
            "false" -> useStd = false
        }

        var emissionType: EmissionType = when (confData["emission"]) {
            "asm", "assembly" -> EmissionType.Assembly
            "ir", "llvm" -> EmissionType.IR
            "obj", "object" -> EmissionType.Object
            null, "bin", "binary" -> EmissionType.Binary
            else -> {
                println(Console.RED + "'emission' must be one of: asm/assembly, ir/llvm, obj/object, bin/binary" + Console.RESET)
                ExitCodes.ERROR()
            }
        }

        if (emissionType != EmissionType.Binary) {
            isExecutable = false
        }

        var i = 1
        while (i < args.size) {
            when (args[i]) {
                "-type" -> if (++i < args.size) {
                    when(args[i].toLowerCase()) {
                        "asm", "assembly" -> emissionType = EmissionType.Assembly
                        "ir", "llvm" -> emissionType = EmissionType.IR
                        "obj", "object" -> emissionType = EmissionType.Object
                        "bin", "binary" -> emissionType = EmissionType.Binary
                        else -> exitAndPrintBuildHelp()
                    }
                }
                "-debug" -> DEBUG = true
                else -> exitAndPrintBuildHelp()
            }
            i++
        }

        val outName = "out/" + System.getProperty("os.name").substringBefore(' ').toLowerCase() + '/' +
                (confData["outFileName"] ?: addOutputExtension(moduleName!!, emissionType))
        val target = System.getProperty("os.name").substringBefore(' ').toLowerCase()

        isProject = true

        val syntaxTrees = Loader.loadProject(moduleName!!)
        compile(moduleName, outName, target, emissionType, true, syntaxTrees)

        return outName
    }

    fun buildFile(args: Array<String>): String {
        val inFileName = args[1]
        if (args.size < 2) {
            exitAndPrintCompileHelp()
        }
        var outName: String? = null
        var target: String? = null
        var emissionType: EmissionType = EmissionType.Binary
        var doSuggestions = true
        var i = 2
        while (i < args.size) {
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
                "-nostd" -> useStd = false
                "-shared" -> {
                    isExecutable = false
                    isSharedLib = true
                }
                "-debug" -> DEBUG = true
                else -> exitAndPrintCompileHelp()
            }
            i++
        }
        if (outName == null) {
            outName = addOutputExtension(inFileName.substringBeforeLast('.'), emissionType)
        }
        if (target == null) {
            target = System.getProperty("os.name").substringBefore(' ').toLowerCase()
        }
        if (emissionType != EmissionType.Binary) {
            isExecutable = false
        }
        val moduleName = inFileName.substringAfterLast(File.separatorChar).substringBefore('.')
        val syntaxTree = Loader.load(inFileName)
        compile(moduleName, outName, target, emissionType, doSuggestions, listOf(syntaxTree))

        return outName
    }

    fun compile(
        moduleName: String,
        outName: String,
        target: String,
        emissionType: EmissionType,
        doSuggestions: Boolean,
        localTrees: Collection<SyntaxTree>
    ) {
        val syntaxTrees = ArrayList(localTrees).apply {
            if (useStd) {
                add(Loader.loadLib("/usr/local/lib/mangoLang/std", "std"))
            }
        }
        val (errors, nonErrors, program) = Compilation.evaluate(syntaxTrees, isSharedLib, isExecutable)
        if (DEBUG) {
            program.functions.forEach {
                val symbol = it.key
                symbol.printStructure()
                val body = program.functions[symbol]
                body?.run {
                    print(" -> ")
                    print(structureString(program.functionBodies))
                }
                println()
                println()
            }
        }
        if (errors.isEmpty()) {
            Emitter.emit(program, moduleName, outName, target, emissionType)
            if (doSuggestions) {
                for (nonError in nonErrors) {
                    printSuggestion(nonError)
                    println()
                }
            }
        } else {
            println(Console.RED + "Compilation failed: ${Console.YELLOW_BOLD_BRIGHT + errors.size} ${Console.RESET + Console.RED + if (errors.size == 1) "error" else "errors"}")
            println()
            for (error in errors) {
                printError(error)
                println()
            }
            if (doSuggestions) {
                for (nonError in nonErrors) {
                    printSuggestion(nonError)
                    println()
                }
            }
            ExitCodes.ERROR()
        }
    }

    private fun addOutputExtension(
        inFileName: String,
        emissionType: EmissionType
    ) = when (emissionType) {
        EmissionType.Binary -> if (isSharedLib) "$inFileName.so" else inFileName
        EmissionType.Object -> "$inFileName.o"
        EmissionType.Assembly -> "$inFileName.asm"
        EmissionType.IR -> "$inFileName.ll"
    }




    private fun exitAndPrintCompileHelp() {
        val p = Console.CYAN_BOLD_BRIGHT
        val d = Console.GRAY
        val r = Console.RESET
        println("usage: ${Console.GREEN_BOLD_BRIGHT}compile $d<${r}file$d>$r $d[${r}parameters$d]$r")
        println("$p-nosuggest     $d│$r Disable warnings and style suggestions")
        println("$p-out           $d│$r Path of the output file")
        println("  asm / assembly")
        println("  ir / llvm")
        println("  obj / object")
        println("  bin / binary")
        println("$p-target        $d│$r Path of the output file")
        println("$p-type          $d│$r Type of the output")
        println("$p-shared        $d│$r Compile to a shared library")
        ExitCodes.ERROR()
    }
}