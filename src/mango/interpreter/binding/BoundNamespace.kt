package mango.interpreter.binding

class BoundNamespace(
    val path: String,
    parent: BoundScope
) : BoundScope(parent) {

    init {
        namespaces[path] = this
    }

    companion object {
        val namespaces = HashMap<String, BoundNamespace>()
        operator fun get(path: String) = namespaces[path]
        fun getOr(path: String, default: () -> BoundNamespace) = namespaces.getOrElse(path, default)
    }
}