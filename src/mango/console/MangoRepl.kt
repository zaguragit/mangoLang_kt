package mango.console

import mango.symbols.VariableSymbol
import mango.compilation.Compilation
import mango.compilation.Diagnostic
import mango.syntax.parser.SyntaxTree
import kotlin.math.max
import kotlin.math.min

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

        val compilation = Compilation(syntaxTree, previous)

        if (showParseTree) {
            syntaxTree.root.printTree()
        }

        if (showBindTree) {
            compilation.printTree()
        }

        val result = compilation.evaluate(variables)
        val errors = result.errors

        if (errors.isEmpty()) {
            previous = compilation
        } else {
            println()

            for (error in errors) {
                val lineNumber = syntaxTree.sourceText.getLineI(error.span.start)
                val charNumber = error.span.start - syntaxTree.sourceText.lines[lineNumber].start
                val locationStr = Console.BLUE_BRIGHT + "$lineNumber, $charNumber"
                val prefix = when (error.diagnosticType) {
                    Diagnostic.Type.Error -> Console.RED + "error(" + locationStr + Console.RED + "): "
                    Diagnostic.Type.Warning -> Console.YELLOW_BRIGHT + "warning(" + locationStr + Console.YELLOW_BRIGHT + "): "
                    Diagnostic.Type.Style -> Console.CYAN + "style(" + locationStr + Console.CYAN + "): "
                }
                val textLine = syntaxTree.sourceText.lines[lineNumber]
                val spanStart = error.span.start
                val spanEnd = error.span.end
                print(prefix + error + Console.RESET + " {\n\t")
                print(text.substring(textLine.start, spanStart))
                print(when (error.diagnosticType) {
                    Diagnostic.Type.Error -> Console.RED_BOLD_BRIGHT
                    Diagnostic.Type.Warning -> Console.YELLOW_BOLD_BRIGHT
                    Diagnostic.Type.Style -> Console.CYAN_BOLD_BRIGHT
                })
                print(text.substring(spanStart, spanEnd))
                print(Console.RESET)
                print(text.substring(spanEnd, textLine.end))
                println()
                println('}')
                println()
            }
        }
    }

    override fun evaluateMetaCommand(cmd: String) = when (cmd) {
        "#showParseTree" -> {
            if (showParseTree) {
                showParseTree = false
                println("Parse tree is now invisible")
            } else {
                showParseTree = true
                println("Parse tree is now visible")
            }
        }
        "#showBindTree" -> {
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