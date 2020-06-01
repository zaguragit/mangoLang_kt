package mango.interpreter.eval

import mango.compilation.Diagnostic

class EvaluationResult(
    val value: Any?,
    val errors: Collection<Diagnostic>,
    val nonErrors: Collection<Diagnostic>
)
