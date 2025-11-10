package org.nest.ast;

import org.nest.ast.functional.ASTAction;
import org.nest.ast.functional.TokenAction;
import org.nest.ast.state.Definition;
import org.nest.ast.state.Rule;
import org.nest.ast.state.Step;
import org.nest.errors.ErrorManager;
import org.nest.tokenization.Coordinates;
import org.nest.tokenization.Token;
import org.nest.tokenization.TokenCursor;
import org.nest.tokenization.TokenList;

import java.util.*;
import java.util.function.Function;

public class ASTRules {

    // Maximum token preview length for error messages
    private static final int MAX_TOKEN_PREVIEW_LENGTH = 15;

    private final List<String> topRuleNames;
    private final Map<String, Rule> ruleTable;
    private final boolean ignoreComments;

    // The current compilation artefacts
    private TokenList tokenList;
    private TokenCursor cursor;
    private ErrorManager errorManager;

    public ASTRules(List<String> topRuleNames, Collection<Rule> rules, boolean ignoreComments) {
        this.topRuleNames = Objects.requireNonNull(topRuleNames);
        this.ruleTable = new HashMap<>();
        rules.forEach(r -> {
            if (r != null) this.ruleTable.put(r.getName(), r);
        });
        this.ignoreComments = ignoreComments;
    }

    public static ASTRulesBuilder builder() {
        return new ASTRulesBuilder();
    }

