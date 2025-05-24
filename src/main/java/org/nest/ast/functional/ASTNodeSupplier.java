package org.nest.ast.functional;

import org.nest.ast.ASTBuildContext;

import java.util.function.Supplier;


@FunctionalInterface
public interface ASTNodeSupplier
{
    Supplier<Object> apply(ASTBuildContext self);
}