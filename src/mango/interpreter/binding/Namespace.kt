package mango.interpreter.binding

class Namespace(
    val path: String,
    parent: Scope
) : Scope(parent) {

    init {
        namespaces[path] = this
    }

    companion object {
        private val namespaces = HashMap<String, Namespace>()
        operator fun get(path: String) = namespaces[path]
        fun getOr(path: String, default: () -> Namespace) = namespaces.getOrElse(path, default)

        val all get() = namespaces.values
    }
}