package mango.compilation

import mango.interpreter.binding.Program

interface Emitter {

    fun emit(
            program: Program,
            moduleName: String
    ): String
}