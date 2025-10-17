package org.nest.sprouts;

import org.nest.ast.ASTWrapper;
import org.nest.errors.ErrorManager;
import org.nest.sprouts.ast.Program;
import org.nest.tokenization.TokenList;
import org.nest.tokenization.TokenPostProcessor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Main runner for Sprout-S programs.
 * Handles parsing, error reporting, and execution.
 * 
 * Usage:
 *   java org.nest.sprouts.SproutSRunner <file.spr>
 *   java org.nest.sprouts.SproutSRunner --sample
 * 
 * Exit codes:
 *   0 = success (or program's exit code)
 *   1 = parse/compilation errors
 *   2 = runtime errors or I/O failures
 */
public class SproutSRunner {
    
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(2);
        }
        
        String code;
        String fileName;
        
        try {
            if (args[0].equals("--sample")) {
                code = getSampleProgram();
                fileName = "sample.spr";
                System.out.println("=== Running Sample Program ===\n");
            } else {
                Path filePath = Path.of(args[0]);
                code = Files.readString(filePath, StandardCharsets.UTF_8);
                fileName = filePath.getFileName().toString();
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(2);
            return;
        }
        
        // Parse the program
        Program program = parse(code, fileName);
        if (program == null) {
            System.exit(1);
            return;
        }
        
        System.out.println("=== Executing Program ===\n");
        
        // Execute the program
        ErrorManager runtimeErrorManager = new ErrorManager();
        runtimeErrorManager.setContext(fileName, code);
        SproutSInterpreter interpreter = new SproutSInterpreter(System.out, runtimeErrorManager);
        int exitCode = interpreter.execute(program);
        
        // Check for runtime errors
        if (runtimeErrorManager.hasErrors()) {
            System.out.println("\n=== Runtime Errors ===\n");
            runtimeErrorManager.printReports(System.out);
            System.exit(2);
            return;
        }
        
        System.out.println("\n=== Program Finished ===");
        System.out.println("Exit code: " + exitCode);
        
        System.exit(exitCode);
    }
    
    /**
     * Parses Sprout-S source code and returns the AST.
     * Prints errors to stdout if parsing fails.
     */
    private static Program parse(String code, String fileName) {
        ErrorManager errorManager = new ErrorManager();
        errorManager.setContext(fileName, code);
        
        try {
            // Tokenize
            TokenPostProcessor postProcessor = TokenPostProcessor.builder().build();
            TokenList tokens = TokenList.create(code, SproutSTokenRules.rules(), postProcessor);
            
            System.out.println("Tokenization: " + tokens.size() + " tokens");
            
            // Parse
            ASTWrapper astWrapper = SproutSASTRules.rules().createAST(tokens, errorManager);
            errorManager.printReports(System.out);

            // Check for errors
            if (errorManager.hasErrors()) {
                System.out.println("\n=== Compilation Errors ===\n");
                return null;
            }
            
            if (astWrapper.get().isEmpty()) {
                System.out.println("Warning: Empty program");
                return new Program(java.util.List.of());
            }
            
            Object result = astWrapper.get().get(0);
            if (!(result instanceof Program)) {
                System.err.println("Error: Parser did not return a Program");
                return null;
            }
            
            Program program = (Program) result;
            System.out.println("Parsing: " + program.stmts().size() + " statement(s)");
            System.out.println("✓ Compilation successful\n");
            
            return program;
            
        } catch (Exception e) {
            System.err.println("Unexpected error during parsing:");
            e.printStackTrace(System.err);
            return null;
        }
    }
    
    /**
     * Returns a sample Sprout-S program for testing
     */
    private static String getSampleProgram() {
        return """
                // Fibonacci-like sequence
                let a = 0;
                let b = 1;
                let n = 15;
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
                
                exit 0;
                """;
    }
    
    /**
     * Prints usage information
     */
    private static void printUsage() {
        System.err.println("Sprout-S Interpreter");
        System.err.println();
        System.err.println("Usage:");
        System.err.println("  java org.nest.sprouts.SproutSRunner <file.spr>");
        System.err.println("  java org.nest.sprouts.SproutSRunner --sample");
        System.err.println();
        System.err.println("Examples:");
        System.err.println("  java org.nest.sprouts.SproutSRunner program.spr");
        System.err.println("  java org.nest.sprouts.SproutSRunner --sample");
    }
}
