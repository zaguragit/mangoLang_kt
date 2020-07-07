package mango.interpreter.syntax

import mango.ExitCodes
import mango.compilation.DiagnosticList
import mango.console.Console
import mango.interpreter.syntax.nodes.NamespaceNode
import mango.interpreter.text.SourceText
import java.io.File

class SyntaxTree private constructor(
    val sourceText: SourceText
) {

    val projectPath = sourceText.fileName.substringAfter("src/").substringBeforeLast('.').replace('/', '.')

    val root: NamespaceNode
    val diagnostics: DiagnosticList

    init {
        val parser = Parser(this)
        root = parser.parseCompilationUnit()
        diagnostics = parser.diagnostics
    }

    companion object {

        fun parse(text: String) = SyntaxTree(SourceText(text, "repl"))

        fun load(fileName: String): SyntaxTree {
            val file = File(fileName)
            if (!file.exists()) {
                print(Console.RED)
                println("Couldn't find file \"$fileName\"")
                ExitCodes.ERROR()
            }
            if (!file.isFile) {
                print(Console.RED)
                println("\"$fileName\" isn't a file")
                ExitCodes.ERROR()
            }
            val text = file.readText()
            val sourceText = SourceText(text, fileName)
            return SyntaxTree(sourceText)
        }

        fun loadProject(): Collection<SyntaxTree> {
            val files = loadDirectory(File("src/"))
            if (files == null) {
                print(Console.RED)
                println("Couldn't find any valid files in the src/ folder")
                ExitCodes.ERROR()
            }
            val trees = ArrayList<SyntaxTree>()
            for (file in files) {
                val text = file.readText()
                val sourceText = SourceText(text, file.path)
                val syntaxTree = SyntaxTree(sourceText)
                trees.add(syntaxTree)
            }
            return trees
        }

        private fun loadDirectory(file: File): Iterable<File>? {
            val validFiles = file.listFiles { f ->
                f.name.endsWith(".m") || f.isDirectory
            } ?: return null
            val list = validFiles.toMutableList()
            var i = 0
            while (i < list.size) {
                val validFile = list[i]
                if (validFile.isDirectory) {
                    list.removeAt(i)
                    val dir = loadDirectory(validFile)
                    if (dir != null) {
                        for (f in dir) {
                            list.add(i++, f)
                        }
                    }
                } else {
                    i++
                }
            }
            return list
        }
    }
}