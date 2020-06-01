package mango.compilation

import mango.interpreter.binding.BoundProgram

interface Emitter {

    fun emit(program: BoundProgram,
             moduleName: String,
             references: Array<String>,
             outputPath: String): String
}