    /// Attempts to parse a single `Step` within a rule.
    /// A `Step` represents an atomic or composite grammar construct, including:
    /// - Terminals: `Keyword`, `Operator`, `Delimiter`
    /// - Typed tokens: `Literal`, `Identifier`
    /// - Rule references: `Rule`
    /// - Combinators: `Repeat`, `Optional`, `Choice`
    /// This method:
    /// - Checks whether the input tokens match the expectations of the given `Step`
    /// - Executes any attached semantic actions if the match succeeds
    /// - Returns a `ParseResponse` indicating match success or failure and number of tokens processed
    /// This function is used internally when parsing rule definitions.
    ///
    /// @param step The grammar step to match.
    /// @return A `ParseResponse` representing the match result and token consumption.
    ParseResponse parseStep(Step step) {
        cursor.markPosition();
        return switch (step) {
            case Step.Keyword(String value, _) -> {
                Token current = cursor.readNextToken();
                if (current instanceof Token.Keyword(_, String v) && v.equals(value)) {
                    cursor.commitPosition();
                    yield new Success(1, Optional.empty());
                }
                cursor.jumpBackToMark();
                yield new Failure(0, Optional.empty());
            }
            case Step.Literal(String type, _) -> {
                Token current = cursor.readNextToken();
                if (current instanceof Token.Literal(_, String t, _) && t.equals(type)) {
                    cursor.commitPosition();
                    yield new Success(1, Optional.empty());
                }
                cursor.jumpBackToMark();
                yield new Failure(0, Optional.empty());
            }
            case Step.Identifier(String type, _) -> {
                Token current = cursor.readNextToken();
                if (current instanceof Token.Identifier(_, String t, _) && t.equals(type)) {
                    cursor.commitPosition();
                    yield new Success(1, Optional.empty());
                }
                cursor.jumpBackToMark();
                yield new Failure(0, Optional.empty());
            }
            case Step.Operator(String value, _) -> {
                Token current = cursor.readNextToken();
                if (current instanceof Token.Operator(_, String v) && v.equals(value)) {
                    cursor.commitPosition();
                    yield new Success(1, Optional.empty());
                }
                cursor.jumpBackToMark();
                yield new Failure(0, Optional.empty());
            }
            case Step.Delimiter(String value, _) -> {
                Token current = cursor.readNextToken();
                if (current instanceof Token.Delimiter(_, String v) && v.equals(value)) {
                    cursor.commitPosition();
                    yield new Success(1, Optional.empty());
                }
                cursor.jumpBackToMark();
                yield new Failure(0, Optional.empty());
            }
            case Step.Rule(String name, _) -> {
                ParseResponse attempt = parseRule(ruleTable.get(name));
                if (!(attempt instanceof Success)) {
                    cursor.jumpBackToMark();
                    yield new Failure(0, Optional.empty());
                }
                // Speculatively advance by the number of tokens the rule would consume
                int n = attempt.numberOfTokensOfParsed();
                for (int i = 0; i < n; i++) {
                    if (cursor.readNextToken() == null)
                        break;
                }
                cursor.commitPosition();
                yield attempt;
            }
            case Step.Repeat(List<Step> children, _) -> {
                int totalConsumed = 0;

                while (true) {
                    int positionBeforeAttempt = cursor.getCurrentPosition();
                    cursor.markPosition();

                    boolean allStepsPassed = true;
                    int consumed = 0;
                    for (Step child : children) {
                        ParseResponse response = parseStep(child);
                        if (response instanceof Failure) {
                            allStepsPassed = false;
                            break;
                        }

                        consumed += response.numberOfTokensOfParsed();
                    }

                    if (!allStepsPassed || consumed == 0) {
                        // Roll back this attempt and stop repeating
                        cursor.jumpBackToMark();
                        break;
                    }

                    totalConsumed += consumed;
                    // Keep the progress of this iteration
                    cursor.commitPosition();

                    // Extra guard: if somehow no advancement happened, break to avoid infinite loop
                    if (cursor.getCurrentPosition() == positionBeforeAttempt)
                        break;
                }

                cursor.commitPosition();
                yield new Success(totalConsumed, Optional.empty());
            }
            case Step.Optional(List<Step> children, _) -> {
                cursor.markPosition();

                int consumed = 0;
                boolean allStepsPassed = true;

                for (Step child : children) {
                    ParseResponse response = parseStep(child);
                    if (response instanceof Failure) {
                        allStepsPassed = false;
                        break;
                    }

                    consumed += response.numberOfTokensOfParsed();
                }

                if (!allStepsPassed) {
                    cursor.jumpBackToMark(); // revert inner optional attempt
                    cursor.commitPosition();  // discard step-level mark
                    yield new Success(consumed, Optional.empty());
                }

                cursor.commitPosition();
                yield new Success(consumed, Optional.empty());
            }
            case Step.Choice(Set<List<Step>> alternatives) -> {
                for (List<Step> alternative : alternatives) {
                    cursor.markPosition();

                    int consumed = 0;
                    boolean allStepsPassed = true;

                    for (Step child : alternative) {
                        ParseResponse response = parseStep(child);
                        if (response instanceof Failure) {
                            allStepsPassed = false;
                            break;
                        }

                        consumed += response.numberOfTokensOfParsed();
                    }

                    if (allStepsPassed) {
                        // Clear alt mark as we're keeping this advancement
                        cursor.commitPosition();
                        // Also discard step-level mark for this step
                        cursor.commitPosition();
                        yield new Success(consumed, Optional.empty());
                    } else {
                        // Revert alt mark before trying next alternative
                        cursor.jumpBackToMark();
                    }
                }

                cursor.jumpBackToMark();
                yield new Failure(0, Optional.empty());
            }
        };
    }

    /// Attempts to parse a complete `Rule` from the current token stream position.
    /// Each rule may contain multiple definitions (alternative step sequences).
    /// This method evaluates all definitions and returns a `ParseResponse` representing the best result.
    /// - If a definition matches fully, the result is `Success`.
    /// - If none succeed, the most-progressing `Failure` is returned.
    /// Note: This method does not consume tokens; it only attempts the match.
    ///
    /// @param rule The rule to try parsing.
    /// @return A `ParseResponse` indicating the match outcome.
    ParseResponse parseRule(Rule rule) {
        if (rule == null) {
            return new Failure(0, Optional.empty());
        }

        cursor.markPosition();
        int initialPosition = cursor.getCurrentPosition();

        ParseResponse best = new Failure(0, Optional.empty());

        for (Definition def : rule.getDefinitions()) {
            cursor.markPosition();

            int consumed = 0;
            boolean allStepsPassed = true;

            for (Step step : def.steps) {
                ParseResponse r = parseStep(step);
                if (r instanceof Failure) {
                    allStepsPassed = false;
                    // progress up to the failure point for this definition
                    consumed += r.numberOfTokensOfParsed();
                    break;
                }

                consumed += r.numberOfTokensOfParsed();
            }

            // Do not consume input in this method — always roll back
            cursor.jumpBackToMark();

            ParseResponse candidate = allStepsPassed
                    ? new Success(consumed, Optional.of(def))
                    : new Failure(consumed, Optional.of(def));

            if (candidate.isBetterThan(best))
                best = candidate;
        }

        // Restore to the initial position for the caller
        cursor.jumpBackToMark();
        cursor.setPosition(initialPosition);

        return best;
    }

