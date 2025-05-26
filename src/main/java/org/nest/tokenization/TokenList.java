package org.nest.tokenization;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Represents a list of tokens generated from an input string, given a set of TokenRules
 * and a TokenPostProcessor. Provides iteration, look-ahead, invalid-token retrieval, and
 * a formatted table output of all tokens.
 */
public final class TokenList implements Iterable<Token>
{
    private final List<Token> tokens;

    private TokenList(List<Token> tokens)
    {
        this.tokens = Collections.unmodifiableList(tokens);
    }

    /**
     * Factory method:
     * 1) Tokenizes the input using the given rules.
     * 2) Applies the post-processor to each token.
     * 3) Returns a new TokenList.
     */
    public static TokenList create(String input, TokenRules rules, TokenPostProcessor postProcessor)
    {
        List<Token> rawTokens = tokenize(input, rules);
        List<Token> processedTokens = applyPostProcessing(rawTokens, postProcessor);
        return new TokenList(processedTokens);
    }

    // ==================================================
    // =============== TOKENIZATION LOGIC ===============
    // ==================================================

    private static List<Token> tokenize(String input, TokenRules rules)
    {
        // 1) Normalize line endings: convert all \r\n or \r to \n
        input = input.replace("\r\n", "\n").replace("\r", "\n");

        // 2) Build prototypes, check presence of Start/End
        List<InternalProto> protoList = buildPrototypes(rules);

        boolean hasStart = rules.tokenPrototypes.stream().anyMatch(tp -> tp instanceof TokenPrototype.Start);
        boolean hasEnd = rules.tokenPrototypes.stream().anyMatch(tp -> tp instanceof TokenPrototype.End);

        // 3) Create result list
        List<Token> result = new ArrayList<>();

        // Possibly add a Start token
        if (hasStart)
        {
            result.add(new Token.Start(new Coordinates(0, 0)));
        }

        // Tokenize line by line
        String[] lines = input.split("\n", -1); // -1 to keep empty lines
        int lineCount = lines.length;

        int line = 1;
        Deque<Integer> indentStack = new ArrayDeque<>();
        indentStack.push(0);
        Character indentationChar = null;

        for (int li = 0; li < lineCount; li++)
        {
            // If not the very first line, and whitespaceMode != IGNORE => produce NewLine token
            if (li > 0 && rules.whitespaceMode != WhitespaceMode.IGNORE)
            {
                result.add(new Token.NewLine(new Coordinates(line - 1, 9999)));
            }

            String currentLine = lines[li];
            if (currentLine.isEmpty())
            {
                // Empty line => no tokens (indentation logic does nothing for empty lines)
                line++;
                continue;
            }

            // Handle indentation if needed
            if (rules.whitespaceMode == WhitespaceMode.INDENTATION)
            {
                handleIndentation(currentLine, line, indentStack, result, indentationChar);
                indentationChar = updateIndentationChar(currentLine, line, result, indentationChar);
            }

            // Tokenize the remainder of the line after indentation
            if (rules.whitespaceMode == WhitespaceMode.INDENTATION)
            {
                int leadingCount = 0;
                while (leadingCount < currentLine.length()
                        && (currentLine.charAt(leadingCount) == ' ' || currentLine.charAt(leadingCount) == '\t'))
                {
                    leadingCount++;
                }
                if (leadingCount < currentLine.length())
                {
                    tokenizeLineSegment(
                            currentLine.substring(leadingCount),
                            line,
                            leadingCount + 1,
                            protoList,
                            rules.caseSensitive,
                            rules.whitespaceMode,
                            t -> result.add(t)
                    );
                }
            }
            else
            {
                // No indentation logic
                tokenizeLineSegment(
                        currentLine,
                        line,
                        1,
                        protoList,
                        rules.caseSensitive,
                        rules.whitespaceMode,
                        t -> result.add(t)
                );
            }

            line++;
        }

        // Pop remaining indentations (Python-like)
        if (rules.whitespaceMode == WhitespaceMode.INDENTATION)
        {
            while (!indentStack.isEmpty() && indentStack.peek() > 0)
            {
                indentStack.pop();
                result.add(new Token.IndentDecr(new Coordinates(line, 1)));
            }
        }

        // Possibly add an End token
        if (hasEnd)
        {
            result.add(new Token.End(new Coordinates(line, 1)));
        }

        return result;
    }

