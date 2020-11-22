package mango.interpreter.binding

class UseStatement(
    val path: String,
    val isInclude: Boolean
) {
    override fun toString() = if (isInclude) "use $path*" else "use $path"
}