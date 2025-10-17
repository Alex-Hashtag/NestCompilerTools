package org.nest.sprouts;

import org.nest.tokenization.TokenRules;
import org.nest.tokenization.WhitespaceMode;

/**
 * Defines the token rules for Sprout-S language parsing.
 * Based on the lexical specification in DocS.md
 */
public class SproutSTokenRules
{
    /**
     * Creates and returns the standard token rules for Sprout-S.
     *
     * @return TokenRules configured for Sprout-S syntax
     */
    public static TokenRules rules()
    {
        return TokenRules.builder()
                // Keywords (must come before identifiers to avoid being parsed as IDENT)
                .keyword("let")
                .keyword("set")
                .keyword("if")
                .keyword("else")
                .keyword("while")
                .keyword("print")
                .keyword("exit")

                // Two-character operators (must come before one-char to ensure longest match)
                .operator("==")
                .operator("!=")
                .operator("<=")
                .operator(">=")
                .operator("&&")
                .operator("||")

                // One-character operators
                .operator("+")
                .operator("-")
                .operator("*")
                .operator("/")
                .operator("%")
                .operator("<")
                .operator(">")
                .operator("!")

                // Delimiters
                .delimiter("(")
                .delimiter(")")
                .delimiter("{")
                .delimiter("}")
                .delimiter(";")
                .delimiter("=")

                // Literals - INT: 0 or [1-9][0-9]*
                .literal("INT", "0|[1-9][0-9]*")

                // Identifiers - [A-Za-z_][A-Za-z0-9_]*
                .identifier("IDENT", "[A-Za-z_][A-Za-z0-9_]*")

                // Line comments - //[^\r\n]*
                .comment("//[^\r\n]*")

                // Settings
                .whitespaceMode(WhitespaceMode.IGNORE)
                .enableLongestMatchFirst()
                .makeCaseSensitive()
                .build();
    }
}
