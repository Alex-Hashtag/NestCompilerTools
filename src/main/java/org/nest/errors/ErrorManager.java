package org.nest.errors;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/// Manages and reports compiler errors and warnings.
///
/// Multiple instances can exist, but it's recommended to use a single instance globally.
public final class ErrorManager
{

    private final List<CompilerError> errors = new ArrayList<>();
    private final List<CompilerError> warnings = new ArrayList<>();

    private String fileName = "<unknown>";
    private List<String> sourceLines = Collections.emptyList();

    /// Creates a new ErrorManager instance.
    public ErrorManager()
    {
    }

    /// Sets the current file and source code for error reporting context.
    public void setContext(String fileName, String source)
    {
        this.fileName = fileName;
        this.sourceLines = List.of(source.split("\n", -1));
    }

    /// Reports an error.
    public void error(String message, int line, int column, String token, String hint)
    {
        errors.add(new SimpleCompilerError(message, line, column, token, hint));
    }

    /// Reports a warning.
    public void warning(String message, int line, int column, String token, String hint)
    {
        warnings.add(new SimpleCompilerError(message, line, column, token, hint));
    }

    /// Checks if any errors have been reported.
    public boolean hasErrors()
    {
        return !errors.isEmpty();
    }

    /// Checks if any warnings have been reported.
    public boolean hasWarnings()
    {
        return !warnings.isEmpty();
    }

    /// Retrieves all errors.
    public List<CompilerError> getErrors()
    {
        return Collections.unmodifiableList(errors);
    }

    /// Retrieves all warnings.
    public List<CompilerError> getWarnings()
    {
        return Collections.unmodifiableList(warnings);
    }

    /// Clears all recorded errors and warnings.
    public void clear()
    {
        errors.clear();
        warnings.clear();
    }

    /// Outputs all reported errors and warnings to the provided PrintStream.
    public void printReports(PrintStream out)
    {
        if (!errors.isEmpty())
        {
            out.println(ANSI.BOLD + "Compiler found " + errors.size() + " error" + (errors.size() > 1 ? "s" : "") + ANSI.RESET);
            errors.forEach(err -> printSingle(out, err, true));
        }

        if (!warnings.isEmpty())
        {
            out.println(ANSI.BOLD + "Compiler emitted " + warnings.size() + " warning" + (warnings.size() > 1 ? "s" : "") + ANSI.RESET);
            warnings.forEach(warn -> printSingle(out, warn, false));
        }
    }

    private void printSingle(PrintStream out, CompilerError error, boolean isError)
    {
        String label = isError ? "error" : "warning";
        String primaryColor = isError ? ANSI.RED : ANSI.YELLOW;
        String secondaryColor = ANSI.CYAN;

        // File location header
        out.println(ANSI.BOLD + primaryColor + label + ANSI.RESET + ANSI.BOLD + ": " + error.getMessage() + ANSI.RESET);
        out.println(ANSI.BLUE + "  --> " + fileName + ":" + error.getLine() + ":" + error.getColumn() + ANSI.RESET);

        // Code context
        printCodeContext(out, error, primaryColor);

        // Hint if available
        if (error.getHint() != null && !error.getHint().isEmpty())
        {
            out.println(secondaryColor + ANSI.BOLD + "help" + ANSI.RESET + secondaryColor + ": " + error.getHint() + ANSI.RESET);
        }

        out.println();
    }

