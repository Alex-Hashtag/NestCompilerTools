package org.nest.lisp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nest.errors.ErrorManager;
import org.nest.lisp.ast.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for the refactored LispInterpreter with ErrorManager-based error handling.
 */
public class LispInterpreterTest {
    private LispInterpreter interpreter;
    private ErrorManager errorManager;

    @BeforeEach
    public void setUp() {
        errorManager = new ErrorManager();
        interpreter = new LispInterpreter(errorManager);
    }

    /**
     * Test basic evaluation of literals
     */
    @Test
    public void testEvaluateLiterals() {
        // Number
        LispNode result = interpreter.evaluate(new LispAtom.LispNumber("42"));
        assertNotNull(result);
        assertTrue(result instanceof LispAtom.LispNumber);
        assertEquals("42", LispPrinter.format(result));
        assertFalse(errorManager.hasErrors());

        // String
        result = interpreter.evaluate(new LispAtom.LispString("hello"));
        assertNotNull(result);
        assertTrue(result instanceof LispAtom.LispString);
        assertEquals("\"hello\"", LispPrinter.format(result));
        assertFalse(errorManager.hasErrors());

        // Boolean
        result = interpreter.evaluate(LispAtom.LispBoolean.TRUE);
        assertNotNull(result);
        assertTrue(result instanceof LispAtom.LispBoolean);
        assertEquals("#t", LispPrinter.format(result));
        assertFalse(errorManager.hasErrors());
    }

    /**
     * Test evaluation of basic arithmetic operations
     */
    @Test
    public void testBasicArithmetic() {
        // (+ 1 2)
        LispList expr = createList("+", "1", "2");
        LispNode result = interpreter.evaluate(expr);
        assertNotNull(result);
        assertTrue(result instanceof LispAtom.LispNumber);
        assertEquals("3", LispPrinter.format(result));
        assertFalse(errorManager.hasErrors());

        // (* 3 4)
        expr = createList("*", "3", "4");
        result = interpreter.evaluate(expr);
        assertNotNull(result);
        assertTrue(result instanceof LispAtom.LispNumber);
        assertEquals("12", LispPrinter.format(result));
        assertFalse(errorManager.hasErrors());
    }

    /**
     * Test variable definition and lookup
     */
    @Test
    public void testVariableDefinition() {
        // (define x 10)
        LispList defineExpr = createList("define", "x", "10");
        LispNode result = interpreter.evaluate(defineExpr);
        assertNotNull(result);
        assertTrue(result instanceof LispAtom.LispNumber);
        assertEquals("10", LispPrinter.format(result));
        assertFalse(errorManager.hasErrors());

        // x
        result = interpreter.evaluate(new LispAtom.LispSymbol("x"));
        assertNotNull(result);
        assertTrue(result instanceof LispAtom.LispNumber);
        assertEquals("10", LispPrinter.format(result));
        assertFalse(errorManager.hasErrors());
    }

    /**
     * Test function definition and application
     */
    @Test
    public void testFunctionDefinition() {
        // (define (add a b) (+ a b))
        LispList defineExpr = new LispList(List.of(
            new LispAtom.LispSymbol("define"),
            new LispList(List.of(
                new LispAtom.LispSymbol("add"),
                new LispAtom.LispSymbol("a"),
                new LispAtom.LispSymbol("b")
            )),
            new LispList(List.of(
                new LispAtom.LispSymbol("+"),
                new LispAtom.LispSymbol("a"),
                new LispAtom.LispSymbol("b")
            ))
        ));
        interpreter.evaluate(defineExpr);
        assertFalse(errorManager.hasErrors());

        // (add 5 7)
        LispList callExpr = createList("add", "5", "7");
        LispNode result = interpreter.evaluate(callExpr);
        assertNotNull(result);
        assertTrue(result instanceof LispAtom.LispNumber);
        assertEquals("12", LispPrinter.format(result));
        assertFalse(errorManager.hasErrors());
    }

    /**
     * Test if special form
     */
    @Test
    public void testIfExpression() {
        // (if #t "yes" "no")
        LispList ifExpr = new LispList(List.of(
            new LispAtom.LispSymbol("if"),
            LispAtom.LispBoolean.TRUE,
            new LispAtom.LispString("yes"),
            new LispAtom.LispString("no")
        ));
        LispNode result = interpreter.evaluate(ifExpr);
        assertNotNull(result);
        assertTrue(result instanceof LispAtom.LispString);
        assertEquals("\"yes\"", LispPrinter.format(result));
        assertFalse(errorManager.hasErrors());

        // (if #f "yes" "no")
        ifExpr = new LispList(List.of(
            new LispAtom.LispSymbol("if"),
            LispAtom.LispBoolean.FALSE,
            new LispAtom.LispString("yes"),
            new LispAtom.LispString("no")
        ));
        result = interpreter.evaluate(ifExpr);
        assertNotNull(result);
        assertTrue(result instanceof LispAtom.LispString);
        assertEquals("\"no\"", LispPrinter.format(result));
        assertFalse(errorManager.hasErrors());
    }

    /**
     * Test begin special form
     */
    @Test
    public void testBeginExpression() {
        // (begin 1 2 3)
        LispList beginExpr = createList("begin", "1", "2", "3");
        LispNode result = interpreter.evaluate(beginExpr);
        assertNotNull(result);
        assertTrue(result instanceof LispAtom.LispNumber);
        assertEquals("3", LispPrinter.format(result));
        assertFalse(errorManager.hasErrors());
    }

