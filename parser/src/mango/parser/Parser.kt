package mango.parser

import mango.parser.nodes.*
import shared.DiagnosticList

class Parser(
    val textFile: TextFile
) {

    var diagnostics = DiagnosticList()
        private set
    internal val tokens: Array<Token>
    var position = 0

    init {
        val tokens = ArrayList<Token>()
        val lexer = Lexer(textFile)
        var token: Token
        do {
            token = lexer.nextToken()
            if (token.kind != SyntaxType.Bad &&
                token.kind != SyntaxType.SingleLineComment &&
                token.kind != SyntaxType.MultilineComment) {
                tokens.add(token)
            }
        }
        while (token.kind != SyntaxType.EOF)

        this.tokens = tokens.toTypedArray()
        diagnostics.append(lexer.diagnostics)
    }

    /**
     * @return The expression returned by [fn] if there were no errors in the parsing
     */
    private inline fun <T> tryParse(fn: () -> T): T? {
        val initPosition = position
        val initDiagnostics = diagnostics
        val newDiagnostics = DiagnosticList()
        diagnostics = newDiagnostics
        trying++
        val ret = try { fn() } catch (p: ParsingError) {
            trying--
            diagnostics = initDiagnostics
            position = initPosition
            return null
        }
        trying--
        diagnostics = initDiagnostics
        diagnostics.append(newDiagnostics)
        return ret
    }
    private var trying = 0
    private class ParsingError : Throwable()

    private inline fun peek(offset: Int): Token {
        val index = position + offset
        return if (index >= tokens.size) {
            tokens[tokens.lastIndex]
        } else tokens[index]
    }

    private inline fun peekOverLineSeparators(offset: Int, startOffset: Int = position): Token {
        var i = 0
        var o = position + startOffset
        while (o < tokens.size) {
            val p = tokens[o++]
            if (p.kind != SyntaxType.LineSeparator) {
                if (i == offset) {
                    return p
                }
                i++
            }
        }
        return tokens[tokens.lastIndex]
    }

    private inline val current get() = peek(0)

    private inline fun next(): Token {
        val current = current
        position++
        return current
    }

    private fun match(type: SyntaxType): Token = when (current.kind) {
        type -> next()
        SyntaxType.LineSeparator -> {
            skipSeparators()
            match(type)
        }
        else -> {
            diagnostics.reportUnexpectedToken(current.location, current.kind, type)
            if (trying != 0) throw ParsingError()
            Token(textFile, type, current.position).apply { isMissing = true }
        }
    }

    private inline fun skipSeparators() {
        while (current.kind == SyntaxType.LineSeparator) {
            position++
        }
    }

    fun parseCompilationUnit(): Collection<TopLevelNode> {
        val members = ArrayList<TopLevelNode>()

        while (current.kind != SyntaxType.EOF) {
            val startToken = current

            val member = parseGlobalStatement()
            member?.let { members.add(it) }

            if (startToken == current) {
                next()
            }

            skipSeparators()
        }
        match(SyntaxType.EOF)
        return members
    }

    internal fun parseAnnotations(): Collection<AnnotationNode> {
        val list = ArrayList<AnnotationNode>()
        var lastAnnotationLine = 0
        skipSeparators()
        while (current.kind == SyntaxType.At) {
            val character = next()
            if (character.location.startLineI > lastAnnotationLine + 1 && list.isNotEmpty()) {
                val last = list.last()
                diagnostics.reportInvalidAnnotation(last.location)
                if (trying != 0) throw ParsingError()
            }
            val identifier = match(SyntaxType.Identifier)
            if (current.kind == SyntaxType.OpenParentheses) {
                val call = parseCall(identifier)
                lastAnnotationLine = call.location.endLineI
                list.add(AnnotationNode(textFile, character, identifier, call))
            } else {
                lastAnnotationLine = identifier.location.endLineI
                list.add(AnnotationNode(textFile, character, identifier, null))
            }
            skipSeparators()
        }
        return list
    }

    internal fun parseLambda(annotations: Collection<AnnotationNode>): LambdaNode {
        val open = match(SyntaxType.OpenParentheses)
        val params = parseParamList()
        val closed = match(SyntaxType.ClosedParentheses)
        val type = parseOptionalValueTypeClause()
        skipSeparators()
        var arrow: Token? = null
        if (current.kind == SyntaxType.LambdaArrow) {
            arrow = match(SyntaxType.LambdaArrow)
        }
        val body = arrow?.let { parseStatement() }
        return LambdaNode(textFile, open, params, closed, type, arrow, body, annotations, null)
    }

    internal fun parseParamList(): SeparatedNodeList<ParameterNode> {
        return parseSeparatedList(SyntaxType.Comma, { it == SyntaxType.ClosedParentheses }) {
            val identifier = match(SyntaxType.Identifier)
            val type = parseTypeClause()
            ParameterNode(textFile, identifier, type)
        }
    }

    internal fun parseGlobalStatement(): TopLevelNode? {
        skipSeparators()
        val statement = if (current.kind == SyntaxType.NamespaceToken) {
            parseNamespace()
        } else parseStatement()
        if (statement !is TopLevelNode) {
            diagnostics.reportStatementCantBeGlobal(statement.location, statement.kind)
            if (trying != 0) throw ParsingError()
            return null
        }
        return statement
    }

    internal fun parseUseStatement(): TopLevelNode {

        val keyword = match(SyntaxType.Use)

        val directories = ArrayList<Token>()
        var isInclude = false

        while (current.kind != SyntaxType.LineSeparator) {
            directories.add(match(SyntaxType.Identifier))
            if (current.kind == SyntaxType.LineSeparator) {
                position++
                break
            } else if (current.kind == SyntaxType.Star) {
                position++
                isInclude = true
                break
            } else {
                match(SyntaxType.Dot)
            }
        }

        return UseStatementNode(
            textFile,
            keyword,
            directories,
            isInclude)
    }

    internal fun parseTypeDeclaration(annotations: Collection<AnnotationNode>): Node {
        val keyword = match(SyntaxType.Type)
        val identifier = match(SyntaxType.Identifier)
        val parent = if (current.kind == SyntaxType.Colon) {
            next()
            parseTypeClause()
        } else null
        val fields = if (current.kind == SyntaxType.OpenBrace) {
            next()
            parseInsideBraces {
                parseValVarDeclaration(parseAnnotations())
            }.also { match(SyntaxType.ClosedBrace) }
        } else null
        return TypeDeclarationNode(textFile, keyword, identifier, parent, fields, annotations)
    }

    internal fun parseNamespace(): NamespaceStatementNode {
        val keyword = match(SyntaxType.NamespaceToken)
        val identifier = match(SyntaxType.Identifier)

        val openBrace = match(SyntaxType.OpenBrace)

        val statements = parseInsideBraces {
            parseGlobalStatement()
        }

        val closedBrace = match(SyntaxType.ClosedBrace)
        return NamespaceStatementNode(textFile, keyword, identifier, openBrace, statements, closedBrace)
    }

    internal fun parseStatement(): Node {
        skipSeparators()
        val annotations = parseAnnotations()
        return when (current.kind) {
            SyntaxType.Val, SyntaxType.Var -> parseValVarDeclaration(annotations)
            SyntaxType.Loop -> parseLoop()
            SyntaxType.Break -> parseBreakStatement()
            SyntaxType.Continue -> parseContinueStatement()
            SyntaxType.Return -> parseReturnStatement()
            SyntaxType.Use -> parseUseStatement()
            SyntaxType.Type -> parseTypeDeclaration(annotations)
            else -> parseAssignmentStatement()
        }
    }

    internal fun parseAssignmentStatement(): Node {
        val left = parseExpression()
        val k = peek(0).kind
        if (isAssignee(left) && isAssignmentSymbol(k)) {
            val operatorToken = next()
            val right = parseExpression()
            return AssignmentNode(textFile, left, operatorToken, right)
        }
        return ExpressionStatementNode(textFile, left)
    }

    private fun isAssignmentSymbol(k: SyntaxType) =
        k == SyntaxType.Equals ||
        k == SyntaxType.PlusEquals ||
        k == SyntaxType.MinusEquals ||
        k == SyntaxType.DivEquals ||
        k == SyntaxType.TimesEquals ||
        k == SyntaxType.RemEquals ||
        k == SyntaxType.OrEquals ||
        k == SyntaxType.AndEquals

    private fun isAssignee(node: Node) =
        node.kind == SyntaxType.NameExpression ||
        node.kind == SyntaxType.IndexExpression ||
        node.kind == SyntaxType.BinaryExpression && (node as BinaryExpressionNode).operator.kind == SyntaxType.Dot

    internal fun parseValVarDeclaration(annotations: Collection<AnnotationNode>): ValVarDeclarationNode {
        skipSeparators()
        val expected = if (current.kind == SyntaxType.Val) {
            SyntaxType.Val
        } else {
            SyntaxType.Var
        }
        val keyword = match(expected)
        val isError = current.kind == SyntaxType.LineSeparator


        val extensionParam: ParameterNode?
        if (current.kind == SyntaxType.OpenParentheses) {
            next()
            val identifier = match(SyntaxType.Identifier)
            val type = parseTypeClause()
            extensionParam = ParameterNode(textFile, identifier, type)
            match(SyntaxType.ClosedParentheses)
        } else { extensionParam = null }


        val identifier = match(SyntaxType.Identifier)
        if (isError) {
            diagnostics.reportDeclarationAndNameOnSameLine(identifier.location)
            if (trying != 0) throw ParsingError()
        }

        val typeClause = if (extensionParam == null && peek(1).kind != SyntaxType.LambdaArrow) parseOptionalValueTypeClause() else null
        var equals: Token? = null

        if (typeClause == null && (
            extensionParam != null ||
            current.kind == SyntaxType.OpenParentheses ||
            current.kind == SyntaxType.LambdaArrow ||
            peek(1).kind == SyntaxType.OpenParentheses
        )) {
            if (current.kind == SyntaxType.Equals) {
                equals = match(SyntaxType.Equals)
            }
            val lambda = parseLambda(annotations)
            return ValVarDeclarationNode(textFile, keyword, identifier, typeClause, equals, lambda, extensionParam, emptyList()).also {
                lambda.declarationNode = it
            }
        } else {
            var initializer: Node? = null
            if (current.kind == SyntaxType.Equals) {
                equals = match(SyntaxType.Equals)
                initializer = parseExpression()
            } else if (typeClause == null) {
                diagnostics.reportCantInferType(identifier.location)
                if (trying != 0) throw ParsingError()
            }
            return ValVarDeclarationNode(textFile, keyword, identifier, typeClause, equals, initializer, extensionParam, annotations)
        }
    }

    internal fun couldBeTypeClause(offset: Int = 0): Pair<Boolean, Int> {
        var i = offset
        while (true) {
            if (peek(i++).kind != SyntaxType.Identifier) return false to 0
            if (peek(i).kind != SyntaxType.Dot) break
            i++
        }
        if (peek(i).kind != SyntaxType.LessThan) return true to i - 1
        val simpleI = i - 1
        i++
        while (true) {
            val (c, ni) = couldBeTypeClause(i)
            if (!c) return true to simpleI
            i = ni + 1
            if (peek(i).kind != SyntaxType.Comma) break
            i++
        }
        if (peek(i).kind != SyntaxType.MoreThan) return true to simpleI
        return true to i
    }

    private fun parseOptionalValueTypeClause(): TypeClauseNode? {
        return if (couldBeTypeClause().first) parseTypeClause() else null
    }

    internal fun parseTypeClause(): TypeClauseNode {
        val identifier = parseSeparatedList(SyntaxType.Dot, { it != SyntaxType.Identifier }) {
            match(SyntaxType.Identifier)
        }
        var start: Token? = null
        var types: SeparatedNodeList<TypeClauseNode>? = null
        var end: Token? = null
        if (current.kind == SyntaxType.LessThan) {
            start = match(SyntaxType.LessThan)
            types = parseSeparatedList(SyntaxType.Comma, { it == SyntaxType.MoreThan }, ::parseTypeClause)
            end = match(SyntaxType.MoreThan)
        }
        else if (current.kind == SyntaxType.OpenParentheses) {
            start = match(SyntaxType.OpenParentheses)
            types = parseSeparatedList(SyntaxType.Comma, { it == SyntaxType.ClosedParentheses }, ::parseTypeClause)
            end = match(SyntaxType.ClosedParentheses)
        }
        return TypeClauseNode(textFile, identifier, start, types, end)
    }

    private inline fun <T : Node> parseSeparatedList(separator: SyntaxType, isEnd: (SyntaxType) -> Boolean, parseNode: () -> T): SeparatedNodeList<T> {
        val nodesNSeparators = ArrayList<Node>()

        skipSeparators()

        while (
            !isEnd(current.kind) &&
            current.kind != SyntaxType.EOF
        ) {
            nodesNSeparators.add(parseNode())
            skipSeparators()
            if (current.kind == separator) {
                nodesNSeparators.add(match(separator))
            } else break
            skipSeparators()
        }

        return SeparatedNodeList(nodesNSeparators)
    }

    internal fun parseBlock(): BlockNode {
        val openBrace = match(SyntaxType.OpenBrace)

        val statements = parseInsideBraces {
            parseStatement()
        }

        val closedBrace = match(SyntaxType.ClosedBrace)
        return BlockNode(textFile, null, openBrace, statements, closedBrace, false)
    }

    private fun parseElseClause(): ElseClauseNode? {
        skipSeparators()
        if (current.kind != SyntaxType.Colon) {
            return null
        }
        val colon = next()
        val then = parseStatement()
        return ElseClauseNode(textFile, colon, then)
    }

    internal fun parseLoop(): Node {
        val keyword = match(SyntaxType.Loop)
        if (peek(1).kind == SyntaxType.Colon) {
            val identifier = match(SyntaxType.Identifier)
            val inToken = match(SyntaxType.Colon)
            val lowerBound = parseExpression()
            val rangeToken = match(SyntaxType.Range)
            val upperBound = parseExpression()
            val body = parseStatement()
            return IterationLoopStatementNode(textFile, keyword, identifier, inToken, lowerBound, rangeToken, upperBound, body)
        }
        val body = parseStatement()
        return LoopStatementNode(textFile, keyword, body)
    }

    internal fun parseBreakStatement(): Node {
        val keyword = match(SyntaxType.Break)
        return BreakNode(textFile, keyword)
    }

    internal fun parseContinueStatement(): Node {
        val keyword = match(SyntaxType.Continue)
        return ContinueNode(textFile, keyword)
    }

    internal fun parseReturnStatement(): Node {
        val keyword = match(SyntaxType.Return)
        val expression = if (current.kind != SyntaxType.LineSeparator && current.kind != SyntaxType.EOF) {
            parseExpression()
        } else null
        return ReturnStatementNode(textFile, keyword, expression)
    }

    internal fun parseExpression() = parseBinaryExpression()

    private fun parsePostUnaryExpression(pre: Node, parentPrecedence: Int): Node {
        if (parentPrecedence != SyntaxType.Dot.getBinaryOperatorPrecedence()) {
            when (current.kind) {
                SyntaxType.OpenBracket -> {
                    val open = match(SyntaxType.OpenBracket)
                    val arguments = parseSeparatedList(SyntaxType.Comma, { it == SyntaxType.ClosedBracket }, ::parseExpression)
                    val closed = match(SyntaxType.ClosedBracket)
                    return IndexExpressionNode(textFile, pre, open, arguments, closed)
                }
                SyntaxType.OpenParentheses -> return parseCall(pre)
                SyntaxType.As -> {
                    val keyword = match(SyntaxType.As)
                    val type = parseTypeClause()
                    return CastExpressionNode(textFile, pre, keyword, type)
                }
                SyntaxType.QuestionMark -> if (parentPrecedence < SyntaxType.QuestionMark.getBinaryOperatorPrecedence()) {
                    val q = match(SyntaxType.QuestionMark)
                    val then = parseStatement()
                    val elseClause = parseElseClause()
                    return IfNode(textFile, pre, q, then, elseClause)
                }
            }
        }
        return pre
    }

    private fun parseCall(pre: Node): CallExpressionNode {
        val open = match(SyntaxType.OpenParentheses)
        val arguments = parseSeparatedList(SyntaxType.Comma, { it == SyntaxType.ClosedParentheses }, ::parseExpression)
        val closed = match(SyntaxType.ClosedParentheses)
        return CallExpressionNode(textFile, pre, open, arguments, closed)
    }

    private fun parseBinaryExpression(parentPrecedence: Int = 0): Node {

        val unaryOperatorPrecedence = current.kind.getUnaryOperatorPrecedence()

        var left = if (unaryOperatorPrecedence != 0 && unaryOperatorPrecedence >= parentPrecedence) {
            val operatorToken = next()
            val operand = parsePostUnaryExpression(parseBinaryExpression(unaryOperatorPrecedence), parentPrecedence)
            UnaryExpressionNode(textFile, operatorToken, operand)
        } else {
            val primary = parsePrimaryExpression()
            primary
        }

        while (true) {
            left = parsePostUnaryExpression(left, parentPrecedence)
            val precedence = current.kind.getBinaryOperatorPrecedence()
            if (precedence == 0 || precedence <= parentPrecedence)
                break
            val operatorToken = next()
            val right = parseBinaryExpression(precedence)
            left = BinaryExpressionNode(textFile, left, operatorToken, right)
        }

        return left
    }

    private fun parsePrimaryExpression(): Node {
        val annotations = parseAnnotations()
        return when (current.kind) {
            SyntaxType.OpenParentheses, SyntaxType.LambdaArrow -> parseLambda(annotations)
            SyntaxType.False, SyntaxType.True -> parseBooleanLiteral()
            SyntaxType.I8,
            SyntaxType.I16,
            SyntaxType.I32,
            SyntaxType.I64 -> parseIntLiteral()
            SyntaxType.Float,
            SyntaxType.Double -> parseFloatLiteral()
            SyntaxType.String -> parseStringLiteral()
            SyntaxType.Char -> parseCharLiteral()
            SyntaxType.Unsafe -> parseUnsafeExpression()
            SyntaxType.OpenBrace -> parseBlock()
            else -> parseNameExpression()
        }
    }

    private fun parseUnsafeExpression(): Node {
        val keyword = match(SyntaxType.Unsafe)
        val openBrace = match(SyntaxType.OpenBrace)

        val statements = parseInsideBraces {
            parseStatement()
        }

        val closedBrace = match(SyntaxType.ClosedBrace)
        return BlockNode(textFile, keyword, openBrace, statements, closedBrace, true)
    }

    private fun parseBooleanLiteral(): BoolConstantNode {
        val token = next()
        return BoolConstantNode(textFile, token)
    }

    private fun parseIntLiteral(): LiteralExpressionNode {
        val token = next()
        return LiteralExpressionNode(textFile, if (
            token.kind == SyntaxType.I8 ||
            token.kind == SyntaxType.I16 ||
            token.kind == SyntaxType.I32 ||
            token.kind == SyntaxType.I64) {
            token
        } else {
            diagnostics.reportUnexpectedToken(current.location, current.kind, SyntaxType.I32)
            if (trying != 0) throw ParsingError()
            Token(textFile, SyntaxType.I32, current.position).apply { isMissing = true }
        })
    }

    private fun parseFloatLiteral(): Node {
        val token = next()
        return LiteralExpressionNode(textFile, if (
            token.kind == SyntaxType.Float ||
            token.kind == SyntaxType.Double) {
            token
        } else {
            diagnostics.reportUnexpectedToken(current.location, current.kind, SyntaxType.Float)
            if (trying != 0) throw ParsingError()
            Token(textFile, SyntaxType.Float, current.position).apply { isMissing = true }
        })
    }

    private fun parseStringLiteral() = TextConstantNode(textFile, match(SyntaxType.String))

    private fun parseCharLiteral() = LiteralExpressionNode(textFile, match(SyntaxType.Char))

    private fun parseNameExpression(): Node {
        val (couldBeType, i) = couldBeTypeClause()
        if (couldBeType) {
            if (peek(i + 1).kind == SyntaxType.OpenBrace) {
                if (peekOverLineSeparators(2, startOffset = i).kind == SyntaxType.Identifier) {
                    if (peekOverLineSeparators(3, startOffset = i).kind == SyntaxType.Colon) {
                        return parseStructInitialization(parseTypeClause())
                    }
                }
                if (levelContains(SyntaxType.Comma)) {
                    return parseCollectionInitialization(parseTypeClause())
                }
            }
        }

        val identifier = match(SyntaxType.Identifier)

        return NameExpressionNode(textFile, identifier)
    }

    private fun levelContains(type: SyntaxType): Boolean {
        var i = 1
        var braceDepth = 0
        var bracketDepth = 0
        var parenthesesDepth = 0
        while (true) {
            when (peek(i).kind) {
                SyntaxType.OpenBrace -> braceDepth++
                SyntaxType.ClosedBrace -> {
                    braceDepth--
                    if (braceDepth == 0) return false
                }
                SyntaxType.OpenBracket -> bracketDepth++
                SyntaxType.ClosedBracket -> {
                    bracketDepth--
                }
                SyntaxType.OpenParentheses -> parenthesesDepth++
                SyntaxType.ClosedParentheses -> {
                    parenthesesDepth--
                }
                SyntaxType.EOF -> return false
                else -> if (parenthesesDepth == 0 &&
                            bracketDepth == 0 &&
                            braceDepth == 0 &&
                            peek(i).kind == type) return true
            }
            i++
        }
    }

    private fun parseStructInitialization(type: TypeClauseNode): Node {
        val openBrace = match(SyntaxType.OpenBrace)
        val statements = parseInsideBraces {
            val name = match(SyntaxType.Identifier)
            val colon = match(SyntaxType.Colon)
            val expression = parseExpression()
            AssignmentNode(textFile, name, colon, expression)
        }
        val closedBrace = match(SyntaxType.ClosedBrace)
        return StructInitializationNode(textFile, type, openBrace, statements, closedBrace)
    }

    private fun parseCollectionInitialization(type: TypeClauseNode): Node {
        val openBrace = match(SyntaxType.OpenBrace)
        val expressions = parseSeparatedList(SyntaxType.Comma, { it == SyntaxType.ClosedBrace }, ::parseExpression)
        val closedBrace = match(SyntaxType.ClosedBrace)
        return CollectionInitializationNode(textFile, type, openBrace, expressions, closedBrace)
    }

    private fun <T : Node> parseInsideBraces(
        fn: () -> T?
    ): ArrayList<T> {
        skipSeparators()
        val statements = ArrayList<T>()
        while (
            current.kind != SyntaxType.EOF &&
            current.kind != SyntaxType.ClosedBrace
        ) {
            skipSeparators()
            val startToken = current

            fn()?.let { statements.add(it) }

            skipSeparators()
            if (startToken == current) {
                skipSeparators()
                next()
            }
        }
        return statements
    }
}