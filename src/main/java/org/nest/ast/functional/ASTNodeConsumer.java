package org.nest.ast.functional;

@FunctionalInterface
public interface ASTNodeConsumer {
    Consumer<Object> apply(ASTBuildContext self);
}