    /**
     * Test lambda expression
     */
    @Test
    public void testLambdaExpression() {
        // ((lambda (x) (* x x)) 5)
        LispList lambdaCall = new LispList(List.of(
            new LispList(List.of(
                new LispAtom.LispSymbol("lambda"),
                new LispList(List.of(new LispAtom.LispSymbol("x"))),
                new LispList(List.of(
                    new LispAtom.LispSymbol("*"),
                    new LispAtom.LispSymbol("x"),
                    new LispAtom.LispSymbol("x")
                ))
            )),
            new LispAtom.LispNumber("5")
        ));
        LispNode result = interpreter.evaluate(lambdaCall);
        assertNotNull(result);
        assertTrue(result instanceof LispAtom.LispNumber);
        assertEquals("25", LispPrinter.format(result));
        assertFalse(errorManager.hasErrors());
    }

    /**
     * Test let special form
     */
    @Test
    public void testLetExpression() {
        // (let ((x 5) (y 7)) (+ x y))
        LispList letExpr = new LispList(List.of(
            new LispAtom.LispSymbol("let"),
            new LispList(List.of(
                new LispList(List.of(
                    new LispAtom.LispSymbol("x"),
                    new LispAtom.LispNumber("5")
                )),
                new LispList(List.of(
                    new LispAtom.LispSymbol("y"),
                    new LispAtom.LispNumber("7")
                ))
            )),
            new LispList(List.of(
                new LispAtom.LispSymbol("+"),
                new LispAtom.LispSymbol("x"),
                new LispAtom.LispSymbol("y")
            ))
        ));
        LispNode result = interpreter.evaluate(letExpr);
        assertNotNull(result);
        assertTrue(result instanceof LispAtom.LispNumber);
        assertEquals("12", LispPrinter.format(result));
        assertFalse(errorManager.hasErrors());
    }

    /**
     * Test error handling for undefined variables
     */
    @Test
    public void testUndefinedVariable() {
        // undefined-var
        LispNode result = interpreter.evaluate(new LispAtom.LispSymbol("undefined-var"));
        assertNull(result);
        assertTrue(errorManager.hasErrors());
        errorManager.clear();
    }

    /**
     * Test error handling for incorrect argument counts
     */
    @Test
    public void testIncorrectArgumentCount() {
        // (if #t)  // if requires at least 2 arguments
        LispList expr = new LispList(List.of(
            new LispAtom.LispSymbol("if"),
            LispAtom.LispBoolean.TRUE
        ));
        LispNode result = interpreter.evaluate(expr);
        assertNull(result);
        assertTrue(errorManager.hasErrors());
        errorManager.clear();
    }

    /**
     * Test error handling for type errors
     */
    @Test
    public void testTypeErrors() {
        // (+ "not-a-number" 1)
        LispList expr = new LispList(List.of(
            new LispAtom.LispSymbol("+"),
            new LispAtom.LispString("not-a-number"),
            new LispAtom.LispNumber("1")
        ));
        LispNode result = interpreter.evaluate(expr);
        assertNull(result);
        assertTrue(errorManager.hasErrors());
        errorManager.clear();
    }

    /**
     * Test error handling in nested expressions
     */
    @Test
    public void testNestedErrors() {
        // (+ 1 (/ 5 0))  // Division by zero
        LispList divExpr = createList("/", "5", "0");
        LispList addExpr = new LispList(List.of(
            new LispAtom.LispSymbol("+"),
            new LispAtom.LispNumber("1"),
            divExpr
        ));
        
        LispNode result = interpreter.evaluate(addExpr);
        assertNull(result);
        assertTrue(errorManager.hasErrors());
        errorManager.clear();
    }

    /**
     * Test macro definitions and expansion
     */
    @Test
    public void testMacroExpansion() {
        // Define a simple macro: (defmacro my-when (condition body) (if condition body nil))
        LispList defmacroExpr = new LispList(List.of(
            new LispAtom.LispSymbol("defmacro"),
            new LispAtom.LispSymbol("my-when"),
            new LispList(List.of(
                new LispAtom.LispSymbol("condition"),
                new LispAtom.LispSymbol("body")
            )),
            new LispList(List.of(
                new LispAtom.LispSymbol("if"),
                new LispAtom.LispSymbol("condition"),
                new LispAtom.LispSymbol("body"),
                LispAtom.LispNil.INSTANCE
            ))
        ));
        
        interpreter.evaluate(defmacroExpr);
        assertFalse(errorManager.hasErrors());
        
        // Use the macro: (my-when #t 42)
        LispList macroUseExpr = new LispList(List.of(
            new LispAtom.LispSymbol("my-when"),
            LispAtom.LispBoolean.TRUE,
            new LispAtom.LispNumber("42")
        ));
        
        LispNode result = interpreter.evaluate(macroUseExpr);
        assertNotNull(result);
        assertTrue(result instanceof LispAtom.LispNumber);
        assertEquals("42", LispPrinter.format(result));
        assertFalse(errorManager.hasErrors());
    }

    /**
     * Helper method to create a simple list of symbols and numbers
     */
    private LispList createList(String operator, String... args) {
        List<LispNode> elements = new java.util.ArrayList<>();
        elements.add(new LispAtom.LispSymbol(operator));
        
        for (String arg : args) {
            try {
                // Try to parse as number first
                Integer.parseInt(arg);
                elements.add(new LispAtom.LispNumber(arg));
            } catch (NumberFormatException e) {
                // If not a number, add as symbol
                elements.add(new LispAtom.LispSymbol(arg));
            }
        }
        
        return new LispList(elements);
    }
}