    /// Parses the body of a given `Definition` (i.e., a specific sequence of `Step` elements).
    /// This method is used internally once a rule has been selected and a specific definition has been chosen to match.
    /// If the definition matches fully, it returns the resulting AST node (built via the rule's `ASTNodeSupplier`).
    /// If the definition fails, `null` is returned.
    ///
    /// @param definition The rule definition to parse.
    /// @return The parsed AST node, or `null` if the definition could not be matched.
    Object parseRule(Definition definition) {
        if (definition == null) {
            return null;
        }

        ASTBuildContext ctx = new ASTBuildContext();
        Deque<Runnable> cleanups = new ArrayDeque<>();

        // Helper lambdas to execute steps (self-referential via 1-element array)
        final Function<Step, Boolean>[] execStep = new Function[]{null};
        execStep[0] = new Function<>() {
            @Override
            public Boolean apply(Step step) {
                return switch (step) {
                    case Step.Keyword(String value, TokenAction action) -> {
                        cursor.markPosition();
                        Token t = cursor.readNextToken();
                        if (t instanceof Token.Keyword(_, String v) && v.equals(value)) {
                            if (action != null) action.apply(ctx).accept(t);
                            cursor.commitPosition();
                            yield true;
                        }
                        cursor.jumpBackToMark();
                        yield false;
                    }
                    case Step.Operator(String value, TokenAction action) -> {
                        cursor.markPosition();
                        Token t = cursor.readNextToken();
                        if (t instanceof Token.Operator(_, String v) && v.equals(value)) {
                            if (action != null) action.apply(ctx).accept(t);
                            cursor.commitPosition();
                            yield true;
                        }
                        cursor.jumpBackToMark();
                        yield false;
                    }
                    case Step.Delimiter(String value, TokenAction action) -> {
                        cursor.markPosition();
                        Token t = cursor.readNextToken();
                        if (t instanceof Token.Delimiter(_, String v) && v.equals(value)) {
                            if (action != null) action.apply(ctx).accept(t);
                            cursor.commitPosition();
                            yield true;
                        }
                        cursor.jumpBackToMark();
                        yield false;
                    }
                    case Step.Literal(String type, TokenAction action) -> {
                        cursor.markPosition();
                        Token t = cursor.readNextToken();
                        if (t instanceof Token.Literal(_, String tpe, _) && tpe.equals(type)) {
                            if (action != null) action.apply(ctx).accept(t);
                            cursor.commitPosition();
                            yield true;
                        }
                        cursor.jumpBackToMark();
                        yield false;
                    }
                    case Step.Identifier(String type, TokenAction action) -> {
                        cursor.markPosition();
                        Token t = cursor.readNextToken();
                        if (t instanceof Token.Identifier(_, String tpe, _) && tpe.equals(type)) {
                            if (action != null) action.apply(ctx).accept(t);
                            cursor.commitPosition();
                            yield true;
                        }
                        cursor.jumpBackToMark();
                        yield false;
                    }
                    case Step.Rule(String name, org.nest.ast.functional.ASTNodeConsumer consumer) -> {
                        Rule ref = ruleTable.get(name);
                        ParseResponse attempt = parseRule(ref);
                        if (!(attempt instanceof Success)) {
                            yield false;
                        }

                        Definition chosen = attempt.definition().orElse(null);
                        Object child = parseRule(chosen);
                        if (child == null) {
                            yield false;
                        }
                        if (consumer != null) consumer.apply(ctx).accept(child);
                        yield true;
                    }
                    case Step.Repeat(List<Step> children, ASTAction initializer) -> {
                        Runnable cleanup = initializer != null ? initializer.apply(ctx) : null;
                        if (cleanup != null) cleanups.push(cleanup);

                        while (true) {
                            cursor.markPosition();
                            int before = cursor.getCurrentPosition();
                            boolean allPassed = true;
                            int consumed = 0;
                            for (Step ch : children) {
                                ParseResponse r = parseStep(ch);
                                if (r instanceof Failure) {
                                    allPassed = false;
                                    break;
                                }

                                consumed += r.numberOfTokensOfParsed();
                            }

                            if (!allPassed || consumed == 0) {
                                // Nothing matched — rollback and stop repeating
                                cursor.jumpBackToMark();
                                break;
                            }

                            // Re-execute with actions
                            cursor.jumpBackToMark();
                            for (Step ch : children) {
                                if (!execStep[0].apply(ch))
                                    // If execution fails (shouldn't), stop repeating
                                    break;
                            }
                        }
                        yield true;
                    }
                    case Step.Optional(List<Step> children, ASTAction fallback) -> {
                        cursor.markPosition();
                        boolean allPassed = true;
                        int consumed = 0;
                        for (Step ch : children) {
                            ParseResponse r = parseStep(ch);
                            if (r instanceof Failure) {
                                allPassed = false;
                                break;
                            }

                            consumed += r.numberOfTokensOfParsed();
                        }

                        cursor.jumpBackToMark();
                        if (allPassed && consumed > 0) {
                            for (Step ch : children)
                                if (!execStep[0].apply(ch))
                                    yield false;
                            yield true;
                        } else {
                            // execute fallback
                            Runnable c = (fallback != null ? fallback.apply(ctx) : null);
                            if (c != null) cleanups.push(c);
                            yield true; // Optional never fails
                        }
                    }
                    case Step.Choice(Set<List<Step>> alternatives) -> {
                        // Try each alternative — choose the first that matches
                        List<Step> chosen = null;
                        for (List<Step> alt : alternatives) {
                            cursor.markPosition();
                            boolean allPassed = true;
                            int consumed = 0;
                            for (Step ch : alt) {
                                ParseResponse r = parseStep(ch);
                                if (r instanceof Failure) {
                                    allPassed = false;
                                    break;
                                }

                                consumed += r.numberOfTokensOfParsed();
                            }
                            cursor.jumpBackToMark();
                            if (allPassed && consumed > 0) {
                                chosen = alt;
                                break;
                            }
                        }

                        if (chosen == null) {
                            yield false;
                        }

                        for (Step ch : chosen)
                            if (!execStep[0].apply(ch))
                                yield false;
                        yield true;
                    }
                };
            }
        };

        for (Step step : definition.steps) {
            if (!execStep[0].apply(step)) {
                // On unexpected failure during execution, stop and return null
                // (Errors are handled by higher-level caller on mismatch.)
                // Clean up any pending initializers
                while (!cleanups.isEmpty())
                    try {
                        cleanups.pop().run();
                    } catch (Exception ignored) {
                    }
                return null;
            }
        }

        try {
            return definition.builder != null ? definition.builder.apply(ctx).get() : null;
        } finally {
            // Run cleanups in LIFO order
            while (!cleanups.isEmpty())
                try {
                    cleanups.pop().run();
                } catch (Exception ignored) {
                }
        }
    }

