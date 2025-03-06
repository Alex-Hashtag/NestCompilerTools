package org.nest.tokenization;

import java.math.BigInteger;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A utility class that provides static token transformation functions.
 * Each method accepts a specific token type and returns a transformed token of the same type.
 * <p>
 * These functions can be referenced via method references (e.g., TokenTransformations::unquoteAndTrimIndentation)
 * when building your TokenPostProcessor.
 */
public final class TokenTransformations
{

    /**
     * Removes surrounding quotes from a string literal token.
     * If the literal is a multi-line string (delimited by triple quotes),
     * it also normalizes indentation.
     *
     * @param lit the literal token to transform; expected to be of type "string"
     * @return a new {@link Token.Literal} with quotes removed and indentation normalized.
     */
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

    /**
     * Processes escape sequences in a string literal token,
     * converting sequences like \n, \t, \\, and \" into their actual characters.
     *
     * @param lit the literal token to transform; expected to be of type "string"
     * @return a new {@link Token.Literal} with escape sequences replaced.
     */
    public static Token.Literal processEscapeSequences(Token.Literal lit) {
        String value = lit.value();
        StringBuilder processed = new StringBuilder();

        // Regex for escape sequences (\n, \t, \r, \\, \", \b, \f, \0, \\uXXXX)
        Pattern pattern = Pattern.compile("\\\\([\"\\\\bfnrt0]|u[0-9a-fA-F]{4})");
        Matcher matcher = pattern.matcher(value);

        int lastIndex = 0;
        while (matcher.find()) {
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

    /**
     * Removes common leading indentation from all non-blank lines of the provided text.
     * Used for normalizing multi-line string literals.
     *
     * @param text the multi-line string (without surrounding triple quotes)
     * @return the text with common indentation removed.
     */
    public static String removeCommonIndentation(String text)
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

    /**
     * Strips comment markers from a comment token.
     * For single-line comments (starting with "//"), removes the first two characters.
     * For multi-line comments (delimited by "/*" and * /"), removes the delimiters.
     *
     * @param comment the comment token to transform.
     * @return a new {@link Token.Comment} with comment markers removed.
     */
    public static Token.Comment stripCommentMarkers(Token.Comment comment)
    {
        String value = comment.value();
        String stripped = value;
        if (value.startsWith("//"))
        {
            stripped = value.substring(2).strip();
        }
        else if (value.startsWith("/*") && value.endsWith("*/"))
        {
            stripped = value.substring(2, value.length() - 2).strip();
        }
        return new Token.Comment(comment.position(), stripped);
    }

    /**
     * Normalizes an integer literal token:
     * - Removes underscores.
     * - Parses the number (handling hexadecimal, binary, and octal formats)
     * - Returns its decimal (base-10) representation.
     *
     * @param lit the literal token to transform; expected to be of type "integer"
     * @return a new {@link Token.Literal} with the integer in decimal form.
     */
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

    /**
     * Normalizes a float literal token:
     * - Removes underscores.
     * - Parses the float value and formats it in scientific notation.
     *
     * @param lit the literal token to transform; expected to be of type "float"
     * @return a new {@link Token.Literal} with the float in scientific notation.
     */
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

    /**
     * Strips the '@' character from an annotation token.
     *
     * @param identifier the identifier token to transform; expected to be of type "annotation"
     * @return a new {@link Token.Identifier} without the '@' prefix.
     */
    public static Token.Identifier stripAnnotation(Token.Identifier identifier)
    {
        String value = identifier.value();
        String stripped = value.startsWith("@") ? value.substring(1) : value;
        return new Token.Identifier(identifier.position(), identifier.type(), stripped);
    }

    /**
     * Strips the trailing '!' character from a macro token.
     *
     * @param identifier the identifier token to transform; expected to be of type "macro"
     * @return a new {@link Token.Identifier} without the trailing '!'.
     */
    public static Token.Identifier stripMacroExclamation(Token.Identifier identifier)
    {
        String value = identifier.value();
        String stripped = value.endsWith("!") ? value.substring(0, value.length() - 1) : value;
        return new Token.Identifier(identifier.position(), identifier.type(), stripped);
    }
}
