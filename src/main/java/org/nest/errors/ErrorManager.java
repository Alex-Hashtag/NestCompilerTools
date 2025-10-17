package org.nest.errors;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/// Manages and reports compiler errors and warnings.
///
/// Multiple instances can exist, but it's recommended to use a single instance globally.
public final class ErrorManager
{
    private final List<CompilerError> errors = new ArrayList<>();
    private final List<CompilerError> warnings = new ArrayList<>();

    private String fileName = "<unknown>";
    private List<String> sourceLines = Collections.emptyList();
    private boolean showErrorCodes = true;
    private int contextLines = 2; // Number of context lines to show above and below error

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

    /// Set whether error codes should be displayed
    public void setShowErrorCodes(boolean showErrorCodes) {
        this.showErrorCodes = showErrorCodes;
    }

    /// Set the number of context lines to show above and below errors
    public void setContextLines(int contextLines) {
        this.contextLines = Math.max(0, contextLines);
    }

    /// Outputs all reported errors and warnings to the provided PrintStream.
    public void printReports(PrintStream out)
    {
        // Summary header
        if (!errors.isEmpty() || !warnings.isEmpty()) {
            printDivider(out);
            out.print(ANSI.BOLD);
            if (!errors.isEmpty()) {
                out.print(ANSI.RED + "error" + ANSI.RESET + ANSI.BOLD + ": ");
                out.print("found " + errors.size() + " error" + (errors.size() > 1 ? "s" : ""));
                
                if (!warnings.isEmpty()) {
                    out.print(" and ");
                }
            }
            
            if (!warnings.isEmpty()) {
                out.print(ANSI.YELLOW + "warning" + ANSI.RESET + ANSI.BOLD + ": ");
                out.print("found " + warnings.size() + " warning" + (warnings.size() > 1 ? "s" : ""));
            }
            out.println(ANSI.RESET);
            printDivider(out);
        }

        // Print all errors first
        if (!errors.isEmpty()) {
            errors.forEach(err -> {
                printSingle(out, err, true);
                out.println();  // Extra blank line between errors
            });
        }

        // Then print all warnings
        if (!warnings.isEmpty()) {
            warnings.forEach(warn -> {
                printSingle(out, warn, false);
                out.println();  // Extra blank line between warnings
            });
        }

        // Final summary if errors exist
        if (!errors.isEmpty()) {
            out.println(ANSI.RED + ANSI.BOLD + "error" + ANSI.RESET + ": aborting due to " + 
                       errors.size() + " previous error" + (errors.size() > 1 ? "s" : ""));
        }
    }

    private void printDivider(PrintStream out) {
        out.println(ANSI.DIM + "=" + "=".repeat(78) + ANSI.RESET);
    }

    private void printSingle(PrintStream out, CompilerError error, boolean isError)
    {
        String label = isError ? "error" : "warning";
        String primaryColor = isError ? ANSI.RED : ANSI.YELLOW;
        String secondaryColor = ANSI.CYAN;
        String errorCode = isError ? "E" + String.format("%04d", error.hashCode() & 0xFFFF) : 
                                    "W" + String.format("%04d", error.hashCode() & 0xFFFF);

        // File location header with error code
        out.print(ANSI.BOLD + primaryColor + label);
        if (showErrorCodes) {
            out.print("[" + errorCode + "]");
        }
        out.println(ANSI.RESET + ANSI.BOLD + ": " + error.getMessage() + ANSI.RESET);
        
        // Location header
        out.println(ANSI.BLUE + "  --> " + fileName + ":" + error.getLine() + ":" + error.getColumn() + ANSI.RESET);

        // Code context
        printCodeContext(out, error, primaryColor);

        // Hint if available with improved formatting
        if (error.getHint() != null && !error.getHint().isEmpty())
        {
            out.println();
            out.print(secondaryColor + ANSI.BOLD + "help" + ANSI.RESET + secondaryColor + ": ");
            
            // Format multi-line hints with proper indentation
            String[] hintLines = error.getHint().split("\n");
            out.println(hintLines[0] + ANSI.RESET);
            
            for (int i = 1; i < hintLines.length; i++) {
                out.println(secondaryColor + "      " + hintLines[i] + ANSI.RESET);
            }
        }

        // Suggest potential fixes based on error type if available
        suggestFix(out, error);
    }

    private void suggestFix(PrintStream out, CompilerError error) {
        // This could be expanded with more error-specific suggestions
        String message = error.getMessage();
        
        if (message.contains("Expected") && message.contains("but got")) {
            out.println();
            out.println(ANSI.GREEN + ANSI.BOLD + "suggestion" + ANSI.RESET + ANSI.GREEN + 
                      ": replace the incorrect token with the expected one" + ANSI.RESET);
        } else if (message.contains("Unknown identifier")) {
            out.println();
            out.println(ANSI.GREEN + ANSI.BOLD + "suggestion" + ANSI.RESET + ANSI.GREEN + 
                      ": check for typos or make sure the identifier is defined before use" + ANSI.RESET);
        }
    }

