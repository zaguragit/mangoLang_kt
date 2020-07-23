package mango.console

import mango.interpreter.symbols.VariableSymbol
import mango.compilation.Compilation
import mango.interpreter.symbols.FunctionSymbol
import mango.interpreter.syntax.SyntaxTree

class MangoRepl : Repl() {

    private var showParseTree = false
    private var showBindTree = false

    private var previous: Compilation? = null

    private val variables = HashMap<VariableSymbol, Any?>()

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

        val syntaxTree = SyntaxTree.parse(text)

        val compilation = Compilation(previous, listOf(syntaxTree))

        if (showParseTree) {
            syntaxTree.root.printTree()
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
            compilation.globalScope.diagnostics.clear()
            previous = compilation
            /*if (result is EvaluationResult && result.value != null) {
                print(Console.YELLOW_BOLD_BRIGHT)
                println(result.value)
                print(Console.RESET)
            }*/
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
                            val result = compilation.globalScope.symbols.find { it.name == args[i] }
                            if (result == null) {
                                compilation = compilation.previous
                                continue
                            }
                            if (result is FunctionSymbol) {
                                compilation.printTree(result)
                            } else {
                                result.printStructure()
                                println()
                            }
                            break
                        }
                    }
                }
            }
            "#reset" -> previous = null
            else -> super.evaluateMetaCommand(cmd)
        }
    }
}