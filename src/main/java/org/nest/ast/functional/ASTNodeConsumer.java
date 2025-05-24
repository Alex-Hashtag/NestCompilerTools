package org.nest.ast.functional;

import org.nest.ast.ASTBuildContext;

import java.util.function.Consumer;


@FunctionalInterface
public interface ASTNodeConsumer
{
    Consumer<Object> apply(ASTBuildContext self);
}