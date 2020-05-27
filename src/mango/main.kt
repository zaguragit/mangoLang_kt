package mango

import mango.console.MangoRepl

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("build      | Compile the program")
        println("run        | Build and run program")
        println("repl       | Use the repl")
        return
    }
    if (args[0] == "repl") {
        val repl = MangoRepl()
        repl.run()
    }
    else {
        val appPath = System.getProperty("user.dir")
    }
}