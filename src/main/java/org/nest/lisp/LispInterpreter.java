package org.nest.lisp;

import org.nest.errors.ErrorManager;
import org.nest.lisp.ast.*;
import org.nest.lisp.parser.LispParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Lisp interpreter that evaluates Lisp expressions.
 * Supports variables, functions, macros, and special forms.
 */
public class LispInterpreter {
    // Definitions environment
    private LispDefinitions rootEnvironment;
    private LispDefinitions currentEnvironment;

    // Error manager for reporting errors
    private final ErrorManager errorManager;
    
    // Return flag and value for loop/return control
    private boolean returning = false;
    private LispNode returnValue = LispAtom.LispNil.INSTANCE;

    /**
     * Creates a new Lisp interpreter with a default error manager.
     */
    public LispInterpreter() {
        this(new ErrorManager());
    }

    /**
     * Creates a new Lisp interpreter with the specified error manager.
     *
     * @param errorManager The error manager for reporting errors
     */
    public LispInterpreter(ErrorManager errorManager) {
        this.errorManager = errorManager;
        this.rootEnvironment = new LispDefinitions();
        this.currentEnvironment = rootEnvironment;
    }

    /**
     * Resets the interpreter state to a clean environment.
     */
    public void reset() {
        this.rootEnvironment = new LispDefinitions();
        this.currentEnvironment = rootEnvironment;
    }

    /**
     * Gets the current environment.
     *
     * @return The current environment
     */
    public LispDefinitions getEnvironment() {
        return currentEnvironment;
    }
    
    /**
     * Checks if the interpreter is currently in a returning state.
     * Used by loop constructs to detect early termination.
     *
     * @return true if a return was triggered, false otherwise
     */
    public boolean isReturning() {
        return returning;
    }
    
    /**
     * Sets the return flag to indicate a return statement was encountered.
     *
     * @param returning true to set the return flag, false to clear it
     */
    public void setReturnFlag(boolean returning) {
        this.returning = returning;
    }
    
    /**
     * Clears the return flag.
     */
    public void clearReturnFlag() {
        this.returning = false;
    }
    
    /**
     * Gets the current return value.
     *
     * @return The current return value
     */
    public LispNode getReturnValue() {
        return returnValue;
    }
    
    /**
     * Sets the return value.
     *
     * @param value The return value to set
     */
    public void setReturnValue(LispNode value) {
        this.returnValue = value;
    }

    /**
     * Gets the error manager.
     *
     * @return The error manager
     */
    public ErrorManager getErrorManager() {
        return errorManager;
    }

    /**
     * Evaluates a Lisp AST and returns the result.
     *
     * @param ast The Lisp AST to evaluate
     * @return The result of evaluating the last expression in the AST, or null if there's an error
     */
    public LispNode evaluate(LispAST ast) {
        LispNode result = LispAtom.LispNil.INSTANCE;

        for (LispNode node : ast.nodes()) {
            result = evaluate(node);
            if (result == null && errorManager.hasErrors()) {
                return null;
            }
        }

        return result;
    }

    /**
     * Evaluates a Lisp string and returns the result.
     *
     * @param code The Lisp code to evaluate
     * @return The result of evaluating the last expression in the code, or null if there's an error
     */
    public LispNode evaluate(String code) {
        LispAST ast = LispParser.parse(code, errorManager);
        if (errorManager.hasErrors()) {
            return null;
        }
        return evaluate(ast);
    }

    /**
     * Evaluates a Lisp node and returns the result.
     *
     * @param node The Lisp node to evaluate
     * @return The result of evaluating the node, or null if there's an error
     */
    public LispNode evaluate(LispNode node) {
        // Handle different node types
        if (node instanceof LispAtom.LispSymbol) {
            return evaluateSymbol(((LispAtom.LispSymbol) node));
        } else if (node instanceof LispList) {
            return evaluateList(((LispList) node).elements());
        } else if (node instanceof LispAtom) {
            // Self-evaluating atoms (numbers, strings, etc.)
            return node;
        } else {
            errorManager.error("Unknown node type: " + node.getClass().getName(), 0, 0, node.toString(), "Check node type");
            return null;
        }
    }

    /**
     * Evaluates a symbol by looking it up in the current environment.
     *
     * @param symbol The symbol to evaluate
     * @return The value of the symbol, or null if there's an error
     */
    private LispNode evaluateSymbol(LispAtom.LispSymbol symbol) {
        String name = symbol.name();

        // Look up variable in the environment
        LispNode value = currentEnvironment.lookupVariable(name);
        if (value != null) {
            return value;
        }

        // Check if it's a function name
        if (currentEnvironment.lookupFunction(name) != null) {
            // Return the symbol itself, which will be resolved during function call
            return symbol;
        }

        // Check if it's a built-in function
        if (currentEnvironment.lookupBuiltin(name) != null) {
            // Return the symbol itself, which will be resolved during function call
            return symbol;
        }

        // If not found, it's an undefined symbol
        errorManager.error("Undefined symbol: " + name, 0, 0, name, "Define the variable before using it");
        return null;
    }

