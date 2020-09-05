package mango.console

import mango.compilation.Compilation
import mango.compilation.EmissionType
import mango.interpreter.symbols.CallableSymbol
import mango.interpreter.syntax.SyntaxTree
import java.io.File

class MangoRepl : Repl() {

    private var showParseTree = false
    private var showBindTree = false

    private var previous: Compilation? = null

    override fun init() {
        val trees = SyntaxTree.loadLib("/usr/local/include/mangoLang/std/", "std")
        val compilation = Compilation(null, trees)
        compilation.evaluate()
        compilation.globalScope.diagnostics.clear()
        previous = compilation
    }

    override fun isCompleteSubmission(string: String): Boolean {
        if (string.isEmpty()) {
            return true
        }

        if (lastWasEmpty && thisIsEmpty) {
            return true
        }

        val syntaxTree = SyntaxTree.parse(string)
        if (syntaxTree.root.members.lastOrNull()?.getLastToken()?.isMissing != false) {
            return false
        }
        return true
    }

    override fun evaluateSubmission(text: String) {

        val tree = SyntaxTree.parse(text)

        val compilation = Compilation(previous, listOf(tree))

        if (showParseTree) {
            tree.root.printTree()
        }

        if (showBindTree) {
            compilation.printTree()
        }

        val result = compilation.evaluate()
        val errors = result.errors
        val nonErrors = result.nonErrors

        if (errors.isEmpty()) {
            for (nonError in nonErrors) {
                nonError.printAsSuggestion()
            }
            val file = File.createTempFile("mangoLang", ".ll").apply {
                deleteOnExit()
            }
            compilation.emit("_", file.path, "", EmissionType.Binary)
            ProcessBuilder(file.path).run {
                inheritIO()
                start().waitFor()
            }
            compilation.globalScope.diagnostics.clear()
            previous = compilation
        } else {
            println()
            for (error in errors) {
                error.printAsError()
                println()
            }
            for (nonError in nonErrors) {
                nonError.printAsSuggestion()
            }
        }
    }

    override fun evaluateMetaCommand(cmd: String) {
        val args = cmd.split(' ')
        when (args[0]) {
            "#show" -> {
                if (args.size == 1) {
                    println("usage: #show (program|tree)")
                } else {
                    when (args[1]) {
                        "tree" -> if (showParseTree) {
                            showParseTree = false
                            println("Parse tree is now invisible")
                        } else {
                            showParseTree = true
                            println("Parse tree is now visible")
                        }
                        "program" -> if (showBindTree) {
                            showBindTree = false
                            println("Bind tree is now invisible")
                        } else {
                            showBindTree = true
                            println("Bind tree is now visible")
                        }
                    }
                }
            }
            "#dump" -> {
                if (args.size == 1) {
                    println("usage: #dump [functionNames]")
                } else {
                    for (i in 1 until args.size) {
                        var compilation = previous
                        while (compilation != null) {
                            for (result in compilation.globalScope.symbols) {
                                if (result.name == args[i]) {
                                    if (result is CallableSymbol) {
                                        compilation.printTree(result)
                                    } else {
                                        result.printStructure()
                                        println()
                                    }
                                }
                            }
                            compilation = compilation.previous
                        }
                    }
                }
            }
            "#reset" -> previous = null
            else -> super.evaluateMetaCommand(cmd)
        }
    }
}