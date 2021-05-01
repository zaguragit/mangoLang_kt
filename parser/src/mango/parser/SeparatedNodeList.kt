package mango.parser

import mango.parser.nodes.Node

class SeparatedNodeList<T : Node>(
    val nodesAndSeparators: Collection<Node>
) : Iterable<T> {

    inline val nodeCount get() = (nodesAndSeparators.size + 1) / 2

    inline operator fun get(i: Int): T = nodesAndSeparators.elementAt(i * 2) as T

    inline fun getSeparator(i: Int): Token = nodesAndSeparators.elementAt(i * 2 + 1) as Token

    override fun iterator() = object : Iterator<T> {
        var i = 0
        override fun hasNext() = i < nodeCount
        override fun next(): T = get(i++)
    }

    inline fun getOrNull(i: Int): T? = if (i * 2 > nodesAndSeparators.size) null else nodesAndSeparators.elementAt(i * 2) as T
}