    /**
     * Evaluates a list as a function call, macro expansion, or special form.
     *
     * @param elements The list elements
     * @return The result of evaluating the list, or null if there's an error
     */
    public LispNode evaluateList(List<LispNode> elements) {
        if (elements.isEmpty()) {
            errorManager.error("Cannot evaluate an empty list", 0, 0, "()", "Provide a function or special form");
            return null;
        }

        // Get the first element (operator)
        LispNode first = elements.get(0);

        // Handle special forms
        if (first instanceof LispAtom.LispSymbol) {
            String name = ((LispAtom.LispSymbol) first).name();
            switch (name) {
                case "quote":
                    return evaluateQuote(elements);
                case "quasiquote":
                    return evaluateQuasiquote(elements);
                case "if":
                    return evaluateIf(elements);
                case "cond":
                    return evaluateCond(elements);
                case "define":
                    return evaluateDefine(elements);
                case "set!":
                    return evaluateSet(elements);
                case "lambda":
                    return evaluateLambda(elements);
                case "let":
                    return evaluateLet(elements);
                case "begin":
                    return evaluateBegin(elements);
                case "defmacro":
                    return evaluateDefmacro(elements);
                case "let*":
                    return evaluateLetStar(elements);
                case "letrec":
                    return evaluateLetRec(elements);
                case "do":
                    return evaluateDo(elements);
                case "progn":
                    return evaluateBegin(elements); // Common Lisp's progn is equivalent to Scheme's begin
                case "when":
                    return evaluateWhen(elements);
                case "unless":
                    return evaluateUnless(elements);
                case "case":
                    return evaluateCase(elements);
            }

            // Check for macro
            LispDefinitions.LispMacro macro = currentEnvironment.lookupMacro(name);
            if (macro != null) {
                return evaluateMacroCall(macro, elements.subList(1, elements.size()));
            }
        }

        // Regular function call
        return evaluateFunctionCall(elements);
    }

    /**
     * Evaluates a quote special form.
     *
     * @param elements The list elements
     * @return The quoted expression, or null if there's an error
     */
    private LispNode evaluateQuote(List<LispNode> elements) {
        if (elements.size() != 2) {
            errorManager.error("quote requires exactly one argument", 0, 0, "quote", "Provide exactly one argument");
            return null;
        }
        return elements.get(1);
    }

    /**
     * Evaluates a quasiquote special form.
     *
     * @param elements The list elements
     * @return The result of evaluating the quasiquote, or null if there's an error
     */
    private LispNode evaluateQuasiquote(List<LispNode> elements) {
        if (elements.size() != 2) {
            errorManager.error("quasiquote requires exactly one argument", 0, 0, "quasiquote", "Provide exactly one argument");
            return null;
        }
        return quasiquote(elements.get(1), 1);
    }

    /**
     * Implements quasiquote with the specified nesting level.
     *
     * @param expr The expression to quasiquote
     * @param level The nesting level
     * @return The result of quasiquoting the expression, or null if there's an error
     */
    private LispNode quasiquote(LispNode expr, int level) {
        if (expr instanceof LispList) {
            List<LispNode> elements = ((LispList) expr).elements();
            if (elements.isEmpty()) {
                return expr;
            }

            // Handle unquote
            if (elements.get(0) instanceof LispAtom.LispSymbol &&
                    ((LispAtom.LispSymbol) elements.get(0)).name().equals("unquote")) {
                if (level == 1) {
                    if (elements.size() != 2) {
                        errorManager.error("unquote requires exactly one argument", 0, 0, "unquote", "Provide exactly one argument");
                        return null;
                    }
                    return evaluate(elements.get(1));
                } else {
                    // Nested unquote - decrement level
                    List<LispNode> unquoteElements = new ArrayList<>();
                    unquoteElements.add(elements.get(0)); // The unquote symbol
                    for (int i = 1; i < elements.size(); i++) {
                        LispNode subResult = quasiquote(elements.get(i), level - 1);
                        if (subResult == null) return null;
                        unquoteElements.add(subResult);
                    }
                    return new LispList(unquoteElements);
                }
            }

            // Handle nested quasiquote
            if (elements.get(0) instanceof LispAtom.LispSymbol &&
                    ((LispAtom.LispSymbol) elements.get(0)).name().equals("quasiquote")) {
                if (elements.size() != 2) {
                    errorManager.error("quasiquote requires exactly one argument", 0, 0, "quasiquote", "Provide exactly one argument");
                    return null;
                }
                // Nested quasiquote - increment level
                List<LispNode> quasiquoteElements = new ArrayList<>();
                quasiquoteElements.add(elements.get(0)); // The quasiquote symbol
                LispNode subResult = quasiquote(elements.get(1), level + 1);
                if (subResult == null) return null;
                quasiquoteElements.add(subResult);
                return new LispList(quasiquoteElements);
            }

            // Handle unquote-splicing
            if (elements.get(0) instanceof LispAtom.LispSymbol &&
                    ((LispAtom.LispSymbol) elements.get(0)).name().equals("unquote-splicing")) {
                // unquote-splicing must be in a list context
                errorManager.error("unquote-splicing not in list context", 0, 0, "unquote-splicing", "Use unquote-splicing inside a list");
                return null;
            }

            // Process the elements
            List<LispNode> result = new ArrayList<>();
            for (int i = 0; i < elements.size(); i++) {
                LispNode elem = elements.get(i);
                if (elem instanceof LispList) {
                    List<LispNode> subElements = ((LispList) elem).elements();
                    if (!subElements.isEmpty() && subElements.get(0) instanceof LispAtom.LispSymbol &&
                            ((LispAtom.LispSymbol) subElements.get(0)).name().equals("unquote-splicing") && level == 1) {
                        if (subElements.size() != 2) {
                            errorManager.error("unquote-splicing requires exactly one argument", 0, 0, "unquote-splicing", "Provide exactly one argument");
                            return null;
                        }
                        // Evaluate the unquote-splicing expression
                        LispNode spliceResult = evaluate(subElements.get(1));
                        if (spliceResult == null) return null;

                        if (!(spliceResult instanceof LispList)) {
                            errorManager.error("unquote-splicing requires a list result", 0, 0, spliceResult.toString(), "Expression must evaluate to a list");
                            return null;
                        }
                        // Add all elements from the splice result
                        result.addAll(((LispList) spliceResult).elements());
                    } else {
                        // Regular quasiquote processing
                        LispNode subResult = quasiquote(elem, level);
                        if (subResult == null) return null;
                        result.add(subResult);
                    }
                } else {
                    // Atoms are returned as-is
                    result.add(elem);
                }
            }
            return new LispList(result);
        } else {
            // Atoms are returned as-is
            return expr;
        }
    }

