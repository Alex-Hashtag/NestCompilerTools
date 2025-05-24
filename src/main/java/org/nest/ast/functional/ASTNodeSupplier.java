package org.nest.ast.functional;

@FunctionalInterface
public interface ASTNodeSupplier {
    Supplier<Object> apply(ASTBuildContext self);
}