    /**
     * Constructs and (optionally) sorts the InternalProtos based on the rules.
     */
    private static List<InternalProto> buildPrototypes(TokenRules rules)
    {
        List<InternalProto> list = new ArrayList<>();
        for (TokenPrototype proto : rules.tokenPrototypes)
        {
            switch (proto)
            {
                case TokenPrototype.Keyword kw -> list.add(InternalProto.keyword(kw.value(), rules.caseSensitive));
                case TokenPrototype.Delimiter d -> list.add(InternalProto.delimiter(d.value()));
                case TokenPrototype.Operator op -> list.add(InternalProto.operator(op.value()));
                case TokenPrototype.Comment c -> list.add(InternalProto.comment(c.regex()));
                case TokenPrototype.Literal lit -> list.add(InternalProto.literal(lit.type(), lit.regex()));
                case TokenPrototype.Identifier id -> list.add(InternalProto.identifier(id.type(), id.regex()));
                case TokenPrototype.NewLine n ->
                {
                    // Handled externally
                }
                case TokenPrototype.End e ->
                {
                    // Handled externally
                }
                case TokenPrototype.Start s ->
                {
                    // Handled externally
                }
            }
        }

        if (rules.longestMatchFirst)
        {
            list.sort(TokenList::compareInternalProto);
        }
        return list;
    }

    /**
     * Sort logic used by both eager and lazy tokenizers:
     * - Compare fixed strings by descending length
     * - Tie-break: if same string, prefer DELIMITER over OPERATOR
     * - Otherwise compare by ordinal
     */
    private static int compareInternalProto(InternalProto a, InternalProto b)
    {
        if (a.fixedString != null && b.fixedString != null)
        {
            int diff = b.fixedString.length() - a.fixedString.length();
            if (diff != 0) return diff;
            // Tie-break if strings match
            if (a.fixedString.equals(b.fixedString))
            {
                // prefer DELIMITER over OPERATOR
                if (a.type == InternalProtoType.DELIMITER && b.type == InternalProtoType.OPERATOR)
                {
                    return -1;
                }
                if (b.type == InternalProtoType.DELIMITER && a.type == InternalProtoType.OPERATOR)
                {
                    return 1;
                }
            }
            // fallback
            return a.type.ordinal() - b.type.ordinal();
        }
        // If only one is fixed
        return a.type.ordinal() - b.type.ordinal();
    }

    /**
     * Tokenizes a substring of a line, respecting the given whitespace mode and case-sensitivity,
     * then feeds each matched token into the provided consumer.
     */
    private static void tokenizeLineSegment(
            String lineContent,
            int line,
            int startCol,
            List<InternalProto> protoList,
            boolean caseSensitive,
            WhitespaceMode whitespaceMode,
            Consumer<Token> tokenConsumer
    )
    {
        int index = 0;
        int col = startCol;
        int length = lineContent.length();

        while (index < length)
        {
            char c = lineContent.charAt(index);

            // Skip whitespace if not indentation-based
            if ((whitespaceMode == WhitespaceMode.IGNORE || whitespaceMode == WhitespaceMode.SIGNIFICANT)
                    && Character.isWhitespace(c))
            {
                index++;
                col++;
                continue;
            }

            // Attempt “longest match” from the available prototypes
            BestMatch best = new BestMatch(-1, null, null);

            for (InternalProto proto : protoList)
            {
                MatchResult mr = proto.match(lineContent, index, line, col, caseSensitive);
                if (mr != null && mr.length > best.length)
                {
                    best = new BestMatch(mr.length, mr.token, proto.type);
                }
                else if (mr != null && mr.length == best.length)
                {
                    // Tie-break: prefer delimiter over operator if same text
                    if (proto.type == InternalProtoType.DELIMITER && best.type == InternalProtoType.OPERATOR)
                    {
                        if (mr.token instanceof Token.Delimiter delT
                                && best.token instanceof Token.Operator opT
                                && delT.value().equals(opT.value()))
                        {
                            best = new BestMatch(mr.length, mr.token, proto.type);
                        }
                    }
                }
            }

            if (best.token != null)
            {
                tokenConsumer.accept(best.token);
                index += best.length;
                col += best.length;
            }
            else
            {
                // No matches => invalid
                tokenConsumer.accept(new Token.Invalid(new Coordinates(line, col),
                        String.valueOf(lineContent.charAt(index))));
                index++;
                col++;
            }
        }
    }

