package org.nest.sprouts;

import org.junit.jupiter.api.Test;
import org.nest.ast.ASTWrapper;
import org.nest.errors.ErrorManager;
import org.nest.sprouts.ast.*;
import org.nest.tokenization.TokenList;
import org.nest.tokenization.TokenPostProcessor;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Sprout-S language parser to verify syntax compiles to AST correctly.
 * Based on the specification in DocS.md
 */
class SproutSParserTest {

    /**
     * Helper method to parse Sprout-S code and return the AST
     */
    private Program parse(String code) {
        return parse(code, true);
    }

    /**
     * Helper method to parse Sprout-S code and return the AST
     * @param code The Sprout-S source code to parse
     * @param verbose If true, prints parsing information
     */
    private Program parse(String code, boolean verbose) {
        ErrorManager errorManager = new ErrorManager();
        errorManager.setContext("test.spr", code);

        try {
            if (verbose) {
                System.out.println("\n" + "=".repeat(60));
                System.out.println("📝 Parsing Sprout-S Code:");
                System.out.println("-".repeat(60));
                System.out.println(code.isEmpty() ? "(empty)" : code);
                System.out.println("-".repeat(60));
            }

            // Tokenize - create empty post-processor since Sprout-S doesn't need token transformations
            TokenPostProcessor postProcessor = TokenPostProcessor.builder().build();
            TokenList tokens = TokenList.create(
                    code,
                    SproutSTokenRules.rules(),
                    postProcessor
            );

            if (verbose) {
                System.out.println("🔤 Tokens: " + tokens.size());
            }

            // Parse
            ASTWrapper astWrapper = SproutSASTRules.rules().createAST(tokens, errorManager);

            // Check for errors
            if (errorManager.hasErrors()) {
                errorManager.printReports(System.err);
                fail("Parsing failed with errors");
            }

            assertNotNull(astWrapper, "Parser returned null");
            assertFalse(astWrapper.get().isEmpty(), "AST wrapper should not be empty");
            
            Object result = astWrapper.get().get(0);
            assertInstanceOf(Program.class, result, "Parser should return a Program");
            Program program = (Program) result;

            if (verbose) {
                System.out.println("✅ Parse successful!");
                System.out.println("🌳 AST: Program with " + program.stmts().size() + " statement(s)");
                printASTSummary(program, "   ");
                System.out.println("=".repeat(60));
            }

            return program;
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception during parsing: " + e.getMessage());
            return null;
        }
    }

    /**
     * Prints a human-readable summary of the AST structure
     */
    private void printASTSummary(Program program, String indent) {
        for (int i = 0; i < program.stmts().size(); i++) {
            Stmt stmt = program.stmts().get(i);
            System.out.print(indent + "├─ ");
            printStmt(stmt, indent + "│  ");
        }
    }

    private void printStmt(Stmt stmt, String indent) {
        switch (stmt) {
            case Stmt.Let let -> {
                System.out.println("Let '" + let.name() + "' = " + exprToString(let.init()));
            }
            case Stmt.Set set -> {
                System.out.println("Set '" + set.name() + "' = " + exprToString(set.expr()));
            }
            case Stmt.Print print -> {
                System.out.println("Print " + exprToString(print.expr()));
            }
            case Stmt.Exit exit -> {
                System.out.println("Exit " + exprToString(exit.expr()));
            }
            case Stmt.If ifStmt -> {
                System.out.println("If " + exprToString(ifStmt.cond()));
                System.out.println(indent + "├─ Then: " + ifStmt.then().stmts().size() + " stmt(s)");
                System.out.println(indent + "└─ Else: " + ifStmt.elseBranch().stmts().size() + " stmt(s)");
            }
            case Stmt.While whileStmt -> {
                System.out.println("While " + exprToString(whileStmt.cond()));
                System.out.println(indent + "└─ Body: " + whileStmt.body().stmts().size() + " stmt(s)");
            }
        }
    }

    private String exprToString(Expr expr) {
        return switch (expr) {
            case Expr.Int i -> String.valueOf(i.value());
            case Expr.Var v -> "var:" + v.name();
            case Expr.Unary u -> u.op() + exprToString(u.expr());
            case Expr.Binary b -> "(" + exprToString(b.left()) + " " + b.op() + " " + exprToString(b.right()) + ")";
            case Expr.Group g -> "(" + exprToString(g.expr()) + ")";
        };
    }

