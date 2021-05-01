package mango.compiler.binding

import mango.compiler.binding.nodes.BoundNode
import mango.compiler.binding.nodes.expressions.*
import mango.compiler.binding.nodes.statements.*
import mango.compiler.ir.Label
import mango.compiler.symbols.CallableSymbol
import mango.compiler.symbols.Symbol
import mango.compiler.symbols.TypeSymbol
import mango.compiler.symbols.VariableSymbol
import java.util.*

class Inliner(val functionBodies: HashMap<CallableSymbol, Statement?>) : TreeRewriter {

    val returnStack = LinkedList<Label>()
    var paramsStack = LinkedList<List<VariableSymbol>>()

    override fun rewriteCallExpression(node: CallExpression): Expression {
        val e = node.expression
        if (e.kind != BoundNode.Kind.NameExpression) {
            return super.rewriteCallExpression(node)
        }
        e as NameExpression
        val fn = e.symbol
        if (fn.kind != Symbol.Kind.Function || !fn.meta.isInline) {
            return super.rewriteCallExpression(node)
        }
        fn as CallableSymbol
        val returnLabel = generateReturnLabel()
        val params = fn.parameters.map { VariableSymbol.local(it.realName, it.type, it.isReadOnly, null) }

        val declarations = params.mapIndexed { i, param ->
            ValVarDeclaration(param, rewriteExpression(node.arguments.elementAt(i)))
        }

        paramsStack.push(params)
        returnStack.push(returnLabel)

        val inlinedStatement = rewriteStatement(functionBodies[fn]!!)

        returnStack.pop()
        paramsStack.pop()

        return BlockExpression(declarations + if (node.type == TypeSymbol.Void) listOf(
            inlinedStatement,
            LabelStatement(returnLabel),
        ) else {
            val returnField = VariableSymbol.local(".return", node.type, false, null)
            listOf(
                ValVarDeclaration(returnField, LiteralExpression.nullEquivalent(node.type)),
                inlinedStatement,
                LabelStatement(returnLabel),
                ExpressionStatement(NameExpression(returnField)),
            )
        }, node.type)
    }

    override fun rewriteReturnStatement(node: ReturnStatement): Statement {
        val e = rewriteExpression(node.expression ?: return Goto(returnStack.peek()))
        return rewriteAssignmentStatement(Assignment(NameExpression(VariableSymbol.local(".return", e.type, false, null)), e))
    }

    override fun rewriteNameExpression(node: NameExpression): NameExpression {
        val symbol = node.symbol
        if (symbol.kind != Symbol.Kind.Parameter) {
            return node
        }
        return NameExpression(paramsStack.peek().also { println(it); println(symbol.realName) }?.find { it.realName == symbol.realName } ?: return node)
    }

    private var labelCount = 0
    private fun generateReturnLabel(): Label {
        val name = "RET${(++labelCount).toString(16)}"
        return Label(name)
    }
}