    /**
     * Post-processes an entire list of tokens via the provided TokenPostProcessor.
     */
    private static List<Token> applyPostProcessing(List<Token> rawTokens, TokenPostProcessor postProcessor)
    {
        List<Token> output = new ArrayList<>(rawTokens.size());
        for (Token t : rawTokens)
        {
            output.add(applyPostProcessing(t, postProcessor));
        }
        return output;
    }

    /**
     * Applies post-processor transformations for a single token.
     */
    private static Token applyPostProcessing(Token t, TokenPostProcessor postProcessor)
    {
        // Determine the key used by the post-processor, if any
        String typeKey = switch (t)
        {
            case Token.Literal lit -> lit.type();
            case Token.Identifier id -> id.type();
            case Token.Keyword kw -> "keyword";
            case Token.Operator op -> "operator";
            case Token.Delimiter d -> "delimiter";
            case Token.Comment c -> "comment";
            default -> null;
        };

        if (typeKey != null)
        {
            for (Function<Token, Token> func : postProcessor.getProcessors(typeKey))
            {
                t = func.apply(t);
            }
        }
        return t;
    }

    /**
     * Handles indentation logic at the start of a line, pushing/popping from the indent stack.
     */
    private static void handleIndentation(String lineContent, int lineNumber, Deque<Integer> indentStack,
                                          List<Token> outTokens, Character indentationChar)
    {
        int indentWidth = 0;
        while (indentWidth < lineContent.length()
                && (lineContent.charAt(indentWidth) == ' ' || lineContent.charAt(indentWidth) == '\t'))
        {
            indentWidth++;
        }
        int currentIndent = indentStack.peek();

        if (indentWidth > currentIndent)
        {
            indentStack.push(indentWidth);
            outTokens.add(new Token.IndentIncr(new Coordinates(lineNumber, 1)));
        }
        else if (indentWidth < currentIndent)
        {
            while (!indentStack.isEmpty() && indentStack.peek() > indentWidth)
            {
                indentStack.pop();
                outTokens.add(new Token.IndentDecr(new Coordinates(lineNumber, 1)));
            }
            if (!indentStack.isEmpty() && indentStack.peek() != indentWidth)
            {
                outTokens.add(new Token.Invalid(new Coordinates(lineNumber, 1),
                        "Inconsistent indentation level " + indentWidth));
            }
            if (indentStack.isEmpty())
            {
                indentStack.push(0);
            }
        }
    }

    /**
     * Detects or checks indentation character usage (spaces vs tabs); throws Invalid if inconsistent.
     */
    private static Character updateIndentationChar(String lineContent, int lineNumber, List<Token> outTokens,
                                                   Character currentIndentChar)
    {
        boolean foundSpace = false;
        boolean foundTab = false;

        for (char c : lineContent.toCharArray())
        {
            if (c != ' ' && c != '\t') break;  // only leading chars matter
            if (c == ' ') foundSpace = true;
            if (c == '\t') foundTab = true;
        }
        if (foundSpace && foundTab)
        {
            outTokens.add(new Token.Invalid(new Coordinates(lineNumber, 1),
                    "Mixed tabs/spaces in indentation"));
        }
        else if (foundSpace || foundTab)
        {
            char usedChar = (foundSpace ? ' ' : '\t');
            if (currentIndentChar == null)
            {
                currentIndentChar = usedChar; // first use sets the indentation char
            }
            else if (!currentIndentChar.equals(usedChar))
            {
                outTokens.add(new Token.Invalid(new Coordinates(lineNumber, 1),
                        "Inconsistent indentation character (expected "
                                + (currentIndentChar == ' ' ? "spaces" : "tabs") + ")"));
            }
        }
        return currentIndentChar;
    }

    // ==================================================
    // ================== COLLECTION API ================
    // ==================================================

    /**
     * Returns an Iterable whose iterator lazily tokenizes the input (including
     * post-processing) one token at a time, preserving all nuance (indentation,
     * newlines, start/end tokens, etc.). This allows constructing parse trees
     * on the fly without storing all tokens in memory first.
     */
    public static Iterable<Token> lazyIterator(String input, TokenRules rules, TokenPostProcessor postProcessor)
    {
        return () -> new LazyTokenizer(input, rules, postProcessor);
    }

