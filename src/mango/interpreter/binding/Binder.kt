package mango.interpreter.binding

import mango.compilation.DiagnosticList
import mango.interpreter.binding.nodes.BoundBinaryOperator
import mango.interpreter.binding.nodes.BoundNodeType
import mango.interpreter.binding.nodes.BoundUnaryOperator
import mango.interpreter.binding.nodes.BoundUnaryOperatorType
import mango.interpreter.binding.nodes.expressions.*
import mango.interpreter.binding.nodes.statements.*
import mango.interpreter.text.TextSpan
import mango.interpreter.symbols.*
import mango.interpreter.syntax.SyntaxTree
import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.Token
import mango.interpreter.syntax.Translator
import mango.interpreter.syntax.nodes.*
import mango.interpreter.text.TextLocation
import mango.isRepl
import mango.isSharedLib
import java.util.*
import kotlin.Exception
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class Binder(
    var scope: BoundScope,
    val function: FunctionSymbol?,
    val functions: HashMap<FunctionSymbol, BoundBlockStatement?>,
    val functionBodies: HashMap<FunctionSymbol, BoundBlockStatement?>?
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
        val condition = bindExpression(node.condition, TypeSymbol.Bool)
        val statement = bindBlockStatement(node.thenStatement)
        val elseStatement: BoundStatement?
        if (node.elseClause != null) {
            elseStatement = bindStatement(node.elseClause.statement)
            if (elseStatement is BoundBlockStatement &&
                elseStatement.statements.size == 1 &&
                elseStatement.statements.elementAt(0) is BoundIfStatement) {
                diagnostics.styleElseIfStatement(TextLocation(
                        node.location.text,
                        TextSpan.fromBounds(
                                node.elseClause.keyword.span.start,
                                node.elseClause.statement.children.elementAt(1)
                                        .children.elementAt(0).span.end)))
            }
        } else {
            elseStatement = null
        }
        return BoundIfStatement(condition, statement, elseStatement)
    }

    private fun bindWhileStatement(node: WhileStatementNode): BoundWhileStatement {
        val condition = bindExpression(node.condition, TypeSymbol.Bool)
        val (body, breakLabel, continueLabel) = bindLoopBody(node.body)
        return BoundWhileStatement(condition, body, breakLabel, continueLabel)
    }

    private fun bindForStatement(node: ForStatementNode): BoundForStatement {
        val lowerBound = bindExpression(node.lowerBound, TypeSymbol.Int)
        val upperBound = bindExpression(node.upperBound, TypeSymbol.Int)

        val previous = scope
        scope = BoundScope(scope)
        val variable = bindVariable(node.identifier, TypeSymbol.Int, true, null)
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
            diagnostics.reportBreakContinueOutsideLoop(node.keyword.location, node.keyword.string!!)
            return bindErrorStatement()
        }
        val breakLabel = loopStack.peek().first
        return BoundGotoStatement(breakLabel)
    }

    private fun bindContinueStatement(node: ContinueStatementNode): BoundStatement {
        if (loopStack.count() == 0) {
            diagnostics.reportBreakContinueOutsideLoop(node.keyword.location, node.keyword.string!!)
            return bindErrorStatement()
        }
        val continueLabel = loopStack.peek().second
        return BoundGotoStatement(continueLabel)
    }

    private fun bindReturnStatement(node: ReturnStatementNode): BoundStatement {
        val expression = node.expression?.let { bindExpression(it) }
        if (function == null) {
            diagnostics.reportReturnOutsideFunction(node.keyword.location)
        }
        else if (function.type == TypeSymbol.Unit) {
            if (expression != null) {
                diagnostics.reportCantReturnInUnitFunction(node.expression.location)
            }
        }
        else {
            if (expression == null) {
                diagnostics.reportCantReturnWithoutValue(node.keyword.location)
            }
            else if (expression.type != TypeSymbol.err && !expression.type.isOfType(function.type)) {
                diagnostics.reportWrongType(node.expression.location, expression.type, function.type)
            }
        }
        return BoundReturnStatement(expression)
    }

    private fun bindBlockStatement(node: BlockStatementNode): BoundBlockStatement {
        val statements = ArrayList<BoundStatement>()
        val previous = scope
        scope = BoundScope(scope)
        for (s in node.statements) {
            when (s.kind) {
                SyntaxType.FunctionDeclaration -> {
                    val symbol = bindFunctionDeclaration(s as FunctionDeclarationNode)
                    if (function != null) {
                        bindFunction(scope, functions, symbol, diagnostics, functionBodies)
                    }
                }
                SyntaxType.UseStatement -> bindUseStatement(s as UseStatementNode)
                else -> {
                    val statement = bindStatement(s)
                    statements.add(statement)
                }
            }
        }
        scope = previous
        return BoundBlockStatement(statements)
    }

    private fun bindExpressionStatement(node: ExpressionStatementNode): BoundExpressionStatement {
        val expression = bindExpression(node.expression, canBeUnit = true)
        if (function == null && !isRepl || function != null && function.declarationNode?.lambdaArrow == null) {
            val kind = expression.boundType
            val isAllowedExpression =
                kind == BoundNodeType.AssignmentExpression ||
                kind == BoundNodeType.CallExpression ||
                kind == BoundNodeType.ErrorExpression
            if (!isAllowedExpression) {
                diagnostics.reportInvalidExpressionStatement(node.location)
                return bindErrorStatement()
            }
        }
        return BoundExpressionStatement(expression)
    }

    private fun bindVariableDeclaration(node: VariableDeclarationNode): BoundVariableDeclaration {
        val isReadOnly = node.keyword.kind == SyntaxType.Val
        val type = bindTypeClause(node.typeClauseNode)
        val initializer = bindExpression(node.initializer)
        val actualType = type ?: initializer.type
        if (!initializer.type.isOfType(actualType) && initializer.type != TypeSymbol.err) {
            diagnostics.reportWrongType(node.initializer.location, initializer, type!!)
        }
        val variable = bindVariable(node.identifier, actualType, isReadOnly, initializer.constantValue)
        return BoundVariableDeclaration(variable, initializer)
    }

    private fun bindTypeClause(node: TypeClauseNode?): TypeSymbol? {
        if (node == null) {
            return null
        }
        val type = TypeSymbol[node.identifier.string!!]
        if (type == null) {
            diagnostics.reportUndefinedType(node.identifier.location, node.identifier.string)
            return TypeSymbol.err
        }
        return type
    }

    private fun bindVariable(identifier: Token, type: TypeSymbol, isReadOnly: Boolean, constant: BoundConstant?): VariableSymbol {
        val name = identifier.string ?: "?"
        val variable: VariableSymbol
        variable = if (function == null && scope is BoundNamespace) {
            VariableSymbol.visible(name, type, isReadOnly, constant, (scope as BoundNamespace).path + '.' + name)
        } else {
            VariableSymbol.local(name, type, isReadOnly, constant)
        }
        if (!identifier.isMissing && !scope.tryDeclare(variable)) {
            diagnostics.reportSymbolAlreadyDeclared(identifier.location, name)
        }
        return variable
    }

    fun bindUseStatement(node: UseStatementNode) {
        val path = node.directories.joinToString(separator = ".") { it.string!! }
        if (BoundNamespace.namespaces[path] == null) {
            diagnostics.reportIncorrectUseStatement(node.location)
        } else {
            scope.use(BoundUse(path, node.isInclude))
        }
    }

    private fun bindExpression(node: ExpressionNode, canBeUnit: Boolean = false, type: TypeSymbol? = null): BoundExpression {
        val result = bindExpressionInternal(node, type)
        if (!canBeUnit && result.type == TypeSymbol.Unit) {
            diagnostics.reportExpressionMustHaveValue(node.location)
            return BoundErrorExpression()
        }
        return result
    }

    private fun bindExpressionInternal(node: ExpressionNode, type: TypeSymbol?): BoundExpression {
        return when (node.kind) {
            SyntaxType.ParenthesizedExpression -> bindParenthesizedExpression(node as ParenthesizedExpressionNode)
            SyntaxType.LiteralExpression -> bindLiteralExpression(node as LiteralExpressionNode, type)
            SyntaxType.UnaryExpression -> bindUnaryExpression(node as UnaryExpressionNode)
            SyntaxType.BinaryExpression -> {
                node as BinaryExpressionNode
                if (node.operator.kind == SyntaxType.Dot) {
                    bindDotAccessExpression(node)
                } else {
                    bindBinaryExpression(node)
                }
            }
            SyntaxType.NameExpression -> bindNameExpression(node as NameExpressionNode)
            SyntaxType.AssignmentExpression -> bindAssignmentExpression(node as AssignmentExpressionNode)
            SyntaxType.CallExpression -> bindCallExpression(node as CallExpressionNode)
            else -> throw Exception("Unexpected node: ${node.kind}")
        }
    }

    private fun bindExpression(node: ExpressionNode, type: TypeSymbol): BoundExpression {
        val result = bindExpression(node)
        if (type != TypeSymbol.err &&
            result.type != TypeSymbol.err &&
            result.type != type) {
            diagnostics.reportWrongType(node.location, result.type, type)
        }
        return result
    }

    private fun bindCallExpression(node: CallExpressionNode): BoundExpression {

        val type = TypeSymbol[node.identifier.string!!]
        if (node.arguments.nodeCount == 1 && type != null) {
            return bindCast(node.arguments[0], type)
        }

        val arguments = ArrayList<BoundExpression>()

        for (a in node.arguments) {
            val arg = bindExpression(a, canBeUnit = true)
            arguments.add(arg)
        }

        val function = scope.tryLookup(
            listOf(node.identifier.string),
            CallableSymbol.generateSuffix(arguments.map { it.type }, false))

        if (function == null) {
            diagnostics.reportUndefinedName(node.identifier.location, node.identifier.string)
            return BoundErrorExpression()
        }

        if (function !is CallableSymbol) {
            diagnostics.reportNotCallable(node.identifier.location, function)
            return BoundErrorExpression()
        }

        if (node.arguments.nodeCount > function.parameters.size) {
            val firstExceedingNode = if (function.parameters.isNotEmpty()) {
                node.arguments.getSeparator(function.parameters.size - 1)
            } else {
                node.arguments[0]
            }
            val span = TextSpan.fromBounds(firstExceedingNode.span.start, node.arguments.last().span.end)
            val location = TextLocation(firstExceedingNode.syntaxTree.sourceText, span)
            diagnostics.reportWrongArgumentCount(location, function.name, node.arguments.nodeCount, function.parameters.size)
            return BoundErrorExpression()
        } else if (node.arguments.nodeCount < function.parameters.size) {
            diagnostics.reportWrongArgumentCount(node.rightBracket.location, function.name, node.arguments.nodeCount, function.parameters.size)
            return BoundErrorExpression()
        }

        for (i in arguments.indices) {
            val arg = arguments[i]
            val param = function.parameters[i]

            if (!arg.type.isOfType(param.type)) {
                if (arg.type != TypeSymbol.err) {
                    diagnostics.reportWrongArgumentType(node.arguments[i].location, param.name, arg.type, param.type)
                }
                return BoundErrorExpression()
            }
        }

        return BoundCallExpression(function, arguments)
    }

    private fun bindCast(node: ExpressionNode, type: TypeSymbol): BoundExpression {
        val expression = bindExpression(node)
        if (expression.type == TypeSymbol.err) {
            return BoundErrorExpression()
        }
        val conversion = Conversion.classify(expression.type, type)
        if (!conversion.exists) {
            diagnostics.reportCantCast(node.location, expression.type, type)
            return BoundErrorExpression()
        }
        if (conversion.isIdentity) {
            return expression
        }
        return BoundCastExpression(type, expression)
    }

    private fun bindAssignmentExpression(node: AssignmentExpressionNode): BoundExpression {
        val name = node.identifierToken.string ?: return BoundErrorExpression()
        val boundExpression = bindExpression(node.expression)
        val variable = scope.tryLookup(listOf(name))
        if (variable == null) {
            diagnostics.reportUndefinedName(node.identifierToken.location, name)
            return boundExpression
        }
        if (variable !is VariableSymbol) {
            diagnostics.reportVarIsConstant(node.equalsToken.location, name)
            return boundExpression
        }
        if (variable.isReadOnly) {
            diagnostics.reportVarIsImmutable(node.equalsToken.location, name)
            return boundExpression
        }
        if (!boundExpression.type.isOfType(variable.type)) {
            diagnostics.reportWrongType(node.identifierToken.location, variable.type, boundExpression.type)
            return boundExpression
        }
        return BoundAssignmentExpression(variable, boundExpression)
    }

    private fun bindNameExpression(node: NameExpressionNode): BoundExpression {
        val name = node.identifier.string ?: return BoundErrorExpression()
        val variable = scope.tryLookup(listOf(name))
        if (variable == null) {
            diagnostics.reportUndefinedName(node.identifier.location, name)
            return BoundErrorExpression()
        }
        return BoundVariableExpression(variable)
    }

    private fun bindParenthesizedExpression(
        node: ParenthesizedExpressionNode
    ) = bindExpression(node.expression)

    private fun bindLiteralExpression(
        node: LiteralExpressionNode,
        type: TypeSymbol?
    ): BoundLiteralExpression {
        return BoundLiteralExpression(node.value, when (node.value) {
            is Byte -> when (type) {
                TypeSymbol.I8 -> TypeSymbol.I8
                TypeSymbol.I16 -> TypeSymbol.I16
                TypeSymbol.I64 -> TypeSymbol.I64
                else -> TypeSymbol.I32
            }
            is Short -> when (type) {
                TypeSymbol.I16 -> TypeSymbol.I16
                TypeSymbol.I64 -> TypeSymbol.I64
                else -> TypeSymbol.I32
            }
            is Int -> when (type) {
                TypeSymbol.I64 -> TypeSymbol.I64
                else -> TypeSymbol.I32
            }
            is Long -> TypeSymbol.I64
            is Float -> TypeSymbol.Float
            is Double -> TypeSymbol.Double
            is Boolean -> TypeSymbol.Bool
            is String -> TypeSymbol.String
            else -> throw Exception("Unexpected literal of type ${node.value?.javaClass}")
        })
    }

    private fun bindUnaryExpression(node: UnaryExpressionNode): BoundExpression {
        val operand = bindExpression(node.operand)
        if (operand.type == TypeSymbol.err) {
            return BoundErrorExpression()
        }
        val operator = BoundUnaryOperator.bind(node.operator.kind, operand.type)
        if (operator == null) {
            val operatorFunction = scope.tryLookup(
                listOf(Translator.unaryOperatorToString(node.operator.kind)),
                CallableSymbol.generateSuffix(listOf(operand.type), true))
            if (operatorFunction != null && operatorFunction.meta.isOperator) {
                operatorFunction as CallableSymbol
                return BoundCallExpression(operatorFunction, listOf(operand))
            }
            diagnostics.reportUnaryOperator(node.operator.location, node.operator.kind, operand.type)
            return BoundErrorExpression()
        }
        return BoundUnaryExpression(operator, operand)
    }

    private fun bindBinaryExpression(node: BinaryExpressionNode): BoundExpression {
        val left = bindExpression(node.left)
        val right = bindExpression(node.right)
        if (left.type == TypeSymbol.err || right.type == TypeSymbol.err) {
            return BoundErrorExpression()
        }
        val operator = BoundBinaryOperator.bind(node.operator.kind, left.type, right.type)
        if (operator == null) {
            if (node.operator.kind == SyntaxType.IsNotEqual) {
                val operatorFunction = scope.tryLookup(
                    listOf(Translator.binaryOperatorToString(SyntaxType.IsEqual)),
                    CallableSymbol.generateSuffix(listOf(left.type, right.type), true))
                if (operatorFunction != null && operatorFunction.meta.isOperator) {
                    operatorFunction as CallableSymbol
                    return BoundUnaryExpression(
                        BoundUnaryOperator(SyntaxType.Not, BoundUnaryOperatorType.Not, TypeSymbol.Bool),
                        BoundCallExpression(operatorFunction, listOf(left, right)))
                }
            } else {
                val operatorFunction = scope.tryLookup(
                    listOf(Translator.binaryOperatorToString(node.operator.kind)),
                    CallableSymbol.generateSuffix(listOf(left.type, right.type), true))
                if (operatorFunction != null && operatorFunction.meta.isOperator) {
                    operatorFunction as CallableSymbol
                    return BoundCallExpression(operatorFunction, listOf(left, right))
                }
            }
            diagnostics.reportBinaryOperator(node.operator.location, left.type, node.operator.kind, right.type)
            return BoundErrorExpression()
        }
        return BoundBinaryExpression(left, operator, right)
    }

    private fun bindDotAccessExpression(node: BinaryExpressionNode): BoundExpression {

        fun bindNamespaceAccess(leftName: String, rightName: String): BoundExpression {

            val symbol = if (node.right.kind == SyntaxType.CallExpression) {
                node.right as CallExpressionNode
                scope.tryLookup(leftName.split('.').toMutableList().apply { add(rightName) }, CallableSymbol.generateSuffix(node.right.arguments.map { bindExpression(it).type }, false))
            } else {
                scope.tryLookup(leftName.split('.').toMutableList().apply { add(rightName) })
            }

            if (symbol != null) {
                if (node.right.kind == SyntaxType.CallExpression) {
                    node.right as CallExpressionNode
                    if (symbol !is CallableSymbol) {
                        diagnostics.reportNotCallable(node.right.location, symbol)
                        return BoundErrorExpression()
                    }
                    return BoundCallExpression(symbol as FunctionSymbol, node.right.arguments.map { bindExpression(it) })
                } else {
                    return BoundVariableExpression(symbol)
                }
            }
            val namespace = BoundNamespace["$leftName.$rightName"]
            if (namespace == null) {
                diagnostics.reportUndefinedName(node.right.location, rightName)
                return BoundErrorExpression()
            }
            return BoundNamespaceFieldAccess(namespace)
        }

        if (node.right.kind != SyntaxType.NameExpression && node.right.kind != SyntaxType.CallExpression) {
            diagnostics.reportCantBeAfterDot(node.right.location)
            return BoundErrorExpression()
        }

        node.right as NameExpressionNode

        if (node.left.kind == SyntaxType.NameExpression) {
            node.left as NameExpressionNode
            val leftName = node.left.identifier.string ?: return BoundErrorExpression()
            val rightName = node.right.identifier.string ?: return BoundErrorExpression()
            scope.tryLookup(listOf(leftName)) ?: return bindNamespaceAccess(leftName, rightName)
        }

        val left = bindExpression(node.left)
        val name = node.right.identifier.string ?: return BoundErrorExpression()

        if (left.boundType == BoundNodeType.NamespaceFieldAccess) {
            left as BoundNamespaceFieldAccess
            val leftName = left.namespace.path
            return bindNamespaceAccess(leftName, name)
        }

        if (left.type.kind == Symbol.Kind.Struct) {
            val i = (left.type as TypeSymbol.StructTypeSymbol).fields.indexOfFirst { it.name == name }
            if (node.right.kind == SyntaxType.NameExpression) {
                if (i == -1) {
                    diagnostics.reportUndefinedName(node.right.location, name)
                    return BoundErrorExpression()
                }
                return BoundStructFieldAccess(left, i)
            }
        }
        if (node.right.kind == SyntaxType.CallExpression) {
            node.right as CallExpressionNode
            val arguments = arrayListOf(left).apply { for (a in node.right.arguments) add(bindExpression(a)) }
            val types = arguments.map { it.type }
            val function = scope.tryLookup(listOf(name), CallableSymbol.generateSuffix(types, true))
            if (function != null) {
                return BoundCallExpression(function as CallableSymbol, arguments)
            }
        }
        diagnostics.reportUndefinedName(node.right.location, name)
        return BoundErrorExpression()
    }

    fun bindGlobalStatement(
        statement: StatementNode,
        syntaxTree: SyntaxTree,
        statementBuilder: ArrayList<BoundStatement>,
        symbols: ArrayList<Symbol>
    ) {
        when (statement.kind) {
            SyntaxType.FunctionDeclaration -> {
                statement as FunctionDeclarationNode
                symbols.add(bindFunctionDeclaration(statement))
            }
            SyntaxType.VariableDeclaration -> {
                statement as VariableDeclarationNode
                val statement = bindVariableDeclaration(statement)
                statementBuilder.add(statement)
                symbols.add(statement.variable)
            }
            SyntaxType.UseStatement -> {
                statement as UseStatementNode
                bindUseStatement(statement)
            }
            SyntaxType.ReplStatement -> {
                statement as ReplStatementNode
                val statement = bindStatement(statement.statementNode)
                statementBuilder.add(statement)
            }
            SyntaxType.NamespaceStatement -> {
                statement as NamespaceStatementNode
                val prev = scope
                val namespace = BoundNamespace(syntaxTree.projectPath + '.' + statement.identifier.string, scope)
                scope = namespace
                for (s in statement.members) {
                    bindGlobalStatement(s, syntaxTree, statementBuilder, symbols)
                }
                scope = prev
            }
            else -> throw Exception("Incorrect statement got to be global (${statement.kind})")
        }
    }

    companion object {

        fun bindGlobalScope(
            previous: BoundGlobalScope?,
            syntaxTrees: Collection<SyntaxTree>
        ): BoundGlobalScope {

            val parentScope = createParentScopes(previous)
            val binder = Binder(parentScope, null, HashMap(), null)

            val symbols = ArrayList<Symbol>()
            val statementBuilder = ArrayList<BoundStatement>()

            for (syntaxTree in syntaxTrees) {
                val strBuilder = StringBuilder()
                var parent = parentScope
                syntaxTree.projectPath.split('.').forEachIndexed { i, s ->
                    val path = if (i == 0) { strBuilder.append(s).toString() }
                        else { strBuilder.append('.').append(s).toString() }
                    parent = BoundNamespace.getOr(path) { BoundNamespace(path, parent) }
                }
            }
            for (syntaxTree in syntaxTrees) {
                binder.diagnostics.append(syntaxTree.diagnostics)
                val prev = binder.scope
                val namespace = BoundNamespace[syntaxTree.projectPath]!!
                binder.scope = namespace
                for (s in syntaxTree.root.members) {
                    binder.bindGlobalStatement(s, syntaxTree, statementBuilder, symbols)
                }
                binder.scope = prev
            }

            var entryFn = symbols.find {
                it.kind == Symbol.Kind.Function &&
                (it as FunctionSymbol).meta.isEntry &&
                it.type == TypeSymbol.Unit &&
                it.parameters.isEmpty()
            } as FunctionSymbol?

            val diagnostics = binder.diagnostics

            if (entryFn == null) {
                entryFn = FunctionSymbol(
                    "main",
                    arrayOf(),
                    if (isRepl) TypeSymbol.Any else TypeSymbol.Unit,
                        "main",
                    null,
                        Symbol.MetaData()
                )
                if (!isRepl && !isSharedLib) {
                    diagnostics.reportNoMainFn()
                }
            }

            if (previous != null) {
                diagnostics.append(previous.diagnostics)
            }
            return BoundGlobalScope(
                previous,
                diagnostics,
                symbols,
                statementBuilder,
                entryFn)
        }

        private fun createParentScopes(
            previous: BoundGlobalScope?
        ): BoundScope {

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
                    v as VisibleSymbol
                    if (v.meta.isEntry) { //path.substringBeforeLast('.') == "main"
                        scope.tryDeclare(v)
                    }
                }
                parent = scope
            }

            return parent
        }

        private fun createRootScope(): BoundScope {
            val result = BoundScope(null)
            if (isRepl) {
                for (fn in BuiltinFunctions.getAll()) {
                    result.tryDeclare(fn)
                }
            }
            return result
        }

        fun bindFunction(
            parentScope: BoundScope,
            functions: HashMap<FunctionSymbol, BoundBlockStatement?>,
            symbol: FunctionSymbol,
            diagnostics: DiagnosticList,
            functionBodies: HashMap<FunctionSymbol, BoundBlockStatement?>?
        ) {
            val binder = Binder(parentScope, symbol, functions, functionBodies)
            when {
                symbol.meta.isExtern -> functions[symbol] = null
                symbol.declarationNode!!.lambdaArrow == null -> {
                    val body = binder.bindBlockStatement(symbol.declarationNode.body as BlockStatementNode)
                    val loweredBody = Lowerer.lower(body)
                    if (symbol.type != TypeSymbol.Unit && !ControlFlowGraph.allPathsReturn(loweredBody)) {
                        diagnostics.reportAllPathsMustReturn(symbol.declarationNode.identifier.location)
                    }
                    functions[symbol] = loweredBody
                    functionBodies?.put(symbol, body)
                }
                else -> {
                    val body = binder.bindExpressionStatement(symbol.declarationNode.body as ExpressionStatementNode)
                    val loweredBody = Lowerer.lower(body.expression)
                    functions[symbol] = loweredBody
                    functionBodies?.put(symbol, loweredBody)
                }
            }
            diagnostics.append(binder.diagnostics)
        }

        fun bindProgram(
            previous: BoundProgram?,
            globalScope: BoundGlobalScope
        ): BoundProgram {

            val parentScope = createParentScopes(globalScope)
            val functions = HashMap<FunctionSymbol, BoundBlockStatement?>()
            val diagnostics = DiagnosticList()

            val functionBodies = if (isSharedLib) {
                val tmp = HashMap<FunctionSymbol, BoundBlockStatement?>()
                for (symbol in globalScope.symbols) {
                    if (symbol is FunctionSymbol) {
                        bindFunction(BoundScope(symbol.namespace!!), functions, symbol, diagnostics, tmp)
                    }
                }
                tmp
            } else {
                for (symbol in globalScope.symbols) {
                    if (symbol is FunctionSymbol) {
                        bindFunction(BoundScope(symbol.namespace!!), functions, symbol, diagnostics, null)
                    }
                }
                null
            }

            val statement: BoundBlockStatement
            if (isRepl) {
                val statements = globalScope.statements
                if (statements.size == 1) {
                    val s = statements[0]
                    if (s is BoundExpressionStatement &&
                        s.expression.type != TypeSymbol.Unit) {
                        statements[0] = BoundReturnStatement(s.expression)
                    }
                }
                else if (statements.size != 0){
                    val nullValue = BoundLiteralExpression("", TypeSymbol.String)
                    statements[0] = BoundReturnStatement(nullValue)
                }
                val body = Lowerer.lower(BoundBlockStatement(
                    globalScope.statements
                ))
                functions[globalScope.mainFn] = body
                statement = BoundBlockStatement(listOf())
            }
            else {
                statement = Lowerer.lower(BoundBlockStatement(
                    globalScope.statements
                ))
            }

            return BoundProgram(
                previous,
                diagnostics,
                globalScope.mainFn,
                functions,
                statement,
                functionBodies)
        }
    }

    private fun bindFunctionDeclaration(node: FunctionDeclarationNode): FunctionSymbol {

        val meta = Symbol.MetaData()
        val params = ArrayList<VariableSymbol>()
        val seenParameterNames = HashSet<String>()

        if (node.extensionType != null) {
            seenParameterNames.add("this")
            params.add(VariableSymbol.param("this", bindTypeClause(node.extensionType)!!))
            meta.isExtension = true
        }

        if (node.params != null) {
            for (paramNode in node.params.iterator()) {
                val name = paramNode.identifier.string!!
                if (!seenParameterNames.add(name)) {
                    diagnostics.reportParamAlreadyExists(paramNode.identifier.location, name)
                } else {
                    val type = bindTypeClause(paramNode.typeClause)!!
                    val parameter = VariableSymbol.param(name, type)
                    params.add(parameter)
                }
            }
        }

        val type: TypeSymbol = if (node.typeClause == null) {
            TypeSymbol.Unit
        } else {
            bindTypeClause(node.typeClause)!!
        }

        val path = if (scope is BoundNamespace) {
            (scope as BoundNamespace).path + '.' + node.identifier.string!!
        } else {
            function!!.mangledName().substringBeforeLast('.') + '.' + Symbol.genFnUID()
        }
        val function = FunctionSymbol(node.identifier.string!!, params.toTypedArray(), type, path, node, meta)
        for (annotation in node.annotations) {
            when (annotation.identifier.string) {
                "inline" -> meta.isInline = true
                "internal" -> meta.isInternal = true
                "extern" -> meta.isExtern = true
                "operator" -> {
                    if (function.meta.isExtension) {
                        function.meta.isOperator = true
                        when (function.name) {
                            "equals" -> {
                                if (function.parameters.size != 2) {
                                    diagnostics.reportInvalidAnnotation(annotation.location)
                                }
                            }
                            "plus" -> {
                                if (function.parameters.size != 2 && function.parameters.size != 1) {
                                    diagnostics.reportInvalidAnnotation(annotation.location)
                                }
                            }
                            "minus" -> {
                                if (function.parameters.size != 2 && function.parameters.size != 1) {
                                    diagnostics.reportInvalidAnnotation(annotation.location)
                                }
                            }
                            "times" -> {
                                if (function.parameters.size != 2) {
                                    diagnostics.reportInvalidAnnotation(annotation.location)
                                }
                            }
                            "divide" -> {
                                if (function.parameters.size != 2) {
                                    diagnostics.reportInvalidAnnotation(annotation.location)
                                }
                            }
                            "rem" -> {
                                if (function.parameters.size != 2) {
                                    diagnostics.reportInvalidAnnotation(annotation.location)
                                }
                            }
                            "and" -> {
                                if (function.parameters.size != 2) {
                                    diagnostics.reportInvalidAnnotation(annotation.location)
                                }
                            }
                            "or" -> {
                                if (function.parameters.size != 2) {
                                    diagnostics.reportInvalidAnnotation(annotation.location)
                                }
                            }
                            "not" -> {
                                if (function.parameters.size != 1) {
                                    diagnostics.reportInvalidAnnotation(annotation.location)
                                }
                            }
                            else -> {
                                diagnostics.reportInvalidAnnotation(annotation.location)
                            }
                        }
                    } else {
                        diagnostics.reportInvalidAnnotation(annotation.location)
                    }
                }
                "entry" -> {
                    if (Symbol.MetaData.entryExists) {
                        diagnostics.reportMultipleEntryFuncs(annotation.location)
                    }
                    meta.isEntry = true
                    Symbol.MetaData.entryExists = true
                }
                "cname" -> {
                    val expression = bindLiteralExpression(annotation.value as LiteralExpressionNode, TypeSymbol.String)
                    meta.cname = expression.value as String
                }
                else -> {
                    diagnostics.reportInvalidAnnotation(annotation.location)
                }
            }
        }

        if (!scope.tryDeclare(function)) {
            diagnostics.reportSymbolAlreadyDeclared(node.identifier.location, function.name)
        }

        return function
    }
}