    /**
     * Evaluates an if special form.
     *
     * @param elements The list elements
     * @return The result of evaluating the appropriate branch, or null if there's an error
     */
    private LispNode evaluateIf(List<LispNode> elements) {
        if (elements.size() < 3 || elements.size() > 4) {
            errorManager.error("if requires 2 or 3 arguments", 0, 0, "if", "Provide 2 or 3 arguments");
            return null;
        }

        LispNode condition = evaluate(elements.get(1));
        if (condition == null && errorManager.hasErrors()) {
            return null;
        }

        // Anything except #f and nil is true
        boolean conditionValue = !(condition instanceof LispAtom.LispBoolean && !((LispAtom.LispBoolean) condition).value())
                && condition != LispAtom.LispNil.INSTANCE;

        if (conditionValue) {
            return evaluate(elements.get(2));
        } else if (elements.size() == 4) {
            return evaluate(elements.get(3));
        } else {
            return LispAtom.LispNil.INSTANCE;
        }
    }

    /**
     * Evaluates a cond special form.
     *
     * @param elements The list elements
     * @return The result of evaluating the first matching clause, or null if there's an error
     */
    private LispNode evaluateCond(List<LispNode> elements) {
        // Each clause is (condition expr)
        for (int i = 1; i < elements.size(); i++) {
            if (!(elements.get(i) instanceof LispList)) {
                errorManager.error("cond clause must be a list", 0, 0, elements.get(i).toString(), "Provide a list for each cond clause");
                return null;
            }

            List<LispNode> clause = ((LispList) elements.get(i)).elements();
            if (clause.isEmpty()) {
                errorManager.error("cond clause cannot be empty", 0, 0, "()", "Provide at least a condition");
                return null;
            }

            // Special case for 'else'
            if (clause.get(0) instanceof LispAtom.LispSymbol &&
                ((LispAtom.LispSymbol) clause.get(0)).name().equals("else")) {
                if (i < elements.size() - 1) {
                    errorManager.error("else must be the last clause in cond", 0, 0, "else", "Place else as the last clause");
                    return null;
                }
                return evaluateBeginLike(clause.subList(1, clause.size()));
            }

            // Evaluate the condition
            LispNode condition = evaluate(clause.get(0));
            if (condition == null && errorManager.hasErrors()) {
                return null;
            }

            // If condition is true, evaluate the body
            if (isTruthy(condition)) {
                return evaluateBeginLike(clause.subList(1, clause.size()));
            }
        }

        // No matching clause
        return LispAtom.LispNil.INSTANCE;
    }

    /**
     * Helper method to determine if a Lisp node is truthy.
     *
     * @param node The node to check
     * @return true if the node is truthy, false otherwise
     */
    private boolean isTruthy(LispNode node) {
        if (node instanceof LispAtom.LispBoolean) {
            return ((LispAtom.LispBoolean) node).value();
        }
        return node != LispAtom.LispNil.INSTANCE;
    }

    /**
     * Evaluates a sequence of expressions like in begin.
     *
     * @param expressions The expressions to evaluate
     * @return The result of the last expression, or null if there's an error
     */
    private LispNode evaluateBeginLike(List<LispNode> expressions) {
        LispNode result = LispAtom.LispNil.INSTANCE;

        for (LispNode expr : expressions) {
            result = evaluate(expr);
            if (result == null && errorManager.hasErrors()) {
                return null;
            }
        }

        return result;
    }

    /**
     * Evaluates a begin special form.
     *
     * @param elements The list elements
     * @return The result of the last expression, or null if there's an error
     */
    private LispNode evaluateBegin(List<LispNode> elements) {
        if (elements.size() < 2) {
            errorManager.error("begin requires at least one expression", 0, 0, "begin", "Provide at least one expression");
            return null;
        }

        return evaluateBeginLike(elements.subList(1, elements.size()));
    }

    /**
     * Evaluates a define special form.
     *
     * @param elements The list elements
     * @return The defined value or function, or null if there's an error
     */
    private LispNode evaluateDefine(List<LispNode> elements) {
        if (elements.size() < 3) {
            errorManager.error("define requires at least 2 arguments", 0, 0, "define", "Provide at least 2 arguments");
            return null;
        }

        // Function definition: (define (name args...) body...)
        if (elements.get(1) instanceof LispList) {
            if (((LispList) elements.get(1)).elements().isEmpty()) {
                errorManager.error("Function name missing in define", 0, 0, "define", "Provide a function name");
                return null;
            }

            if (!(((LispList) elements.get(1)).elements().get(0) instanceof LispAtom.LispSymbol)) {
                errorManager.error("Function name must be a symbol", 0, 0, ((LispList) elements.get(1)).elements().get(0).toString(), "Use a symbol for function name");
                return null;
            }

            String functionName = ((LispAtom.LispSymbol) ((LispList) elements.get(1)).elements().get(0)).name();

            // Extract parameter names
            List<String> params = new ArrayList<>();
            for (int i = 1; i < ((LispList) elements.get(1)).elements().size(); i++) {
                if (!(((LispList) elements.get(1)).elements().get(i) instanceof LispAtom.LispSymbol)) {
                    errorManager.error("Function parameter must be a symbol", 0, 0, ((LispList) elements.get(1)).elements().get(i).toString(), "Use symbols for parameter names");
                    return null;
                }
                params.add(((LispAtom.LispSymbol) ((LispList) elements.get(1)).elements().get(i)).name());
            }

            // Create the function body
            LispNode body;
            if (elements.size() == 3) {
                body = elements.get(2);
            } else {
                // Wrap multiple expressions in a begin
                List<LispNode> beginBody = new ArrayList<>();
                beginBody.add(new LispAtom.LispSymbol("begin"));
                beginBody.addAll(elements.subList(2, elements.size()));
                body = new LispList(beginBody);
            }

            // Define the function
            currentEnvironment.defineFunction(functionName, params, body);

            // Return the function name as a symbol
            return new LispAtom.LispSymbol(functionName);
        }
        // Variable definition: (define name value)
        else if (elements.get(1) instanceof LispAtom.LispSymbol) {
            String variableName = ((LispAtom.LispSymbol) elements.get(1)).name();
            LispNode value = evaluate(elements.get(2));
            if (value == null && errorManager.hasErrors()) {
                return null;
            }
            currentEnvironment.defineVariable(variableName, value);
            return value;  // Return the value instead of nil
        } else {
            errorManager.error("First argument to define must be a symbol or list", 0, 0, elements.get(1).toString(), "Use a symbol for variable name or a list for function definition");
            return null;
        }
    }

