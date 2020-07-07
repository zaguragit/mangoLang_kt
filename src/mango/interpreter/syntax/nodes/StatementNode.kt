package mango.interpreter.syntax.nodes

import mango.interpreter.syntax.SyntaxTree

abstract class StatementNode(
    syntaxTree: SyntaxTree
) : Node(syntaxTree)