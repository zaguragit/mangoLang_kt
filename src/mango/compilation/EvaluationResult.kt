package mango.compilation

class EvaluationResult(
    val errors: Collection<Diagnostic>,
    val nonErrors: Collection<Diagnostic>
)
