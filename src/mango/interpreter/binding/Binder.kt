package mango.interpreter.binding

import mango.compilation.DiagnosticList
import mango.interpreter.text.TextSpan
import mango.interpreter.lowering.Lowerer
import mango.interpreter.symbols.*
import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Token
import mango.interpreter.syntax.parser.*
import mango.interpreter.text.TextLocation
import mango.isRepl
import java.util.*
import kotlin.Exception
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class Binder(
    var scope: BoundScope,
    val function: FunctionSymbol?,
    val functions: HashMap<FunctionSymbol, BoundBlockStatement?>
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
        val condition = bindExpression(node.condition, TypeSymbol.bool)
        val (body, breakLabel, continueLabel) = bindLoopBody(node.body)
        return BoundWhileStatement(condition, body, breakLabel, continueLabel)
    }

    private fun bindForStatement(node: ForStatementNode): BoundForStatement {
        val lowerBound = bindExpression(node.lowerBound, TypeSymbol.int)
        val upperBound = bindExpression(node.upperBound, TypeSymbol.int)

        val previous = scope
        scope = BoundScope(scope)
        val variable = bindVariable(node.identifier, TypeSymbol.int, true, null)
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
        else if (function.type == TypeSymbol.unit) {
            if (expression != null) {
                diagnostics.reportCantReturnInUnitFunction(node.expression.location)
            }
        }
        else {
            if (expression == null) {
                diagnostics.reportCantReturnWithoutValue(node.keyword.location)
            }
            else if (!expression.type.isOfType(function.type)) {
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
                        bindFunction(scope, functions, symbol, diagnostics)
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
        if (!initializer.type.isOfType(actualType) && initializer.type != TypeSymbol.error) {
            diagnostics.reportWrongType(node.initializer.location, initializer, type!!)
        }
        val variable = bindVariable(node.identifier, actualType, isReadOnly, initializer.constantValue)
        return BoundVariableDeclaration(variable, initializer)
    }

    private fun bindTypeClause(node: TypeClauseNode?): TypeSymbol? {
        if (node == null) {
            return null
        }
        val type = TypeSymbol.lookup(node.identifier.string!!)
        if (type == null) {
            diagnostics.reportUndefinedType(node.identifier.location, node.identifier.string)
        }
        return type
    }

    private fun bindVariable(identifier: Token, type: TypeSymbol, isReadOnly: Boolean, constant: BoundConstant?): VariableSymbol {
        val name = identifier.string ?: "?"
        val variable: VariableSymbol
        variable = if (function == null && scope is BoundNamespace) {
            VisibleVariableSymbol(name, type, isReadOnly, constant, (scope as BoundNamespace).path + '.' + name)
        } else {
            LocalVariableSymbol(name, type, isReadOnly, constant)
        }
        if (!identifier.isMissing && !scope.tryDeclare(variable)) {
            diagnostics.reportSymbolAlreadyDeclared(identifier.location, name)
        }
        return variable
    }

    fun bindUseStatement(node: UseStatementNode) {
        val path = node.directories.joinToString(separator = ".") { it.string!! }
        if (BoundNamespace.namespaces[path] == null) {
            //println(path)
            diagnostics.reportIncorrectUseStatement(node.location)
        } else {
            scope.use(BoundUse(path, node.isInclude))
        }
    }

    private fun bindExpression(node: ExpressionNode, canBeUnit: Boolean = false): BoundExpression {
        val result = bindExpressionInternal(node)
        if (!canBeUnit && result.type == TypeSymbol.unit) {
            diagnostics.reportExpressionMustHaveValue(node.location)
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
            diagnostics.reportWrongType(node.location, result, type)
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

        val (function, result) = scope.tryLookupFunction(listOf(node.identifier.string))

        if (!result) {
            diagnostics.reportUndefinedName(node.identifier.location, node.identifier.string)
            return BoundErrorExpression()
        }

        if (node.arguments.nodeCount > function!!.parameters.size) {
            val firstExceedingNode = if (function.parameters.isNotEmpty()) {
                node.arguments.getSeparator(function.parameters.size - 1)
            } else {
                node.arguments[0]
            }
            val span = TextSpan.fromBounds(firstExceedingNode.span.start, node.arguments.last().span.end)
            val location = TextLocation(firstExceedingNode.syntaxTree.sourceText, span)
            diagnostics.reportWrongArgumentCount(location, function.name, node.arguments.nodeCount, function.parameters.size)
            return  BoundErrorExpression()
        } else if (node.arguments.nodeCount < function.parameters.size) {
            diagnostics.reportWrongArgumentCount(node.rightBracket.location, function.name, node.arguments.nodeCount, function.parameters.size)
            return  BoundErrorExpression()
        }

        for (i in arguments.indices) {
            val arg = arguments[i]
            val param = function.parameters[i]

            if (!arg.type.isOfType(param.type)) {
                if (arg.type != TypeSymbol.error) {
                    diagnostics.reportWrongArgumentType(node.arguments[i].location, param.name, arg.type, param.type)
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
            diagnostics.reportCantCast(node.location, expression.type, type)
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
        val (variable, result) = scope.tryLookupVariable(listOf(name))
        if (!result) {
            diagnostics.reportUndefinedName(node.identifierToken.location, name)
            return boundExpression
        }
        if (variable!!.isReadOnly) {
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
        val name = node.identifierToken.string ?: return BoundErrorExpression()
        val (variable, result) = scope.tryLookupVariable(listOf(name))
        if (!result) {
            diagnostics.reportUndefinedName(node.identifierToken.location, name)
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
            diagnostics.reportUnaryOperator(node.operator.location, node.operator.kind, operand.type)
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
            diagnostics.reportBinaryOperator(node.operator.location, left.type, node.operator.kind, right.type)
            return BoundErrorExpression()
        }
        return BoundBinaryExpression(left, operator, right)
    }

    companion object {

        fun bindGlobalScope(
            previous: BoundGlobalScope?,
            syntaxTrees: Collection<SyntaxTree>
        ): BoundGlobalScope {

            val parentScope = createParentScopes(previous)
            val binder = Binder(parentScope, null, HashMap())

            val symbols = ArrayList<Symbol>()
            val statementBuilder = ArrayList<BoundStatement>()

            for (syntaxTree in syntaxTrees) {
                BoundNamespace(syntaxTree.projectPath, parentScope)
            }
            for (syntaxTree in syntaxTrees) {
                binder.diagnostics.append(syntaxTree.diagnostics)
                val prev = binder.scope
                val namespace = BoundNamespace[syntaxTree.projectPath]
                binder.scope = namespace
                //println("namespace: " + namespace.path)
                for (s in syntaxTree.root.members) {
                    when (s) {
                        is FunctionDeclarationNode -> {
                            symbols.add(binder.bindFunctionDeclaration(s))
                        }
                        is VariableDeclarationNode -> {
                            val statement = binder.bindVariableDeclaration(s)
                            statementBuilder.add(statement)
                            symbols.add(statement.variable)
                        }
                        is UseStatementNode -> {
                            binder.bindUseStatement(s)
                        }
                        is ReplStatementNode -> {
                            val statement = binder.bindStatement(s.statementNode)
                            statementBuilder.add(statement)
                        }
                    }
                }
                binder.scope = prev
            }

            var entryFn = symbols.find {
                it.kind == Symbol.Kind.Function &&
                (it as FunctionSymbol).meta.isEntry &&
                it.type == TypeSymbol.unit &&
                it.parameters.isEmpty()
            } as FunctionSymbol?

            val diagnostics = binder.diagnostics

            if (entryFn == null) {
                entryFn = FunctionSymbol(
                    "main",
                    arrayOf(),
                    if (isRepl) TypeSymbol.any else TypeSymbol.unit,
                        "main",
                    null,
                        FunctionSymbol.MetaData()
                )
                if (!isRepl) {
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
                    if (v.path.substringBeforeLast('.') == "main") {
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
            diagnostics: DiagnosticList
        ) {
            val binder = Binder(parentScope, symbol, functions)
            when {
                symbol.meta.isExtern -> functions[symbol] = null
                symbol.declarationNode!!.lambdaArrow == null -> {
                    val body = binder.bindBlockStatement(symbol.declarationNode.body as BlockStatementNode)
                    val loweredBody = Lowerer.lower(body)
                    if (symbol.type != TypeSymbol.unit && !ControlFlowGraph.allPathsReturn(loweredBody)) {
                        diagnostics.reportAllPathsMustReturn(symbol.declarationNode.identifier.location)
                    }
                    functions[symbol] = loweredBody
                }
                else -> {
                    val body = binder.bindExpressionStatement(symbol.declarationNode.body as ExpressionStatementNode)
                    val loweredBody = Lowerer.lower(body.expression)
                    functions[symbol] = loweredBody
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

            for (symbol in globalScope.symbols) {
                if (symbol is FunctionSymbol) {
                    bindFunction(BoundNamespace[symbol.path.substringBeforeLast('.')], functions, symbol, diagnostics)
                }
            }

            val statement: BoundBlockStatement
            if (isRepl) {
                val statements = globalScope.statements
                if (statements.size == 1) {
                    val s = statements[0]
                    if (s is BoundExpressionStatement &&
                        s.expression.type != TypeSymbol.unit) {
                        statements[0] = BoundReturnStatement(s.expression)
                    }
                }
                else if (statements.size != 0){
                    val nullValue = BoundLiteralExpression("")
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
                statement)
        }
    }

    private fun bindFunctionDeclaration(node: FunctionDeclarationNode): FunctionSymbol {
        val params = ArrayList<ParameterSymbol>()

        val seenParameterNames = HashSet<String>()

        if (node.params != null) {
            for (paramNode in node.params.iterator()) {
                val name = paramNode.identifier.string!!
                if (!seenParameterNames.add(name)) {
                    diagnostics.reportParamAlreadyExists(paramNode.identifier.location, name)
                } else {
                    val type = bindTypeClause(paramNode.typeClause)!!
                    val parameter = ParameterSymbol(name, type)
                    params.add(parameter)
                }
            }
        }

        val type: TypeSymbol = if (node.typeClause == null) {
            TypeSymbol.unit
        } else {
            bindTypeClause(node.typeClause)!!
        }

        val meta = FunctionSymbol.MetaData()
        val path = if (scope is BoundNamespace) {
            (scope as BoundNamespace).path + '.' + node.identifier.string!!
        } else {
            function!!.path.substringBeforeLast('.') + '.' + Symbol.genFnUID()
        }
        val function = FunctionSymbol(node.identifier.string!!, params.toTypedArray(), type, path, node, meta)
        for (annotation in node.annotations) {
            when (annotation.identifier.string) {
                "inline" -> meta.isInline = true
                "extern" -> meta.isExtern = true
                "entry" -> {
                    if (FunctionSymbol.MetaData.entryExists) {
                        diagnostics.reportMultipleEntryFuncs(annotation.location)
                    }
                    meta.isEntry = true
                    FunctionSymbol.MetaData.entryExists = true
                }
                "cname" -> {
                    val expression = bindLiteralExpression(annotation.value as LiteralExpressionNode)
                    meta.cName = expression.value as String
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