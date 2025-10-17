package org.nest.sprouts.ast;

import java.util.List;

/**
 * Root AST node representing a Sprout-S program
 */
public record Program(List<Stmt> stmts) implements SproutSNode {
}
