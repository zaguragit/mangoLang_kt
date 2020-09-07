package mango.interpreter.binding

import mango.interpreter.binding.nodes.BoundNode
import mango.interpreter.binding.nodes.UnOperator
import mango.interpreter.binding.nodes.expressions.Expression
import mango.interpreter.binding.nodes.expressions.LiteralExpression
import mango.interpreter.binding.nodes.expressions.UnaryExpression
import mango.interpreter.binding.nodes.statements.*
import mango.interpreter.symbols.TypeSymbol
import mango.interpreter.syntax.SyntaxType
import mango.util.BinderError

class ControlFlowGraph private constructor(
    val start: BasicBlock,
    val end: BasicBlock,
    val blocks: List<BasicBlock>,
    val branches: List<BasicBlockBranch>
) {

    class BasicBlock {

        private val isEnd: Boolean
        private val isStart: Boolean

        constructor() {
            this.isStart = false
            this.isEnd = false
        }
        constructor(isStart: Boolean) {
            this.isStart = isStart
            this.isEnd = !isStart
        }

        val statements = ArrayList<Statement>()
        val incoming = ArrayList<BasicBlockBranch>()
        val outgoing = ArrayList<BasicBlockBranch>()

        override fun toString() = when {
            isStart -> "<start>"
            isEnd -> "<end>"
            else -> "<" + hashCode() + '>'
        }
    }

    class BasicBlockBranch(
        val from: BasicBlock,
        val to: BasicBlock,
        val condition: Expression?
    ) {
        override fun toString() = "_" + hashCode() + '_'
    }

    class BasicBlockBuilder {

        private val blocks = ArrayList<BasicBlock>()
        private var statements = ArrayList<Statement>()

        fun build(block: BlockStatement): ArrayList<BasicBlock> {
            for (statement in block.statements) {
                when (statement.kind) {
                    BoundNode.Kind.LabelStatement -> {
                        startBlock()
                        statements.add(statement)
                    }
                    BoundNode.Kind.GotoStatement,
                    BoundNode.Kind.ConditionalGotoStatement,
                    BoundNode.Kind.ReturnStatement -> {
                        statements.add(statement)
                        startBlock()
                    }
                    BoundNode.Kind.ExpressionStatement,
                    BoundNode.Kind.AssignmentStatement,
                    BoundNode.Kind.VariableDeclaration -> {
                        statements.add(statement)
                    }
                    BoundNode.Kind.NopStatement -> {}
                    else -> throw BinderError("Unexpected statement ${statement.kind}")
                }
            }
            endBlock()
            return blocks
        }

        private fun startBlock() = endBlock()
        private fun endBlock() {
            if (statements.any()) {
                val block = BasicBlock()
                block.statements.addAll(statements)
                blocks.add(block)
                statements.clear()
            }
        }
    }

    class GraphBuilder {

        private val blockFromStatement = HashMap<Statement, BasicBlock>()
        private val blockFromLabel = HashMap<Label, BasicBlock>()
        private val branches = ArrayList<BasicBlockBranch>()
        private val start = BasicBlock(isStart = true)
        private val end = BasicBlock(isStart = false)

        fun build(blocks: ArrayList<BasicBlock>): ControlFlowGraph {

            if (blocks.any()) { connect(start, blocks.first()) }
            else { connect(start, end) }

            for (block in blocks) {
                for (statement in block.statements) {
                    blockFromStatement[statement] = block
                    if (statement is LabelStatement) {
                        blockFromLabel[statement.symbol] = block
                    }
                }
            }

            for (i in blocks.indices) {

                val block = blocks[i]
                val next = blocks.getOrElse(i + 1) { end }

                for (statement in block.statements) {
                    when (statement.kind) {
                        BoundNode.Kind.GotoStatement -> {
                            statement as GotoStatement
                            val toBlock = blockFromLabel[statement.label]!!
                            connect(block, toBlock)
                        }
                        BoundNode.Kind.ConditionalGotoStatement -> {
                            statement as ConditionalGotoStatement
                            val thenBlock = blockFromLabel[statement.label]!!
                            val negatedCondition = negate(statement.condition)
                            val thenCondition =
                                    if (statement.jumpIfTrue) statement.condition
                                    else negatedCondition
                            val elseCondition =
                                    if (statement.jumpIfTrue) negatedCondition
                                    else statement.condition
                            connect(block, thenBlock, thenCondition)
                            connect(block, next, elseCondition)
                        }
                        BoundNode.Kind.ReturnStatement -> {
                            connect(block, end)
                        }
                        BoundNode.Kind.LabelStatement,
                        BoundNode.Kind.ExpressionStatement,
                        BoundNode.Kind.AssignmentStatement,
                        BoundNode.Kind.VariableDeclaration -> {
                            if (statement == block.statements.last()) {
                                connect(block, next)
                            }
                        }
                        BoundNode.Kind.NopStatement -> {}
                        else -> throw BinderError("Unexpected statement ${statement.kind}")
                    }
                }
            }

            scanForUnreachables(blocks)

            blocks.add(0, start)
            blocks.add(end)
            return ControlFlowGraph(start, end, blocks, branches)
        }

        private tailrec fun scanForUnreachables(blocks: MutableList<BasicBlock>) {
            var changed = false
            val iter = blocks.iterator()
            for (block in iter) {
                if (!block.incoming.any()) {
                    removeBlock(iter, block)
                    changed = true
                }
            }
            if (changed) {
                scanForUnreachables(blocks)
            }
        }

        private fun removeBlock(iter: MutableIterator<BasicBlock>, block: BasicBlock) {
            for (branch in block.incoming) {
                branch.from.outgoing.remove(branch)
                branches.remove(branch)
            }
            for (branch in block.outgoing) {
                branch.to.incoming.remove(branch)
                branches.remove(branch)
            }
            iter.remove()
        }

        private fun negate(condition: Expression): Expression {
            if (condition is LiteralExpression) {
                val value = condition.value as Boolean
                return LiteralExpression(!value, TypeSymbol.Bool)
            }
            val unaryOperator = UnOperator.bind(SyntaxType.Bang, TypeSymbol.Bool)!!
            return UnaryExpression(unaryOperator, condition)
        }

        private fun connect(from: BasicBlock, to: BasicBlock, condition: Expression? = null) {
            var condition = condition
            if (condition is LiteralExpression) {
                val value = condition.value as Boolean
                if (value) { condition = null }
                else return
            }
            val branch = BasicBlockBranch(from, to, condition)
            from.outgoing.add(branch)
            to.incoming.add(branch)
            branches.add(branch)
        }
    }

    fun print() {
        println("ControlFlowGraph {")
        for (b in blocks) {
            println("    Block $b {")
            println("        incoming: ${b.incoming.joinToString(", ")}")
            println("        outgoing: ${b.outgoing.joinToString(", ")}")
            if (b.statements.any()) {
                print("        statements ")
                print(BlockStatement(b.statements).structureString(indent = 2, sameLine = true))
            }
            println("    }")
        }
        for (b in branches) {
            print("    Branch $b: ")
            print(b.from)
            print(" --${b.condition ?: ""}--> ")
            println(b.to)
        }
        println('}')
    }

    companion object {
        fun create(body: BlockStatement): ControlFlowGraph {
            val builder = BasicBlockBuilder()
            val blocks = builder.build(body)
            val graphBuilder = GraphBuilder()
            return graphBuilder.build(blocks)
        }

        fun allPathsReturn(body: BlockStatement): Boolean {
            val graph = create(body)

            for (branch in graph.end.incoming) {
                if (branch.from.statements.isEmpty() ||
                    branch.from.statements.last().kind != BoundNode.Kind.ReturnStatement) {
                    return false
                }
            }

            return true
        }
    }
}