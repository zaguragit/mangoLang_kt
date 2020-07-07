package mango.compilation

open class CompilationResult(
    val errors: Collection<Diagnostic>,
    val nonErrors: Collection<Diagnostic>
)
