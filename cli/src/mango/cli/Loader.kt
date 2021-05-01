package mango.cli

import mango.cli.console.Console
import mango.parser.MangoParser
import mango.parser.SyntaxTree
import java.io.File

object Loader {

    private fun fileNameToPackage(fileName: String) = fileName
            .substringBeforeLast('.')
            .replace('/', '.')

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
        return MangoParser.parse(text, fileNameToPackage(fileName.substringAfterLast("/")))
    }

    fun loadProject(moduleName: String): Collection<SyntaxTree> {
        val files = loadDirectory(File("src/"))
        if (files == null) {
            print(Console.RED)
            println("Couldn't find any valid files in the src/ folder")
            ExitCodes.ERROR()
        }
        val trees = ArrayList<SyntaxTree>()
        for (file in files) {
            val text = file.readText()
            val fileName = file.path
            val syntaxTree = MangoParser.parse(text, moduleName + '.' + fileNameToPackage(fileName.substringAfter("src/")))
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

    fun loadLib(path: String, name: String): SyntaxTree {
        val headersFileName = "$path/headers.m"
        val headersFile = File(headersFileName)
        if (!headersFile.exists() || !headersFile.isFile) {
            print(Console.RED)
            println("$path isn't a valid library path")
            ExitCodes.ERROR()
        }
        val text = headersFile.readText()
        return MangoParser.parse(text, name)
    }
}