package org.nest.sprouts;

import org.nest.errors.ErrorManager;
import org.nest.sprouts.ast.*;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Interpreter for the Sprout-S language.
 * Executes Sprout-S AST with proper scoping, error handling, and output.
 * 
 * Features:
 * - Variable scoping with shadowing support
 * - Short-circuit evaluation for && and ||
 * - Division by zero detection
 * - Integer overflow handling (wraps in 32-bit)
 * - Proper exit code handling
 * - Error reporting via ErrorManager
 */
public class SproutSInterpreter {
    
    private final PrintStream output;
    private final ErrorManager errorManager;
    private final Stack<Map<String, Integer>> scopes;
    private int exitCode;
    private boolean exited;
    
    /**
     * Creates a new interpreter with specified output stream and error manager
     */
    public SproutSInterpreter(PrintStream output, ErrorManager errorManager) {
        this.output = output;
        this.errorManager = errorManager;
        this.scopes = new Stack<>();
        this.exitCode = 0;
        this.exited = false;
        
        // Push global scope
        scopes.push(new HashMap<>());
    }
    
    /**
     * Creates a new interpreter with default output stream and new error manager
     */
    public SproutSInterpreter() {
        this(System.out, new ErrorManager());
    }
    
    /**
     * Gets the error manager
     */
    public ErrorManager getErrorManager() {
        return errorManager;
    }
    
    /**
     * Executes a Sprout-S program and returns the exit code
     */
    public int execute(Program program) {
        try {
            for (Stmt stmt : program.stmts()) {
                if (exited) break;
                executeStmt(stmt);
                if (errorManager.hasErrors()) {
                    return 2; // Runtime error exit code
                }
            }
        } catch (RuntimeException e) {
            errorManager.error("Runtime error: " + e.getMessage(), 0, 0, "", "");
            return 2; // Runtime error exit code
        }
        
        return exitCode;
    }
    
    /**
     * Executes a single statement
     */
    private void executeStmt(Stmt stmt) {
        if (exited) return;
        
        switch (stmt) {
            case Stmt.Let let -> executeLet(let);
            case Stmt.Set set -> executeSet(set);
            case Stmt.If ifStmt -> executeIf(ifStmt);
            case Stmt.While whileStmt -> executeWhile(whileStmt);
            case Stmt.Print print -> executePrint(print);
            case Stmt.Exit exit -> executeExit(exit);
        }
    }
    
    /**
     * Executes a let statement (variable declaration)
     */
    private void executeLet(Stmt.Let let) {
        int value = evaluateExpr(let.init());
        
        // Check if variable already exists in current scope
        Map<String, Integer> currentScope = scopes.peek();
        if (currentScope.containsKey(let.name())) {
            errorManager.error(
                "Variable '" + let.name() + "' already declared in current scope",
                0, 0, let.name(),
                "Use a different variable name or use 'set' to modify existing variable"
            );
            return;
        }
        
        currentScope.put(let.name(), value);
    }
    
    /**
     * Executes a set statement (variable assignment)
     */
    private void executeSet(Stmt.Set set) {
        int value = evaluateExpr(set.expr());
        
        // Find variable in scope chain
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Map<String, Integer> scope = scopes.get(i);
            if (scope.containsKey(set.name())) {
                scope.put(set.name(), value);
                return;
            }
        }
        