    /// Builds a detailed error message to help explain why parsing failed.
    /// The message includes:
    /// - The unexpected token value and its type
    /// - A list of all expected tokens at the failure point
    /// - Per-rule expectations to give context about what was likely being parsed
    /// This message is used by the `ErrorManager` to produce developer-friendly feedback.
    ///
    /// @param tokenValue           The actual token value encountered.
    /// @param tokenType            The type of the encountered token.
    /// @param allExpectedTokens    A list of all tokens that were expected.
    /// @param ruleToExpectedTokens A mapping from rule names to the tokens they were expecting.
    /// @return A formatted, multi-line error message.
    String buildDetailedErrorMessage(String tokenValue, String tokenType,
                                     List<String> allExpectedTokens,
                                     Map<String, List<String>> ruleToExpectedTokens) {
        String preview = tokenValue == null ? "<eof>" : tokenValue;
        if (preview.length() > MAX_TOKEN_PREVIEW_LENGTH)
            preview = preview.substring(0, MAX_TOKEN_PREVIEW_LENGTH) + "...";

        StringBuilder sb = new StringBuilder();
        sb.append("Unexpected token '").append(preview).append("'");
        if (tokenType != null && !tokenType.isBlank())
            sb.append(" (type ").append(tokenType).append(")");
        sb.append('.').append('\n');

        if (allExpectedTokens != null && !allExpectedTokens.isEmpty()) {
            List<String> uniq = new ArrayList<>(new LinkedHashSet<>(allExpectedTokens));
            sb.append("Expected one of: ");
            for (int i = 0; i < uniq.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(uniq.get(i));
            }
            sb.append('\n');
        }

        if (ruleToExpectedTokens != null && !ruleToExpectedTokens.isEmpty()) {
            sb.append("\nWhere each rule expects:\n");
            for (Map.Entry<String, List<String>> e : ruleToExpectedTokens.entrySet()) {
                sb.append("  - ").append(e.getKey()).append(": ");
                List<String> vals = e.getValue() == null ? List.of() : new ArrayList<>(new LinkedHashSet<>(e.getValue()));
                for (int i = 0; i < vals.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(vals.get(i));
                }
                sb.append('\n');
            }
        }

        return sb.toString();
    }

