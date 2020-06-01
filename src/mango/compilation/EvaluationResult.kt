package mango.compilation

class EvaluationResult(
    val value: Any?,
    val errors: Collection<Diagnostic>,
    val nonErrors: Collection<Diagnostic>
)