    @Override
    public Iterator<Token> iterator()
    {
        return new LookAheadIterator();
    }

    /**
     * Returns a list of all Invalid tokens in this TokenList.
     */
    public List<Token.Invalid> getInvalid()
    {
        List<Token.Invalid> invalids = new ArrayList<>();
        for (Token t : tokens)
        {
            if (t instanceof Token.Invalid inv)
            {
                invalids.add(inv);
            }
        }
        return invalids;
    }

    /**
     * A formatted string of all tokens as a neat table:
     * Columns: #, Type, Position, Value
     */
    @Override
    public String toString()
    {
        final String[] headers = {"#", "Type", "Position", "Value"};
        int[] widths = {2, 4, 10, 5};

        List<String[]> rows = new ArrayList<>();
        int i = 0;
        for (Token t : tokens)
        {
            i++;
            String idx = String.valueOf(i);
            String type = t.getClass().getSimpleName();
            Coordinates pos = t.position();
            String position = "(" + pos.line() + "," + pos.column() + ")";
            String val = t.getValue();

            String[] row = {idx, type, position, val};
            rows.add(row);

            widths[0] = Math.max(widths[0], idx.length());
            widths[1] = Math.max(widths[1], type.length());
            widths[2] = Math.max(widths[2], position.length());
            widths[3] = Math.max(widths[3], val.length());
        }

        String fmt = String.format("%%-%ds | %%-%ds | %%-%ds | %%-%ds\n",
                widths[0], widths[1], widths[2], widths[3]);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(fmt, headers[0], headers[1], headers[2], headers[3]));
        sb.append(buildSeparator(widths));
        for (String[] row : rows)
        {
            sb.append(String.format(fmt, row[0], row[1], row[2], row[3]));
        }