    private String tokenTypeString(Token t) {
        if (t == null) return "<eof>";
        return switch (t) {
            case Token.Keyword _ -> "keyword";
            case Token.Operator _ -> "operator";
            case Token.Delimiter _ -> "delimiter";
            case Token.Comment _ -> "comment";
            case Token.NewLine _ -> "newline";
            case Token.IndentIncr _ -> "indent+";
            case Token.IndentDecr _ -> "indent-";
            case Token.Start _ -> "start";
            case Token.End _ -> "end";
            case Token.Invalid _ -> "invalid";
            case Token.Literal(_, String type, _) -> "literal:" + type;
            case Token.Identifier(_, String type, _) -> "identifier:" + type;
        };
    }

    private Set<String> expectedTokensForRule(Rule rule) {
        return expectedTokensForRule(rule, new HashSet<>());
    }

    private Set<String> expectedTokensForRule(Rule rule, Set<String> visited) {
        if (rule == null) return Set.of();
        if (!visited.add(rule.getName())) return Set.of(); // prevent cycles

        Set<String> out = new LinkedHashSet<>();
        for (Definition d : rule.getDefinitions())
            out.addAll(expectedTokensFromSteps(d.steps, visited));
        return out;
    }

    // =============================
    // ===== Helper utilities ======
    // =============================

    private Set<String> expectedTokensFromSteps(List<Step> steps, Set<String> visited) {
        Set<String> out = new LinkedHashSet<>();
        for (int i = 0; i < steps.size(); i++) {
            Step s = steps.get(i);
            Set<String> part = expectedTokensForStep(s, visited);
            out.addAll(part);

            // If the step can legally match zero tokens (Optional/Repeat), also include
            // expectations from the next step to reflect what can come next.
            if ((s instanceof Step.Optional) || (s instanceof Step.Repeat)) {
                out.addAll(expectedTokensFromSteps(steps.subList(i + 1, steps.size()), visited));
            }

            // For most steps, the first non-zero-optional step determines the initial expectations
            if (!(s instanceof Step.Optional) && !(s instanceof Step.Repeat))
                break;
        }
        return out;
    }

