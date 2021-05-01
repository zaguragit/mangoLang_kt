package mango.compiler.binding

import mango.compiler.*
import mango.compiler.binding.nodes.BiOperator
import mango.compiler.binding.nodes.BoundNode
import mango.compiler.binding.nodes.UnOperator
import mango.compiler.binding.nodes.expressions.*
import mango.compiler.binding.nodes.statements.*
import mango.compiler.ir.ControlFlowGraph
import mango.compiler.ir.Label
import mango.compiler.ir.Lowerer
import mango.compiler.symbols.CallableSymbol
import mango.compiler.symbols.Symbol
import mango.compiler.symbols.TypeSymbol
import mango.compiler.symbols.VariableSymbol
import mango.parser.SyntaxTree
import mango.parser.SyntaxType
import mango.parser.Token
import mango.parser.Translator
import mango.parser.nodes.*
import shared.DiagnosticList
import shared.text.TextLocation
import shared.text.TextSpan
import java.util.*

class Binder(
    var scope: Scope,
    val function: CallableSymbol?,
    val functions: HashMap<CallableSymbol, BlockExpression?>,
    val functionBodies: HashMap<CallableSymbol, Statement?>,
    val symbolsToBind: ArrayList<Symbol>?
) {

    var isUnsafe = false
    val diagnostics = DiagnosticList()
    private val loopStack = Stack<Pair<Label, Label>>()
    private var loopCount = 0

    inline fun <T> inScope(scope: Scope, block: () -> T): T {
        val tmp = this.scope
        this.scope = scope
        val r = block()
        this.scope = tmp
        return r
    }

    init {
        if (function != null) {
            for (p in function.parameters) {
                scope.tryDeclare(p)
            }
        }
    }

    private fun bindStatement(node: Node, isActuallyExpression: Boolean = false): Statement {
        return when (node.kind) {
            SyntaxType.ExpressionStatement -> bindExpressionStatement(node as ExpressionStatementNode, isActuallyExpression)
            SyntaxType.ValVarDeclaration -> bindValVarDeclaration(node as ValVarDeclarationNode)
            SyntaxType.LoopStatement -> bindLoopStatement(node as LoopStatementNode)
            SyntaxType.IterationLoopStatement -> bindIterationLoopStatement(node as IterationLoopStatementNode)
            SyntaxType.BreakStatement -> bindBreakStatement(node as BreakNode)
            SyntaxType.ContinueStatement -> bindContinueStatement(node as ContinueNode)
            SyntaxType.ReturnStatement -> bindReturnStatement(node as ReturnStatementNode)
            SyntaxType.AssignmentStatement -> bindAssignmentStatement(node as AssignmentNode)
            else -> throw BinderError("Unexpected node: ${node.kind}")
        }
    }

    private fun bindErrorStatement() = ExpressionStatement(ErrorExpression())

    private fun bindIfExpression(node: IfNode): IfExpression {
        val condition = bindExpression(node.condition, type = TypeSymbol.Bool)
        val then = run {
            val t = bindStatement(node.thenExpression, isActuallyExpression = true)
            if (t is ExpressionStatement) t.expression else BlockExpression(listOf(t), TypeSymbol.Void)
        }
        val elseExpression = node.elseClause?.let {
            val t = bindStatement(it.expression, isActuallyExpression = true)
            if (t is ExpressionStatement) t.expression else BlockExpression(listOf(t), TypeSymbol.Void)
        }
        return IfExpression(condition, then, elseExpression)
    }

    private fun bindLambdaExpression(node: LambdaNode, typeHint: TypeSymbol?, nameHint: String?, extensionParam: VariableSymbol?): Lambda {

        val meta = Symbol.MetaData()
        val params = ArrayList<VariableSymbol>()
        val seenParameterNames = HashSet<String>()

        if (extensionParam != null) {
            seenParameterNames.add(extensionParam.name)
            params.add(extensionParam)
            meta.isExtension = true
        }

        for (paramNode in node.params.iterator()) {
            val name = paramNode.identifier.string ?: continue
            if (!seenParameterNames.add(name)) {
                diagnostics.reportParamAlreadyExists(paramNode.identifier.location, name)
            } else {
                val type = bindTypeClause(paramNode.typeClause)
                val parameter = VariableSymbol.param(name, type)
                params.add(parameter)
            }
        }

        val type: TypeSymbol = if (node.returnType == null) {
            TypeSymbol.Void
        } else {
            bindTypeClause(node.returnType!!)
        }

        val path = if (scope is Namespace && nameHint != null) {
            (scope as Namespace).path + '.' + nameHint
        } else {
            Symbol.genFnUID()
        }

        if (node.arrow == null) {
            meta.isExtern = true
            if (node.annotations.find { it.getIdentifierString() == "cname" } == null) {
                diagnostics.reportExternFnRequireCName(node.location)
            }
        }

        val function = CallableSymbol(nameHint ?: path,
            params.toTypedArray(),
            TypeSymbol.Fn(type, params.map { it.type }),
            path, node.declarationNode, node, meta)

        if (!scope.tryDeclare(function)) {
            throw BinderError("Lambda path already used ($path)")
        }

        val lambda = Lambda(function)

        bindFunctionAnnotations(node.annotations, lambda)

        if (symbolsToBind == null) {
            bindFunction(scope, functions, function, diagnostics, functionBodies)
        } else {
            symbolsToBind.add(function)
        }

        return lambda
    }

    private fun bindLoopStatement(node: LoopStatementNode): LoopStatement {
        val (body, breakLabel, continueLabel) = bindLoopBody(node.body)
        return LoopStatement(body, breakLabel, continueLabel)
    }

    private fun bindIterationLoopStatement(node: IterationLoopStatementNode): ForStatement {
        val lowerBound = bindExpression(node.lowerBound, type = TypeSymbol.Int)
        val upperBound = bindExpression(node.upperBound, type = TypeSymbol.Int)

        val (variable, a) = inScope(Scope(scope)) {
            val variable = bindVariable(node.identifier, TypeSymbol.Int, true, null)
            variable to bindLoopBody(node.body)
        }
        val (body, breakLabel, continueLabel) = a

        return ForStatement(variable, lowerBound, upperBound, body, breakLabel, continueLabel)
    }

    private fun bindLoopBody(node: Node): Triple<Statement, Label, Label> {
        val numStr = loopCount++.toString(16)
        val breakLabel = Label("Break$numStr")
        val continueLabel = Label("Continue$numStr")
        loopStack.push(breakLabel to continueLabel)
        val body = bindStatement(node)
        loopStack.pop()
        return Triple(body, breakLabel, continueLabel)
    }

    private fun bindBreakStatement(node: BreakNode): Statement {
        if (loopStack.count() == 0) {
            diagnostics.reportBreakContinueOutsideLoop(node.keyword.location, node.keyword.string!!)
            return bindErrorStatement()
        }
        val breakLabel = loopStack.peek().first
        return Goto(breakLabel, Goto.Type.Break)
    }

    private fun bindContinueStatement(node: ContinueNode): Statement {
        if (loopStack.count() == 0) {
            diagnostics.reportBreakContinueOutsideLoop(node.keyword.location, node.keyword.string!!)
            return bindErrorStatement()
        }
        val continueLabel = loopStack.peek().second
        return Goto(continueLabel, Goto.Type.Continue)
    }

    private fun bindReturnStatement(node: ReturnStatementNode): Statement {
        val expression = node.expression?.let { bindExpression(it) }
        if (function == null) {
            diagnostics.reportReturnOutsideFunction(node.keyword.location)
        }
        else if (function.returnType == TypeSymbol.Void) {
            if (expression != null) {
                diagnostics.reportCantReturnInUnitFunction(node.expression!!.location)
            }
        }
        else {
            if (expression == null) {
                diagnostics.reportCantReturnWithoutValue(node.keyword.location)
            }
            else typeCheck(expression, function.returnType, node.expression!!.location)
        }
        return ReturnStatement(expression)
    }

    /**
     * Returns true if there's an error
     */
    inline fun typeCheck(expression: Expression, expectedType: TypeSymbol, location: TextLocation): Boolean =
        typeCheck(expression.type, expectedType, location)

    fun typeCheck(type: TypeSymbol, expectedType: TypeSymbol, location: TextLocation): Boolean {
        if (!type.isOfType(expectedType)) {
            if (type != TypeSymbol.err) {
                diagnostics.reportWrongType(location, type, expectedType)
            }
            return true
        }
        return false
    }

    private fun bindExpressionStatement(node: ExpressionStatementNode, isActuallyExpression: Boolean): Statement {
        when (node.expression.kind) {
            SyntaxType.Block -> {
                return ExpressionStatement(bindBlock(
                    node.expression as BlockNode,
                    isExpression = isActuallyExpression,
                    canBeUnit = true,
                    isUnsafe = (node.expression as BlockNode).isUnsafe
                ))
            }
            else -> {
                val expression = bindExpression(node.expression, canBeUnit = true)
                if (function != null) {
                    val kind = expression.kind
                    val isAllowedExpression = isActuallyExpression ||
                        kind == BoundNode.Kind.BlockExpression ||
                        kind == BoundNode.Kind.CallExpression ||
                        kind == BoundNode.Kind.IfExpression ||
                        kind == BoundNode.Kind.ErrorExpression
                    if (!isAllowedExpression) {
                        diagnostics.reportInvalidExpressionStatement(node.location)
                        return bindErrorStatement()
                    }
                }
                return ExpressionStatement(expression)
            }
        }
    }

    private fun bindValVarDeclaration(node: ValVarDeclarationNode): ValVarDeclaration {
        val isReadOnly = node.keyword.kind == SyntaxType.Val

        val type = node.typeClauseNode?.let { bindTypeClause(it) }
        if (node.initializer == null) {
            val variable = bindVariable(node.identifier, type ?: TypeSymbol.Any, isReadOnly, null)
            diagnostics.reportHasToBeInitialized(node.identifier.location)
            return ValVarDeclaration(variable, ErrorExpression())
        }

        val isLambda = node.initializer!!.kind == SyntaxType.LambdaExpression

        val initializer = if (isLambda) {
            val extensionType = node.extensionParam?.let { VariableSymbol.param(it.identifier.string!!, bindTypeClause(it.typeClause)) }
            bindLambdaExpression(node.initializer as LambdaNode, type, node.identifier.string!!, extensionType)
        } else bindExpression(node.initializer!!, type = type)

        val actualType = type ?: initializer.type
        typeCheck(initializer, actualType, node.initializer!!.location)

        val variable = if (isLambda) {
            val l = initializer as Lambda
            l.symbol
        } else bindVariable(node.identifier, actualType, isReadOnly, initializer.constantValue)

        return ValVarDeclaration(variable, initializer)
    }

    private fun bindTypeClause(node: TypeClauseNode): TypeSymbol {
        val path = node.identifier.map { it.string!! }
        val type = scope.tryLookupType(path)
        if (type == null) {
            diagnostics.reportUndefinedType(node.location, path.joinToString("."))
            return TypeSymbol.err
        }
        val typeParams = node.types
        if (typeParams != null) {
            if (node.start?.kind == SyntaxType.LessThan) {
                if (type.paramCount != typeParams.nodeCount) {
                    diagnostics.reportWrongArgumentCount(node.start!!.location, typeParams.nodeCount, type.paramCount)
                }
                return type(Array(type.paramCount) {
                    val t = bindTypeClause(typeParams[it])
                    if (typeCheck(t, type.params[it], typeParams[it].location)) {
                        return TypeSymbol.err
                    }
                    t
                })
            } else {
                return TypeSymbol.Fn(type, typeParams.map { bindTypeClause(it) })
            }
        }
        return type
    }

    private fun bindVariable(identifier: Token, type: TypeSymbol, isReadOnly: Boolean, constant: BoundConstant?): VariableSymbol {
        val name = identifier.string ?: "?"
        val variable = if (function == null && scope is Namespace) {
            VariableSymbol.visible(name, type, isReadOnly, constant, (scope as Namespace).path + '.' + name)
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
        if (Namespace[path] == null) {
            val p = node.textFile.projectPath.substringBefore('.') + '.' + path
            if (Namespace[p] == null) {
                diagnostics.reportIncorrectUseStatement(node.location)
            } else scope.use(UseStatement(p, node.isInclude))
        } else {
            scope.use(UseStatement(path, node.isInclude))
        }
    }

    private fun bindExpression(node: Node, canBeUnit: Boolean = false, type: TypeSymbol? = null): Expression {
        val result = bindExpressionInternal(node, type)

        if (!canBeUnit && result.type == TypeSymbol.Void) {
            diagnostics.reportExpressionMustHaveValue(node.location)
            return ErrorExpression()
        }

        if (type != null && type != TypeSymbol.err) {
            typeCheck(result, type, node.location)
        }
        return result
    }

    private fun bindExpressionInternal(node: Node, typeHint: TypeSymbol?): Expression {
        return when (node.kind) {
            SyntaxType.LiteralExpression -> bindLiteralExpression(node as LiteralExpressionNode, typeHint)
            SyntaxType.BoolConst -> bindBoolConst(node as BoolConstantNode, typeHint)
            SyntaxType.TextConst -> bindTextConst(node as TextConstantNode, typeHint)
            SyntaxType.UnaryExpression -> bindUnaryExpression(node as UnaryExpressionNode)
            SyntaxType.BinaryExpression -> {
                node as BinaryExpressionNode
                if (node.operator.kind == SyntaxType.Dot) {
                    bindDotAccessExpression(node).first
                } else {
                    bindBinaryExpression(node)
                }
            }
            SyntaxType.NameExpression -> bindNameExpression(node as NameExpressionNode)
            SyntaxType.CallExpression -> bindCallExpression(node as CallExpressionNode)
            SyntaxType.IndexExpression -> bindIndexExpression(node as IndexExpressionNode)
            SyntaxType.Block -> bindBlock(
                node as BlockNode,
                isExpression = true,
                canBeUnit = false,
                isUnsafe = node.isUnsafe
            )
            SyntaxType.StructInitialization -> bindStructInitialization(node as StructInitializationNode)
            SyntaxType.CollectionInitialization -> bindCollectionInitialization(node as CollectionInitializationNode)
            SyntaxType.CastExpression -> bindCastExpression(node as CastExpressionNode)
            SyntaxType.IfExpression -> bindIfExpression(node as IfNode)
            SyntaxType.LambdaExpression -> bindLambdaExpression(node as LambdaNode, typeHint, null, null)
            else -> throw BinderError("Unexpected node: ${node.kind}, ${node.location}")
        }
    }

    private fun bindCallExpression(node: CallExpressionNode): Expression {

        val arguments = ArrayList<Expression>()

        for (a in node.arguments) {
            val arg = bindExpression(a)
            if (arg.isError()) {
                return ErrorExpression()
            }
            arguments.add(arg)
        }

        var isExtensionFunction = false

        val function = if (node.function.kind == SyntaxType.NameExpression) {
            val types = arguments.map { it.type }
            val name = node.function as NameExpressionNode
            val symbol = scope.tryLookup(listOf(name.identifier.string!!), CallableSymbol.Info(types, false))
            if (symbol != null) {
                symbol as VariableSymbol
                NameExpression(symbol)
            } else {
                val symbol2 = scope.tryLookup(listOf(name.identifier.string!!))
                if (symbol2 != null) {
                    symbol2 as VariableSymbol
                    NameExpression(symbol2)
                } else {
                    diagnostics.reportUndefinedFunction(name.location, name.identifier.string!!, types, false)
                    return ErrorExpression()
                }
            }
        } else if (node.function.kind == SyntaxType.BinaryExpression && (node.function as BinaryExpressionNode).operator.kind == SyntaxType.Dot) {
            val types = arguments.map { it.type }
            val (f, firstArg) = bindDotAccessExpression(node.function as BinaryExpressionNode, types)
            firstArg?.let {
                isExtensionFunction = true
                arguments.add(0, it)
            }
            f
        } else {
            bindExpression(node.function)
        }
        if (function.isError()) {
            return ErrorExpression()
        }
        if (function.type.kind != Symbol.Kind.FunctionType) {
            diagnostics.reportNotCallable(node.function.location, function.type)
            return ErrorExpression()
        }
        val functionType = function.type as TypeSymbol.Fn

        if (arguments.size > functionType.args.size) {
            val firstExceedingNode = if (functionType.args.isNotEmpty()) {
                node.arguments.getSeparator(functionType.args.size - 1)
            } else {
                node.arguments[0]
            }
            val span = TextSpan.fromBounds(firstExceedingNode.span.start, node.arguments.last().span.end)
            val location = TextLocation(firstExceedingNode.textFile.sourceText, span)
            diagnostics.reportWrongArgumentCount(location, node.arguments.nodeCount, functionType.args.size)
            return ErrorExpression()
        } else if (arguments.size < functionType.args.size) {
            diagnostics.reportWrongArgumentCount(node.rightBracket.location, node.arguments.nodeCount, functionType.args.size)
            return ErrorExpression()
        }

        for (i in arguments.indices) {
            val arg = arguments[i]
            val param = functionType.args.elementAt(i)

            if (!arg.type.isOfType(param)) {
                if (arg.isNotError()) {
                    diagnostics.reportWrongArgumentType(node.arguments[i].location, param.path, arg.type, param)
                }
                return ErrorExpression()
            }
        }

        return CallExpression(function, arguments, isExtensionFunction)
    }

    private fun bindIndexExpression(node: IndexExpressionNode): Expression {
        val expression = bindExpression(node.expression)

        if (expression.isError()) {
            return ErrorExpression()
        }

        val list = ArrayList<Expression>()
        list.add(expression)
        for (it in node.arguments) {
            list.add(bindExpression(it))
        }

        if (expression.type.isOfType(TypeSymbol.Ptr) && list.size == 2 && list[1].type.isOfType(TypeSymbol.Integer)) {
            if (!isUnsafe) {
                diagnostics.reportPointerOperationsAreUnsafe(node.location)
                return ErrorExpression()
            }
            return PointerAccess(expression, list[1])
        }

        val operatorFunction = scope.tryLookup(
            listOf("get"),
            CallableSymbol.Info(list.map { it.type }, true)
        )

        if (operatorFunction == null) {
            diagnostics.reportUndefinedFunction(node.location, "get", list.map { it.type }, true)
            return ErrorExpression()
        }

        if (!operatorFunction.meta.isOperator) {
            diagnostics.reportNotOperator(node.location, operatorFunction)
            return ErrorExpression()
        }

        operatorFunction as CallableSymbol
        return CallExpression(NameExpression(operatorFunction), list)
    }

    private fun bindBlock(node: BlockNode, isExpression: Boolean, canBeUnit: Boolean, isUnsafe: Boolean = false): BlockExpression {
        val statements = ArrayList<Statement>()
        val type = inScope(Scope(scope)) {
            var type = TypeSymbol.Void
            val isLastUnsafe = this.isUnsafe
            this.isUnsafe = this.isUnsafe || isUnsafe
            for (i in node.statements.indices) {
                val s = node.statements.elementAt(i)
                if (s.kind == SyntaxType.UseStatement) bindUseStatement(s as UseStatementNode)
                else if (isExpression && i == node.statements.size - 1 && s.kind == SyntaxType.ExpressionStatement) {
                    s as ExpressionStatementNode
                    val expression = bindExpression(s.expression, canBeUnit)
                    type = expression.type
                    statements.add(ExpressionStatement(expression))
                } else {
                    val statement = bindStatement(s)
                    statements.add(statement)
                }
            }
            this.isUnsafe = isLastUnsafe
            type
        }
        return if (isExpression) {
            BlockExpression(statements, type, isUnsafe = isUnsafe)
        } else {
            BlockExpression(statements, TypeSymbol.Void, isUnsafe = isUnsafe)
        }
    }

    private fun bindStructInitialization(node: StructInitializationNode): Expression {
        val type = bindTypeClause(node.type)
        if (type == TypeSymbol.err) {
            return ErrorExpression()
        }
        if (type.kind != Symbol.Kind.StructType) {
            if (type.isOfType(TypeSymbol.Ptr)) {
                var length: Expression? = null
                for (param in node.params) {
                    val name = (param.assignee as Token).string!!
                    when (name) {
                        "length" -> {
                            val expression = bindExpression(param.expression)
                            if (typeCheck(expression, TypeSymbol.I32, param.expression.location)) {
                                return ErrorExpression()
                            }
                            length = expression
                        }
                        else -> {
                            diagnostics.reportNotValidField(param.assignee.location, name)
                            return ErrorExpression()
                        }
                    }
                }
                return PointerArrayInitialization(type, length ?: LiteralExpression(0, TypeSymbol.I32), null)
            }
            diagnostics.reportTypeNotStruct(node.type.location, node.type.identifier.joinToString(".") { it.string!! })
            return ErrorExpression()
        }
        type as TypeSymbol.StructTypeSymbol
        return StructInitialization(type, HashMap<TypeSymbol.StructTypeSymbol.Field, Expression>().apply {
            node.params.forEach { param ->
                val name = (param.assignee as Token).string!!
                val field = type.getField(name)
                if (field == null) {
                    diagnostics.reportNoSuchField(param.assignee.location, type, name)
                    return ErrorExpression()
                }
                val expression = bindExpression(param.expression)
                if (typeCheck(expression, field.type, param.expression.location)) {
                    return ErrorExpression()
                }
                this[field] = expression
            }
        })
    }

    private fun bindCollectionInitialization(node: CollectionInitializationNode): Expression {
        val type = bindTypeClause(node.type)
        if (type == TypeSymbol.err) {
            return ErrorExpression()
        }
        if (type.isOfType(TypeSymbol.Ptr)) {
            val isFullTypeWritten = false
            val elementType = type.params[0]
            var actualElementType: TypeSymbol? = null
            val expressions = ArrayList<Expression>()

            for (e in node.expressions) {
                val expression = bindExpression(e, type = elementType)
                expressions.add(expression)

                actualElementType = if (actualElementType == null) {
                    expression.type
                } else {
                    expression.type.commonType(actualElementType)
                }
            }

            if (actualElementType == null) actualElementType = TypeSymbol.Any

            val actualType = if (isFullTypeWritten) type else type(arrayOf(actualElementType))
            return PointerArrayInitialization(actualType, null, expressions)
        }
        diagnostics.reportTypeNotStruct(node.type.location, node.type.identifier.joinToString(".") { it.string!! })
        return ErrorExpression()
    }

    private fun bindCastExpression(node: CastExpressionNode): Expression {
        val expression = bindExpression(node.expression)
        val type = bindTypeClause(node.type)
        if (type == TypeSymbol.err) {
            return ErrorExpression()
        }
        return CastExpression(type, expression)
    }

    private fun bindAssignmentStatement(node: AssignmentNode): Statement {
        val a by lazy { bindExpression(node.assignee) }
        val boundExpression = let {
            val e = bindExpression(node.expression)
            if (node.equalsToken.kind == SyntaxType.Equals) e
            else bindBinaryOperation(node.equalsToken.location, a, when (node.equalsToken.kind) {
                SyntaxType.PlusEquals -> SyntaxType.Plus
                SyntaxType.MinusEquals -> SyntaxType.Minus
                SyntaxType.DivEquals -> SyntaxType.Div
                SyntaxType.TimesEquals -> SyntaxType.Star
                SyntaxType.RemEquals -> SyntaxType.Rem
                SyntaxType.OrEquals -> SyntaxType.BitOr
                SyntaxType.AndEquals -> SyntaxType.BitAnd
                else -> throw BinderError("Invalid token used for assignment")
            }, e)
        }
        if (node.assignee.kind == SyntaxType.IndexExpression) {
            return bindIndexAssignment(node.assignee as IndexExpressionNode, boundExpression)
        }
        when (a.kind) {
            BoundNode.Kind.NameExpression -> {
                val variable = (a as NameExpression).symbol
                if (variable.isReadOnly) {
                    diagnostics.reportVarIsImmutable(node.equalsToken.location, variable)
                    return bindErrorStatement()
                }
                if (typeCheck(boundExpression, variable.type, node.expression.location)) {
                    return bindErrorStatement()
                }
            }
            BoundNode.Kind.StructFieldAccess -> {
                val s = (a as StructFieldAccess)
                if (s.field.isReadOnly) {
                    diagnostics.reportFieldIsImmutable(node.equalsToken.location, s.field)
                    return bindErrorStatement()
                }
                if (typeCheck(boundExpression, s.field.type, node.expression.location)) {
                    return bindErrorStatement()
                }
            }
            BoundNode.Kind.ErrorExpression -> return bindErrorStatement()
            else -> throw BinderError("${a.kind} isn't a valid assignee")
        }
        return Assignment(a, boundExpression)
    }

    private fun bindIndexAssignment(node: IndexExpressionNode, value: Expression): Statement {
        val expression = bindExpression(node.expression)

        if (expression.isError()) {
            return bindErrorStatement()
        }

        val list = ArrayList<Expression>()
        list.add(expression)
        for (it in node.arguments) {
            val e = bindExpression(it)
            if (e.isError()) return bindErrorStatement()
            list.add(e)
        }
        if (expression.type.isOfType(TypeSymbol.Ptr) && list.size == 2 && list[1].type.isOfType(TypeSymbol.Integer)) {
            if (!isUnsafe) {
                diagnostics.reportPointerOperationsAreUnsafe(node.location)
                return bindErrorStatement()
            }
            if (typeCheck(value, expression.type.params[0], node.location)) {
                return bindErrorStatement()
            }
            return PointerAccessAssignment(expression, list[1], value)
        }
        list.add(value)
        val operatorFunction = scope.tryLookup(
            listOf("set"),
            CallableSymbol.Info(list.map { it.type }, true)
        )
        if (operatorFunction == null) {
            if (list.find { it.isError() } == null) {
                diagnostics.reportUndefinedFunction(node.location, "set", list.map { it.type }, true)
            }
            return bindErrorStatement()
        }
        if (!operatorFunction.meta.isOperator) {
            diagnostics.reportNotOperator(node.location, operatorFunction)
            return bindErrorStatement()
        }
        operatorFunction as CallableSymbol
        return ExpressionStatement(CallExpression(NameExpression(operatorFunction), list))
    }

    private fun bindNameExpression(node: NameExpressionNode): Expression {
        val name = node.identifier.string!!
        val variable = scope.tryLookup(listOf(name))
        if (variable == null) {
            diagnostics.reportUndefinedName(node.identifier.location, name)
            return ErrorExpression()
        }
        if (variable !is VariableSymbol) {
            diagnostics.reportUndefinedName(node.identifier.location, name)
            return ErrorExpression()
        }
        return NameExpression(variable)
    }

    private fun bindLiteralExpression(
        node: LiteralExpressionNode,
        expectedType: TypeSymbol?
    ): LiteralExpression {
        val type = when (node.value) {
            is Byte -> when (expectedType) {
                TypeSymbol.I8 -> TypeSymbol.I8
                TypeSymbol.I16 -> TypeSymbol.I16
                TypeSymbol.I64 -> TypeSymbol.I64
                else -> TypeSymbol.I32
            }
            is Short -> when (expectedType) {
                TypeSymbol.I16 -> TypeSymbol.I16
                TypeSymbol.I64 -> TypeSymbol.I64
                else -> TypeSymbol.I32
            }
            is Int -> when (expectedType) {
                TypeSymbol.I64 -> TypeSymbol.I64
                else -> TypeSymbol.I32
            }
            is Long -> TypeSymbol.I64
            is Float -> TypeSymbol.Float
            is Double -> TypeSymbol.Double
            is Char -> TypeSymbol.Char
            else -> throw BinderError("Unexpected literal of type ${node.value?.javaClass}")
        }
        return LiteralExpression(when (type) {
            TypeSymbol.I8 -> (node.value as Number).toByte()
            TypeSymbol.I16 -> (node.value as Number).toShort()
            TypeSymbol.I32 -> (node.value as Number).toInt()
            TypeSymbol.I64 -> (node.value as Number).toLong()
            TypeSymbol.Char -> (node.value as Char).toShort()
            TypeSymbol.Float -> (node.value as Number).toFloat()
            else -> node.value
        }, type)
    }

    private fun bindBoolConst(
        node: BoolConstantNode,
        expectedType: TypeSymbol?
    ): LiteralExpression {
        return LiteralExpression(node.value, TypeSymbol.Bool)
    }

    private fun bindTextConst(
        node: TextConstantNode,
        expectedType: TypeSymbol?
    ): LiteralExpression {
        return LiteralExpression(node.value, TypeSymbol.String)
    }

    private fun bindUnaryExpression(node: UnaryExpressionNode): Expression {
        val operand = bindExpression(node.operand)
        if (operand.isError()) {
            return ErrorExpression()
        }
        if (node.operator.kind == SyntaxType.BitAnd) {
            if (!isUnsafe) {
                diagnostics.reportPointerOperationsAreUnsafe(node.operator.location)
                return ErrorExpression()
            }
            if (operand.kind != BoundNode.Kind.NameExpression) {
                diagnostics.reportReferenceRequiresMutableVar(node.operator.location)
                return ErrorExpression()
            }
            operand as NameExpression
            if (operand.symbol.isReadOnly) {
                diagnostics.reportReferenceRequiresMutableVar(node.operator.location)
                return ErrorExpression()
            }
            return Reference(operand)
        }
        val operator = UnOperator.bind(node.operator.kind, operand.type)
        if (operator == null) {
            val operatorFunction = scope.tryLookup(
                listOf(Translator.unaryOperatorToString(node.operator.kind)),
                CallableSymbol.Info(listOf(operand.type), true))
            if (operatorFunction != null && operatorFunction.meta.isOperator) {
                operatorFunction as CallableSymbol
                return CallExpression(NameExpression(operatorFunction), listOf(operand))
            }
            diagnostics.reportUnaryOperator(node.operator.location, node.operator.kind, operand.type)
            return ErrorExpression()
        }
        return UnaryExpression(operator, operand)
    }

    private fun bindBinaryExpression(node: BinaryExpressionNode): Expression {
        val left = bindExpression(node.left)
        val right = bindExpressionInternal(node.right, typeHint = left.type)
        return bindBinaryOperation(node.operator.location, left, node.operator.kind, right)
    }
    private fun bindBinaryOperation(location: TextLocation, left: Expression, operatorKind: SyntaxType, right: Expression): Expression {

        if (left.isError() || right.isError()) {
            return ErrorExpression()
        }

        val operator = BiOperator.bind(operatorKind, left.type, right.type)
        if (operator != null) {
            return BinaryExpression(left, operator, right)
        }

        if (operatorKind == SyntaxType.IsNotEqual) {
            val operatorFunction = scope.tryLookup(
                listOf(Translator.binaryOperatorToString(SyntaxType.IsEqual)),
                CallableSymbol.Info(listOf(left.type, right.type), true))
            if (operatorFunction != null && operatorFunction.meta.isOperator) {
                operatorFunction as CallableSymbol
                return UnaryExpression(
                    UnOperator(SyntaxType.Bang, UnOperator.Type.Not, TypeSymbol.Bool),
                    CallExpression(NameExpression(operatorFunction), listOf(left, right)))
            }
        } else {
            val operatorFunction = scope.tryLookup(
                listOf(Translator.binaryOperatorToString(operatorKind)),
                CallableSymbol.Info(listOf(left.type, right.type), true))
            if (operatorFunction != null && operatorFunction.meta.isOperator) {
                operatorFunction as CallableSymbol
                return CallExpression(NameExpression(operatorFunction), listOf(left, right))
            }
        }
        diagnostics.reportBinaryOperator(location, left.type, operatorKind, right.type)
        return ErrorExpression()
    }

    /**
     * @return (resulting expression, initial expression if there was one)
     *
     * The resulting expression may be one of:
     *   - ErrorExpression
     *   - NameExpression
     *   - StructFieldAccess
     *   - NamespaceFieldAccess
     */
    private fun bindDotAccessExpression(node: BinaryExpressionNode, types: List<TypeSymbol>? = null): Pair<Expression, Expression?> {

        if (node.right.kind != SyntaxType.NameExpression) {
            /*if (node.right.kind == SyntaxType.StructInitialization) {
                node.right as StructInitializationNode
                node.right.
                return bindStructInitialization(node.right)
            }*/
            diagnostics.reportCantBeAfterDot(node.right.location)
            return ErrorExpression() to null
        }

        node.right as NameExpressionNode

        fun bindNamespaceAccess(leftName: String, rightName: String): Expression {
            run {
                val symbol = scope.tryLookup(leftName.split('.').toMutableList().apply { add(rightName) }, types?.let { CallableSymbol.Info(it, false) })
                if (symbol != null) {
                    symbol as VariableSymbol
                    return NameExpression(symbol)
                }
            }
            run {
                val symbol = scope.tryLookup(leftName.split('.').toMutableList().apply { add(rightName) })
                if (symbol != null) {
                    symbol as VariableSymbol
                    return NameExpression(symbol)
                }
            }
            val leftNamespace = Namespace[leftName]
            if (leftNamespace == null) {
                diagnostics.reportUndefinedName(node.left.location, leftName)
                return ErrorExpression()
            }
            val namespace = Namespace["$leftName.$rightName"]
            if (namespace != null) {
                return NamespaceFieldAccess(namespace)
            }
            diagnostics.reportNotFoundInNamespace(node.right.location, rightName, leftNamespace)
            return ErrorExpression()
        }

        if (node.left.kind == SyntaxType.NameExpression) {
            node.left as NameExpressionNode
            val leftName = (node.left as NameExpressionNode).identifier.string!!
            val rightName = (node.right as NameExpressionNode).identifier.string!!
            scope.tryLookup(listOf(leftName)) ?: return bindNamespaceAccess(leftName, rightName) to null
        }

        val left = bindExpression(node.left)
        val name = (node.right as NameExpressionNode).identifier.string!!

        if (left.kind == BoundNode.Kind.NamespaceFieldAccess) {
            left as NamespaceFieldAccess
            val leftName = left.namespace.path
            return bindNamespaceAccess(leftName, name) to null
        }

        if (left.isError()) {
            return ErrorExpression() to null
        }

        if (left.type.kind == Symbol.Kind.StructType) {
            val i = (left.type as TypeSymbol.StructTypeSymbol).getFieldI(name)
            if (i != -1) {
                return StructFieldAccess(left, i) to left
            }
        }
        if (types != null) {
            val t = ArrayList<TypeSymbol>().apply {
                add(left.type)
                addAll(types)
            }
            val function = scope.tryLookup(listOf(name), CallableSymbol.Info(t, true))
            if (function != null) {
                function as VariableSymbol
                return NameExpression(function) to left
            }
            diagnostics.reportUndefinedFunction(node.right.location, name, t, true)
            return ErrorExpression() to null
        }
        if (left.type.kind == Symbol.Kind.StructType) {
            diagnostics.reportNoSuchField(node.right.location, left.type as TypeSymbol.StructTypeSymbol, name)
            return ErrorExpression() to null
        }
        diagnostics.reportUndefinedName(node.right.location, name)
        return ErrorExpression() to null
    }


    private fun bindField(node: ValVarDeclarationNode, struct: TypeSymbol.StructTypeSymbol, declaredTypes: HashMap<TypeSymbol.StructTypeSymbol, TypeDeclarationNode>): TypeSymbol.StructTypeSymbol.Field {

        fun isNew(node: ValVarDeclarationNode, struct: TypeSymbol.StructTypeSymbol, declaredTypes: HashMap<TypeSymbol.StructTypeSymbol, TypeDeclarationNode>): Pair<Boolean, TypeSymbol.StructTypeSymbol> {
            val p = struct.parentType
            return when {
                p == TypeSymbol.Any -> true to struct
                declaredTypes[p]?.fields?.find { it.identifier.string == node.identifier.string && it !== node } == null -> {
                    if (p is TypeSymbol.StructTypeSymbol) isNew(node, p, declaredTypes)
                    else true to struct
                }
                else -> false to struct
            }
        }

        val isReadOnly = node.keyword.kind == SyntaxType.Val
        var type = node.typeClauseNode?.let { bindTypeClause(it) }
        val name = node.identifier.string!!
        val (isNew, typeWithOriginalField) = isNew(node, struct, declaredTypes)
        var isOverride = false
        for (annotation in node.annotations) {
            when (annotation.getIdentifierString()) {
                "override" -> isOverride = true
                else -> diagnostics.reportInvalidAnnotation(annotation.location)
            }
        }

        if (isOverride) {
            if (isNew) {
                diagnostics.reportOverridesNothing(node.identifier.location, name)
                isOverride = false
            }
        } else if (!isNew) {
            diagnostics.reportFieldAlreadyDeclared(node.identifier.location, typeWithOriginalField)
        }
        if (node.initializer != null) {
            diagnostics.reportMustNotInitField(node.identifier.location)
        }
        if (type == null) {
            diagnostics.reportCantInferType(node.identifier.location)
            type = TypeSymbol.Any
        }
        return TypeSymbol.StructTypeSymbol.Field(name, type, isReadOnly, isOverride)
    }

    companion object {

        private fun createAllNamespaces(binder: Binder, syntaxTrees: Collection<SyntaxTree>) {
            fun iterate(statement: Node, path: String) {
                if (statement.kind == SyntaxType.NamespaceStatement) {
                    statement as NamespaceStatementNode
                    val p = path + '.' + statement.identifier.string
                    binder.inScope(Namespace(p, binder.scope)) {
                        for (s in statement.members) {
                            iterate(s, p)
                        }
                    }
                }
            }
            for (syntaxTree in syntaxTrees) {
                binder.diagnostics.append(syntaxTree.diagnostics)
                binder.inScope(Namespace[syntaxTree.projectPath]!!) {
                    for (s in syntaxTree.members) {
                        iterate(s, syntaxTree.projectPath)
                    }
                }
            }
        }

        private fun processAllStatementsInNamespaces(binder: Binder, syntaxTrees: Collection<SyntaxTree>, fn: (statement: Node, path: String) -> Unit) {
            fun iterate(statement: Node, path: String) {
                if (statement.kind == SyntaxType.NamespaceStatement) {
                    statement as NamespaceStatementNode
                    val p = path + '.' + statement.identifier.string
                    binder.inScope(Namespace[p]!!) {
                        for (s in statement.members) {
                            iterate(s, p)
                        }
                    }
                } else {
                    fn(statement, path)
                }
            }
            for (syntaxTree in syntaxTrees) {
                binder.inScope(Namespace[syntaxTree.projectPath]!!) {
                    for (s in syntaxTree.members) {
                        iterate(s, syntaxTree.projectPath)
                    }
                }
            }
        }

        fun bindGlobalScope(
                syntaxTrees: Collection<SyntaxTree>,
                requireEntryFunc: Boolean
        ): GlobalScope {

            val parentScope = createRootScope()
            val symbols = ArrayList<Symbol>()
            val binder = Binder(parentScope, null, HashMap(), HashMap(), symbols)
            val statementBuilder = ArrayList<Statement>()

            for (syntaxTree in syntaxTrees) {
                val strBuilder = StringBuilder()
                var parent = parentScope
                syntaxTree.projectPath.split('.').forEachIndexed { i, s ->
                    val path = if (i == 0) { strBuilder.append(s).toString() }
                        else { strBuilder.append('.').append(s).toString() }
                    parent = Namespace.getOr(path) { Namespace(path, parent) }
                }
            }
            createAllNamespaces(binder, syntaxTrees)
            val declaredTypes = HashMap<TypeSymbol.StructTypeSymbol, TypeDeclarationNode>()
            processAllStatementsInNamespaces(binder, syntaxTrees) { statement, path ->
                when (statement.kind) {
                    SyntaxType.TypeDeclaration -> {
                        statement as TypeDeclarationNode
                        val typePath = path + '.' + statement.identifier.string!!
                        val type = TypeSymbol.StructTypeSymbol(typePath, binder.scope)
                        declaredTypes[type] = statement
                        TypeSymbol.declared.add(type)
                        binder.scope.tryDeclare(type)
                    }
                    SyntaxType.UseStatement -> {}
                    SyntaxType.ValVarDeclaration -> {}
                    else -> throw BinderError("Incorrect statement got to be global (${statement.kind})")
                }
            }

            processAllStatementsInNamespaces(binder, syntaxTrees) { statement, _ ->
                when (statement.kind) {
                    SyntaxType.UseStatement -> {
                        statement as UseStatementNode
                        binder.bindUseStatement(statement)
                    }
                    SyntaxType.TypeDeclaration -> {}
                    SyntaxType.ValVarDeclaration -> {}
                    else -> throw BinderError("Incorrect statement got to be global (${statement.kind})")
                }
            }

            if (!binder.diagnostics.hasErrors()) {
                for ((type, statement) in declaredTypes) {
                    binder.inScope(type.declarationScope) {
                        val parent = statement.parent?.let { binder.bindTypeClause(it) } ?: TypeSymbol.Any
                        if (parent.isOfType(type)) {
                            binder.diagnostics.reportChickenEggWithTypes(statement.location, type, parent)
                        }
                        type.parentType = parent
                    }
                }

                for ((type, statement) in declaredTypes) {
                    binder.inScope(type.declarationScope) {
                        val fields = statement.fields?.let {
                            Array(statement.fields!!.size) {
                                binder.bindField(statement.fields!![it], type, declaredTypes)
                            }
                        } ?: arrayOf()
                        type.fields = fields
                    }
                }

                if (!binder.diagnostics.hasErrors()) {
                    processAllStatementsInNamespaces(binder, syntaxTrees) { statement, path ->
                        when (statement.kind) {
                            SyntaxType.ValVarDeclaration -> {
                                statement as ValVarDeclarationNode
                                val s = binder.bindValVarDeclaration(statement)
                                if (s.initializer.kind != BoundNode.Kind.Lambda) {
                                    statementBuilder.add(s)
                                    symbols.add(s.variable)
                                }
                            }
                            SyntaxType.TypeDeclaration -> {}
                            SyntaxType.UseStatement -> {}
                            else -> throw BinderError("Incorrect statement got to be global (${statement.kind})")
                        }
                    }
                }
            }

            val diagnostics = binder.diagnostics

            if (requireEntryFunc && symbols.find {
                it.kind == Symbol.Kind.Function &&
                it.meta.isEntry &&
                it.type == TypeSymbol.Fn.entry
            } == null) {
                diagnostics.reportNoMainFn()
            }

            return GlobalScope(
                diagnostics,
                symbols,
                statementBuilder)
        }

        private fun createRootScope(): Scope {
            return Scope(null)
        }

        fun bindFunction(
            parentScope: Scope,
            functions: HashMap<CallableSymbol, BlockExpression?>,
            symbol: CallableSymbol,
            diagnostics: DiagnosticList,
            functionBodies: HashMap<CallableSymbol, Statement?>
        ) {
            val symbolMangledName = symbol.mangledName()
            if (symbolMangledName == "malloc") {
                diagnostics.reportReservedFunctionPath(symbol.declaration!!.identifier.location, symbolMangledName)
            } else {
                val sameFunction = functions.keys.find { it.mangledName() == symbolMangledName }
                if (sameFunction != null) {
                    diagnostics.reportFunctionPathAlreadyDeclared(
                        (symbol.declaration?.identifier ?: symbol.lambda)!!.location,
                        symbolMangledName,
                        (sameFunction.declaration?.identifier ?: sameFunction.lambda)!!.location)
                }
            }
            when {
                symbol.meta.isExtern -> functions[symbol] = null
                else -> {
                    val binder = Binder(parentScope, symbol, functions, functionBodies, null)
                    val body = binder.bindStatement(symbol.lambda.body!!, isActuallyExpression = true)
                    val loweredBody = Lowerer.lower(body)

                    if (body is ExpressionStatement) {
                        binder.typeCheck(body.expression, symbol.returnType, symbol.lambda.body!!.location)
                    } else {
                        if (!TypeSymbol.Void.isOfType(symbol.returnType) && !ControlFlowGraph.allPathsReturn(loweredBody.statements)) {
                            diagnostics.reportWrongType(symbol.lambda.body!!.location, TypeSymbol.Void, symbol.returnType)
                            //diagnostics.reportAllPathsMustReturn(symbol.declarationNode.identifier.location)
                        }
                    }

                    functions[symbol] = loweredBody
                    functionBodies?.put(symbol, body)
                    diagnostics.append(binder.diagnostics)
                }
            }
        }

        fun bindProgram(
            globalScope: GlobalScope,
            isSharedLib: Boolean
        ): Program {

            val functions = HashMap<CallableSymbol, BlockExpression?>()
            val diagnostics = DiagnosticList()

            val functionBodies = HashMap<CallableSymbol, Statement?>()
            for (symbol in globalScope.symbols) {
                if (symbol is CallableSymbol) {
                    bindFunction(Scope(symbol.namespace!!), functions, symbol, diagnostics, functionBodies)
                }
            }

            /*for ((fn, body) in functionBodies) {
                if (body != null) {
                    println(fn.mangledName())
                    functions[fn] = Lowerer.lower(Inliner(functionBodies).rewriteStatement(body).also { println(it.structureString()) })
                    println()
                    println()
                }
            }*/

            val statement = Lowerer.lower(ExpressionStatement(BlockExpression(
                globalScope.statements, TypeSymbol.Void
            )))

            return Program(
                diagnostics,
                functions,
                statement,
                functionBodies)
        }
    }

    private fun bindFunctionAnnotations(annotations: Collection<AnnotationNode>, l: Lambda) {
        for (annotation in annotations) {
            when (annotation.getIdentifierString()) {
                "inline" -> l.symbol.meta.isInline = true
                "private" -> {
                    l.symbol.meta.isPrivate = true
                    l.symbol.meta.isInternal = true
                }
                "internal" -> l.symbol.meta.isInternal = true
                "operator" -> {
                    if (l.symbol.meta.isExtension) {
                        l.symbol.meta.isOperator = true
                        when (l.symbol.name) {
                            "equals" -> {
                                if (l.symbol.parameters.size != 2) {
                                    diagnostics.reportInvalidAnnotation(annotation.location)
                                }
                            }
                            "plus" -> {
                                if (l.symbol.parameters.size != 2 && l.symbol.parameters.size != 1) {
                                    diagnostics.reportInvalidAnnotation(annotation.location)
                                }
                            }
                            "minus" -> {
                                if (l.symbol.parameters.size != 2 && l.symbol.parameters.size != 1) {
                                    diagnostics.reportInvalidAnnotation(annotation.location)
                                }
                            }
                            "times" -> {
                                if (l.symbol.parameters.size != 2) {
                                    diagnostics.reportInvalidAnnotation(annotation.location)
                                }
                            }
                            "divide" -> {
                                if (l.symbol.parameters.size != 2) {
                                    diagnostics.reportInvalidAnnotation(annotation.location)
                                }
                            }
                            "rem" -> {
                                if (l.symbol.parameters.size != 2) {
                                    diagnostics.reportInvalidAnnotation(annotation.location)
                                }
                            }
                            "and" -> {
                                if (l.symbol.parameters.size != 2) {
                                    diagnostics.reportInvalidAnnotation(annotation.location)
                                }
                            }
                            "or" -> {
                                if (l.symbol.parameters.size != 2) {
                                    diagnostics.reportInvalidAnnotation(annotation.location)
                                }
                            }
                            "not" -> {
                                if (l.symbol.parameters.size != 1) {
                                    diagnostics.reportInvalidAnnotation(annotation.location)
                                }
                            }
                            "get", "set" -> {
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
                    l.symbol.meta.isEntry = true
                    Symbol.MetaData.entryExists = true
                }
                "cname" -> {
                    val expression = bindTextConst(annotation.getParameter(0) as TextConstantNode, TypeSymbol.String)
                    l.symbol.meta.cname = expression.value as String
                }
                "generic" -> {}
                else -> {
                    diagnostics.reportInvalidAnnotation(annotation.location)
                }
            }
        }
    }

    class BinderError(message: String? = null) : Exception(message)
}