package org.nest;

import org.nest.tokenization.*;


public class Main
{
    public static void main(String[] args)
    {

        TokenRules lispRules = TokenRules.builder()
                // Lisp uses parentheses as structure
                .delimeter("(")
                .delimeter(")")

                // Identifiers (symbols, function names)
                .identifier("symbol", "[^\\s()]+") // Anything that's not space or parentheses

                // Numbers
                .literal("integer", "[+-]?[0-9]+")
                .literal("float", "[+-]?[0-9]*\\.[0-9]+")
                .literal("boolean", "#t|#f")
                .literal("nil", "nil")

                // Strings
                .literal("string", "^\"(?:\\\\.|[^\"\\\\])*\"")

                // Comments (Lisp often uses ';' for line comments)
                .comment(";.*")

                // Settings
                .whitespaceMode(WhitespaceMode.IGNORE)
                .enableLongestMatchFirst()
                .makeCaseSensitive()
                .build();


        TokenPostProcessor lispPost = TokenPostProcessor.builder()
                .literal("string", TokenTransformations::processEscapeSequences)
                .literal("string", TokenTransformations::unquoteAndTrimIndentation)
                .literal("integer", TokenTransformations::normalizeInteger)
                .literal("float", TokenTransformations::normalizeFloat)
                .comment("comment", Main.stripLispCommentMarker)
                .build();

        String lispCode = """
      (define (square x)
        (* x x)) ; This computes the square
      (print (square 5))
    """;

        TokenList lispTokens = TokenList.create(lispCode, lispRules, lispPost);
        System.out.println(lispTokens);
    }

    public static final java.util.function.Function<Token.Comment, Token.Comment> stripLispCommentMarker =
            (Token.Comment comment) -> {
                String value = comment.value();
                String stripped = value.startsWith(";")
                        ? value.substring(1).strip()
                        : value.strip(); // fallback
                return new Token.Comment(comment.position(), stripped);
            };
}