    private Set<String> expectedTokensForStep(Step step, Set<String> visited) {
        return switch (step) {
            case Step.Keyword(String value, _) -> Set.of("'" + value + "'");
            case Step.Operator(String value, _) -> Set.of("operator '" + value + "'");
            case Step.Delimiter(String value, _) -> Set.of("delimiter '" + value + "'");
            case Step.Literal(String type, _) -> Set.of("literal:" + type);
            case Step.Identifier(String type, _) -> Set.of("identifier:" + type);
            case Step.Rule(String name, _) -> expectedTokensForRule(ruleTable.get(name), visited);
            case Step.Repeat(java.util.List<Step> children, _) -> expectedTokensFromSteps(children, visited);
            case Step.Optional(java.util.List<Step> children, _) -> expectedTokensFromSteps(children, visited);
            case Step.Choice(java.util.Set<java.util.List<Step>> alternatives) -> {
                Set<String> out = new LinkedHashSet<>();
                for (java.util.List<Step> alt : alternatives)
                    out.addAll(expectedTokensFromSteps(alt, visited));
                yield out;
            }
        };
    }

    /// Constructs an Abstract Syntax Tree (AST) from a stream of tokens using the top-level parsing rules.
    /// Parsing Strategy:
    /// 1. Each top-level rule name (from `topRules`) is resolved to its corresponding `Rule` object.
    /// 2. For each rule, `parseRule(Rule)` is called to try parsing the token stream from the current position.
    ///    - A `Success` is preferred over a `Failure`.
    ///    - Among results of the same type, the one that consumes more tokens is considered better.
    /// 3. If a rule is successfully parsed, its tokens are consumed and the corresponding AST node is built using the rule's `ASTNodeSupplier`.
    /// 4. This process repeats until the entire token stream is consumed or EOF is reached.
    ///
    /// Step Matching:
    /// - `Keyword`, `Operator`, `Delimiter`: Must exactly match a fixed token value.
    /// - `Literal`, `Identifier`: Match a specific token type and can trigger a `TokenAction`.
    /// - `Rule`: Parses another named rule and passes the result to an `ASTNodeConsumer`.
    /// - `Repeat`: Matches the internal steps as many times as possible (greedy), collecting multiple results.
    /// - `Optional`: Tries once; if it fails, the parse still succeeds and a fallback action is triggered.
    /// - `Choice`: Attempts multiple alternatives in order and uses the first successful match (like a mini rule).
    /// Rule Completion:
    /// After all steps in a definition are matched, the rule's `ASTNodeSupplier` is invoked with the current `ASTBuildContext`
    /// to produce the resulting AST node.
    /// Failure Handling:
    /// Even if `parseRule(Definition)` returns `null`, an error has already been reported via the `ErrorManager`,
    /// and a placeholder or partial structure will still be recorded in the AST. This guarantees that something is always added to the AST
    /// for every top-level rule attempt, allowing the rest of the pipeline (e.g., type checking or code generation) to proceed without interruption.
    /// Any parse errors are logged through the `ErrorManager`, including contextual info about expected tokens and rule progress.
    ///
    /// @param tokens The list of input tokens to parse.
    /// @param errors The error manager for logging parse failures and diagnostics.
    /// @return The resulting `ASTWrapper` representing the full parsed tree.
    public ASTWrapper createAST(TokenList tokens, ErrorManager errors) {
        this.tokenList = Objects.requireNonNull(tokens);
        this.errorManager = Objects.requireNonNull(errors);
        this.cursor = TokenCursor.wrap(tokenList, ignoreComments);

        List<Object> astNodes = new ArrayList<>();

        int iteration = 0;

        while (!cursor.isAtEnd()) {
            iteration++;
            int positionBefore = cursor.getCurrentPosition();
            Token currentToken = cursor.peek();

            ParseResponse response = new Failure(0, Optional.empty());

            for (String ruleName : topRuleNames) {
                ParseResponse result = parseRule(ruleTable.get(ruleName));
                if (result.isBetterThan(response))
                    response = result;
            }

            if (response instanceof Success) {
                // Execute the chosen definition to build the AST node and advance the cursor
                Object node = parseRule(response.definition().orElse(null));
                astNodes.add(node);
                int positionAfter = cursor.getCurrentPosition();
            } else {
                // Failure: produce a helpful error, consume one token to make progress
                int failOffset = response.numberOfTokensOfParsed();
                Token offending = cursor.peek(Math.max(0, failOffset));

                // Treat End token as EOF for error reporting
                boolean isEndToken = offending instanceof Token.End;
                String tokenVal = (offending != null && !isEndToken) ? offending.getValue() : "<eof>";
                String tokenTyp = tokenTypeString(offending);

                // Aggregate expectations per top-level rule
                Map<String, List<String>> perRule = new LinkedHashMap<>();
                Set<String> all = new LinkedHashSet<>();
                for (String ruleName : topRuleNames) {
                    Rule r = ruleTable.get(ruleName);
                    List<String> vals = new ArrayList<>(expectedTokensForRule(r));
                    perRule.put(ruleName, vals);
                    all.addAll(vals);
                }

                String message = buildDetailedErrorMessage(tokenVal, tokenTyp, new ArrayList<>(all), perRule);

                // Use the offending token position if available
                // For EOF, use the position after the last real token
                Coordinates pos;
                if (offending != null && !isEndToken) {
                    pos = offending.position();
                } else {
                    // EOF: try to get position from the last token before EOF
                    Token lastToken = cursor.peek(-1);
                    if (lastToken != null && !(lastToken instanceof Token.End)) {
                        Coordinates lastPos = lastToken.position();
                        // Position after the last token
                        pos = new Coordinates(lastPos.line(), lastPos.column() + lastToken.getValue().length());
                    } else {
                        // Fallback: use line count from source
                        int lastLine = tokenList.size() > 0 ? tokenList.get(tokenList.size() - 1).position().line() : 1;
                        pos = new Coordinates(lastLine, 1);
                    }
                }

                String hint = response.definition().map(d -> d.hint).orElse("");
                errorManager.error(message, pos.line(), pos.column(), tokenVal, hint);

                // Consume one token to avoid infinite loop and continue
                Token consumed = cursor.readNextToken();
                astNodes.add(null);
            }

            // Safety check for infinite loop
            int positionAfterIteration = cursor.getCurrentPosition();
            if (positionAfterIteration == positionBefore)
                break;
        }

        return new ASTWrapper(astNodes, errorManager);
    }