    private void printCodeContext(PrintStream out, CompilerError error, String highlightColor)
    {
        int errorLine = error.getLine();
        String token = error.getToken();
        boolean isEOF = "<eof>".equals(token) || "End".equals(token);
        
        // Check if we have valid source lines
        if (errorLine <= 0 || sourceLines.isEmpty()) {
            return;
        }
        
        // If error is beyond source lines (EOF case), adjust to show last non-empty line
        if (errorLine > sourceLines.size())
        {
            if (isEOF) {
                // Show the last non-empty line with EOF indicator
                errorLine = sourceLines.size();
            } else {
                return;
            }
        }
        
        // For EOF errors, adjust to show the last meaningful line
        if (isEOF) {
            // If error line is beyond source, use the last line
            if (errorLine > sourceLines.size()) {
                errorLine = sourceLines.size();
            }
            
            // If on an empty line, find the last non-empty line
            if (errorLine <= sourceLines.size() && errorLine > 0) {
                String currentLine = sourceLines.get(errorLine - 1);
                if (currentLine.trim().isEmpty()) {
                    // Find the last non-empty line before this
                    for (int i = errorLine - 1; i >= 1; i--) {
                        if (!sourceLines.get(i - 1).trim().isEmpty()) {
                            errorLine = i;
                            break;
                        }
                    }
                }
            }
        }

        // Calculate line number width for padding
        int maxLineNum = Math.min(sourceLines.size(), errorLine + contextLines);
        int lineNumWidth = String.valueOf(maxLineNum).length();
        lineNumWidth = Math.max(lineNumWidth, 3); // Minimum width of 3

        String lineNumFormat = "%" + lineNumWidth + "d";
        String emptyLineNum = " ".repeat(lineNumWidth);

        // Print top border
        out.println(ANSI.BLUE + emptyLineNum + " |" + ANSI.RESET);

        // Print context lines before error
        int contextStart = Math.max(1, errorLine - contextLines);
        for (int i = contextStart; i < errorLine; i++) {
            printCodeLine(out, i, lineNumFormat, ANSI.BLUE, null, 0, 0);
        }

        // Print the error line
        // For EOF errors, highlight at the end of the line or show empty line
        int tokenLength = isEOF ? 0 : error.getToken().length();
        int column = error.getColumn();
        
        // If EOF and we're showing the last line, position at end of line
        if (isEOF && errorLine <= sourceLines.size()) {
            String lastLine = sourceLines.get(errorLine - 1);
            column = lastLine.length() + 1; // Position after last character
            tokenLength = 1; // Show a single caret
        }
        
        printCodeLine(out, errorLine, lineNumFormat, ANSI.BLUE + ANSI.BOLD, highlightColor,
                column, tokenLength);

        // Print underline with more informative pointer
        int underlineStart = column - 1;
        int underlineLength = Math.max(1, tokenLength);

        out.print(ANSI.BLUE + emptyLineNum + " |" + ANSI.RESET + " ");
        out.print(" ".repeat(Math.max(0, underlineStart)));
        
        // Create underline with primary error message
        out.print(ANSI.BOLD + highlightColor);
        
        // If token is very small, use a caret, otherwise use underlines with carets at the ends
        if (underlineLength <= 2) {
            out.print("^");
        } else {
            out.print("^" + "~".repeat(underlineLength - 2) + "^");
        }
        
        // Add inline error note for better context
        String shortDesc = isEOF ? "unexpected end of file" : getShortErrorDescription(error);
        out.println(" " + shortDesc + ANSI.RESET);

        // Print context lines after error
        int contextEnd = Math.min(sourceLines.size(), errorLine + contextLines);
        for (int i = errorLine + 1; i <= contextEnd; i++) {
            printCodeLine(out, i, lineNumFormat, ANSI.BLUE, null, 0, 0);
        }

        // Print bottom border
        out.println(ANSI.BLUE + emptyLineNum + " |" + ANSI.RESET);
    }

    private String getShortErrorDescription(CompilerError error) {
        // Create a shorter, more specific error message for inline display
        String message = error.getMessage();
        
        // Truncate if too long
        int maxLength = 40;
        if (message.length() > maxLength) {
            return message.substring(0, maxLength - 3) + "...";
        }
        return message;
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
        public static final String DIM = "\u001B[2m";
        public static final String ITALIC = "\u001B[3m";
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