    /**
     * Evaluates a set! special form.
     *
     * @param elements The list elements
     * @return The assigned value or nil, or null if there's an error
     */
    private LispNode evaluateSet(List<LispNode> elements) {
        if (elements.size() != 3) {
            errorManager.error("set! requires exactly 2 arguments", 0, 0, "set!", "Provide exactly 2 arguments");
            return null;
        }

        if (!(elements.get(1) instanceof LispAtom.LispSymbol)) {
            errorManager.error("First argument to set! must be a symbol", 0, 0, elements.get(1).toString(), "Use a symbol for variable name");
            return null;
        }

        String variableName = ((LispAtom.LispSymbol) elements.get(1)).name();
        LispNode value = evaluate(elements.get(2));
        if (value == null && errorManager.hasErrors()) {
            return null;
        }

        if (!currentEnvironment.setVariable(variableName, value, errorManager)) {
            return null; // Error already reported by setVariable
        }

        return value; // Return the assigned value
    }

    /**
     * Evaluates a lambda special form.
     *
     * @param elements The list elements
     * @return A function object, or null if there's an error
     */
    private LispNode evaluateLambda(List<LispNode> elements) {
        if (elements.size() < 3) {
            errorManager.error("lambda requires at least 2 arguments", 0, 0, "lambda", "Provide parameters list and body");
            return null;
        }

        if (!(elements.get(1) instanceof LispList)) {
            errorManager.error("Lambda parameters must be a list", 0, 0, elements.get(1).toString(), "Provide a list of parameter names");
            return null;
        }

        // Extract parameter names
        List<String> params = new ArrayList<>();
        for (LispNode param : ((LispList) elements.get(1)).elements()) {
            if (!(param instanceof LispAtom.LispSymbol)) {
                errorManager.error("Lambda parameter must be a symbol", 0, 0, param.toString(), "Use symbols for parameter names");
                return null;
            }
            params.add(((LispAtom.LispSymbol) param).name());
        }

        // Create the function body
        LispNode body;
        if (elements.size() == 3) {
            body = elements.get(2);
        } else {
            // Wrap multiple expressions in a begin
            List<LispNode> beginBody = new ArrayList<>();
            beginBody.add(new LispAtom.LispSymbol("begin"));
            beginBody.addAll(elements.subList(2, elements.size()));
            body = new LispList(beginBody);
        }

        // Define the function in the current environment
        LispDefinitions.LispFunction func = new LispDefinitions.LispFunction(params, body);

        // Store the lambda in the current environment as a symbol
        String lambdaId = "_lambda_" + System.identityHashCode(func);
        currentEnvironment.defineVariable(lambdaId, new LispList(elements));

        // Return the lambda identifier as a symbol
        return new LispAtom.LispSymbol(lambdaId);
    }

    /**
     * Evaluates a let special form.
     *
     * @param elements The list elements
     * @return The result of evaluating the body in the new environment, or null if there's an error
     */
    private LispNode evaluateLet(List<LispNode> elements) {
        if (elements.size() < 3) {
            errorManager.error("let requires at least 2 arguments", 0, 0, "let", "Provide bindings and body");
            return null;
        }

        if (!(elements.get(1) instanceof LispList)) {
            errorManager.error("let bindings must be a list", 0, 0, elements.get(1).toString(), "Provide a list of bindings");
            return null;
        }

        // Create a new environment for the let
        LispDefinitions letEnv = currentEnvironment.createChildScope();
        LispDefinitions savedEnv = currentEnvironment;
        currentEnvironment = letEnv;

        try {
            // Process bindings
            for (LispNode binding : ((LispList) elements.get(1)).elements()) {
                if (!(binding instanceof LispList) || ((LispList) binding).elements().size() != 2) {
                    errorManager.error("let binding must be a list of two elements", 0, 0, binding.toString(), "Provide (name value) pairs");
                    return null;
                }

                if (!(((LispList) binding).elements().get(0) instanceof LispAtom.LispSymbol)) {
                    errorManager.error("let binding name must be a symbol", 0, 0, ((LispList) binding).elements().get(0).toString(), "Use a symbol for binding name");
                    return null;
                }

                String variableName = ((LispAtom.LispSymbol) ((LispList) binding).elements().get(0)).name();
                LispNode value = evaluate(((LispList) binding).elements().get(1));
                if (value == null && errorManager.hasErrors()) {
                    return null;
                }

                letEnv.defineVariable(variableName, value);
            }

            // Evaluate the body in the new environment
            return evaluateBeginLike(elements.subList(2, elements.size()));
        } finally {
            // Restore the original environment
            currentEnvironment = savedEnv;
        }
    }

