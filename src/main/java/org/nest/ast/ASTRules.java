package org.nest.ast;

public interface ASTRules {
    ASTWrapper createAST(TokenList tokens, ErrorManager errors);
}

