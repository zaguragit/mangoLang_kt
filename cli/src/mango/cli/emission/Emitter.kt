package mango.cli.emission

import mango.cli.console.Console
import mango.cli.emission.headers.HeaderEmitter
import mango.cli.emission.llvm.LLVMEmitter
import mango.cli.isProject
import mango.cli.isSharedLib
import mango.cli.useStd
import mango.compiler.binding.Program
import java.io.File
import java.io.FileNotFoundException

interface Emitter {

    fun emit(
        program: Program,
        moduleName: String
    ): String

    companion object {

        fun emit(program: Program, moduleName: String, outputPath: String, target: String, emissionType: EmissionType) {

            val outFile = File(outputPath)
            outFile.parentFile.mkdirs()

            if (isSharedLib) {
                File(if (isProject) "out/headers.m" else outputPath.substringBeforeLast('/') + "/headers.m").run {
                    createNewFile()
                    writeText(HeaderEmitter.emit(program, moduleName))
                }
            }

            val code = LLVMEmitter.emit(program, moduleName)
            if (emissionType == EmissionType.IR) {
                try { outFile.writeText(code) }
                catch (e: FileNotFoundException) {
                    println(Console.RED + "Couldn't write to file $outputPath")
                }
                return
            }
            val llFile = File.createTempFile("mangoLang", ".ll").apply {
                deleteOnExit()
                writeText(code)
            }
            when (emissionType) {
                EmissionType.Assembly -> {
                    ProcessBuilder("llc", llFile.absolutePath, "-o=$outputPath", "-filetype=asm", "-relocation-model=pic").run {
                        inheritIO()
                        start().waitFor()
                    }
                }
                EmissionType.Object -> {
                    ProcessBuilder("llc", llFile.absolutePath, "-o=$outputPath", "-filetype=obj", "-relocation-model=pic").run {
                        inheritIO()
                        start().waitFor()
                    }
                }
                else -> {
                    val objFile = File.createTempFile("mangoLang", ".o").apply {
                        deleteOnExit()
                        ProcessBuilder("llc", llFile.absolutePath, "-o=$absolutePath", "-filetype=obj", "-relocation-model=pic").run {
                            inheritIO()
                            start().waitFor()
                        }
                    }
                    when {
                        isSharedLib -> ProcessBuilder("gcc", objFile.absolutePath, "-o", outputPath, "-shared")
                        useStd -> ProcessBuilder("gcc", objFile.absolutePath, "/usr/local/lib/mangoLang/std/$target/std.so", "-o", outputPath)
                        else -> ProcessBuilder("gcc", objFile.absolutePath, "-o", outputPath)
                    }.run {
                        inheritIO()
                        start().waitFor()
                    }
                }
            }
        }
    }
}