package org.nest.sprouts;

import org.junit.jupiter.api.Test;
import org.nest.ast.ASTWrapper;
import org.nest.errors.ErrorManager;
import org.nest.sprouts.ast.Program;
import org.nest.tokenization.TokenList;
import org.nest.tokenization.TokenPostProcessor;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Sprout-S interpreter.
 * Verifies correct execution of Sprout-S programs.
 */
class SproutSInterpreterTest {
    
    /**
     * Helper to parse and execute Sprout-S code
     */
    private ExecutionResult execute(String code) {
        // Parse
        ErrorManager errorManager = new ErrorManager();
        errorManager.setContext("test.spr", code);
        
        TokenPostProcessor postProcessor = TokenPostProcessor.builder().build();
        TokenList tokens = TokenList.create(code, SproutSTokenRules.rules(), postProcessor);
        ASTWrapper astWrapper = SproutSASTRules.rules().createAST(tokens, errorManager);
        
        if (errorManager.hasErrors()) {
            fail("Parse errors: " + errorManager.getErrors());
        }
        
        // Handle empty programs
        Program program;
        if (astWrapper.get().isEmpty()) {
            program = new Program(java.util.List.of());
        } else {
            program = (Program) astWrapper.get().get(0);
        }
        
        // Execute
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream output = new PrintStream(outputStream);
        
        ErrorManager runtimeErrorManager = new ErrorManager();
        runtimeErrorManager.setContext("test.spr", code);
        SproutSInterpreter interpreter = new SproutSInterpreter(output, runtimeErrorManager);
        int exitCode = interpreter.execute(program);
        
        // Collect runtime errors
        String errorOutput = "";
        if (runtimeErrorManager.hasErrors()) {
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            PrintStream errorPrint = new PrintStream(errorStream);
            runtimeErrorManager.printReports(errorPrint);
            errorOutput = errorStream.toString().trim();
        }
        
        return new ExecutionResult(
            exitCode,
            outputStream.toString().trim(),
            errorOutput
        );
    }
    
    record ExecutionResult(int exitCode, String output, String error) {}
    
    @Test
    void testSimpleArithmetic() {
        String code = """
                let x = 2 + 3 * 4;
                print x;
                """;
        
        ExecutionResult result = execute(code);
        assertEquals(0, result.exitCode);
        assertEquals("14", result.output);
        assertTrue(result.error.isEmpty());
    }
    
    @Test
    void testVariableScope() {
        String code = """
                let x = 1;
                if (1) {
                    let x = 2;
                    print x;
                } else {
                    print 0;
                }
                print x;
                """;
        
        ExecutionResult result = execute(code);
        assertEquals(0, result.exitCode);
        assertEquals("2\n1", result.output);
    }
    
    @Test
    void testWhileLoop() {
        String code = """
                let s = 0;
                let i = 1;
                while (i <= 5) {
                    set s = s + i;
                    set i = i + 1;
                }
                print s;
                """;
        
        ExecutionResult result = execute(code);
        assertEquals(0, result.exitCode);
        assertEquals("15", result.output);
    }
    
    @Test
    void testShortCircuitAnd() {
        String code = """
                let a = 0;
                let b = 1;
                print ((a != 0) && (b / a));
                """;
        
        ExecutionResult result = execute(code);
        assertEquals(0, result.exitCode);
        assertEquals("0", result.output);
        assertTrue(result.error.isEmpty(), "Should not divide by zero due to short-circuit");
    }
    
    @Test
    void testShortCircuitOr() {
        String code = """
                let b = 1;
                let a = 0;
                print ((b != 0) || (a / b));
                """;
        
        ExecutionResult result = execute(code);
        assertEquals(0, result.exitCode);
        assertEquals("1", result.output);
    }
    
    @Test
    void testDivisionByZeroError() {
        String code = """
                let z = 0;
                print (1 / z);
                """;
        
        ExecutionResult result = execute(code);
        assertEquals(2, result.exitCode);
        assertTrue(result.error.contains("Division by zero"));
    }
    
    @Test
    void testModuloByZeroError() {
        String code = """
                let z = 0;
                print (5 % z);
                """;
        
        ExecutionResult result = execute(code);
        assertEquals(2, result.exitCode);
        assertTrue(result.error.contains("Modulo by zero"));
    }
    
    @Test
    void testUndefinedVariableError() {
        String code = """
                print x;
                """;
        
        ExecutionResult result = execute(code);
        assertEquals(2, result.exitCode);
        assertTrue(result.error.contains("Undefined variable"));
    }
    
