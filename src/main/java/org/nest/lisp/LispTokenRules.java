package org.nest.lisp;

import org.nest.tokenization.TokenRules;
import org.nest.tokenization.WhitespaceMode;


/// Defines the token rules for Lisp language parsing.
public class LispTokenRules
{

    /// Creates and returns the standard token rules for Lisp.
    ///
    /// @return TokenRules configured for Lisp syntax
    public static TokenRules create()
    {
        return TokenRules.builder()
                // Lisp uses parentheses as structure
                .delimiter("(")
                .delimiter(")")

                // Quote characters
                .operator("'")   // Quote
                .operator("`")   // Quasiquote
                .operator(",")   // Unquote
                .operator(",@")  // Unquote-splicing

                // Identifiers (symbols, function names)
                .identifier("symbol", "[^\\s(),'`,@:]+") // Updated to exclude colon and quote characters
                .identifier("keyword", ":[^\\s(),'`,@]+") // Keywords start with colon

                // Numbers
                .literal("integer", "[+-]?[0-9]+")
                .literal("float", "[+-]?[0-9]*\\.[0-9]+")
                .literal("boolean", "#t|#f")
                .literal("nil", "nil")

                // Character literals
                .literal("character", "#\\\\.")

                // Strings
                .literal("string", "^\"(?:\\\\.|[^\"\\\\])*\"")

                // Comments (Lisp often uses ';' for line comments)
                .comment(";.*")

                // Settings
                .whitespaceMode(WhitespaceMode.IGNORE)
                .enableLongestMatchFirst()
                .makeCaseSensitive()
                .build();
    }
}
