package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxTree

abstract class TopLevelNode(
    syntaxTree: SyntaxTree
) : StatementNode(syntaxTree)