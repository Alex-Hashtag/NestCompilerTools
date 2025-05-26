package org.nest.lisp;

import org.nest.tokenization.Token;
import org.nest.tokenization.TokenPostProcessor;

import java.math.BigInteger;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/// Defines post-processing operations for Lisp tokens.
public class LispTokenProcessor
{

    /// Strips the leading semicolon from a Lisp comment and trims whitespace.
    ///
    /// @param comment the comment token to transform
    /// @return a new Comment token with the leading semicolon removed and whitespace trimmed
    public static Token.Comment stripLispCommentMarker(Token.Comment comment)
    {
        String value = comment.value();
        String stripped = value.startsWith(";")
                ? value.substring(1).strip()
                : value.strip(); // fallback
        return new Token.Comment(comment.position(), stripped);
    }

    /// Processes escape sequences in a string literal token,
    /// converting sequences like \n, \t, \\, and \" into their actual characters.
    ///
    /// @param lit the literal token to transform; expected to be of type "string"
    /// @return a new [Token.Literal] with escape sequences replaced.
    public static Token.Literal processEscapeSequences(Token.Literal lit)
    {
        String value = lit.value();
        StringBuilder processed = new StringBuilder();

        // Regex for escape sequences (\n, \t, \r, \\, \", \b, \f, \0)
        Pattern pattern = Pattern.compile("\\\\([\"\\\\bfnrt0]|u[0-9a-fA-F]{4})");
        Matcher matcher = pattern.matcher(value);

        int lastIndex = 0;
        while (matcher.find())
        {
            processed.append(value, lastIndex, matcher.start());

            String match = matcher.group(1);
            switch (match)
            {
                case "n" -> processed.append('\n');
                case "t" -> processed.append('\t');
                case "r" -> processed.append('\r');
                case "b" -> processed.append('\b');
                case "f" -> processed.append('\f');
                case "0" -> processed.append('\0');
                case "\\" -> processed.append('\\');
                case "\"" -> processed.append('\"');
                default ->
                {
                    if (match.startsWith("u"))
                    {
                        processed.append((char) Integer.parseInt(match.substring(1), 16));
                    }
                }
            }

            lastIndex = matcher.end();
        }

        // Append remaining part of the string
        processed.append(value.substring(lastIndex));

        return new Token.Literal(lit.position(), lit.type(), processed.toString());
    }

    /// Removes surrounding quotes from a string literal token.
    /// If the literal is a multi-line string (delimited by triple quotes),
    /// it also normalizes indentation.
    ///
    /// @param lit the literal token to transform; expected to be of type "string"
    /// @return a new [Token.Literal] with quotes removed and indentation normalized.
    public static Token.Literal unquoteAndTrimIndentation(Token.Literal lit)
    {
        String value = lit.value();
        String unquoted;
        if (value.startsWith("\"\"\""))
        {
            // Multi-line string: remove triple quotes and normalize indentation.
            unquoted = value.substring(3, value.length() - 3);
            unquoted = removeCommonIndentation(unquoted);
        }
        else if (value.startsWith("\""))
        {
            // Single-line string: remove surrounding quotes.
            unquoted = value.substring(1, value.length() - 1);
        }
        else
        {
            unquoted = value;
        }
        return new Token.Literal(lit.position(), lit.type(), unquoted);
    }

    /// Removes common leading indentation from all non-blank lines of the provided text.
    /// Used for normalizing multi-line string literals.
    ///
    /// @param text the multi-line string (without surrounding triple quotes)
    /// @return the text with common indentation removed.
    private static String removeCommonIndentation(String text)
    {
        String[] lines = text.split("\n");
        int minIndent = Integer.MAX_VALUE;
        for (String line : lines)
        {
            if (!line.isBlank())
            {
                int indent = line.length() - line.stripLeading().length();
                if (indent < minIndent)
                {
                    minIndent = indent;
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String line : lines)
        {
            sb.append(line.length() >= minIndent ? line.substring(minIndent) : line)
                    .append("\n");
        }
        return sb.toString().stripTrailing();
    }

    /// Normalizes an integer literal token:
    /// - Removes underscores.
    /// - Parses the number (handling hexadecimal, binary, and octal formats)
    /// - Returns its decimal (base-10) representation.
    ///
    /// @param lit the literal token to transform; expected to be of type "integer"
    /// @return a new [Token.Literal] with the integer in decimal form.
    public static Token.Literal normalizeInteger(Token.Literal lit)
    {
        String raw = lit.value().replace("_", "");
        int base = 10;
        if (raw.startsWith("0x") || raw.startsWith("0X"))
        {
            base = 16;
            raw = raw.substring(2);
        }
        else if (raw.startsWith("0b") || raw.startsWith("0B"))
        {
            base = 2;
            raw = raw.substring(2);
        }
        else if (raw.startsWith("0o") || raw.startsWith("0O"))
        {
            base = 8;
            raw = raw.substring(2);
        }
        BigInteger bi = new BigInteger(raw, base);
        String normalized = bi.toString(10);
        return new Token.Literal(lit.position(), lit.type(), normalized);
    }

    /// Normalizes a float literal token:
    /// - Removes underscores.
    /// - Parses the float value and formats it in scientific notation.
    ///
    /// @param lit the literal token to transform; expected to be of type "float"
    /// @return new [Token.Literal] with the float in scientific notation.
    public static Token.Literal normalizeFloat(Token.Literal lit)
    {
        String raw = lit.value().replace("_", "");
        try
        {
            double d = Double.parseDouble(raw);
            String normalized = String.format(Locale.ROOT, "%e", d);
            return new Token.Literal(lit.position(), lit.type(), normalized);
        } catch (NumberFormatException e)
        {
            return lit;
        }
    }

    /// Creates and returns the standard token post-processor for Lisp.
    ///
    /// @return TokenPostProcessor configured for Lisp token processing
    public static TokenPostProcessor create()
    {
        return TokenPostProcessor.builder()
                .literal("string", LispTokenProcessor::processEscapeSequences)
                .literal("string", LispTokenProcessor::unquoteAndTrimIndentation)
                .literal("integer", LispTokenProcessor::normalizeInteger)
                .literal("float", LispTokenProcessor::normalizeFloat)
                .comment("comment", LispTokenProcessor::stripLispCommentMarker)
                .build();
    }
}
