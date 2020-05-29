package mango.interpreter.binding

import mango.compilation.DiagnosticList
import mango.compilation.TextSpan
import mango.interpreter.lowering.Lowerer
import mango.interpreter.symbols.*
import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Token
import mango.interpreter.syntax.parser.*
import java.util.*
import kotlin.Exception
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class Binder(
    var scope: BoundScope,
    val function: FunctionSymbol?
) {

    val diagnostics = DiagnosticList()
    private val loopStack = Stack<Pair<BoundLabel, BoundLabel>>()
    private var labelCount = 0

    init {
        if (function != null) {
            for (p in function.parameters) {
                scope.tryDeclare(p)
            }
        }
    }

    private fun bindStatement(node: StatementNode): BoundStatement {
        return when (node.kind) {
            SyntaxType.BlockStatement -> bindBlockStatement(node as BlockStatementNode)
            SyntaxType.ExpressionStatement -> bindExpressionStatement(node as ExpressionStatementNode)
            SyntaxType.VariableDeclaration -> bindVariableDeclaration(node as VariableDeclarationNode)
            SyntaxType.IfStatement -> bindIfStatement(node as IfStatementNode)
            SyntaxType.WhileStatement -> bindWhileStatement(node as WhileStatementNode)
            SyntaxType.ForStatement -> bindForStatement(node as ForStatementNode)
            SyntaxType.BreakStatement -> bindBreakStatement(node as BreakStatementNode)
            SyntaxType.ContinueStatement -> bindContinueStatement(node as ContinueStatementNode)
            SyntaxType.ReturnStatement -> bindReturnStatement(node as ReturnStatementNode)
            else -> throw Exception("Unexpected node: ${node.kind}")
        }
    }

    private fun bindErrorStatement() = BoundExpressionStatement(BoundErrorExpression())

    private fun bindIfStatement(node: IfStatementNode): BoundIfStatement {
        val condition = bindExpression(node.condition, TypeSymbol.bool)
        val statement = bindBlockStatement(node.thenStatement)
        val elseStatement: BoundStatement?
        if (node.elseClause != null) {
            elseStatement = bindStatement(node.elseClause.statement)
            if (elseStatement is BoundBlockStatement &&
                elseStatement.statements.size == 1 &&
                elseStatement.statements.elementAt(0) is BoundIfStatement) {
                diagnostics.styleElseIfStatement(node.elseClause.span)
            }
        } else {
            elseStatement = null
        }
        return BoundIfStatement(condition, statement, elseStatement)
    }

    private fun bindWhileStatement(node: WhileStatementNode): BoundWhileStatement {
        val condition = bindExpression(node.condition, TypeSymbol.bool)
        val (body, breakLabel, continueLabel) = bindLoopBody(node.body)
        return BoundWhileStatement(condition, body, breakLabel, continueLabel)
    }

    private fun bindForStatement(node: ForStatementNode): BoundForStatement {
        val lowerBound = bindExpression(node.lowerBound, TypeSymbol.int)
        val upperBound = bindExpression(node.upperBound, TypeSymbol.int)

        val previous = scope
        scope = BoundScope(scope)
        val variable = bindVariable(node.identifier, TypeSymbol.int, true)
        val (body, breakLabel, continueLabel) = bindLoopBody(node.body)
        scope = previous

        return BoundForStatement(variable, lowerBound, upperBound, body, breakLabel, continueLabel)
    }

    private fun bindLoopBody(node: BlockStatementNode): Triple<BoundBlockStatement, BoundLabel, BoundLabel> {
        val numStr = labelCount.toString(16)
        val breakLabel = BoundLabel("B$numStr")
        val continueLabel = BoundLabel("C$numStr")
        labelCount++
        loopStack.push(breakLabel to continueLabel)
        val body = bindBlockStatement(node)
        loopStack.pop()
        return Triple(body, breakLabel, continueLabel)
    }

    private fun bindBreakStatement(node: BreakStatementNode): BoundStatement {
        if (loopStack.count() == 0) {
            diagnostics.reportBreakContinueOutsideLoop(node.keyword.span, node.keyword.string!!)
            return bindErrorStatement()
        }
        val breakLabel = loopStack.peek().first
        return BoundGotoStatement(breakLabel)
    }

    private fun bindContinueStatement(node: ContinueStatementNode): BoundStatement {
        if (loopStack.count() == 0) {
            diagnostics.reportBreakContinueOutsideLoop(node.keyword.span, node.keyword.string!!)
            return bindErrorStatement()
        }
        val continueLabel = loopStack.peek().second
        return BoundGotoStatement(continueLabel)
    }

    private fun bindReturnStatement(node: ReturnStatementNode): BoundStatement {
        val expression = node.expression?.let { bindExpression(it) }
        if (function == null) {
            diagnostics.reportReturnOutsideFunction(node.keyword.span)
        }
        else if (function.type == TypeSymbol.unit) {
            if (expression != null) {
                diagnostics.reportCantReturnInUnitFunction(node.expression.span)
            }
        }
        else {
            if (expression == null) {
                diagnostics.reportCantReturnWithoutValue(node.keyword.span)
            }
            else if (!expression.type.isOfType(function.type)) {
                diagnostics.reportWrongType(node.expression.span, expression.type, function.type)
            }
        }
        return BoundReturnStatement(expression)
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
        val isReadOnly = node.keyword.kind == SyntaxType.Val
        val type = bindTypeClause(node.typeClauseNode)
        val initializer = bindExpression(node.initializer)
        val actualType = type ?: initializer.type
        if (!initializer.type.isOfType(actualType) && initializer.type != TypeSymbol.error) {
            diagnostics.reportWrongType(node.initializer.span, initializer, type!!)
        }
        val variable = bindVariable(node.identifier, actualType, isReadOnly)
        return BoundVariableDeclaration(variable, initializer)
    }

    private fun bindTypeClause(node: TypeClauseNode?): TypeSymbol? {
        if (node == null) {
            return null
        }
        val type = TypeSymbol.lookup(node.identifier.string!!)
        if (type == null) {
            diagnostics.reportUndefinedType(node.identifier.span, node.identifier.string)
        }
        return type
    }

    private fun bindVariable(identifier: Token, type: TypeSymbol, isReadOnly: Boolean): VariableSymbol {
        val name = identifier.string ?: "?"
        val variable =
            if (function == null) { GlobalVariableSymbol(name, type, isReadOnly) }
            else { LocalVariableSymbol(name, type, isReadOnly) }
        if (!identifier.isMissing && !scope.tryDeclare(variable)) {
            diagnostics.reportSymbolAlreadyDeclared(identifier.span, name)
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

        val type = TypeSymbol.lookup(node.identifier.string!!)
        if (node.arguments.nodeCount == 1 && type != null) {
            return bindCast(node.arguments[0], type)
        }

        val arguments = ArrayList<BoundExpression>()

        for (a in node.arguments) {
            val arg = bindExpression(a, canBeUnit = true)
            arguments.add(arg)
        }

        val (function, result) = scope.tryLookupFunction(node.identifier.string!!)

        if (!result) {
            diagnostics.reportUndefinedName(node.identifier.span, node.identifier.string)
            return BoundErrorExpression()
        }

        if (node.arguments.nodeCount > function!!.parameters.size) {
            val firstExceedingNode = if (function.parameters.isNotEmpty()) {
                node.arguments.getSeparator(function.parameters.size - 1)
            } else {
                node.arguments[0]
            }
            val span = TextSpan.fromBounds(firstExceedingNode.span.start, node.arguments.last().span.end)
            diagnostics.reportWrongArgumentCount(span, function.name, node.arguments.nodeCount, function.parameters.size)
            return  BoundErrorExpression()
        } else if (node.arguments.nodeCount < function.parameters.size) {
            diagnostics.reportWrongArgumentCount(node.rightBracket.span, function.name, node.arguments.nodeCount, function.parameters.size)
            return  BoundErrorExpression()
        }

        for (i in arguments.indices) {
            val arg = arguments[i]
            val param = function.parameters[i]

            if (!arg.type.isOfType(param.type)) {
                if (arg.type != TypeSymbol.error) {
                    diagnostics.reportWrongArgumentType(node.arguments[i].span, param.name, arg.type, param.type)
                }
                return BoundErrorExpression()
            }
        }

        return BoundCallExpression(function, arguments)
    }

    private fun bindCast(node: ExpressionNode, type: TypeSymbol): BoundExpression {
        val expression = bindExpression(node)
        if (expression.type == TypeSymbol.error) {
            return BoundErrorExpression()
        }
        val conversion = Conversion.classify(expression.type, type)
        if (!conversion.exists) {
            diagnostics.reportCantCast(node.span, expression.type, type)
            return BoundErrorExpression()
        }
        if (conversion.isIdentity) {
            return expression
        }
        return BoundCastExpression(type, expression)
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
        fun bindGlobalScope(node: CompilationUnitNode, previous: BoundGlobalScope?): BoundGlobalScope {
            val parentScope = createParentScopes(previous)
            val binder = Binder(BoundScope(parentScope), null)

            for (function in node.members) {
                if (function is FunctionDeclarationNode) {
                    binder.bindFunctionDeclaration(function)
                }
            }

            val statementBuilder = ArrayList<BoundStatement>()

            for (globalStatement in node.members) {
                if (globalStatement is GlobalStatementNode) {
                    val statement = binder.bindStatement(globalStatement.statement)
                    statementBuilder.add(statement)
                }
            }

            val symbols = binder.scope.symbols
            val diagnostics = binder.diagnostics
            if (previous != null) {
                diagnostics.append(previous.diagnostics)
            }
            return BoundGlobalScope(previous, diagnostics, symbols, statementBuilder)
        }

        private fun createParentScopes(previous: BoundGlobalScope?): BoundScope {

            val stack = Stack<BoundGlobalScope>()
            var prev = previous

            while (prev != null) {
                stack.push(prev)
                prev = prev.previous
            }

            var parent: BoundScope = createRootScope()

            while (stack.count() > 0) {
                val previous = stack.pop()
                val scope = BoundScope(parent)
                for (v in previous.symbols) {
                    scope.tryDeclare(v)
                }
                parent = scope
            }

            return parent
        }

        private fun createRootScope(): BoundScope {
            val result = BoundScope(null)
            for (fn in BuiltinFunctions.getAll()) {
                result.tryDeclare(fn)
            }
            return result
        }

        fun bindProgram(globalScope: BoundGlobalScope): BoundProgram {

            val parentScope = createParentScopes(globalScope)
            val functionBodies = HashMap<FunctionSymbol, BoundStatement>()
            val diagnostics = DiagnosticList()

            var scope: BoundGlobalScope? = globalScope
            while (scope != null) {
                for (symbol in scope.symbols) {
                    if (symbol is FunctionSymbol) {
                        val binder = Binder(parentScope, symbol)
                        val body = binder.bindStatement(symbol.declarationNode!!.body)
                        val loweredBody = Lowerer.lower(body)
                        if (symbol.type != TypeSymbol.unit && !ControlFlowGraph.allPathsReturn(loweredBody)) {
                            diagnostics.reportAllPathsMustReturn(symbol.declarationNode.identifier.span)
                        }
                        functionBodies[symbol] = loweredBody
                        diagnostics.append(binder.diagnostics)
                    }
                }
                scope = scope.previous
            }

            val statement = Lowerer.lower(BoundBlockStatement(globalScope.statements))

            return BoundProgram(diagnostics, functionBodies, statement)
        }
    }

    private fun bindFunctionDeclaration(node: FunctionDeclarationNode) {
        val params = ArrayList<ParameterSymbol>()

        val seenParameterNames = HashSet<String>()

        if (node.params != null) {
            for (paramNode in node.params.iterator()) {
                val name = paramNode.identifier.string!!
                if (!seenParameterNames.add(name)) {
                    diagnostics.reportParamAlreadyExists(paramNode.identifier.span, name)
                } else {
                    val type = bindTypeClause(paramNode.typeClause)!!
                    val parameter = ParameterSymbol(name, type)
                    params.add(parameter)
                }
            }
        }

        val type: TypeSymbol

        if (node.typeClause == null) {
            if (node.lambdaArrow == null) {
                type = TypeSymbol.unit
            } else {
                type = TypeSymbol.unit
            }
        } else {
            type = bindTypeClause(node.typeClause)!!
        }

        val function = FunctionSymbol(node.identifier.string!!, params.toTypedArray(), type, node)
        if (!scope.tryDeclare(function)) {
            diagnostics.reportSymbolAlreadyDeclared(node.identifier.span, function.name)
        }
    }
}