        return sb.toString();
    }

    private String buildSeparator(int[] widths)
    {
        StringBuilder sep = new StringBuilder();
        for (int i = 0; i < widths.length; i++)
        {
            if (i > 0) sep.append("-+-");
            sep.append("-".repeat(widths[i]));
        }
        sep.append("\n");
        return sep.toString();
    }

    public int size()
    {
        return tokens.size();
    }

    public Token get(int i)
    {
        return tokens.get(i);
    }

    // ==================================================
    // ================ HELPER STRUCTS ==================
    // ==================================================

    private enum InternalProtoType
    {
        KEYWORD,
        DELIMITER,
        OPERATOR,
        COMMENT,
        LITERAL,
        IDENTIFIER
    }

    private record BestMatch(int length, Token token, InternalProtoType type)
    {
    }

    private static class InternalProto
    {
        final InternalProtoType type;
        final String fixedString;   // for Keyword, Operator, Delimiter
        final String literalType;   // for Literal/Identifier
        final Pattern pattern;      // for Comment, Literal, Identifier

        private InternalProto(InternalProtoType type, String fixedString, String literalType, Pattern pattern)
        {
            this.type = type;
            this.fixedString = fixedString;
            this.literalType = literalType;
            this.pattern = pattern;
        }

        static InternalProto keyword(String word, boolean caseSensitive)
        {
            String val = caseSensitive ? word : word.toLowerCase(Locale.ROOT);
            return new InternalProto(InternalProtoType.KEYWORD, val, null, null);
        }

        static InternalProto delimiter(String val)
        {
            return new InternalProto(InternalProtoType.DELIMITER, val, null, null);
        }

        static InternalProto operator(String val)
        {
            return new InternalProto(InternalProtoType.OPERATOR, val, null, null);
        }

        static InternalProto comment(String userRegex)
        {
            String cleaned = cleanAnchors(userRegex);
            Pattern p = Pattern.compile("^(?:" + cleaned + ")");
            return new InternalProto(InternalProtoType.COMMENT, null, null, p);
        }

        static InternalProto literal(String type, String userRegex)
        {
            String cleaned = cleanAnchors(userRegex);
            Pattern p = Pattern.compile("^(?:" + cleaned + ")");
            return new InternalProto(InternalProtoType.LITERAL, null, type, p);
        }

        static InternalProto identifier(String type, String userRegex)
        {
            String cleaned = cleanAnchors(userRegex);
            Pattern p = Pattern.compile("^(?:" + cleaned + ")");
            return new InternalProto(InternalProtoType.IDENTIFIER, null, type, p);
        }

        private static String cleanAnchors(String regex)
        {
            String out = regex;
            if (out.startsWith("^"))
            {
                out = out.substring(1);
            }
            if (out.endsWith("$"))
            {
                out = out.substring(0, out.length() - 1);
            }
            return out;
        }

        private static boolean isAlpha(String s)
        {
            for (char c : s.toCharArray())
            {
                if (!Character.isLetter(c)) return false;
            }
            return true;
        }

        private static boolean isIdentifierChar(char c)
        {
            return Character.isLetterOrDigit(c) || c == '_';
        }

        MatchResult match(String input, int index, int line, int col, boolean caseSensitive)
        {
            if (index >= input.length()) return null;

            return switch (type)
            {
                case KEYWORD -> matchKeyword(input, index, line, col, caseSensitive);
                case DELIMITER, OPERATOR -> matchFixedString(input, index, line, col);
                case COMMENT, LITERAL, IDENTIFIER -> matchRegex(input, index, line, col);
            };
        }

        private MatchResult matchKeyword(String input, int index, int line, int col, boolean caseSensitive)
        {
            String kw = this.fixedString;
            int len = kw.length();

            if (index + len > input.length()) return null;
            String chunk = input.substring(index, index + len);
            if (!caseSensitive)
            {
                chunk = chunk.toLowerCase(Locale.ROOT);
            }
            if (!chunk.equals(kw)) return null;

            // If purely alphabetical, ensure boundary
            if (isAlpha(kw) && index + len < input.length())
            {
                char next = input.charAt(index + len);
                if (isIdentifierChar(next))
                {
                    return null; // e.g. "and" vs "andres"
                }
            }

            return new MatchResult(len,
                    new Token.Keyword(new Coordinates(line, col), input.substring(index, index + len)));
        }

        private MatchResult matchFixedString(String input, int index, int line, int col)
        {
            String val = this.fixedString;
            int len = val.length();
            if (index + len > input.length()) return null;

            String chunk = input.substring(index, index + len);
            if (!chunk.equals(val)) return null;

            // Boundary check if alphabetical
            if (isAlpha(val) && index + len < input.length())
            {
                char next = input.charAt(index + len);
                if (isIdentifierChar(next))
                {
                    return null;
                }
            }

            return switch (type)
            {
                case DELIMITER -> new MatchResult(len,
                        new Token.Delimiter(new Coordinates(line, col), val));
                case OPERATOR -> new MatchResult(len,
                        new Token.Operator(new Coordinates(line, col), val));
                default -> null; // not expected
            };
        }

        private MatchResult matchRegex(String input, int index, int line, int col)
        {
            Matcher m = pattern.matcher(input.substring(index));
            if (!m.find() || m.start() != 0) return null;

            String matchedText = m.group();
            return switch (type)
            {
                case COMMENT -> new MatchResult(matchedText.length(),
                        new Token.Comment(new Coordinates(line, col), matchedText));
                case LITERAL -> new MatchResult(matchedText.length(),
                        new Token.Literal(new Coordinates(line, col), literalType, matchedText));
                case IDENTIFIER -> new MatchResult(matchedText.length(),
                        new Token.Identifier(new Coordinates(line, col), literalType, matchedText));
                default -> null;
            };
        }
    }

    private record MatchResult(int length, Token token)
    {
    }

    /**
     * Private class that implements the entire scanning logic in a streaming fashion:
     * it yields tokens on-demand, one at a time, rather than building a list all at once.
     */
    private static class LazyTokenizer implements Iterator<Token>
    {
        private final TokenRules rules;
        private final TokenPostProcessor postProcessor;

        private final String[] lines;  // normalized input, split by \n
        private final int lineCount;
        // For indentation logic (Python-like), if needed:
        private final Deque<Integer> indentStack = new ArrayDeque<>();
        // A queue of tokens waiting to be consumed; we refill it as needed.
        private final Queue<Token> pending = new ArrayDeque<>();
        // We need the prototypes, sorted if longestMatchFirst is true
        private final List<InternalProto> protoList = new ArrayList<>();
        private int lineIndex = 0;     // which line we're on
        // If the grammar includes Start/End tokens
        private boolean hasStart;
        private boolean hasEnd;
        private boolean startEmitted = false;
        private boolean endEmitted = false;
        private Character indentationChar = null; // ' ' or '\t', or null if not known
        // Track current line number for Coordinates
        private int currentLineNumber = 1;

        public LazyTokenizer(String input, TokenRules rules, TokenPostProcessor postProcessor)
        {
            this.rules = rules;
            this.postProcessor = postProcessor;

            // 1) Normalize line endings
            input = input.replace("\r\n", "\n").replace("\r", "\n");

            // 2) Check for Start/End prototypes and build internal proto
            for (TokenPrototype proto : rules.tokenPrototypes)
            {
                switch (proto)
                {
                    case TokenPrototype.Keyword kw ->
                            protoList.add(InternalProto.keyword(kw.value(), rules.caseSensitive));
                    case TokenPrototype.Delimiter d -> protoList.add(InternalProto.delimiter(d.value()));
                    case TokenPrototype.Operator op -> protoList.add(InternalProto.operator(op.value()));
                    case TokenPrototype.Comment c -> protoList.add(InternalProto.comment(c.regex()));
                    case TokenPrototype.Literal lit -> protoList.add(InternalProto.literal(lit.type(), lit.regex()));
                    case TokenPrototype.Identifier id -> protoList.add(InternalProto.identifier(id.type(), id.regex()));
                    case TokenPrototype.Start s -> this.hasStart = true;
                    case TokenPrototype.End e -> this.hasEnd = true;
                    case TokenPrototype.NewLine n ->
                    {
                        // We'll handle newline tokens ourselves
                    }
                }
            }
            // For any prototypes not covered above:
            this.hasStart = rules.tokenPrototypes.stream().anyMatch(tp -> tp instanceof TokenPrototype.Start);
            this.hasEnd = rules.tokenPrototypes.stream().anyMatch(tp -> tp instanceof TokenPrototype.End);

            // 3) Sort if needed
            if (rules.longestMatchFirst)
            {
                protoList.sort(TokenList::compareInternalProto);
            }

            // 4) Split into lines
            this.lines = input.split("\n", -1);
            this.lineCount = lines.length;

            // 5) Initialize indentation stack if INDENTATION is used
            if (rules.whitespaceMode == WhitespaceMode.INDENTATION)
            {
                indentStack.push(0);
            }
        }

        @Override
        public boolean hasNext()
        {
            if (!pending.isEmpty())
            {
                return true;
            }
            if (!startEmitted && hasStart)
            {
                return true;
            }
            if (lineIndex < lineCount)
            {
                return true;
            }
            if (rules.whitespaceMode == WhitespaceMode.INDENTATION && canPopIndent())
            {
                return true;
            }
            return !endEmitted && hasEnd;
        }

        @Override
        public Token next()
        {
            if (!hasNext())
            {
                throw new NoSuchElementException("No more tokens.");
            }

            // 1) If we haven't emitted Start yet, do so now
            if (!startEmitted && hasStart)
            {
                startEmitted = true;
                return applyPostProcessing(new Token.Start(new Coordinates(0, 0)), postProcessor);
            }

            // 2) If pending tokens are available, return one
            if (!pending.isEmpty())
            {
                return applyPostProcessing(pending.poll(), postProcessor);
            }

            // 3) If out of lines but have indentation to pop
            if (rules.whitespaceMode == WhitespaceMode.INDENTATION && canPopIndent())
            {
                return applyPostProcessing(popIndent(), postProcessor);
            }

            // 4) If we’re out of lines, emit End if needed
            if (lineIndex >= lineCount)
            {
                endEmitted = true;
                return applyPostProcessing(
                        new Token.End(new Coordinates(currentLineNumber, 1)),
                        postProcessor
                );
            }

            // 5) Otherwise, tokenize the next line into pending
            tokenizeNextLine();
            // If we got some tokens queued, return the first
            if (!pending.isEmpty())
            {
                return applyPostProcessing(pending.poll(), postProcessor);
            }

            // Possibly the line was empty; recurse
            return next();
        }

        private void tokenizeNextLine()
        {
            String currentLine = lines[lineIndex];
            lineIndex++;
            int thisLineNumber = currentLineNumber;
            currentLineNumber++;

            // Possibly produce a NewLine token if not the very first line
            if ((lineIndex > 1) && rules.whitespaceMode != WhitespaceMode.IGNORE)
            {
                pending.add(new Token.NewLine(new Coordinates(thisLineNumber - 1, 9999)));
            }

            if (currentLine.isEmpty())
            {
                // Empty line => do nothing else
                return;
            }

            if (rules.whitespaceMode == WhitespaceMode.INDENTATION)
            {
                // Measure indentation
                handleIndentation(currentLine, thisLineNumber);
            }

            // Tokenize remainder
            if (rules.whitespaceMode == WhitespaceMode.INDENTATION)
            {
                int idx = 0;
                while (idx < currentLine.length() &&
                        (currentLine.charAt(idx) == ' ' || currentLine.charAt(idx) == '\t'))
                {
                    idx++;
                }
                if (idx < currentLine.length())
                {
                    tokenizeLineSegment(
                            currentLine.substring(idx),
                            thisLineNumber,
                            idx + 1,
                            protoList,
                            rules.caseSensitive,
                            rules.whitespaceMode,
                            pending::add
                    );
                }
            }
            else
            {
                tokenizeLineSegment(
                        currentLine,
                        thisLineNumber,
                        1,
                        protoList,
                        rules.caseSensitive,
                        rules.whitespaceMode,
                        pending::add
                );
            }
        }

        private boolean canPopIndent()
        {
            return !indentStack.isEmpty() && indentStack.peek() > 0;
        }

        private Token popIndent()
        {
            if (indentStack.isEmpty())
            {
                return null;
            }
            indentStack.pop();
            return new Token.IndentDecr(new Coordinates(currentLineNumber, 1));
        }

        private void handleIndentation(String lineContent, int lineNum)
        {
            int indentWidth = 0;
            boolean foundSpace = false;
            boolean foundTab = false;

            while (indentWidth < lineContent.length()
                    && (lineContent.charAt(indentWidth) == ' ' || lineContent.charAt(indentWidth) == '\t'))
            {
                if (lineContent.charAt(indentWidth) == ' ') foundSpace = true;
                if (lineContent.charAt(indentWidth) == '\t') foundTab = true;
                indentWidth++;
            }

            // Check mixing
            if (foundSpace && foundTab)
            {
                pending.add(new Token.Invalid(new Coordinates(lineNum, 1),
                        "Mixed tabs/spaces in indentation"));
            }
            else if (foundSpace || foundTab)
            {
                char usedChar = foundSpace ? ' ' : '\t';
                if (indentationChar == null)
                {
                    indentationChar = usedChar;
                }
                else if (!indentationChar.equals(usedChar))
                {
                    pending.add(new Token.Invalid(new Coordinates(lineNum, 1),
                            "Inconsistent indentation character"));
                }
            }

            int currentIndent = indentStack.peek();
            if (indentWidth > currentIndent)
            {
                indentStack.push(indentWidth);
                pending.add(new Token.IndentIncr(new Coordinates(lineNum, 1)));
            }
            else if (indentWidth < currentIndent)
            {
                while (!indentStack.isEmpty() && indentStack.peek() > indentWidth)
                {
                    indentStack.pop();
                    pending.add(new Token.IndentDecr(new Coordinates(lineNum, 1)));
                }
                if (!indentStack.isEmpty() && indentStack.peek() != indentWidth)
                {
                    pending.add(new Token.Invalid(new Coordinates(lineNum, 1),
                            "Inconsistent indentation level " + indentWidth));
                }
                if (indentStack.isEmpty())
                {
                    indentStack.push(0);
                }
            }
        }
    }

    /**
     * Special iterator that supports lookAhead without consuming tokens.
     */
    public class LookAheadIterator implements Iterator<Token>
    {
        private int currentIndex = 0;

        @Override
        public boolean hasNext()
        {
            return currentIndex < tokens.size();
        }

        @Override
        public Token next()
        {
            if (!hasNext()) throw new NoSuchElementException();
            return tokens.get(currentIndex++);
        }

        public Token lookAhead(int steps)
        {
            int idx = currentIndex + steps;
            if (idx < 0 || idx >= tokens.size())
            {
                return null;
            }
            return tokens.get(idx);
        }
    }
}
