package mango.interpreter.syntax.parser

import mango.compilation.DiagnosticList
import mango.interpreter.syntax.SyntaxType
import mango.interpreter.syntax.lex.Lexer
import mango.interpreter.syntax.lex.Token
import mango.interpreter.text.SourceText

class Parser(val sourceText: SourceText) {

    val diagnostics = DiagnosticList()
    private val tokens: Array<Token>
    var position = 0

    init {
        val tokens = ArrayList<Token>()
        val lexer = Lexer(sourceText)
        var token: Token
        do {
            token = lexer.nextToken()
            if (token.kind != SyntaxType.Bad) tokens.add(token)
        }
        while (token.kind != SyntaxType.EOF)

        this.tokens = tokens.toTypedArray()
        diagnostics.append(lexer.diagnostics)
    }

    private inline fun peek(offset: Int): Token {
        val index = position + offset
        return if (index >= tokens.size) tokens[tokens.lastIndex] else tokens[index]
    }

    private inline val current get() = peek(0)

    private inline fun next(): Token {
        val current = current
        position++
        return current
    }

    private inline fun match(type: SyntaxType) = if (current.kind == type) next()
        else {
            diagnostics.reportUnexpectedToken(current.span, current.kind, type)
            Token(type, current.position).apply { isMissing = true }
        }

    fun parseCompilationUnit(): CompilationUnitNode {
        val statement = parseMembers()
        return CompilationUnitNode(statement, match(SyntaxType.EOF))
    }

    private fun parseMembers(): Collection<MemberNode> {
        val members = ArrayList<MemberNode>()

        while (current.kind != SyntaxType.EOF) {
            val startToken = current

            val member = parseMember()
            members.add(member)

            if (startToken == current) {
                next()
            }
        }

        return members
    }

    private fun parseMember(): MemberNode {
        if (current.kind == SyntaxType.Fn) {
            return parseFunctionDeclaration()
        }
        return parseGlobalStatement()
    }

    private fun parseFunctionDeclaration(): MemberNode {
        val keyword = match(SyntaxType.Fn)
        val identifier = match(SyntaxType.Identifier)
        var type: TypeClauseNode? = null
        if (current.kind == SyntaxType.Identifier) {
            type = parseTypeClause()
        }
        var params: SeparatedNodeList<ParameterNode>? = null
        if (current.kind == SyntaxType.Colon) {
            next()
            params = parseParamList()
        }

        var lambdaArrow: Token? = null
        val body: StatementNode
        if (current.kind == SyntaxType.LambdaArrow) {
            lambdaArrow = next()
            body = parseExpressionStatement()
        } else {
            body = parseBlockStatement()
        }
        return FunctionDeclarationNode(keyword, identifier, type, params, lambdaArrow, body)
    }

    private fun parseParamList(): SeparatedNodeList<ParameterNode> {
        val nodesNSeparators = ArrayList<Node>()

        while (
            current.kind != SyntaxType.OpenCurlyBracket &&
            current.kind != SyntaxType.LambdaArrow &&
            current.kind != SyntaxType.EOF
        ) {
            val param = parseParam()
            nodesNSeparators.add(param)

            if (current.kind == SyntaxType.Comma) {
                val comma = match(SyntaxType.Comma)
                nodesNSeparators.add(comma)
            } else {
                break
            }
        }

        return SeparatedNodeList(nodesNSeparators)
    }

    private fun parseParam(): ParameterNode {
        val identifier = match(SyntaxType.Identifier)
        val type = parseTypeClause()
        return ParameterNode(identifier, type)
    }

    private fun parseGlobalStatement(): MemberNode {
        val statement = parseStatement()
        return GlobalStatementNode(statement)
    }