    @Test
    void testEmptyProgram() {
        String code = "";
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Parsing Empty Sprout-S Program:");
        System.out.println("-".repeat(60));
        System.out.println("(empty)");
        System.out.println("-".repeat(60));
        
        ErrorManager errorManager = new ErrorManager();
        errorManager.setContext("test.spr", code);
        
        TokenPostProcessor postProcessor = TokenPostProcessor.builder().build();
        TokenList tokens = TokenList.create(code, SproutSTokenRules.rules(), postProcessor);
        
        System.out.println("🔤 Tokens: " + tokens.size());
        
        ASTWrapper astWrapper = SproutSASTRules.rules().createAST(tokens, errorManager);
        
        assertFalse(errorManager.hasErrors(), "Empty program should parse without errors");
        assertNotNull(astWrapper);
        
        // Empty programs may return an empty wrapper or a program with no statements
        if (!astWrapper.get().isEmpty()) {
            Program program = (Program) astWrapper.get().get(0);
            assertTrue(program.stmts().isEmpty(), "Empty program should have no statements");
            System.out.println("✅ Parse successful!");
            System.out.println("🌳 AST: Program with 0 statements");
        } else {
            System.out.println("✅ Parse successful!");
            System.out.println("🌳 AST: Empty wrapper (valid for empty program)");
        }
        System.out.println("=".repeat(60));
    }

    @Test
    void testSimpleLetStatement() {
        String code = "let x = 42;";
        Program program = parse(code);
        
        assertEquals(1, program.stmts().size());
        Stmt stmt = program.stmts().get(0);
        assertInstanceOf(Stmt.Let.class, stmt);
        
        Stmt.Let letStmt = (Stmt.Let) stmt;
        assertEquals("x", letStmt.name());
        assertInstanceOf(Expr.Int.class, letStmt.init());
        assertEquals(42, ((Expr.Int) letStmt.init()).value());
    }

    @Test
    void testArithmeticExpression() {
        // T1: arithmetic.spr - let x = 2 + 3 * 4;
        String code = "let x = 2 + 3 * 4;";
        Program program = parse(code);
        
        assertEquals(1, program.stmts().size());
        Stmt.Let letStmt = (Stmt.Let) program.stmts().get(0);
        assertEquals("x", letStmt.name());
        
        // Should parse as: 2 + (3 * 4) due to precedence
        assertInstanceOf(Expr.Binary.class, letStmt.init());
        Expr.Binary addExpr = (Expr.Binary) letStmt.init();
        assertEquals("+", addExpr.op());
        assertInstanceOf(Expr.Int.class, addExpr.left());
        assertEquals(2, ((Expr.Int) addExpr.left()).value());
        assertInstanceOf(Expr.Binary.class, addExpr.right());
        
        Expr.Binary mulExpr = (Expr.Binary) addExpr.right();
        assertEquals("*", mulExpr.op());
        assertEquals(3, ((Expr.Int) mulExpr.left()).value());
        assertEquals(4, ((Expr.Int) mulExpr.right()).value());
    }

    @Test
    void testSetStatement() {
        String code = "set y = 100;";
        Program program = parse(code);
        
        assertEquals(1, program.stmts().size());
        assertInstanceOf(Stmt.Set.class, program.stmts().get(0));
        
        Stmt.Set setStmt = (Stmt.Set) program.stmts().get(0);
        assertEquals("y", setStmt.name());
        assertInstanceOf(Expr.Int.class, setStmt.expr());
        assertEquals(100, ((Expr.Int) setStmt.expr()).value());
    }

    @Test
    void testPrintStatement() {
        String code = "print 42;";
        Program program = parse(code);
        
        assertEquals(1, program.stmts().size());
        assertInstanceOf(Stmt.Print.class, program.stmts().get(0));
        
        Stmt.Print printStmt = (Stmt.Print) program.stmts().get(0);
        assertInstanceOf(Expr.Int.class, printStmt.expr());
        assertEquals(42, ((Expr.Int) printStmt.expr()).value());
    }

    @Test
    void testExitStatement() {
        String code = "exit 1;";
        Program program = parse(code);
        
        assertEquals(1, program.stmts().size());
        assertInstanceOf(Stmt.Exit.class, program.stmts().get(0));
        
        Stmt.Exit exitStmt = (Stmt.Exit) program.stmts().get(0);
        assertInstanceOf(Expr.Int.class, exitStmt.expr());
        assertEquals(1, ((Expr.Int) exitStmt.expr()).value());
    }

    @Test
    void testIfStatement() {
        String code = """
                if (x > 0) {
                    print 1;
                } else {
                    print 0;
                }
                """;
        Program program = parse(code);
        
        assertEquals(1, program.stmts().size());
        assertInstanceOf(Stmt.If.class, program.stmts().get(0));
        
        Stmt.If ifStmt = (Stmt.If) program.stmts().get(0);
        assertInstanceOf(Expr.Binary.class, ifStmt.cond());
        assertNotNull(ifStmt.then());
        assertNotNull(ifStmt.elseBranch());
        
        assertEquals(1, ifStmt.then().stmts().size());
        assertEquals(1, ifStmt.elseBranch().stmts().size());
    }