    /**
     * Evaluates a defmacro special form.
     *
     * @param elements The list elements
     * @return The defined macro or nil, or null if there's an error
     */
    private LispNode evaluateDefmacro(List<LispNode> elements) {
        if (elements.size() < 4) {
            errorManager.error("defmacro requires at least 3 arguments", 0, 0, "defmacro", "Provide name, parameters, and body");
            return null;
        }

        // Extract macro name
        if (!(elements.get(1) instanceof LispAtom.LispSymbol)) {
            errorManager.error("Macro name must be a symbol", 0, 0, elements.get(1).toString(), "Use a symbol for macro name");
            return null;
        }

        String macroName = ((LispAtom.LispSymbol) elements.get(1)).name();

        // Extract parameter list
        if (!(elements.get(2) instanceof LispList)) {
            errorManager.error("Macro parameters must be a list", 0, 0, elements.get(2).toString(), "Provide a list of parameter names");
            return null;
        }

        List<String> params = new ArrayList<>();
        for (LispNode param : ((LispList) elements.get(2)).elements()) {
            if (!(param instanceof LispAtom.LispSymbol)) {
                errorManager.error("Macro parameter must be a symbol", 0, 0, param.toString(), "Use symbols for parameter names");
                return null;
            }
            params.add(((LispAtom.LispSymbol) param).name());
        }

        // Create the macro body
        LispNode body;
        if (elements.size() == 4) {
            body = elements.get(3);
        } else {
            // Wrap multiple expressions in a begin
            List<LispNode> beginBody = new ArrayList<>();
            beginBody.add(new LispAtom.LispSymbol("begin"));
            beginBody.addAll(elements.subList(3, elements.size()));
            body = new LispList(beginBody);
        }

        // Define the macro
        currentEnvironment.defineMacro(macroName, params, body);
        return new LispAtom.LispSymbol(macroName); // Return the macro name
    }

    /**
     * Evaluates a let* special form.
     * Similar to let, but allows each binding to refer to previous bindings.
     *
     * @param elements The list elements
     * @return The result of evaluating the body in the new environment, or null if there's an error
     */
    private LispNode evaluateLetStar(List<LispNode> elements) {
        if (elements.size() < 3) {
            errorManager.error("let* requires at least 2 arguments", 0, 0, "let*", "Provide bindings and body");
            return null;
        }

        if (!(elements.get(1) instanceof LispList)) {
            errorManager.error("let* bindings must be a list", 0, 0, elements.get(1).toString(), "Provide a list of bindings");
            return null;
        }

        // Create a new environment for the let*
        LispDefinitions letEnv = currentEnvironment.createChildScope();
        LispDefinitions savedEnv = currentEnvironment;
        currentEnvironment = letEnv;

        try {
            // Process bindings sequentially
            for (LispNode binding : ((LispList) elements.get(1)).elements()) {
                if (!(binding instanceof LispList) || ((LispList) binding).elements().size() != 2) {
                    errorManager.error("let* binding must be a list of two elements", 0, 0, binding.toString(), "Provide (name value) pairs");
                    return null;
                }

                if (!(((LispList) binding).elements().get(0) instanceof LispAtom.LispSymbol)) {
                    errorManager.error("let* binding name must be a symbol", 0, 0, ((LispList) binding).elements().get(0).toString(), "Use a symbol for binding name");
                    return null;
                }

                String variableName = ((LispAtom.LispSymbol) ((LispList) binding).elements().get(0)).name();
                LispNode value = evaluate(((LispList) binding).elements().get(1));
                if (value == null && errorManager.hasErrors()) {
                    return null;
                }

                letEnv.defineVariable(variableName, value);
            }

            // Evaluate the body in the new environment
            return evaluateBeginLike(elements.subList(2, elements.size()));
        } finally {
            // Restore the original environment
            currentEnvironment = savedEnv;
        }
    }

    /**
     * Evaluates a letrec special form.
     * Like let, but allows recursion among the bindings.
     *
     * @param elements The list elements
     * @return The result of evaluating the body in the new environment, or null if there's an error
     */
    private LispNode evaluateLetRec(List<LispNode> elements) {
        if (elements.size() < 3) {
            errorManager.error("letrec requires at least 2 arguments", 0, 0, "letrec", "Provide bindings and body");
            return null;
        }

        if (!(elements.get(1) instanceof LispList)) {
            errorManager.error("letrec bindings must be a list", 0, 0, elements.get(1).toString(), "Provide a list of bindings");
            return null;
        }

        // Create a new environment for the letrec
        LispDefinitions letEnv = currentEnvironment.createChildScope();
        LispDefinitions savedEnv = currentEnvironment;
        currentEnvironment = letEnv;

        try {
            // First pass: define variables with placeholder values
            for (LispNode binding : ((LispList) elements.get(1)).elements()) {
                if (!(binding instanceof LispList) || ((LispList) binding).elements().size() != 2) {
                    errorManager.error("letrec binding must be a list of two elements", 0, 0, binding.toString(), "Provide (name value) pairs");
                    return null;
                }

                if (!(((LispList) binding).elements().get(0) instanceof LispAtom.LispSymbol)) {
                    errorManager.error("letrec binding name must be a symbol", 0, 0, ((LispList) binding).elements().get(0).toString(), "Use a symbol for binding name");
                    return null;
                }

                String variableName = ((LispAtom.LispSymbol) ((LispList) binding).elements().get(0)).name();
                letEnv.defineVariable(variableName, LispAtom.LispNil.INSTANCE);
            }

            // Second pass: set the actual values
            for (LispNode binding : ((LispList) elements.get(1)).elements()) {
                String variableName = ((LispAtom.LispSymbol) ((LispList) binding).elements().get(0)).name();
                LispNode value = evaluate(((LispList) binding).elements().get(1));
                if (value == null && errorManager.hasErrors()) {
                    return null;
                }
                letEnv.setVariable(variableName, value, errorManager);
                if (errorManager.hasErrors()) {
                    return null;
                }
            }

            // Evaluate the body in the new environment
            return evaluateBeginLike(elements.subList(2, elements.size()));
        } finally {
            // Restore the original environment
            currentEnvironment = savedEnv;
        }
    }

