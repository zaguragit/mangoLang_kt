package mango.cli

import mango.cli.console.Console
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        exitAndPrintHelp()
    }
    when (args[0]) {
        "compile" -> Builder.buildFile(args)
        "build" -> Builder.build(args)
        "run" -> ProcessBuilder(Builder.build(args)).run {
            inheritIO()
            start().waitFor()
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
    println("${p}build           $d│$r Build the project")
    println("${p}run             $d│$r Build and run project")
    ExitCodes.ERROR()
}

fun exitAndPrintBuildHelp() {
    val p = Console.CYAN_BOLD_BRIGHT
    val d = Console.GRAY
    val r = Console.RESET
    println("usage: ${Console.GREEN_BOLD_BRIGHT}build/run $d<${r}file$d>$r $d[${r}parameters$d]$r")
    println("$p-type          $d│$r Type of the output")
    println("  asm / assembly")
    println("  ir / llvm")
    println("  obj / object")
    println("  bin / binary")
    ExitCodes.ERROR()
}

enum class ExitCodes {
    SUCCESS,
    ERROR;

    operator fun invoke(): Nothing = exitProcess(ordinal)
}