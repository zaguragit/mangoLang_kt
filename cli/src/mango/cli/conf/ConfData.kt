package mango.cli.conf

class ConfData {

    val table = HashMap<String, String>()

    inline operator fun get(key: String) = table[key]
}