    @Test
    void testWhileStatement() {
        // T3: while.spr
        String code = """
                let s = 0;
                let i = 1;
                while (i <= 5) {
                    set s = s + i;
                    set i = i + 1;
                }
                print s;
                """;
        Program program = parse(code);
        
        assertEquals(4, program.stmts().size());
        assertInstanceOf(Stmt.Let.class, program.stmts().get(0));
        assertInstanceOf(Stmt.Let.class, program.stmts().get(1));
        assertInstanceOf(Stmt.While.class, program.stmts().get(2));
        assertInstanceOf(Stmt.Print.class, program.stmts().get(3));
        
        Stmt.While whileStmt = (Stmt.While) program.stmts().get(2);
        assertInstanceOf(Expr.Binary.class, whileStmt.cond());
        assertEquals(2, whileStmt.body().stmts().size());
    }

    @Test
    void testNestedBlocks() {
        // T2: scope.spr - adapted since grammar doesn't support standalone blocks
        // Using if-else to demonstrate nested scoping instead
        String code = """
                let x = 1;
                if (1) {
                    let x = 2;
                    print x;
                } else {
                    print 0;
                }
                """;
        Program program = parse(code);
        
        assertEquals(2, program.stmts().size());
        assertInstanceOf(Stmt.Let.class, program.stmts().get(0));
        assertInstanceOf(Stmt.If.class, program.stmts().get(1));
        
        Stmt.If ifStmt = (Stmt.If) program.stmts().get(1);
        assertEquals(2, ifStmt.then().stmts().size());
        assertInstanceOf(Stmt.Let.class, ifStmt.then().stmts().get(0));
        assertInstanceOf(Stmt.Print.class, ifStmt.then().stmts().get(1));
    }

    @Test
    void testUnaryOperators() {
        String code = """
                let a = -5;
                let b = !0;
                """;
        Program program = parse(code);
        
        assertEquals(2, program.stmts().size());
        
        Stmt.Let letA = (Stmt.Let) program.stmts().get(0);
        assertInstanceOf(Expr.Unary.class, letA.init());
        Expr.Unary unaryA = (Expr.Unary) letA.init();
        assertEquals("-", unaryA.op());
        assertEquals(5, ((Expr.Int) unaryA.expr()).value());
        
        Stmt.Let letB = (Stmt.Let) program.stmts().get(1);
        assertInstanceOf(Expr.Unary.class, letB.init());
        Expr.Unary unaryB = (Expr.Unary) letB.init();
        assertEquals("!", unaryB.op());
        assertEquals(0, ((Expr.Int) unaryB.expr()).value());
    }

    @Test
    void testComparisonOperators() {
        String code = """
                let a = 1 < 2;
                let b = 3 <= 3;
                let c = 5 > 4;
                let d = 6 >= 6;
                let e = 7 == 7;
                let f = 8 != 9;
                """;
        Program program = parse(code);
        
        assertEquals(6, program.stmts().size());
        
        String[] expectedOps = {"<", "<=", ">", ">=", "==", "!="};
        for (int i = 0; i < 6; i++) {
            Stmt.Let letStmt = (Stmt.Let) program.stmts().get(i);
            assertInstanceOf(Expr.Binary.class, letStmt.init());
            Expr.Binary binExpr = (Expr.Binary) letStmt.init();
            assertEquals(expectedOps[i], binExpr.op());
        }
    }

    @Test
    void testLogicalOperators() {
        // T4: logic_shortcircuit.spr
        String code = """
                let a = 0;
                let b = 1;
                print ((a != 0) && (b / a));
                print ((b != 0) || (a / b));
                """;
        Program program = parse(code);
        
        assertEquals(4, program.stmts().size());
        
        Stmt.Print print1 = (Stmt.Print) program.stmts().get(2);
        assertInstanceOf(Expr.Group.class, print1.expr());
        Expr.Group group1 = (Expr.Group) print1.expr();
        assertInstanceOf(Expr.Binary.class, group1.expr());
        Expr.Binary andExpr = (Expr.Binary) group1.expr();
        assertEquals("&&", andExpr.op());
        
        Stmt.Print print2 = (Stmt.Print) program.stmts().get(3);
        assertInstanceOf(Expr.Group.class, print2.expr());
        Expr.Group group2 = (Expr.Group) print2.expr();
        assertInstanceOf(Expr.Binary.class, group2.expr());
        Expr.Binary orExpr = (Expr.Binary) group2.expr();
        assertEquals("||", orExpr.op());
    }

