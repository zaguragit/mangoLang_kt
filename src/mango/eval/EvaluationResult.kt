package mango.eval

import mango.compilation.CompilationResult
import mango.compilation.Diagnostic

class EvaluationResult(
    val value: Any?,
    errors: Collection<Diagnostic>,
    nonErrors: Collection<Diagnostic>
) : CompilationResult(errors, nonErrors)
