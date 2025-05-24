package org.nest.ast.functional;

import org.nest.ast.ASTBuildContext;


@FunctionalInterface
public interface ASTAction
{
    Runnable apply(ASTBuildContext self);
}