    private fun parseStatement() = when (current.kind) {
        SyntaxType.OpenCurlyBracket -> parseBlockStatement()
        SyntaxType.Val, SyntaxType.Var -> parseVariableDeclaration()
        SyntaxType.If -> parseIfStatement()
        SyntaxType.While -> parseWhileStatement()
        SyntaxType.For -> parseForStatement()
        SyntaxType.Break -> parseBreakStatement()
        SyntaxType.Continue -> parseContinueStatement()
        SyntaxType.Return -> parseReturnStatement()
        else -> parseExpressionStatement()
    }

    private fun parseVariableDeclaration(): VariableDeclarationNode {
        val expected = if (current.kind == SyntaxType.Val) { SyntaxType.Val }
            else { SyntaxType.Var }
        val keyword = match(expected)
        val identifier = match(SyntaxType.Identifier)
        val typeClause = parseOptionalValueTypeClause()
        val equals = match(SyntaxType.Equals)
        val initializer = parseExpression()
        return VariableDeclarationNode(keyword, identifier, typeClause, equals, initializer)
    }

    private fun parseOptionalValueTypeClause(): TypeClauseNode? {
        if (current.kind == SyntaxType.Equals) {
            return null
        }
        return parseTypeClause()
    }

    private fun parseTypeClause(): TypeClauseNode {
        val identifier = match(SyntaxType.Identifier)
        return TypeClauseNode(identifier)
    }

    private fun parseExpressionStatement(): ExpressionStatementNode {
        val expression = parseExpression()
        return ExpressionStatementNode(expression)
    }

    private fun parseBlockStatement(): BlockStatementNode {
        val statements = ArrayList<StatementNode>()
        val openBrace = match(SyntaxType.OpenCurlyBracket)

        while (
            current.kind != SyntaxType.EOF &&
            current.kind != SyntaxType.ClosedCurlyBracket
        ) {
            val startToken = current

            val statement = parseStatement()
            statements.add(statement)

            if (startToken == current) {
                next()
            }
        }

        val closeBrace = match(SyntaxType.ClosedCurlyBracket)
        return BlockStatementNode(openBrace, statements, closeBrace)
    }

    private fun parseIfStatement(): IfStatementNode {
        val keyword = match(SyntaxType.If)
        val condition = parseExpression()
        val statement = parseBlockStatement()
        val elseClause = parseElseClause()
        return IfStatementNode(keyword, condition, statement, elseClause)
    }

    private fun parseElseClause(): ElseClauseNode? {
        if (current.kind != SyntaxType.Else) {
            return null
        }
        val keyword = next()
        val isIfNext = current.kind == SyntaxType.If
        val statement = if (isIfNext) { parseIfStatement() } else { parseBlockStatement() }
        return ElseClauseNode(keyword, statement)
    }

    private fun parseWhileStatement(): WhileStatementNode {
        val keyword = match(SyntaxType.While)
        val condition = parseExpression()
        val body = parseBlockStatement()
        return WhileStatementNode(keyword, condition, body)
    }

    private fun parseForStatement(): ForStatementNode {
        val keyword = match(SyntaxType.For)
        val identifier = match(SyntaxType.Identifier)
        val inToken = match(SyntaxType.In)
        val lowerBound = parseExpression()
        val rangeToken = match(SyntaxType.Range)
        val upperBound = parseExpression()
        val body = parseBlockStatement()
        return ForStatementNode(keyword, identifier, inToken, lowerBound, rangeToken, upperBound, body)
    }

    private fun parseBreakStatement(): StatementNode {
        val keyword = match(SyntaxType.Break)
        return BreakStatementNode(keyword)
    }

    private fun parseContinueStatement(): StatementNode {
        val keyword = match(SyntaxType.Continue)
        return ContinueStatementNode(keyword)
    }

    private fun parseReturnStatement(): StatementNode {
        val keyword = match(SyntaxType.Return)
        val keywordLine = sourceText.getLineI(keyword.span.start)
        val currentLine = sourceText.getLineI(current.span.start)
        val expression = if (currentLine == keywordLine && current.kind != SyntaxType.EOF) {
            parseExpression()
        } else null
        return ReturnStatementNode(keyword, expression)
    }

