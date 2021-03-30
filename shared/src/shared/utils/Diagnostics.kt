package shared.utils

import shared.text.TextLocation

class Diagnostic(
    val location: TextLocation?,
    val message: String,
    val diagnosticType: Type = Type.Error
) {
    override fun toString(): String = message

    enum class Type {
        Error,
        Warning,
        Style
    }
}

class DiagnosticList {

    fun sortBySpan() {
        val comparator = Comparator { d0: Diagnostic, d1 ->
            if (d0.location == null || d1.location == null) {
                return@Comparator 0
            }
            var cmp = d0.location.span.start - d1.location.span.start
            if (cmp == 0) {
                cmp = d0.location.span.length - d1.location.span.length
            }
            cmp
        }
        errors.sortWith(comparator)
        nonErrors.sortWith(comparator)
    }

    private val errors = ArrayList<Diagnostic>()
    private val nonErrors = ArrayList<Diagnostic>()

    val errorList: List<Diagnostic> get() = errors
    val nonErrorList: List<Diagnostic> get() = nonErrors

    fun append(other: DiagnosticList) {
        errors.addAll(other.errors)
        nonErrors.addAll(other.nonErrors)
    }
    fun hasErrors() = errors.any()

    fun clear() {
        errors.clear()
        nonErrors.clear()
    }


    /// STYLE //////////////////////////////////////////////////////////////////////////////////////////////////////////

    private inline fun style(
        location: TextLocation,
        message: String
    ) = nonErrors.add(Diagnostic(location, message, Diagnostic.Type.Style))


    /// WARNINGS ///////////////////////////////////////////////////////////////////////////////////////////////////////

    private inline fun warn(
        location: TextLocation,
        message: String
    ) = nonErrors.add(Diagnostic(location, message, Diagnostic.Type.Warning))


    /// ERRORS /////////////////////////////////////////////////////////////////////////////////////////////////////////

    fun report(
        location: TextLocation?,
        message: String
    ) = errors.add(Diagnostic(location, message, Diagnostic.Type.Error))
}