    private void printCodeContext(PrintStream out, CompilerError error, String highlightColor)
    {
        int errorLine = error.getLine();

        // Check if we have valid source lines
        if (errorLine <= 0 || errorLine > sourceLines.size())
        {
            return;
        }

        // Calculate line number width for padding
        int lineNumWidth = String.valueOf(errorLine).length();
        lineNumWidth = Math.max(lineNumWidth, 3); // Minimum width of 3

        String lineNumFormat = "%" + lineNumWidth + "d";
        String emptyLineNum = " ".repeat(lineNumWidth);

        // Print lines before the error for context (up to 2 lines)
        int contextStart = Math.max(1, errorLine - 2);

        // Print top border
        out.println(ANSI.BLUE + emptyLineNum + " |" + ANSI.RESET);

        // Print context lines before error
        for (int i = contextStart; i < errorLine; i++)
        {
            printCodeLine(out, i, lineNumFormat, ANSI.BLUE, null, 0, 0);
        }

        // Print the error line
        printCodeLine(out, errorLine, lineNumFormat, ANSI.BLUE + ANSI.BOLD, highlightColor,
                error.getColumn(), error.getToken().length());

        // Print error indicator line
        int underlineStart = error.getColumn() - 1;
        int underlineLength = Math.max(1, error.getToken().length());

        out.print(ANSI.BLUE + emptyLineNum + " |" + ANSI.RESET + " ");
        out.print(" ".repeat(Math.max(0, underlineStart)));
        out.println(ANSI.BOLD + highlightColor + "^".repeat(underlineLength) + " " + error.getMessage() + ANSI.RESET);

        // Print bottom border
        out.println(ANSI.BLUE + emptyLineNum + " |" + ANSI.RESET);
    }

    private void printCodeLine(PrintStream out, int lineNum, String lineNumFormat,
                               String lineNumColor, String highlightColor,
                               int errorColumn, int errorLength)
    {
        String lineText = sourceLines.get(lineNum - 1);

        // Line number and separator
        out.print(lineNumColor + String.format(lineNumFormat, lineNum) + " |" + ANSI.RESET + " ");

        // If there's an error to highlight on this line
        if (highlightColor != null && errorColumn > 0 && errorLength > 0)
        {
            // Split the line into before, error, and after parts
            int errorStart = Math.max(0, errorColumn - 1);
            int errorEnd = Math.min(lineText.length(), errorStart + errorLength);

            String beforeError = lineText.substring(0, errorStart);
            String errorText = lineText.substring(errorStart, errorEnd);
            String afterError = "";

            if (errorEnd < lineText.length())
            {
                afterError = lineText.substring(errorEnd);
            }

            // Print with highlighting
            out.print(beforeError);
            out.print(ANSI.BOLD + highlightColor + errorText + ANSI.RESET);
            out.println(afterError);
        }
        else
        {
            // Print normal line
            out.println(lineText);
        }
    }

    /// Standardized error messages.
    public static final class Standard
    {
        public static String expectedGot(String expected, String got)
        {
            return String.format("Expected '%s', but got '%s'.", expected, got);
        }

        public static String unknownIdentifier(String identifier)
        {
            return String.format("Unknown identifier '%s'.", identifier);
        }

        public static String unexpectedToken(String token)
        {
            return String.format("Unexpected token '%s'.", token);
        }

        public static String syntaxError(String detail)
        {
            return String.format("Syntax error: %s", detail);
        }

        public static String unterminatedStringLiteral()
        {
            return "Unterminated string literal.";
        }

        public static String indentationError()
        {
            return "Inconsistent indentation detected.";
        }
    }

    /// ANSI escape codes for colored terminal output.
    public static class ANSI
    {
        public static final String RED = "\u001B[31m";
        public static final String GREEN = "\u001B[32m";
        public static final String YELLOW = "\u001B[33m";
        public static final String BLUE = "\u001B[34m";
        public static final String MAGENTA = "\u001B[35m";
        public static final String CYAN = "\u001B[36m";
        public static final String WHITE = "\u001B[37m";
        public static final String BOLD = "\u001B[1m";
        public static final String UNDERLINE = "\u001B[4m";
        public static final String RESET = "\u001B[0m";

        // Disable colors if necessary
        private static boolean enabled = true;

        public static boolean isEnabled()
        {
            return enabled;
        }

        public static void setEnabled(boolean enabled)
        {
            ANSI.enabled = enabled;
        }
    }
}
