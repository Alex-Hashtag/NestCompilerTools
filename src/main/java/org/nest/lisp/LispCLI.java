package org.nest.lisp;

import org.nest.errors.ErrorManager;
import org.nest.lisp.ast.*;
import org.nest.lisp.parser.LispParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Command Line Interface for the Lisp interpreter.
 * Provides a REPL (Read-Eval-Print-Loop) environment for executing Lisp code.
 */
public class LispCLI {
    private final LispInterpreter interpreter;
    private final ErrorManager errorManager;
    private final LispParser parser;
    private boolean running = true;
    private static final String PROMPT = "lisp> ";
    private static final String MULTILINE_PROMPT = "... ";
    private static final String EXIT_COMMAND = "(exit)";
    private static final String HELP_COMMAND = "(help)";
    private static final String LOAD_COMMAND_PREFIX = "(load ";

    /**
     * Creates a new LispCLI with a fresh interpreter and error manager.
     */
    public LispCLI() {
        this.errorManager = new ErrorManager();
        this.interpreter = new LispInterpreter(errorManager);
        this.parser = new LispParser();
    }

    /**
     * Starts the REPL (Read-Eval-Print-Loop).
     */
    public void start() {
        printWelcome();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (running) {
                System.out.print(PROMPT);
                String input = reader.readLine();
                
                if (input == null) {
                    // End of input (Ctrl+D)
                    break;
                }
                
                input = input.trim();
                
                if (input.isEmpty()) {
                    continue;
                }
                
                // Handle multi-line input for unbalanced parentheses
                int openParens = countChar(input, '(');
                int closeParens = countChar(input, ')');
                
                StringBuilder fullInput = new StringBuilder(input);
                
                while (openParens > closeParens) {
                    System.out.print(MULTILINE_PROMPT);
                    String additionalInput = reader.readLine();
                    
                    if (additionalInput == null) {
                        break;
                    }
                    
                    fullInput.append(" ").append(additionalInput);
                    openParens += countChar(additionalInput, '(');
                    closeParens += countChar(additionalInput, ')');
                }
                
                String command = fullInput.toString().trim();
                
                // Handle special commands
                if (EXIT_COMMAND.equalsIgnoreCase(command)) {
                    running = false;
                    System.out.println("Goodbye!");
                    continue;
                } else if (HELP_COMMAND.equalsIgnoreCase(command)) {
                    printHelp();
                    continue;
                } else if (command.startsWith(LOAD_COMMAND_PREFIX) && command.endsWith(")")) {
                    // Extract filename from (load "filename")
                    String filename = command.substring(LOAD_COMMAND_PREFIX.length(), command.length() - 1).trim();
                    // Remove quotes if present
                    if (filename.startsWith("\"") && filename.endsWith("\"")) {
                        filename = filename.substring(1, filename.length() - 1);
                    }
                    loadFile(filename);
                    continue;
                }
                
                // Parse and evaluate the input
                evaluateAndPrint(command);
            }
        } catch (IOException e) {
            System.err.println("Error reading input: " + e.getMessage());
        }
    }
    
    /**
     * Parses, evaluates, and prints the result of a Lisp expression.
     * 
     * @param input The Lisp expression to evaluate
     */
    public void evaluateAndPrint(String input) {
        // Set context for error reporting
        errorManager.setContext("REPL", input);

        // Parse the input
        LispAST ast = LispParser.parse(input, errorManager);

        if (errorManager.hasErrors()) {
            // Handle parsing errors
            System.err.println();
            errorManager.printReports(System.err);
            errorManager.clear();
            System.err.println();
            return;
        }

        if (ast == null || ast.nodes().isEmpty()) {
            System.out.println("nil");
            return;
        }

        // Evaluate each expression in the input
        for (LispNode node : ast.nodes()) {
            // Evaluate the expression
            LispNode result = interpreter.evaluate(node);

            // Print any errors
            if (errorManager.hasErrors()) {
                System.err.println(); // Add a newline before error messages for readability
                errorManager.printReports(System.err);
                errorManager.clear();  // Clear errors after each execution
                System.err.println(); // Add a newline after error messages for readability
                continue;
            }

            // Print the result
            if (result != null) {
                System.out.println(LispPrinter.format(result));
            } else {
                System.out.println("nil");
            }
        }
    }

    /**
     * Loads and evaluates a Lisp file.
     *
     * @param filename The file to load
     */
    private void loadFile(String filename) {
        try {
            Path filePath = Path.of(filename);
            if (!Files.exists(filePath)) {
                System.err.println("File not found: " + filename);
                return;
            }

            String content = Files.readString(filePath);
            System.out.println("Loading file: " + filename);

            // Clear any previous errors
            errorManager.clear();

            // Set context for error reporting with the actual filename
            errorManager.setContext(filename, content);

            evaluateAndPrint(content);

            if (!errorManager.hasErrors()) {
                System.out.println("File loaded successfully: " + filename);
            }
        } catch (IOException e) {
            System.err.println("Error loading file: " + e.getMessage());
        }
    }
    
    /**
     * Counts occurrences of a character in a string.
     * 
     * @param str The string to search
     * @param c The character to count
     * @return The number of occurrences
     */
    private int countChar(String str, char c) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Prints the welcome message.
     */
    private void printWelcome() {
        System.out.println("NestCompilerTools Lisp Interpreter");
        System.out.println("Type (help) for available commands");
        System.out.println("Type (exit) to quit");
        System.out.println();
    }
    
    /**
     * Prints help information.
     */
    private void printHelp() {
        System.out.println("Available commands:");
        System.out.println("  (help)        - Show this help message");
        System.out.println("  (exit)        - Exit the interpreter");
        System.out.println("  (load \"file\") - Load and evaluate a Lisp file");
        System.out.println();
        System.out.println("Special forms:");
        System.out.println("  (quote x)                - Return x unevaluated");
        System.out.println("  (if cond then else)      - Conditional expression");
        System.out.println("  (cond (c1 e1) ... (cn en)) - Multiple condition expression");
        System.out.println("  (define name value)      - Define a variable");
        System.out.println("  (define (name args) body) - Define a function");
        System.out.println("  (lambda (args) body)     - Create an anonymous function");
        System.out.println("  (begin e1 e2 ... en)     - Evaluate expressions in sequence");
        System.out.println("  (let ((v1 e1) ... (vn en)) body) - Create local bindings");
        System.out.println();
    }
    
    /**
     * Main method to run the LispCLI.
     * 
     * @param args Command-line arguments (not used)
     */
    public static void main(String[] args) {
        LispCLI cli = new LispCLI();
        cli.start();
    }
}