    @Test
    void testVariableReferences() {
        String code = """
                let x = 10;
                let y = x + 5;
                print y;
                """;
        Program program = parse(code);
        
        assertEquals(3, program.stmts().size());
        
        Stmt.Let letY = (Stmt.Let) program.stmts().get(1);
        assertInstanceOf(Expr.Binary.class, letY.init());
        Expr.Binary addExpr = (Expr.Binary) letY.init();
        assertInstanceOf(Expr.Var.class, addExpr.left());
        assertEquals("x", ((Expr.Var) addExpr.left()).name());
    }

    @Test
    void testGroupedExpressions() {
        String code = "let x = (1 + 2) * 3;";
        Program program = parse(code);
        
        Stmt.Let letStmt = (Stmt.Let) program.stmts().get(0);
        assertInstanceOf(Expr.Binary.class, letStmt.init());
        Expr.Binary mulExpr = (Expr.Binary) letStmt.init();
        assertEquals("*", mulExpr.op());
        assertInstanceOf(Expr.Group.class, mulExpr.left());
        
        Expr.Group group = (Expr.Group) mulExpr.left();
        assertInstanceOf(Expr.Binary.class, group.expr());
        Expr.Binary addExpr = (Expr.Binary) group.expr();
        assertEquals("+", addExpr.op());
    }

    @Test
    void testComplexProgram() {
        String code = """
                // Fibonacci-like program
                let a = 0;
                let b = 1;
                let n = 10;
                let i = 0;
                
                while (i < n) {
                    print a;
                    let temp = a + b;
                    set a = b;
                    set b = temp;
                    set i = i + 1;
                }
                
                exit 0;
                """;
        Program program = parse(code);
        
        // Should have: 4 let statements, 1 while, 1 exit
        assertEquals(6, program.stmts().size());
        assertInstanceOf(Stmt.Let.class, program.stmts().get(0));
        assertInstanceOf(Stmt.Let.class, program.stmts().get(1));
        assertInstanceOf(Stmt.Let.class, program.stmts().get(2));
        assertInstanceOf(Stmt.Let.class, program.stmts().get(3));
        assertInstanceOf(Stmt.While.class, program.stmts().get(4));
        assertInstanceOf(Stmt.Exit.class, program.stmts().get(5));
        
        Stmt.While whileStmt = (Stmt.While) program.stmts().get(4);
        assertEquals(5, whileStmt.body().stmts().size());
    }

    @Test
    void testAllOperatorPrecedence() {
        String code = "let result = 1 + 2 * 3 - 4 / 2 % 3;";
        Program program = parse(code);
        
        Stmt.Let letStmt = (Stmt.Let) program.stmts().get(0);
        // Just verify it parses without errors
        assertNotNull(letStmt.init());
        assertInstanceOf(Expr.Binary.class, letStmt.init());
    }

    @Test
    void testCommentsAreIgnored() {
        String code = """
                // This is a comment
                let x = 5; // inline comment
                // Another comment
                print x;
                """;
        Program program = parse(code);
        
        assertEquals(2, program.stmts().size());
        assertInstanceOf(Stmt.Let.class, program.stmts().get(0));
        assertInstanceOf(Stmt.Print.class, program.stmts().get(1));
    }

    @Test
    void testZeroLiteral() {
        String code = "let zero = 0;";
        Program program = parse(code);
        
        Stmt.Let letStmt = (Stmt.Let) program.stmts().get(0);
        assertInstanceOf(Expr.Int.class, letStmt.init());
        assertEquals(0, ((Expr.Int) letStmt.init()).value());
    }

    @Test
    void testLargeNumbers() {
        String code = "let big = 2147483647;";
        Program program = parse(code);
        
        Stmt.Let letStmt = (Stmt.Let) program.stmts().get(0);
        assertInstanceOf(Expr.Int.class, letStmt.init());
        assertEquals(2147483647, ((Expr.Int) letStmt.init()).value());
    }

    @Test
    void testMultipleStatementsInBlock() {
        String code = """
                if (1) {
                    let a = 1;
                    let b = 2;
                    print a;
                    print b;
                } else {
                    exit 1;
                }
                """;
        Program program = parse(code);
        
        Stmt.If ifStmt = (Stmt.If) program.stmts().get(0);
        assertEquals(4, ifStmt.then().stmts().size());
        assertEquals(1, ifStmt.elseBranch().stmts().size());
    }
}
