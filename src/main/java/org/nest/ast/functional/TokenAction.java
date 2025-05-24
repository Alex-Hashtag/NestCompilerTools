package org.nest.ast.functional;

import org.nest.ast.ASTBuildContext;
import org.nest.tokenization.Token;

import java.util.function.Consumer;


@FunctionalInterface
public interface TokenAction
{
    Consumer<Token> apply(ASTBuildContext self);
}