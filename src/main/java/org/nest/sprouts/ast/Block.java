package org.nest.sprouts.ast;

import java.util.List;


/**
 * Block: a sequence of statements enclosed in braces
 */
public record Block(List<Stmt> stmts) implements SproutSNode
{
}
