package mango.interpreter.binding

class Label(
    val name: String
) {

    override fun toString() = name
    override fun hashCode() = name.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Label
        return name == other.name
    }
}