    private fun parseExpression() = parseAssignmentExpression()

    private fun parseAssignmentExpression(): ExpressionNode {
        if (peek(0).kind == SyntaxType.Identifier &&
            peek(1).kind == SyntaxType.Equals) {
            val identifierToken = next()
            val operatorToken = next()
            val right = parseAssignmentExpression()
            return AssignmentExpressionNode(identifierToken, operatorToken, right)
        }
        return parseBinaryExpression()
    }

    private fun parseBinaryExpression(parentPrecedence: Int = 0): ExpressionNode {
        var left: ExpressionNode

        val unaryOperatorPrecedence = current.kind.getUnaryOperatorPrecedence()
        if (unaryOperatorPrecedence != 0 && unaryOperatorPrecedence >= parentPrecedence) {
            val operatorToken = next()
            val operand = parseBinaryExpression(unaryOperatorPrecedence)
            left = UnaryExpressionNode(operatorToken, operand)
        } else {
            left = parsePrimaryExpression()
        }

        while (true) {
            val precedence = current.kind.getBinaryOperatorPrecedence()
            if (precedence == 0 || precedence <= parentPrecedence)
                break
            val operatorToken = next()
            val right = parseBinaryExpression(precedence)
            left = BinaryExpressionNode(left, operatorToken, right)
        }
        return left
    }

    private fun parsePrimaryExpression() = when (current.kind) {
        SyntaxType.OpenRoundedBracket -> parseParenthesizedExpression()
        SyntaxType.False, SyntaxType.True -> parseBooleanLiteral()
        SyntaxType.Int -> parseNumberLiteral()
        SyntaxType.String -> parseStringLiteral()
        SyntaxType.In -> parseNumberLiteral()
        else -> parseNameOrCallExpression()
    }

    private fun parseParenthesizedExpression(): ParenthesizedExpressionNode {
        val left = match(SyntaxType.OpenRoundedBracket)
        val expression = parseExpression()
        val right = match(SyntaxType.ClosedRoundedBracket)
        return ParenthesizedExpressionNode(left, expression, right)
    }

    private fun parseBooleanLiteral(): LiteralExpressionNode {
        val token = next()
        val isTrue = token.kind == SyntaxType.True
        return LiteralExpressionNode(token, isTrue)
    }

    private fun parseNumberLiteral() = LiteralExpressionNode(match(SyntaxType.Int))

    private fun parseStringLiteral() = LiteralExpressionNode(match(SyntaxType.String))

    private fun parseNameOrCallExpression(): ExpressionNode {
        if (peek(0).kind == SyntaxType.Identifier &&
            peek(1).kind == SyntaxType.OpenRoundedBracket) {
            return parseCallExpression()
        }
        return parseNameExpression()
    }

    private fun parseNameExpression(): NameExpressionNode {
        val identifierToken = match(SyntaxType.Identifier)
        return NameExpressionNode(identifierToken)
    }

    private fun parseCallExpression(): CallExpressionNode {
        val identifierToken = match(SyntaxType.Identifier)
        val leftBracket = match(SyntaxType.OpenRoundedBracket)
        val arguments = parseArguments()
        val rightBracket = match(SyntaxType.ClosedRoundedBracket)
        return CallExpressionNode(identifierToken, leftBracket, arguments, rightBracket)
    }

    private fun parseArguments(): SeparatedNodeList<ExpressionNode> {

        val nodesNSeparators = ArrayList<Node>()

        while (
            current.kind != SyntaxType.ClosedRoundedBracket &&
            current.kind != SyntaxType.EOF
        ) {
            val expression = parseExpression()
            nodesNSeparators.add(expression)

            if (current.kind == SyntaxType.Comma) {
                val comma = match(SyntaxType.Comma)
                nodesNSeparators.add(comma)
            } else {
                break
            }
        }

        return SeparatedNodeList(nodesNSeparators)
    }
}