package org.nest.ast;

import org.nest.errors.ErrorManager;
import org.nest.tokenization.TokenList;


public interface ASTRules
{
    ASTWrapper createAST(TokenList tokens, ErrorManager errors);
}

