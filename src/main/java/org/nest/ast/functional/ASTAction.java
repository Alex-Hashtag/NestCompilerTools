package org.nest.ast.functional;

@FunctionalInterface
public interface ASTAction {
    Runnable apply(ASTBuildContext self);
}