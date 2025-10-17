package org.nest.sprouts.ast;

/**
 * Sealed interface representing statements in Sprout-S
 */
public sealed interface Stmt extends SproutSNode
        permits Stmt.Let, Stmt.Set, Stmt.If, Stmt.While, Stmt.Print, Stmt.Exit {

    /**
     * Let statement: declares and initializes a variable
     */
    record Let(String name, Expr init) implements Stmt {}

    /**
     * Set statement: assigns to an existing variable
     */
    record Set(String name, Expr expr) implements Stmt {}

    /**
     * If statement: conditional with mandatory else branch
     */
    record If(Expr cond, Block then, Block elseBranch) implements Stmt {}

    /**
     * While statement: loop
     */
    record While(Expr cond, Block body) implements Stmt {}

    /**
     * Print statement: outputs an expression value
     */
    record Print(Expr expr) implements Stmt {}

    /**
     * Exit statement: terminates with exit code
     */
    record Exit(Expr expr) implements Stmt {}
}