    /// Represents the result of parsing: either a Success or a Failure.
    /// - `Success` indicates the step matched and tokens were consumed.
    /// - `Failure` indicates the step did not match, but progress may still be reported (e.g., for error recovery).
    sealed interface ParseResponse permits Success, Failure {
        default boolean isBetterThan(ParseResponse other) {
            // Prefer any Success over any Failure
            if (this instanceof Success && other instanceof Failure)
                return true;
            if (this instanceof Failure && other instanceof Success)
                return false;

            // Between two Successes, prefer the one that consumed more tokens
            if (this instanceof Success sThis && other instanceof Success sOther)
                return sThis.numberOfTokensOfParsed() > sOther.numberOfTokensOfParsed();

            // Between two Failures, prefer the one that progressed further before failing
            if (this instanceof Failure fThis && other instanceof Failure fOther)
                return fThis.numberOfTokensOfParsedUntilFailure() >= fOther.numberOfTokensOfParsedUntilFailure();

            return false;
        }

        default int numberOfTokensOfParsed() {
            return switch (this) {
                case Success(int numberOfTokensOfParsed, _) -> numberOfTokensOfParsed;
                case Failure(int numberOfTokensOfParsedUntilFailure, _) -> numberOfTokensOfParsedUntilFailure;
            };
        }

        default Optional<Definition> definition() {
            return switch (this) {
                case Success(_, Optional<Definition> definition) -> definition;
                case Failure(_, Optional<Definition> definition) -> definition;
            };
        }
    }

    record Success(int numberOfTokensOfParsed, Optional<Definition> definition) implements ParseResponse {
    }

    record Failure(int numberOfTokensOfParsedUntilFailure, Optional<Definition> definition) implements ParseResponse {
    }
}
