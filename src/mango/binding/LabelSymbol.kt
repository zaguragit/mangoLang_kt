package mango.binding

class LabelSymbol(
    val name: String
) {

    override fun toString() = name
    override fun hashCode() = name.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as LabelSymbol
        return name == other.name
    }
}
