package mango.compiler.binding

import mango.compiler.binding.nodes.BoundNode
import mango.compiler.binding.nodes.UnOperator
import mango.compiler.binding.nodes.expressions.BlockExpression
import mango.compiler.binding.nodes.expressions.Expression
import mango.compiler.binding.nodes.expressions.LiteralExpression
import mango.compiler.binding.nodes.expressions.UnaryExpression
import mango.compiler.binding.nodes.statements.Statement
import mango.compiler.ir.Label
import mango.compiler.ir.instructions.ConditionalGotoStatement
import mango.compiler.ir.instructions.GotoStatement
import mango.compiler.ir.instructions.LabelStatement
import mango.compiler.symbols.TypeSymbol
import mango.parser.SyntaxType
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

    private class BasicBlockBuilder {

        private val blocks = ArrayList<BasicBlock>()
        private var statements = ArrayList<Statement>()

        fun build(block: BlockExpression): ArrayList<BasicBlock> {
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
                    BoundNode.Kind.PointerAccessAssignment,
                    BoundNode.Kind.ValVarDeclaration -> {
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

    private class GraphBuilder {

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
                        BoundNode.Kind.PointerAccessAssignment,
                        BoundNode.Kind.ValVarDeclaration -> {
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

    companion object {
        fun create(body: BlockExpression): ControlFlowGraph {
            val builder = BasicBlockBuilder()
            val blocks = builder.build(body)
            val graphBuilder = GraphBuilder()
            return graphBuilder.build(blocks)
        }

        fun allPathsReturn(body: BlockExpression): Boolean {
            val graph = create(body)

            for (branch in graph.end.incoming) {
                if (branch.from.statements.isEmpty() ||
                    branch.from.statements.last().kind != BoundNode.Kind.ReturnStatement) {
                    //graph.print()
                    //println(body.structureString())
                    return false
                }
            }

            return true
        }
    }
}