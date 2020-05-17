package mango.syntax.parser

import mango.syntax.lex.Token

class SeparatedNodeList<T : Node>(
    val nodesAndSeparators: Collection<Node>
) : Iterable<T> {

    inline val nodeCount get() = (nodesAndSeparators.size + 1) / 2

    inline operator fun get(i: Int) = nodesAndSeparators.elementAt(i * 2) as T

    inline fun getSeparator(i: Int) = nodesAndSeparators.elementAt(i * 2 + 1) as Token

    override fun iterator() = object : Iterator<T> {
        var i = 0
        override fun hasNext() = i < nodeCount
        override fun next(): T = get(i++)
    }
}