    @Test
    void testSetUndeclaredVariableError() {
        String code = """
                set x = 5;
                """;
        
        ExecutionResult result = execute(code);
        assertEquals(2, result.exitCode);
        assertTrue(result.error.contains("undeclared variable"));
    }
    
    @Test
    void testExitStatement() {
        String code = """
                print 1;
                exit 42;
                print 2;
                """;
        
        ExecutionResult result = execute(code);
        assertEquals(42, result.exitCode);
        assertEquals("1", result.output);
    }
    
    @Test
    void testUnaryOperators() {
        String code = """
                let a = -5;
                let b = !0;
                let c = !1;
                print a;
                print b;
                print c;
                """;
        
        ExecutionResult result = execute(code);
        assertEquals(0, result.exitCode);
        assertEquals("-5\n1\n0", result.output);
    }
    
    @Test
    void testComparisonOperators() {
        String code = """
                print (1 < 2);
                print (3 <= 3);
                print (5 > 4);
                print (6 >= 6);
                print (7 == 7);
                print (8 != 9);
                """;
        
        ExecutionResult result = execute(code);
        assertEquals(0, result.exitCode);
        assertEquals("1\n1\n1\n1\n1\n1", result.output);
    }
    
    @Test
    void testLogicalOperators() {
        String code = """
                print (1 && 1);
                print (1 && 0);
                print (0 && 1);
                print (1 || 0);
                print (0 || 0);
                """;
        
        ExecutionResult result = execute(code);
        assertEquals(0, result.exitCode);
        assertEquals("1\n0\n0\n1\n0", result.output);
    }
    
    @Test
    void testIfElse() {
        String code = """
                let x = 5;
                if (x > 3) {
                    print 1;
                } else {
                    print 0;
                }
                """;
        
        ExecutionResult result = execute(code);
        assertEquals(0, result.exitCode);
        assertEquals("1", result.output);
    }
    
    @Test
    void testNestedScopes() {
        String code = """
                let x = 1;
                while (x < 3) {
                    let y = x * 10;
                    print y;
                    set x = x + 1;
                }
                """;
        
        ExecutionResult result = execute(code);
        assertEquals(0, result.exitCode);
        assertEquals("10\n20", result.output);
    }
    
    @Test
    void testEmptyProgram() {
        String code = "";
        
        ExecutionResult result = execute(code);
        assertEquals(0, result.exitCode);
        assertTrue(result.output.isEmpty());
    }
    
    @Test
    void testComplexFibonacci() {
        String code = """
                let a = 0;
                let b = 1;
                let n = 5;
                let i = 0;
                
                print a;
                print b;
                
                while (i < n) {
                    let temp = a + b;
                    set a = b;
                    set b = temp;
                    print temp;
                    set i = i + 1;
                }
                """;
        
        ExecutionResult result = execute(code);
        assertEquals(0, result.exitCode);
        assertEquals("0\n1\n1\n2\n3\n5\n8", result.output);
    }
    
    @Test
    void testArithmeticPrecedence() {
        String code = """
                print (2 + 3 * 4);
                print ((2 + 3) * 4);
                print (10 - 2 * 3);
                print (20 / 4 + 2);
                """;
        
        ExecutionResult result = execute(code);
        assertEquals(0, result.exitCode);
        assertEquals("14\n20\n4\n7", result.output);
    }
    
    @Test
    void testIntegerOverflow() {
        // Java int wraps on overflow
        String code = """
                let max = 2147483647;
                let overflow = max + 1;
                print overflow;
                """;
        
        ExecutionResult result = execute(code);
        assertEquals(0, result.exitCode);
        assertEquals("-2147483648", result.output);
    }
    
    @Test
    void testDivisionTruncation() {
        String code = """
                print (7 / 2);
                print (-7 / 2);
                print (7 / -2);
                print (-7 / -2);
                """;
        
        ExecutionResult result = execute(code);
        assertEquals(0, result.exitCode);
        assertEquals("3\n-3\n-3\n3", result.output);
    }
    
    @Test
    void testModuloOperation() {
        String code = """
                print (7 % 3);
                print (-7 % 3);
                print (7 % -3);
                """;
        
        ExecutionResult result = execute(code);
        assertEquals(0, result.exitCode);
        assertEquals("1\n-1\n1", result.output);
    }
}
