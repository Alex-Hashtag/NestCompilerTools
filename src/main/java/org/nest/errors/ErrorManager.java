package org.nest.errors;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Manages and reports compiler errors and warnings.
///
/// Multiple instances can exist, but it's recommended to use a single instance globally.
public final class ErrorManager {

    private final List<CompilerError> errors = new ArrayList<>();
    private final List<CompilerError> warnings = new ArrayList<>();

    private String fileName = "<unknown>";
    private List<String> sourceLines = Collections.emptyList();

    /// Creates a new ErrorManager instance.
    public ErrorManager() {}

    /// Sets the current file and source code for error reporting context.
    public void setContext(String fileName, String source) {
        this.fileName = fileName;
        this.sourceLines = List.of(source.split("\n", -1));
    }

    /// Reports an error.
    public void error(String message, int line, int column, String token, String hint) {
        errors.add(new SimpleCompilerError(message, line, column, token, hint));
    }

    /// Reports a warning.
    public void warning(String message, int line, int column, String token, String hint) {
        warnings.add(new SimpleCompilerError(message, line, column, token, hint));
    }

    /// Checks if any errors have been reported.
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /// Checks if any warnings have been reported.
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /// Retrieves all errors.
    public List<CompilerError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /// Retrieves all warnings.
    public List<CompilerError> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    /// Clears all recorded errors and warnings.
    public void clear() {
        errors.clear();
        warnings.clear();
    }

    /// Outputs all reported errors and warnings to the provided PrintStream.
    public void printReports(PrintStream out) {
        errors.forEach(err -> printSingle(out, err, true));
        warnings.forEach(warn -> printSingle(out, warn, false));
    }

    private void printSingle(PrintStream out, CompilerError error, boolean isError) {
        String label = isError ? "error" : "warning";
        String color = isError ? ANSI.RED : ANSI.YELLOW;

        out.println(ANSI.BOLD + color + label + " [" + fileName + "]:" + ANSI.RESET);
        out.println("  " + error.getMessage());
        out.printf("   --> line %d:%d%n", error.getLine(), error.getColumn());

        if (error.getLine() > 0 && error.getLine() <= sourceLines.size()) {
            String lineText = sourceLines.get(error.getLine() - 1);
            out.println("    |");
            out.printf("%3d | %s%n", error.getLine(), lineText);

            int underlineStart = error.getColumn() - 1;
            int caretCount = Math.max(1, error.getToken().length());
            out.print("    | ");
            out.print(" ".repeat(Math.max(0, underlineStart)));
            out.println(ANSI.BOLD + ANSI.GREEN + "^".repeat(caretCount) + ANSI.RESET);
        }

        if (error.getHint() != null && !error.getHint().isEmpty()) {
            out.println(ANSI.CYAN + "  = help: " + error.getHint() + ANSI.RESET);
        }

        out.println();
    }

    /// Standardized error messages.
    public static final class Standard {
        public static String expectedGot(String expected, String got) {
            return String.format("Expected '%s', but got '%s'.", expected, got);
        }

        public static String unknownIdentifier(String identifier) {
            return String.format("Unknown identifier '%s'.", identifier);
        }

        public static String unexpectedToken(String token) {
            return String.format("Unexpected token '%s'.", token);
        }

        public static String syntaxError(String detail) {
            return String.format("Syntax error: %s", detail);
        }

        public static String unterminatedStringLiteral() {
            return "Unterminated string literal.";
        }

        public static String indentationError() {
            return "Inconsistent indentation detected.";
        }
    }

    /// ANSI escape codes for colored terminal output.
    private static class ANSI {
        static final String RED = "\u001B[31m";
        static final String GREEN = "\u001B[32m";
        static final String YELLOW = "\u001B[33m";
        static final String CYAN = "\u001B[36m";
        static final String BOLD = "\u001B[1m";
        static final String RESET = "\u001B[0m";
    }
}
