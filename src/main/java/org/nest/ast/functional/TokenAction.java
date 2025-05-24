package org.nest.ast.functional;

@FunctionalInterface
public interface TokenAction {
    Consumer<Token> apply(ASTBuildContext self);
}