    /**
     * Evaluates a do special form.
     * Provides iteration with an explicit exit condition.
     *
     * @param elements The list elements
     * @return The result of evaluating the do expression, or null if there's an error
     */
    private LispNode evaluateDo(List<LispNode> elements) {
        if (elements.size() < 3) {
            errorManager.error("do requires at least 2 arguments", 0, 0, "do", "Provide variable specs and test form");
            return null;
        }

        if (!(elements.get(1) instanceof LispList)) {
            errorManager.error("First argument to do must be a list of variable specs", 0, 0, elements.get(1).toString(), "Provide a list of variable specifications");
            return null;
        }

        if (!(elements.get(2) instanceof LispList)) {
            errorManager.error("Second argument to do must be a test and result form", 0, 0, elements.get(2).toString(), "Provide a test and result form");
            return null;
        }

        List<LispNode> varSpecs = ((LispList) elements.get(1)).elements();
        List<LispNode> testResult = ((LispList) elements.get(2)).elements();

        if (testResult.isEmpty()) {
            errorManager.error("Test and result form cannot be empty", 0, 0, "()", "Provide at least a test condition");
            return null;
        }

        // Extract variable names, initial values, and step expressions
        List<String> varNames = new ArrayList<>();
        List<LispNode> initialValues = new ArrayList<>();
        List<LispNode> stepExpressions = new ArrayList<>();

        for (LispNode spec : varSpecs) {
            if (!(spec instanceof LispList)) {
                errorManager.error("Variable specification must be a list", 0, 0, spec.toString(), "Provide (name init step) triplets");
                return null;
            }

            List<LispNode> specElements = ((LispList) spec).elements();

            if (specElements.size() < 2) {
                errorManager.error("Variable specification must include at least a name and initial value", 0, 0, spec.toString(), "Provide at least (name init)");
                return null;
            }

            if (!(specElements.get(0) instanceof LispAtom.LispSymbol)) {
                errorManager.error("Variable name must be a symbol", 0, 0, specElements.get(0).toString(), "Use a symbol for variable name");
                return null;
            }

            varNames.add(((LispAtom.LispSymbol) specElements.get(0)).name());
            initialValues.add(specElements.get(1));

            // Step expression is optional, default to the variable itself
            if (specElements.size() > 2) {
                stepExpressions.add(specElements.get(2));
            } else {
                stepExpressions.add(specElements.get(0));
            }
        }

        // Create an environment for the loop
        LispDefinitions loopEnv = currentEnvironment.createChildScope();
        LispDefinitions savedEnv = currentEnvironment;
        currentEnvironment = loopEnv;

        try {
            // Initialize variables
            for (int i = 0; i < varNames.size(); i++) {
                LispNode initValue = evaluate(initialValues.get(i));
                if (initValue == null && errorManager.hasErrors()) {
                    return null;
                }
                loopEnv.defineVariable(varNames.get(i), initValue);
            }

            // Main loop
            while (true) {
                // Check the test condition
                LispNode testCondition = evaluate(testResult.get(0));
                if (testCondition == null && errorManager.hasErrors()) {
                    return null;
                }

                if (isTruthy(testCondition)) {
                    // Return the result expressions or nil if none
                    if (testResult.size() > 1) {
                        return evaluateBeginLike(testResult.subList(1, testResult.size()));
                    } else {
                        return LispAtom.LispNil.INSTANCE;
                    }
                }

                // Execute the body
                for (int i = 3; i < elements.size(); i++) {
                    LispNode bodyResult = evaluate(elements.get(i));
                    if (bodyResult == null && errorManager.hasErrors()) {
                        return null;
                    }
                }

                // Compute the new values without changing the current values yet
                List<LispNode> newValues = new ArrayList<>();
                for (int i = 0; i < varNames.size(); i++) {
                    LispNode stepValue = evaluate(stepExpressions.get(i));
                    if (stepValue == null && errorManager.hasErrors()) {
                        return null;
                    }
                    newValues.add(stepValue);
                }

                // Update the variables with new values
                for (int i = 0; i < varNames.size(); i++) {
                    loopEnv.setVariable(varNames.get(i), newValues.get(i), errorManager);
                    if (errorManager.hasErrors()) {
                        return null;
                    }
                }
            }
        } finally {
            // Restore the original environment
            currentEnvironment = savedEnv;
        }
    }

    /**
     * Evaluates a when special form.
     *
     * @param elements The list elements
     * @return The result of evaluating the body if the test is true, or nil otherwise, or null if there's an error
     */
    private LispNode evaluateWhen(List<LispNode> elements) {
        if (elements.size() < 3) {
            errorManager.error("when requires at least 2 arguments", 0, 0, "when", "Provide test condition and body");
            return null;
        }

        LispNode condition = evaluate(elements.get(1));
        if (condition == null && errorManager.hasErrors()) {
            return null;
        }

        if (isTruthy(condition)) {
            return evaluateBeginLike(elements.subList(2, elements.size()));
        } else {
            return LispAtom.LispNil.INSTANCE;
        }
    }

    /**
     * Evaluates an unless special form.
     *
     * @param elements The list elements
     * @return The result of evaluating the body if the test is false, or nil otherwise, or null if there's an error
     */
    private LispNode evaluateUnless(List<LispNode> elements) {
        if (elements.size() < 3) {
            errorManager.error("unless requires at least 2 arguments", 0, 0, "unless", "Provide test condition and body");
            return null;
        }

        LispNode condition = evaluate(elements.get(1));
        if (condition == null && errorManager.hasErrors()) {
            return null;
        }

        if (!isTruthy(condition)) {
            return evaluateBeginLike(elements.subList(2, elements.size()));
        } else {
            return LispAtom.LispNil.INSTANCE;
        }
    }