        errorManager.error(
            "Cannot assign to undeclared variable '" + set.name() + "'",
            0, 0, set.name(),
            "Use 'let' to declare the variable first"
        );
    }
    
    /**
     * Executes an if statement
     */
    private void executeIf(Stmt.If ifStmt) {
        int condition = evaluateExpr(ifStmt.cond());
        
        if (condition != 0) {
            executeBlock(ifStmt.then());
        } else {
            executeBlock(ifStmt.elseBranch());
        }
    }
    
    /**
     * Executes a while loop
     */
    private void executeWhile(Stmt.While whileStmt) {
        while (!exited) {
            int condition = evaluateExpr(whileStmt.cond());
            if (condition == 0) break;
            
            executeBlock(whileStmt.body());
        }
    }
    
    /**
     * Executes a print statement
     */
    private void executePrint(Stmt.Print print) {
        int value = evaluateExpr(print.expr());
        output.println(value);
    }
    
    /**
     * Executes an exit statement
     */
    private void executeExit(Stmt.Exit exit) {
        exitCode = evaluateExpr(exit.expr());
        exited = true;
    }
    
    /**
     * Executes a block of statements (creates new scope)
     */
    private void executeBlock(Block block) {
        scopes.push(new HashMap<>());
        try {
            for (Stmt stmt : block.stmts()) {
                if (exited) break;
                executeStmt(stmt);
            }
        } finally {
            scopes.pop();
        }
    }
    
    /**
     * Evaluates an expression and returns its integer value
     */
    private int evaluateExpr(Expr expr) {
        return switch (expr) {
            case Expr.Int i -> i.value();
            case Expr.Var v -> lookupVariable(v.name());
            case Expr.Unary u -> evaluateUnary(u);
            case Expr.Binary b -> evaluateBinary(b);
            case Expr.Group g -> evaluateExpr(g.expr());
        };
    }
    
    /**
     * Looks up a variable in the scope chain
     */
    private int lookupVariable(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Map<String, Integer> scope = scopes.get(i);
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }
        
        errorManager.error(
            "Undefined variable '" + name + "'",
            0, 0, name,
            "Declare the variable with 'let' before using it"
        );
        return 0; // Return default value
    }
    
    /**
     * Evaluates a unary expression
     */
    private int evaluateUnary(Expr.Unary unary) {
        int operand = evaluateExpr(unary.expr());
        
        return switch (unary.op()) {
            case "-" -> -operand;  // Arithmetic negation (wraps on overflow)
            case "!" -> operand == 0 ? 1 : 0;  // Logical not
            default -> throw new RuntimeException("Unknown unary operator: " + unary.op());
        };
    }
    
    /**
     * Evaluates a binary expression
     */
    private int evaluateBinary(Expr.Binary binary) {
        // Handle short-circuit operators first
        if (binary.op().equals("&&")) {
            int left = evaluateExpr(binary.left());
            if (left == 0) return 0;  // Short-circuit: false && x = false
            int right = evaluateExpr(binary.right());
            return (right != 0) ? 1 : 0;
        }
        
        if (binary.op().equals("||")) {
            int left = evaluateExpr(binary.left());
            if (left != 0) return 1;  // Short-circuit: true || x = true
            int right = evaluateExpr(binary.right());
            return (right != 0) ? 1 : 0;
        }
        
        // For all other operators, evaluate both operands
        int left = evaluateExpr(binary.left());
        int right = evaluateExpr(binary.right());
        
        return switch (binary.op()) {
            // Arithmetic operators
            case "+" -> left + right;
            case "-" -> left - right;
            case "*" -> left * right;
            case "/" -> {
                if (right == 0) {
                    errorManager.error(
                        "Division by zero",
                        0, 0, "/",
                        "Ensure divisor is not zero"
                    );
                    yield 0;
                }
                yield left / right;  // Truncates toward zero
            }
            case "%" -> {
                if (right == 0) {
                    errorManager.error(
                        "Modulo by zero",
                        0, 0, "%",
                        "Ensure divisor is not zero"
                    );
                    yield 0;
                }
                yield left % right;
            }
            
            // Comparison operators
            case "<" -> left < right ? 1 : 0;
            case "<=" -> left <= right ? 1 : 0;
            case ">" -> left > right ? 1 : 0;
            case ">=" -> left >= right ? 1 : 0;
            case "==" -> left == right ? 1 : 0;
            case "!=" -> left != right ? 1 : 0;
            
            default -> {
                errorManager.error(
                    "Unknown binary operator: " + binary.op(),
                    0, 0, binary.op(),
                    "Use a valid operator"
                );
                yield 0;
            }
        };
    }
    
    /**
     * Returns the current exit code
     */
    public int getExitCode() {
        return exitCode;
    }
    
    /**
     * Returns whether the program has exited
     */
    public boolean hasExited() {
        return exited;
    }
}
