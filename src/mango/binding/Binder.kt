package mango.binding

import mango.compilation.DiagnosticList
import mango.symbols.BuiltinFunctions
import mango.symbols.TypeSymbol
import mango.symbols.VariableSymbol
import mango.syntax.SyntaxType
import mango.syntax.lex.Token
import mango.syntax.parser.*
import java.util.*
import kotlin.Exception
import kotlin.collections.ArrayList

class Binder(
    var scope: BoundScope
) {

    val diagnostics = DiagnosticList()

    private fun bindStatement(node: StatementNode): BoundStatement {
        return when (node.kind) {
            SyntaxType.BlockStatement -> bindBlockStatement(node as BlockStatementNode)
            SyntaxType.ExpressionStatement -> bindExpressionStatement(node as ExpressionStatementNode)
            SyntaxType.VariableDeclaration -> bindVariableDeclaration(node as VariableDeclarationNode)
            SyntaxType.IfStatement -> bindIfStatement(node as IfStatementNode)
            SyntaxType.WhileStatement -> bindWhileStatement(node as WhileStatementNode)
            SyntaxType.ForStatement -> bindForStatement(node as ForStatementNode)
            else -> throw Exception("Unexpected node: ${node.kind}")
        }
    }

    private fun bindIfStatement(node: IfStatementNode): BoundIfStatement {
        val condition = bindExpression(node.condition, TypeSymbol.bool)
        val statement = bindBlockStatement(node.thenStatement)
        val elseStatement = node.elseClause?.let { bindStatement(it.statement) }
        return BoundIfStatement(condition, statement, elseStatement)
    }

    private fun bindWhileStatement(node: WhileStatementNode): BoundWhileStatement {
        val condition = bindExpression(node.condition, TypeSymbol.bool)
        val body = bindBlockStatement(node.body)
        return BoundWhileStatement(condition, body)
    }

    private fun bindForStatement(node: ForStatementNode): BoundForStatement {
        val lowerBound = bindExpression(node.lowerBound, TypeSymbol.int)
        val upperBound = bindExpression(node.upperBound, TypeSymbol.int)

        val previous = scope
        scope = BoundScope(scope)
        val variable = bindVariable(node.identifier, TypeSymbol.int, true)
        val body = bindBlockStatement(node.body)
        scope = previous

        return BoundForStatement(variable, lowerBound, upperBound, body)
    }

    private fun bindBlockStatement(node: BlockStatementNode): BoundBlockStatement {
        val statements = ArrayList<BoundStatement>()
        val previous = scope
        scope = BoundScope(scope)
        for (s in node.statements) {
            val statement = bindStatement(s)
            statements.add(statement)
        }
        scope = previous
        return BoundBlockStatement(statements)
    }

    private fun bindExpressionStatement(node: ExpressionStatementNode): BoundExpressionStatement {
        val expression = bindExpression(node.expression, canBeUnit = true)
        return BoundExpressionStatement(expression)
    }

    private fun bindVariableDeclaration(node: VariableDeclarationNode): BoundStatement {
        val  isReadOnly = node.keyword.kind == SyntaxType.Val
        val initializer = bindExpression(node.initializer)
        val variable = bindVariable(node.identifier, initializer.type, isReadOnly)
        return BoundVariableDeclaration(variable, initializer)
    }

    private fun bindVariable(identifier: Token, type: TypeSymbol, isReadOnly: Boolean): VariableSymbol {
        val name = identifier.string ?: "?"
        val variable = VariableSymbol(name, type, isReadOnly)
        if (!identifier.isMissing && !scope.tryDeclareVariable(variable)) {
            diagnostics.reportVarAlreadyDeclared(identifier.span, name)
        }
        return variable
    }

    private fun bindExpression(node: ExpressionNode, canBeUnit: Boolean = false): BoundExpression {
        val result = bindExpressionInternal(node)
        if (!canBeUnit && result.type == TypeSymbol.unit) {
            diagnostics.reportExpressionMustHaveValue(node.span)
            return BoundErrorExpression()
        }
        return result
    }

    private fun bindExpressionInternal(node: ExpressionNode): BoundExpression {
        return when (node.kind) {
            SyntaxType.ParenthesizedExpression -> bindParenthesizedExpression(node as ParenthesizedExpressionNode)
            SyntaxType.LiteralExpression -> bindLiteralExpression(node as LiteralExpressionNode)
            SyntaxType.UnaryExpression -> bindUnaryExpression(node as UnaryExpressionNode)
            SyntaxType.BinaryExpression -> bindBinaryExpression(node as BinaryExpressionNode)
            SyntaxType.NameExpression -> bindNameExpression(node as NameExpressionNode)
            SyntaxType.AssignmentExpression -> bindAssignmentExpression(node as AssignmentExpressionNode)
            SyntaxType.CallExpression -> bindCallExpression(node as CallExpressionNode)
            else -> throw Exception("Unexpected node: ${node.kind}")
        }
    }

    private fun bindExpression(node: ExpressionNode, type: TypeSymbol): BoundExpression {
        val result = bindExpression(node)
        if (type != TypeSymbol.error &&
            result.type != TypeSymbol.error &&
            result.type != type) {
            diagnostics.reportWrongType(node.span, result, type)
        }
        return result
    }

    private fun bindCallExpression(node: CallExpressionNode): BoundExpression {

        val arguments = ArrayList<BoundExpression>()

        for (a in node.arguments) {
            val arg = bindExpression(a, canBeUnit = true)
            arguments.add(arg)
        }

        //val functions = BuiltinFunctions.getAll()
        //val function = functions.singleOrNull { it.name == node.identifier.string }

        val (function, result) = scope.tryLookupFunction(node.identifier.string!!)

        if (!result) {
            diagnostics.reportUndefinedName(node.identifier.span, node.identifier.string)
            return BoundErrorExpression()
        }

        if (node.arguments.nodeCount != function!!.parameters.size) {
            diagnostics.reportWrongArgumentCount(node.span, function.name, node.arguments.nodeCount, function.parameters.size)
            return  BoundErrorExpression()
        }

        for (i in arguments.indices) {
            val arg = arguments[i]
            val param = function.parameters[i]

            if (!arg.type.isOfType(param.type)) {
                diagnostics.reportWrongParameterType(node.span, param.name, param.type, function.type)
                return BoundErrorExpression()
            }
        }

        return BoundCallExpression(function, arguments)
    }

    private fun bindAssignmentExpression(node: AssignmentExpressionNode): BoundExpression {
        val name = node.identifierToken.string ?: return BoundLiteralExpression(0)
        val boundExpression = bindExpression(node.expression)
        val (variable, result) = scope.tryLookupVariable(name)
        if (!result) {
            diagnostics.reportUndefinedName(node.identifierToken.span, name)
            return boundExpression
        }
        if (variable!!.isReadOnly) {
            diagnostics.reportVarIsImmutable(node.equalsToken.span, name)
            return boundExpression
        }
        if (variable.type != boundExpression.type) {
            diagnostics.reportWrongType(node.identifierToken.span, name, boundExpression.type)
            return boundExpression
        }
        return BoundAssignmentExpression(variable, boundExpression)
    }

    private fun bindNameExpression(node: NameExpressionNode): BoundExpression {
        val name = node.identifierToken.string ?: return BoundErrorExpression()
        val (variable, result) = scope.tryLookupVariable(name)
        if (!result) {
            diagnostics.reportUndefinedName(node.identifierToken.span, name)
            return BoundErrorExpression()
        }
        return BoundVariableExpression(variable!!)
    }

    private fun bindParenthesizedExpression(
            node: ParenthesizedExpressionNode
    ) = bindExpression(node.expression)

    private fun bindLiteralExpression(
        node: LiteralExpressionNode
    ) = BoundLiteralExpression(node.value)

    private fun bindUnaryExpression(node: UnaryExpressionNode): BoundExpression {
        val operand = bindExpression(node.operand)
        if (operand.type == TypeSymbol.error) {
            return BoundErrorExpression()
        }
        val operator = BoundUnaryOperator.bind(node.operator.kind, operand.type)
        if (operator == null) {
            diagnostics.reportUnaryOperator(node.operator.span, node.operator.kind, operand.type)
            return BoundErrorExpression()
        }
        return BoundUnaryExpression(operator, operand)
    }

    private fun bindBinaryExpression(node: BinaryExpressionNode): BoundExpression {
        val left = bindExpression(node.left)
        val right = bindExpression(node.right)
        if (left.type == TypeSymbol.error || right.type == TypeSymbol.error) {
            return BoundErrorExpression()
        }
        val operator = BoundBinaryOperator.bind(node.operator.kind, left.type, right.type)
        if (operator == null) {
            diagnostics.reportBinaryOperator(node.operator.span, left.type, node.operator.kind, right.type)
            return BoundErrorExpression()
        }
        return BoundBinaryExpression(left, operator, right)
    }

    companion object {
        fun bindGlobalScope(fileUnit: FileUnit, previous: BoundGlobalScope?): BoundGlobalScope {
            val parentScope = createParentScopes(previous)
            val binder = Binder(BoundScope(parentScope))
            val expression = binder.bindStatement(fileUnit.statementNode)
            val variables = binder.scope.variables
            val diagnostics = binder.diagnostics.list.toMutableList()
            if (previous != null) {
                diagnostics.addAll(previous.diagnostics)
            }
            return BoundGlobalScope(previous, diagnostics, variables, expression)
        }

        private fun createParentScopes(previous: BoundGlobalScope?): BoundScope? {

            val stack = Stack<BoundGlobalScope>()
            var prev = previous

            while (prev != null) {
                stack.push(prev)
                prev = prev.previous
            }

            var parent: BoundScope? = createRootScope()

            while (stack.count() > 0) {
                val previous = stack.pop()
                val scope = BoundScope(parent)
                for (v in previous.variables) {
                    scope.tryDeclareVariable(v)
                }
                parent = scope
            }

            return parent
        }

        private fun createRootScope(): BoundScope? {
            val result = BoundScope(null)
            for (fn in BuiltinFunctions.getAll()) {
                result.tryDeclareFunction(fn)
            }
            return result
        }
    }
}