    /**
     * Evaluates a case special form.
     *
     * @param elements The list elements
     * @return The result of evaluating the first matching clause, or null if there's an error
     */
    private LispNode evaluateCase(List<LispNode> elements) {
        if (elements.size() < 3) {
            errorManager.error("case requires at least 2 arguments", 0, 0, "case", "Provide key expression and clauses");
            return null;
        }

        LispNode keyForm = evaluate(elements.get(1));
        if (keyForm == null && errorManager.hasErrors()) {
            return null;
        }

        for (int i = 2; i < elements.size(); i++) {
            if (!(elements.get(i) instanceof LispList)) {
                errorManager.error("case clause must be a list", 0, 0, elements.get(i).toString(), "Provide proper case clauses");
                return null;
            }

            List<LispNode> clause = ((LispList) elements.get(i)).elements();

            if (clause.isEmpty()) {
                errorManager.error("case clause cannot be empty", 0, 0, "()", "Provide selector and expressions");
                return null;
            }

            // Check for the else/otherwise/t clause
            if ((clause.get(0) instanceof LispAtom.LispSymbol &&
                 (((LispAtom.LispSymbol) clause.get(0)).name().equals("else") ||
                  ((LispAtom.LispSymbol) clause.get(0)).name().equals("otherwise"))) ||
                (clause.get(0) instanceof LispAtom.LispBoolean && ((LispAtom.LispBoolean) clause.get(0)).value())) {

                if (i < elements.size() - 1) {
                    errorManager.error("else/otherwise clause must be the last clause in case", 0, 0, clause.get(0).toString(), "Place else as the last clause");
                    return null;
                }

                return evaluateBeginLike(clause.subList(1, clause.size()));
            }

            // Normal clause - first element is a list of keys to match against
            if (!(clause.get(0) instanceof LispList)) {
                errorManager.error("case selector must be a list of values", 0, 0, clause.get(0).toString(), "Provide a list of values to match against");
                return null;
            }

            List<LispNode> keys = ((LispList) clause.get(0)).elements();

            for (LispNode key : keys) {
                // We use eqv? semantics (structural equality)
                if (keyForm.equals(key)) {
                    return evaluateBeginLike(clause.subList(1, clause.size()));
                }
            }
        }

        // No matching clause
        return LispAtom.LispNil.INSTANCE;
    }

    /**
     * Evaluates a macro call by expanding it and then evaluating the result.
     *
     * @param macro The macro definition
     * @param args The macro arguments (unevaluated)
     * @return The result of evaluating the expanded macro, or null if there's an error
     */
    private LispNode evaluateMacroCall(LispDefinitions.LispMacro macro, List<LispNode> args) {
        // Create a new environment for the macro expansion
        LispDefinitions macroEnv = currentEnvironment.createChildScope();
        LispDefinitions savedEnv = currentEnvironment;
        currentEnvironment = macroEnv;

        try {
            // Bind parameters to arguments (unevaluated)
            List<String> params = macro.params();

            // Check if there's a rest parameter (indicated by a dot before the last parameter)
            boolean hasRestParam = false;
            String restParam = null;

            if (params.size() >= 2 && params.get(params.size() - 2).equals(".")) {
                hasRestParam = true;
                restParam = params.get(params.size() - 1);
                params = params.subList(0, params.size() - 2); // Remove the dot and rest param

                if (args.size() < params.size()) {
                    errorManager.error("Not enough arguments for macro", 0, 0, "macro", "Provide at least " + params.size() + " arguments");
                    return null;
                }
            } else if (args.size() != params.size()) {
                errorManager.error("Wrong number of arguments to macro", 0, 0, "macro",
                                  "Expected " + params.size() + " but got " + args.size());
                return null;
            }

            // Bind regular parameters
            for (int i = 0; i < params.size(); i++) {
                macroEnv.defineVariable(params.get(i), args.get(i));
            }

            // Bind rest parameter if any
            if (hasRestParam) {
                List<LispNode> restArgs = args.subList(params.size(), args.size());
                macroEnv.defineVariable(restParam, new LispList(restArgs));
            }

            // Expand the macro by evaluating its body
            LispNode expanded = evaluate(macro.body());
            if (expanded == null && errorManager.hasErrors()) {
                return null;
            }

            // Evaluate the expanded form in the original environment
            currentEnvironment = savedEnv;
            return evaluate(expanded);
        } finally {
            // Restore the original environment
            currentEnvironment = savedEnv;
        }
    }

