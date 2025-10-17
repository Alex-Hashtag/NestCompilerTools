package org.nest.sprouts.ast;

/**
 * Sealed interface representing expressions in Sprout-S
 */
public sealed interface Expr extends SproutSNode
        permits Expr.Int, Expr.Var, Expr.Unary, Expr.Binary, Expr.Group {

    /**
     * Integer literal
     */
    record Int(int value) implements Expr {}

    /**
     * Variable reference
     */
    record Var(String name) implements Expr {}

    /**
     * Unary expression: - or !
     */
    record Unary(String op, Expr expr) implements Expr {}

    /**
     * Binary expression: arithmetic, comparison, logical
     */
    record Binary(String op, Expr left, Expr right) implements Expr {}

    /**
     * Grouped expression (parentheses)
     */
    record Group(Expr expr) implements Expr {}
}
