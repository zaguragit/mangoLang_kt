package mango.console

import mango.interpreter.symbols.VariableSymbol
import mango.compilation.Compilation
import mango.interpreter.syntax.parser.SyntaxTree

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
        if (syntaxTree.root.members.lastOrNull()?.getLastToken()?.isMissing == true) {
            return false
        }
        return true
    }

    override fun evaluateSubmission(text: String) {

        val syntaxTree = SyntaxTree.parse(text)

        val compilation = Compilation(previous, syntaxTree)

        if (showParseTree) {
            syntaxTree.root.printTree()
        }

        if (showBindTree) {
            compilation.printTree()
        }

        val result = compilation.evaluate(variables)
        val errors = result.errors
        val nonErrors = result.nonErrors

        if (errors.isEmpty()) {
            for (nonError in nonErrors) {
                nonError.printAsSuggestion()
            }
            compilation.globalScope.diagnostics.clear()
            previous = compilation
            if (result.value != null) {
                print(Console.YELLOW_BOLD_BRIGHT)
                println(result.value)
                print(Console.RESET)
            }
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

    override fun evaluateMetaCommand(cmd: String) = when (cmd) {
        "#showTree" -> {
            if (showParseTree) {
                showParseTree = false
                println("Parse tree is now invisible")
            } else {
                showParseTree = true
                println("Parse tree is now visible")
            }
        }
        "#showProgram" -> {
            if (showBindTree) {
                showBindTree = false
                println("Bind tree is now invisible")
            } else {
                showBindTree = true
                println("Bind tree is now visible")
            }
        }
        "#reset" -> previous = null
        else -> super.evaluateMetaCommand(cmd)
    }
}