    /**
     * Evaluates a function call.
     *
     * @param elements The list elements (operator and arguments)
     * @return The result of the function call, or null if there's an error
     */
    private LispNode evaluateFunctionCall(List<LispNode> elements) {
        if (elements.isEmpty()) {
            errorManager.error("Cannot evaluate an empty list", 0, 0, "()", "Provide a function to call");
            return null;
        }

        // Evaluate the operator
        LispNode operator = evaluate(elements.get(0));
        if (operator == null && errorManager.hasErrors()) {
            return null;
        }

        // Evaluate the arguments
        List<LispNode> args = new ArrayList<>();
        for (int i = 1; i < elements.size(); i++) {
            LispNode arg = evaluate(elements.get(i));
            if (arg == null && errorManager.hasErrors()) {
                return null;
            }
            args.add(arg);
        }

        // Handle built-in functions
        if (operator instanceof LispAtom.LispSymbol) {
            String funcName = ((LispAtom.LispSymbol) operator).name();
            LispDefinitions.BuiltinFunction builtin = currentEnvironment.lookupBuiltin(funcName);

            if (builtin != null) {
                return builtin.apply(this, args, errorManager);
            }

            // Look up user-defined function
            LispDefinitions.LispFunction func = currentEnvironment.lookupFunction(funcName);
            if (func != null) {
                return applyFunction(funcName, func.params(), func.body(), args);
            }

            // Check if it's a lambda symbol
            if (funcName.startsWith("_lambda_")) {
                LispNode lambdaExpr = currentEnvironment.lookupVariable(funcName);
                if (lambdaExpr instanceof LispList) {
                    // Retrieve the parameters and body from the stored lambda expression
                    List<LispNode> lambdaElements = ((LispList) lambdaExpr).elements();
                    if (lambdaElements.size() >= 3 &&
                        lambdaElements.get(0) instanceof LispAtom.LispSymbol &&
                        ((LispAtom.LispSymbol) lambdaElements.get(0)).name().equals("lambda") &&
                        lambdaElements.get(1) instanceof LispList) {

                        // Extract parameter names
                        List<String> params = new ArrayList<>();
                        for (LispNode param : ((LispList) lambdaElements.get(1)).elements()) {
                            if (param instanceof LispAtom.LispSymbol) {
                                params.add(((LispAtom.LispSymbol) param).name());
                            }
                        }

                        // Get the body
                        LispNode body;
                        if (lambdaElements.size() == 3) {
                            body = lambdaElements.get(2);
                        } else {
                            List<LispNode> beginBody = new ArrayList<>();
                            beginBody.add(new LispAtom.LispSymbol("begin"));
                            beginBody.addAll(lambdaElements.subList(2, lambdaElements.size()));
                            body = new LispList(beginBody);
                        }

                        return applyFunction("lambda", params, body, args);
                    }
                }
            }

            errorManager.error("Undefined function: " + funcName, 0, 0, funcName, "Define the function before using it");
            return null;
        }

        // Handle inline lambda expressions: ((lambda (x) ...) arg)
        if (operator instanceof LispList) {
            List<LispNode> lambdaElements = ((LispList) operator).elements();

            if (!lambdaElements.isEmpty() &&
                lambdaElements.get(0) instanceof LispAtom.LispSymbol &&
                ((LispAtom.LispSymbol) lambdaElements.get(0)).name().equals("lambda")) {

                // First evaluate the lambda to get its ID
                LispNode lambdaResult = evaluateLambda(lambdaElements);
                if (lambdaResult == null && errorManager.hasErrors()) {
                    return null;
                }

                if (lambdaResult instanceof LispAtom.LispSymbol) {
                    String lambdaId = ((LispAtom.LispSymbol) lambdaResult).name();
                    // Now retrieve and apply the lambda
                    LispNode lambdaExpr = currentEnvironment.lookupVariable(lambdaId);
                    if (lambdaExpr instanceof LispList) {
                        // Extract parameters and body from the stored lambda expression
                        List<LispNode> storedLambda = ((LispList) lambdaExpr).elements();
                        if (storedLambda.size() >= 3 &&
                            storedLambda.get(1) instanceof LispList) {

                            // Extract parameter names
                            List<String> params = new ArrayList<>();
                            for (LispNode param : ((LispList) storedLambda.get(1)).elements()) {
                                if (param instanceof LispAtom.LispSymbol) {
                                    params.add(((LispAtom.LispSymbol) param).name());
                                }
                            }

                            // Get the body
                            LispNode body;
                            if (storedLambda.size() == 3) {
                                body = storedLambda.get(2);
                            } else {
                                List<LispNode> beginBody = new ArrayList<>();
                                beginBody.add(new LispAtom.LispSymbol("begin"));
                                beginBody.addAll(storedLambda.subList(2, storedLambda.size()));
                                body = new LispList(beginBody);
                            }

                            return applyFunction("lambda", params, body, args);
                        }
                    }
                }

                errorManager.error("Invalid lambda expression", 0, 0, lambdaElements.toString(), "Fix the lambda expression");
                return null;
            }
        }

        errorManager.error("Not a function: " + operator, 0, 0, operator.toString(), "Use a function name or lambda expression");
        return null;
    }

    /**
     * Applies a function to a list of arguments using the current environment.
     *
     * @param name The function name (for error reporting)
     * @param params The function parameters
     * @param body The function body
     * @param args The arguments to apply
     * @return The result of applying the function, or null if there's an error
     */
    private LispNode applyFunction(String name, List<String> params, LispNode body, List<LispNode> args) {
        return applyFunction(name, params, body, args, currentEnvironment);
    }

    /**
     * Applies a function to a list of arguments.
     *
     * @param name The function name (for error reporting)
     * @param params The function parameters
     * @param body The function body
     * @param args The arguments to apply
     * @param closureEnv The closure environment
     * @return The result of applying the function, or null if there's an error
     */
    private LispNode applyFunction(String name, List<String> params, LispNode body, List<LispNode> args, LispDefinitions closureEnv) {
        // Check argument count
        if (args.size() != params.size()) {
            errorManager.error("Function " + name + " expects " + params.size() +
                              " arguments but got " + args.size(), 0, 0, name,
                              "Provide exactly " + params.size() + " arguments");
            return null;
        }

        // Create a new environment for the function call
        LispDefinitions funcEnv = closureEnv.createChildScope();
        LispDefinitions savedEnv = currentEnvironment;
        currentEnvironment = funcEnv;

        try {
            // Bind parameters to arguments
            for (int i = 0; i < params.size(); i++) {
                funcEnv.defineVariable(params.get(i), args.get(i));
            }

            // Evaluate the body in the function environment
            return evaluate(body);
        } finally {
            // Restore the original environment
            currentEnvironment = savedEnv;
        }
    }

    public LispDefinitions getDefinitions()
    {
        return currentEnvironment;
    }
}
