package org.nest.ast;

import org.nest.ast.functional.ASTAction;
import org.nest.ast.functional.TokenAction;
import org.nest.ast.state.Definition;
import org.nest.ast.state.Rule;
import org.nest.ast.state.Step;
import org.nest.errors.ErrorManager;
import org.nest.errors.SimpleCompilerError;
import org.nest.tokenization.Coordinates;
import org.nest.tokenization.Token;
import org.nest.tokenization.TokenCursor;
import org.nest.tokenization.TokenList;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;


public class ASTRules
{
    // Maximum token preview length for error messages
    private static final int MAX_TOKEN_PREVIEW_LENGTH = 15;
    // Stack of contexts for nested rule processing
    private final Deque<ASTBuildContext> contextStack = new ArrayDeque<>();
    // Cache of rule trees for faster matching
    private final Map<String, RuleTree> ruleTrees = new HashMap<>();
    // Track the current parsing path for better error reporting
    private final Deque<String> parsingPathStack = new ArrayDeque<>();
    List<String> topRules;
    List<Rule> rules;
    boolean ignoreComments;
    TokenList tokenList;

    public ASTRules(List<String> topRules, List<Rule> rules, boolean ignoreComments)
    {
        this.topRules = topRules;
        this.rules = rules;
        this.ignoreComments = ignoreComments;
        this.tokenList = null;

        // Build rule trees for all rules
        buildRuleTrees();
    }

    public static ASTRulesBuilder builder()
    {
        return new ASTRulesBuilder();
    }

    /// Builds a tree structure for each rule to optimize rule matching.
    /// This pre-processes the rules once at initialization time to avoid
    /// repeatedly examining the same definitions during parsing.
    private void buildRuleTrees()
    {
        // Create a map for fast rule lookup
        Map<String, Rule> ruleMap = new HashMap<>();
        for (Rule rule : rules)
            ruleMap.put(rule.getName(), rule);

        // Build a tree for each rule
        for (Rule rule : rules)
        {
            RuleTree tree = new RuleTree(rule);
            ruleTrees.put(rule.getName(), tree);
        }
    }

    /// Creates an Abstract Syntax Tree (AST) from a token list.
    ///
    /// @param tokens the token list to parse
    /// @param errors the error manager to report errors to
    /// @return the root of the AST
    public ASTWrapper createAST(TokenList tokens, ErrorManager errors)
    {
        // 1. Wrap the TokenList in a lookahead-capable cursor
        TokenCursor cursor = new TokenCursor(tokens, ignoreComments);

        // 2. Build a Map<String, Rule> from your List<Rule> for fast lookup
        Map<String, Rule> ruleMap = new HashMap<>();
        for (Rule rule : rules)
            ruleMap.put(rule.getName(), rule);

        // 3. Initialize the root node collection
        List<Object> rootNodes = new ArrayList<>();

        // 4. Process tokens until the end of the stream
        while (!cursor.isAtEnd())
        {
            boolean matchedAnyRule = false;

            // Collect all error information across all top rules
            List<String> allExpectedTokens = new ArrayList<>();
            Map<String, List<String>> ruleToExpectedTokens = new HashMap<>();
            int bestTokensConsumed = 0;
            MatchResult bestErrorMatch = null;

            // Clear the parsing path stack at the beginning of each top-level rule attempt
            parsingPathStack.clear();

            // Try to match any of the top-level rules
            for (String topRuleName : topRules)
            {
                Rule rule = ruleMap.get(topRuleName);

                if (rule == null)
                {
                    errors.error("Undefined top rule: " + topRuleName, 0, 0, "",
                            "Check the grammar definition to ensure this rule is properly defined.");
                    continue;
                }

                // Push the current rule onto the parsing path stack
                parsingPathStack.push(topRuleName);

                // Save the current position for backtracking
                int savePos = cursor.savePosition();

                // Try to match this rule using the optimized rule tree
                RuleTree ruleTree = ruleTrees.get(topRuleName);
                MatchResult result = ruleTree != null
                        ? matchRuleUsingTree(cursor, ruleTree, ruleMap, errors)
                        : matchRule(cursor, rule, ruleMap, errors);

                if (result != null && result.matched)
                {
                    // Successfully matched this rule, process the AST
                    if (result.definition != null && result.definition.builder != null)
                    {
                        // Create context for the builder
                        ASTBuildContext builderContext = new ASTBuildContext();

                        // Build an empty context to be populated while creating the AST
                        for (Map.Entry<String, Object> entry : result.localData.entrySet())
                            builderContext.put(entry.getKey(), entry.getValue());

                        // Add the node to the list
                        Supplier<Object> nodeBuilder = result.definition.builder.apply(builderContext);
                        Object node = nodeBuilder.get();
                        if (node != null)
                            rootNodes.add(node);

                        // No need to commit position again, it was already committed in matchRule

                        // We've successfully matched and processed a rule, break out of the rule loop
                        matchedAnyRule = true;
                        parsingPathStack.pop(); // Pop the current rule from the stack
                        break;
                    }
                    else // Reset to the start position if this rule didn't match
                        cursor.backtrack();
                }
                else if (result != null && !result.matched)
                {
                    // This rule didn't match, but collect error information
                    if (result.expectedTokens != null && !result.expectedTokens.isEmpty())
                    {
                        allExpectedTokens.addAll(result.expectedTokens);
                        ruleToExpectedTokens.put(topRuleName, new ArrayList<>(result.expectedTokens));
                    }

                    // Track the best partial match
                    int tokensConsumed = result.consumedTokens != null ? result.consumedTokens.size() : 0;
                    if (tokensConsumed > bestTokensConsumed)
                    {
                        bestTokensConsumed = tokensConsumed;
                        bestErrorMatch = result;
                    }

                    // Reset position
                    cursor.backtrack();
                }
                else // Reset position
                    cursor.backtrack();

                // Pop the current rule from the stack
                parsingPathStack.pop();
            }

            // If none of the top-level rules matched, report an error and skip this token
            if (!matchedAnyRule)
            {
                Token currentToken = cursor.peek();
                String tokenValue = currentToken != null ? currentToken.getValue() : "END_OF_FILE";
                String tokenType = getTokenTypeName(currentToken);

                int line = 0;
                int column = 0;

                // Check if we have a better position from a partial match
                if (bestTokensConsumed > 0 && bestErrorMatch != null &&
                        bestErrorMatch.consumedTokens != null && !bestErrorMatch.consumedTokens.isEmpty())
                {
                    // Use the position of the last consumed token plus the current token
                    Token lastToken = bestErrorMatch.consumedTokens.get(bestErrorMatch.consumedTokens.size() - 1);
                    if (lastToken != null && lastToken.position() != null)
                    {
                        line = lastToken.position().line();
                        column = lastToken.position().column();
                        // Use the current token value but point to where the error actually occurred
                        tokenValue = lastToken.getValue();
                    }
                }
                else if (currentToken != null && currentToken.position() != null)
                {
                    // Fallback to current token if no better position is available
                    line = currentToken.position().line();
                    column = currentToken.position().column();
                }

                // Generate a more helpful error message with expected tokens
                String errorMessage = buildDetailedErrorMessage(tokenValue, tokenType, allExpectedTokens, ruleToExpectedTokens);

                // Generate a helpful hint based on the context
                String hint = generateHint(currentToken, allExpectedTokens, bestTokensConsumed);

                errors.error(
                        errorMessage,
                        line, column, tokenValue, hint);

                // Skip this token and continue
                cursor.consume();

                // Commit this position
                cursor.commitPosition();
            }
        }

        // Create the root ASTWrapper (e.g., named "Program") to collect top-level nodes
        ASTWrapper root = new ASTWrapper(rootNodes);

        return root;
    }

    /// Builds a detailed error message with context about what was expected
    private String buildDetailedErrorMessage(String tokenValue, String tokenType,
                                             List<String> allExpectedTokens,
                                             Map<String, List<String>> ruleToExpectedTokens)
    {
        StringBuilder message = new StringBuilder();

        // Special case for end of file errors
        if (tokenType.equals("end of file"))
        {
            message.append("Unexpected end of file");
            if (!allExpectedTokens.isEmpty())
            {
                message.append(". Expected ");
                if (allExpectedTokens.size() == 1)
                    message.append(allExpectedTokens.get(0));
                else
                {
                    message.append("one of: ");
                    message.append(String.join(", ", new ArrayList<>(new LinkedHashSet<>(allExpectedTokens))));
                }
            }
        }
        // Special case for delimiters that match what's expected (usually nested delimiters)
        else if (tokenType.equals("delimiter") &&
                allExpectedTokens.stream().anyMatch(e -> e.contains("delimiter") && e.contains("'" + tokenValue + "'")))
        {
            // This is the case when we find a closing delimiter but it's for a different level
            // For example, finding a closing parenthesis for an inner expression when we needed one for an outer expression
            message.append("Missing closing delimiter. Found ").append(tokenType).append(": '")
                    .append(truncateTokenForDisplay(tokenValue)).append("'");

            // Since this is likely a nested delimiter issue, make it clearer
            message.append(" which closes an inner expression, but outer expression is not closed");
        }
        // Default case
        else
        {
            // Start with the basic unexpected token message
            message.append("Unexpected ").append(tokenType).append(": '")
                    .append(truncateTokenForDisplay(tokenValue)).append("'");

            // Add expected tokens if available
            if (!allExpectedTokens.isEmpty())
            {
                // Sort and deduplicate expected tokens for better readability
                List<String> uniqueExpected = new ArrayList<>(new LinkedHashSet<>(allExpectedTokens));

                message.append(". Expected ");
                if (uniqueExpected.size() == 1)
                    message.append(uniqueExpected.get(0));
                else
                {
                    message.append("one of: ");
                    message.append(String.join(", ", uniqueExpected));
                }
            }
        }

        // Add rule-specific expectations for more context
        if (ruleToExpectedTokens.size() > 0 && ruleToExpectedTokens.size() <= 3)
        {
            message.append("\n\nIn context of rules:");
            for (Map.Entry<String, List<String>> entry : ruleToExpectedTokens.entrySet())
                if (!entry.getValue().isEmpty())
                    message.append("\n  • ").append(entry.getKey()).append(": expected ")
                            .append(String.join(", ", entry.getValue()));
        }

        return message.toString();
    }

    /// Generates a helpful hint based on the error context
    private String generateHint(Token token, List<String> expectedTokens, int bestTokensConsumed)
    {
        if (token == null || token instanceof Token.End)
            return "Unexpected end of file. The syntax may be incomplete.";

        // If we have some context about what was partially matched
        if (bestTokensConsumed > 0)
        {
            // For delimiter errors, give more specific guidance
            if (expectedTokens.stream().noneMatch(s -> s.contains("delimiter")))
                return "A partial match was found. Check for a syntax error near this token.";
            if (expectedTokens.stream().anyMatch(s -> s.contains("')'")))
                return "Missing closing parenthesis ')'. Check that all opening parentheses have matching closing ones.";
            if (expectedTokens.stream().anyMatch(s -> s.contains("'}'")))
                return "Missing closing brace '}'. Check that all opening braces have matching closing ones.";
            if (expectedTokens.stream().anyMatch(s -> s.contains("']'")))
                return "Missing closing bracket ']'. Check that all opening brackets have matching closing ones.";
            return "Missing closing delimiter. Check for unbalanced delimiters in your code.";

        }

        // If we expected a delimiter
        if (expectedTokens.stream().anyMatch(s -> s.contains("delimiter")))
        {
            if (expectedTokens.stream().anyMatch(s -> s.contains("'('")))
                return "Opening parenthesis '(' may be missing.";
            if (expectedTokens.stream().anyMatch(s -> s.contains("')'")))
                return "Closing parenthesis ')' may be missing.";
            if (expectedTokens.stream().anyMatch(s -> s.contains("'{'")))
                return "Opening brace '{' may be missing.";
            if (expectedTokens.stream().anyMatch(s -> s.contains("'}'")))
                return "Closing brace '}' may be missing.";
            if (expectedTokens.stream().anyMatch(s -> s.contains("'['")))
                return "Opening bracket '[' may be missing.";
            if (expectedTokens.stream().anyMatch(s -> s.contains("']'")))
                return "Closing bracket ']' may be missing.";
            return "Check for missing delimiters or punctuation.";
        }

        // Token-specific hints
        if (token instanceof Token.Identifier)
            return "This identifier appears in an unexpected context. Check for typos or misplaced tokens.";

        if (token instanceof Token.Keyword)
            return "This keyword is used in an unexpected context. Check the correct syntax for using this keyword.";

        if (token instanceof Token.Operator)
            return "This operator appears in an invalid position. Check the syntax or for missing operands.";

        return "Check for syntax errors or unexpected tokens near this location.";
    }

    /// Get a user-friendly name for the token type
    private String getTokenTypeName(Token token)
    {
        return switch (token)
        {
            case null -> "end of file";
            case Token.Keyword keyword -> "keyword";
            case Token.Literal literal -> "literal";
            case Token.Identifier identifier -> "identifier";
            case Token.Operator operator -> "operator";
            case Token.Delimiter delimiter -> "delimiter";
            case Token.Comment comment -> "comment";
            default -> "token";
        };
    }

    /// Truncates long token values for display in error messages
    private String truncateTokenForDisplay(String tokenValue)
    {
        if (tokenValue == null)
            return "";

        if (tokenValue.length() <= MAX_TOKEN_PREVIEW_LENGTH)
            return tokenValue;

        return tokenValue.substring(0, MAX_TOKEN_PREVIEW_LENGTH - 3) + "...";
    }

    /// Helper method to create SimpleCompilerError with the correct parameters
    private SimpleCompilerError createError(String message, Coordinates coords)
    {
        if (coords == null)
            return new SimpleCompilerError(message, 0, 0, "", "");
        return new SimpleCompilerError(message, coords.line(), coords.column(), "", "");
    }

    /// Attempts to match a rule using the pre-built rule tree.
    /// This leverages the cached tree structure for more efficient rule matching.
    ///
    /// @param cursor   TokenCursor for token management
    /// @param ruleTree The pre-built rule tree for the rule
    /// @param ruleMap  Map of rule names to rules for subrule lookups
    /// @param errors   ErrorManager for reporting errors
    /// @return A MatchResult containing match details, or null if no match
    private MatchResult matchRuleUsingTree(TokenCursor cursor, RuleTree ruleTree, Map<String, Rule> ruleMap, ErrorManager errors)
    {
        // Track the best match
        MatchResult bestMatch = null;
        int bestTokensConsumed = 0;

        // Track the best error match for better error reporting
        MatchResult bestErrorMatch = null;
        int bestErrorTokensConsumed = 0;
        List<String> allExpectedTokens = new ArrayList<>();

        // Get the current token
        Token token = cursor.peek();
        if (token == null)
            return null;

        // Find all matching path branches in the tree for the current token
        List<RuleTreeNode> potentialMatches = ruleTree.findPotentialMatches(token);

        // Try each potential matching path
        for (RuleTreeNode matchNode : potentialMatches)
        {
            Definition definition = matchNode.definition();

            // Mark the cursor position for backtracking
            int startPosition = cursor.savePosition();

            // Add the current definition to the parsing path for better error messages
            if (definition.name != null && !definition.name.isEmpty())
                parsingPathStack.push(ruleTree.getRule().getName() + ":" + definition.name);
            else
                parsingPathStack.push(ruleTree.getRule().getName() + ":variant" + potentialMatches.indexOf(matchNode));

            // Try to match the definition
            MatchResult result = matchDefinition(cursor, definition, ruleMap, errors);

            // Remove the definition from the parsing path
            parsingPathStack.pop();

            if (result.matched)
            {
                int tokensConsumed = result.consumedTokens.size();

                // Keep track of the definition that consumes the most tokens (longest match)
                if (tokensConsumed > bestTokensConsumed)
                {
                    bestMatch = result;
                    bestTokensConsumed = tokensConsumed;
                }
            }
            else
            {
                // Even if it didn't match, track the best error information
                int tokensConsumed = result.consumedTokens != null ? result.consumedTokens.size() : 0;
                if (tokensConsumed > bestErrorTokensConsumed)
                {
                    bestErrorMatch = result;
                    bestErrorTokensConsumed = tokensConsumed;
                }

                // Collect all expected tokens for comprehensive error reporting
                if (result.expectedTokens != null)
                {
                    allExpectedTokens.addAll(result.expectedTokens);
                }
            }

            // Reset to try the next definition
            cursor.backtrack();
        }

        // If we found a match, advance the cursor past the consumed tokens
        if (bestMatch != null)
        {
            // Advance cursor by the number of tokens consumed by the best match
            for (int i = 0; i < bestTokensConsumed; i++)
                cursor.consume();

            // Commit this position
            cursor.commitPosition();

            return bestMatch;
        }
        else if (bestErrorMatch != null)
        {
            // No match, but we have error information
            // Create a new error result with combined error information
            return new MatchResult(
                    false,
                    bestErrorMatch.definition,
                    bestErrorMatch.consumedTokens,
                    bestErrorMatch.childNodes,
                    bestErrorMatch.localData,
                    allExpectedTokens,
                    bestErrorMatch.actualToken
            );
        }

        // Fallback to traditional matching if the tree doesn't provide a match
        return matchRule(cursor, ruleTree.getRule(), ruleMap, errors);
    }

    /// Attempts to match a rule at the current cursor position.
    ///
    /// @param cursor  TokenCursor for token management
    /// @param rule    The rule to match
    /// @param ruleMap Map of rule names to rules for subrule lookups
    /// @param errors  ErrorManager for reporting errors
    /// @return A MatchResult containing match details, or null if no match
    private MatchResult matchRule(TokenCursor cursor, Rule rule, Map<String, Rule> ruleMap, ErrorManager errors)
    {
        // Track the best match among all definition variants
        MatchResult bestMatch = null;
        int bestTokensConsumed = 0;

        // Also track the best error match for better error reporting
        MatchResult bestErrorMatch = null;
        int bestErrorTokensConsumed = 0;
        List<String> allExpectedTokens = new ArrayList<>();

        // Push current rule onto parsing path stack for better error messages
        parsingPathStack.push(rule.getName());

        // Try each definition variant of the rule
        for (Definition definition : rule.getDefinitions())
        {
            // Skip empty definitions
            if (definition.steps.isEmpty())
                continue;

            // Mark the cursor position for backtracking
            int startPosition = cursor.savePosition();

            // Add definition variant to parsing path if it has a name
            if (definition.name != null && !definition.name.isEmpty())
            {
                parsingPathStack.push(rule.getName() + ":" + definition.name);
            }

            // Match result for this definition attempt
            MatchResult result = matchDefinition(cursor, definition, ruleMap, errors);

            // Remove definition from parsing path
            parsingPathStack.pop();

            if (result != null)
            {
                if (result.matched)
                {
                    int tokensConsumed = result.consumedTokens.size();

                    // Keep track of the definition that consumes the most tokens (longest match)
                    if (tokensConsumed > bestTokensConsumed)
                    {
                        bestMatch = result;
                        bestTokensConsumed = tokensConsumed;
                    }
                }
                else
                {
                    // Even if it didn't match, track the best error information
                    int tokensConsumed = result.consumedTokens != null ? result.consumedTokens.size() : 0;
                    if (tokensConsumed > bestErrorTokensConsumed)
                    {
                        bestErrorMatch = result;
                        bestErrorTokensConsumed = tokensConsumed;
                    }

                    // Collect all expected tokens for comprehensive error reporting
                    if (result.expectedTokens != null)
                        allExpectedTokens.addAll(result.expectedTokens);
                }
            }

            // Reset to try the next definition
            cursor.backtrack();
        }

        // Pop the rule from the parsing path stack
        parsingPathStack.pop();

        // If we found a match, advance the cursor past the consumed tokens
        if (bestMatch != null)
        {
            // Advance cursor by the number of tokens consumed by the best match
            for (int i = 0; i < bestTokensConsumed; i++)
                cursor.consume();

            // Commit this position
            cursor.commitPosition();

            return bestMatch;
        }
        else if (bestErrorMatch != null)
        {
            // Enhance error information with the current parsing path context
            List<String> enhancedExpectedTokens = new ArrayList<>(allExpectedTokens);

            // Create a new error result with combined error information
            return new MatchResult(
                    false,
                    bestErrorMatch.definition,
                    bestErrorMatch.consumedTokens,
                    bestErrorMatch.childNodes,
                    bestErrorMatch.localData,
                    enhancedExpectedTokens,
                    bestErrorMatch.actualToken
            );
        }

        // No match and no error information
        return null;
    }

    /// Attempts to match a definition at the current cursor position.
    ///
    /// @param cursor     TokenCursor for token management
    /// @param definition The definition to match
    /// @param ruleMap    Map of rule names to rules for subrule lookups
    /// @param errors     ErrorManager for reporting errors
    /// @return A MatchResult containing match details, or null if no match
    private MatchResult matchDefinition(TokenCursor cursor, Definition definition, Map<String, Rule> ruleMap, ErrorManager errors)
    {
        List<Token> consumedTokens = new ArrayList<>();
        List<Object> childNodes = new ArrayList<>();
        Map<String, Object> localData = new HashMap<>();

        // Keep track of the position before each step attempt for better error reporting
        List<Token> successfullyConsumedTokens = new ArrayList<>();
        List<String> expectedTokens = new ArrayList<>();

        // Attempt to match each step in the definition
        for (int i = 0; i < definition.steps.size(); i++)
        {
            Step step = definition.steps.get(i);

            // Push step information into parsing path
            String stepDesc = getStepDescription(step, i);
            parsingPathStack.push(stepDesc);

            boolean stepMatched = matchStep(cursor, step, ruleMap, consumedTokens, childNodes, localData, errors);

            // Pop step information from parsing path
            parsingPathStack.pop();

            if (!stepMatched)
            {
                // Get the current token that caused the failure
                Token failedAtToken = cursor.peek();

                // Collect expected token information based on the step type
                switch (step)
                {
                    case Step.Keyword keyword -> expectedTokens.add("keyword '" + keyword.value() + "'");
                    case Step.Literal literal -> expectedTokens.add("literal of type '" + literal.type() + "'");
                    case Step.Identifier identifier ->
                            expectedTokens.add("identifier of type '" + identifier.type() + "'");
                    case Step.Operator operator -> expectedTokens.add("operator '" + operator.value() + "'");
                    case Step.Delimiter delimiter -> expectedTokens.add("delimiter '" + delimiter.value() + "'");
                    case Step.Rule ruleStep -> expectedTokens.add("rule '" + ruleStep.ruleName() + "'");
                    case null, default ->
                    {
                        // For complex steps like Repeat or Optional, provide more specific information
                        switch (step)
                        {
                            case Step.Repeat _ ->
                                    expectedTokens.add("repeated sequence in '" + definition.name + "'");
                            case Step.Optional _ ->
                                    expectedTokens.add("optional sequence in '" + definition.name + "'");
                            case null, default ->
                                    expectedTokens.add("valid syntax for step " + (i + 1) + " in rule '" + definition.name + "'");
                        }
                    }
                }

                // Generate a helpful parsing path for context in error messages
                String parsingPath = String.join(" → ", new ArrayList<>(parsingPathStack));
                if (!parsingPath.isEmpty())
                    expectedTokens.add("(in path: " + parsingPath + ")");

                // Create a MatchResult with the failure information
                return new MatchResult(
                        false,
                        definition,
                        successfullyConsumedTokens,
                        childNodes,
                        localData,
                        expectedTokens,
                        failedAtToken
                );
            }

            // Update our tracking of successful tokens
            successfullyConsumedTokens = new ArrayList<>(consumedTokens);
        }

        // If we get here, all steps matched successfully
        return new MatchResult(true, definition, consumedTokens, childNodes, localData, null, null);
    }

    /// Gets a descriptive string for a step to use in error messages
    private String getStepDescription(Step step, int index)
    {
        return switch (step)
        {
            case Step.Keyword keyword -> "keyword:" + keyword.value();
            case Step.Literal literal -> "literal:" + literal.type();
            case Step.Identifier identifier -> "identifier:" + identifier.type();
            case Step.Operator operator -> "operator:" + operator.value();
            case Step.Delimiter delimiter -> "delimiter:" + delimiter.value();
            case Step.Rule ruleStep -> "rule:" + ruleStep.ruleName();
            case Step.Repeat repeat -> "repeat:" + index;
            case Step.Optional optional -> "optional:" + index;
            case null, default -> "step:" + index;
        };
    }

    /// Attempts to match a step at the current cursor position.
    ///
    /// @param cursor         TokenCursor for token management
    /// @param step           The step to match
    /// @param ruleMap        Map of rule names to rules for subrule lookups
    /// @param consumedTokens List to collect consumed tokens
    /// @param childNodes     List to collect child nodes
    /// @param localData      Map to collect local data from steps
    /// @param errors         ErrorManager for reporting errors
    /// @return true if the step matched, false otherwise
    private boolean matchStep(
            TokenCursor cursor,
            Step step,
            Map<String, Rule> ruleMap,
            List<Token> consumedTokens,
            List<Object> childNodes,
            Map<String, Object> localData,
            ErrorManager errors)
    {
        switch (step)
        {
            case Step.Keyword keyword ->
            {
                Token token = cursor.peek();
                if (token instanceof Token.Keyword && keyword.value().equals(token.getValue()))
                {
                    consumedTokens.add(cursor.consume());
                    if (keyword.action() != null)
                        applyTokenAction(keyword.action(), token, localData);
                    return true;
                }
                return false;
            }
            case Step.Literal literal ->
            {
                Token token = cursor.peek();
                if (token instanceof Token.Literal && literal.type().equals(((Token.Literal) token).type()))
                {
                    consumedTokens.add(cursor.consume());
                    if (literal.action() != null)
                        applyTokenAction(literal.action(), token, localData);
                    return true;
                }
                return false;
            }
            case Step.Identifier identifier ->
            {
                Token token = cursor.peek();
                if (token instanceof Token.Identifier && identifier.type().equals(((Token.Identifier) token).type()))
                {
                    consumedTokens.add(cursor.consume());
                    if (identifier.action() != null)
                        applyTokenAction(identifier.action(), token, localData);
                    return true;
                }
                return false;
            }
            case Step.Operator operator ->
            {
                Token token = cursor.peek();
                if (token instanceof Token.Operator && operator.value().equals(token.getValue()))
                {
                    consumedTokens.add(cursor.consume());
                    if (operator.action() != null)
                        applyTokenAction(operator.action(), token, localData);
                    return true;
                }
                return false;
            }
            case Step.Delimiter delimiter ->
            {
                Token token = cursor.peek();
                if (token instanceof Token.Delimiter && delimiter.value().equals(token.getValue()))
                {
                    consumedTokens.add(cursor.consume());
                    if (delimiter.action() != null)
                        applyTokenAction(delimiter.action(), token, localData);
                    return true;
                }
                return false;
            }
            case Step.Rule ruleStep ->
            {
                String ruleName = ruleStep.ruleName();
                Rule subrule = ruleMap.get(ruleName);

                if (subrule == null)
                {
                    Token token = cursor.peek();
                    int line = 0;
                    int column = 0;
                    String tokenValue = "";

                    if (token != null && token.position() != null)
                    {
                        line = token.position().line();
                        column = token.position().column();
                        tokenValue = token.getValue();
                    }

                    errors.error("Undefined rule: " + ruleName, line, column, tokenValue, "");
                    return false;
                }

                cursor.savePosition();

                // Try to match the subrule
                MatchResult result = matchRule(cursor, subrule, ruleMap, errors);

                if (result != null && result.matched)
                {
                    // Add the tokens that were consumed by the subrule
                    consumedTokens.addAll(result.consumedTokens);

                    // Create a node from the rule result
                    if (result.definition != null && result.definition.builder != null)
                    {
                        // Create context for the builder
                        ASTBuildContext builderContext = new ASTBuildContext();

                        // Transfer local data to context
                        for (Map.Entry<String, Object> entry : result.localData.entrySet())
                            builderContext.put(entry.getKey(), entry.getValue());

                        // Build the node
                        Supplier<Object> nodeBuilder = result.definition.builder.apply(builderContext);
                        Object node = nodeBuilder.get();

                        // Call the consumer to process the child node
                        if (ruleStep.consumer() != null && node != null)
                        {
                            ASTBuildContext consumerContext = new ASTBuildContext();
                            // First, transfer any local data from the parent context
                            for (Map.Entry<String, Object> entry : localData.entrySet())
                                consumerContext.put(entry.getKey(), entry.getValue());

                            // Ensure any lists required by the consumer exist
                            // This is particularly important for "elements" list used in Main.java:88
                            if (localData.containsKey("elements") && localData.get("elements") instanceof List)
                                ensureList(consumerContext.context, "elements");

                            // Then add the node itself
                            consumerContext.put("node", node);

                            // Call the consumer with the node
                            Consumer<Object> consumer = ruleStep.consumer().apply(consumerContext);
                            if (consumer != null)
                                consumer.accept(node);

                            // Transfer any changes back to the local data
                            for (String key : consumerContext.context.keySet())
                                localData.put(key, consumerContext.get(key));

                            childNodes.add(node);
                        }
                    }

                    return true;
                }
                else
                {
                    // Reset cursor position
                    cursor.backtrack();
                    return false;
                }
            }
            case Step.Repeat repeat ->
            {
                // Initialize any accumulators
                if (repeat.initializer() != null)
                {
                    // Create context for initializer
                    ASTBuildContext initContext = new ASTBuildContext();

                    // Transfer all local data to the initializer context
                    for (Map.Entry<String, Object> entry : localData.entrySet())
                        initContext.put(entry.getKey(), entry.getValue());

                    // Apply the initializer to set up any accumulator data structures
                    ASTAction initializer = repeat.initializer();
                    if (initializer != null)
                    {
                        initializer.apply(initContext);

                        // Ensure key data structures exist in both contexts
                        // This addresses the specific NPE with the elements list
                        if (initContext.context.containsKey("elements"))
                            ensureList(localData, "elements");
                    }

                    // Transfer any new context values back to localData
                    for (String key : initContext.context.keySet())
                        localData.put(key, initContext.get(key));
                }

                boolean matchedOnce = false;

                // Keep matching the repeat children as long as possible
                while (true)
                {
                    int savePos = cursor.savePosition();
                    boolean allChildrenMatched = true;

                    // Try to match all children in the repeat
                    List<Token> repeatTokens = new ArrayList<>();
                    List<Object> repeatNodes = new ArrayList<>();
                    Map<String, Object> repeatLocalData = new HashMap<>(localData); // Clone current local data

                    for (Step childStep : repeat.children())
                    {
                        boolean childMatched = matchStep(
                                cursor, childStep, ruleMap, repeatTokens, repeatNodes, repeatLocalData, errors);

                        if (!childMatched)
                        {
                            allChildrenMatched = false;
                            break;
                        }
                    }

                    if (allChildrenMatched)
                    {
                        // If all children matched, add their tokens and nodes
                        consumedTokens.addAll(repeatTokens);
                        childNodes.addAll(repeatNodes);
                        matchedOnce = true;

                        // Transfer any new context values back to localData
                        for (String key : repeatLocalData.keySet())
                            localData.put(key, repeatLocalData.get(key));

                        // Commit this position before trying again
                        cursor.commitPosition();
                    }
                    else
                    {
                        // If any child didn't match, backtrack and break the loop
                        cursor.backtrack();
                        break;
                    }
                }

                // A repeat can match zero times and still be successful
                return true;

                // A repeat can match zero times and still be successful
            }
            case Step.Optional optional ->
            {
                int savePos = cursor.savePosition();
                boolean allChildrenMatched = true;

                // Try to match all children in the optional group
                List<Token> optionalTokens = new ArrayList<>();
                List<Object> optionalNodes = new ArrayList<>();

                for (Step childStep : optional.children())
                {
                    boolean childMatched = matchStep(
                            cursor, childStep, ruleMap, optionalTokens, optionalNodes, localData, errors);

                    if (!childMatched)
                    {
                        allChildrenMatched = false;
                        break;
                    }
                }

                if (allChildrenMatched)
                {
                    // If all children matched, add their tokens and nodes
                    consumedTokens.addAll(optionalTokens);
                    childNodes.addAll(optionalNodes);

                    // Commit this position
                    cursor.commitPosition();
                }
                else
                {
                    // If any child didn't match, backtrack and apply the fallback
                    cursor.backtrack();

                    // Apply optional fallback
                    if (optional.fallback() != null)
                    {
                        // Create context for fallback
                        ASTBuildContext fallbackContext = new ASTBuildContext();
                        for (Map.Entry<String, Object> entry : localData.entrySet())
                            fallbackContext.put(entry.getKey(), entry.getValue());
                        optional.fallback().apply(fallbackContext);
                        // Transfer any new context values back to localData
                        for (String key : fallbackContext.context.keySet())
                            localData.put(key, fallbackContext.get(key));
                    }
                }

                // An optional step always succeeds, regardless of whether its content matched
                return true;
            }
            case null, default ->
            {
            }
        }

        // If we get here, the step type is not recognized
        return false;
    }

    /// Helper method to apply a TokenAction with the correct context
    private void applyTokenAction(TokenAction action, Token token, Map<String, Object> localData)
    {
        if (action == null) return;

        // Create a context for the action
        ASTBuildContext actionContext = new ASTBuildContext();

        // Copy all local data to the context
        for (Map.Entry<String, Object> entry : localData.entrySet())
            actionContext.put(entry.getKey(), entry.getValue());

        // Apply the action with the context
        Consumer<Token> consumer = action.apply(actionContext);
        if (consumer != null)
            consumer.accept(token);

        // Copy any new or modified data back to the local data map
        for (String key : actionContext.context.keySet())
            localData.put(key, actionContext.get(key));
    }

    /// Ensures a list exists in the localData map for the given key.
    /// If the key doesn't exist or its value is null, creates a new ArrayList.
    private <T> List<T> ensureList(Map<String, Object> localData, String key)
    {
        @SuppressWarnings("unchecked")
        List<T> list = (List<T>) localData.get(key);
        if (list == null)
        {
            list = new ArrayList<>();
            localData.put(key, list);
        }
        return list;
    }

    /// Enum representing the kinds of tokens for matching purposes.
    private enum TokenKind
    {
        KEYWORD,
        LITERAL,
        IDENTIFIER,
        OPERATOR,
        DELIMITER,
        ANY
    }

    /// Class that holds the result of a rule match attempt.
    private static class MatchResult
    {
        final boolean matched;
        final Definition definition;
        final List<Token> consumedTokens;
        final List<Object> childNodes;
        final Map<String, Object> localData;
        final List<String> expectedTokens;
        final Token actualToken;

        // Constructor for successful matches
        MatchResult(boolean matched, Definition definition, List<Token> consumedTokens,
                    List<Object> childNodes, Map<String, Object> localData)
        {
            this(matched, definition, consumedTokens, childNodes, localData, null, null);
        }

        // Extended constructor with error information
        MatchResult(boolean matched, Definition definition, List<Token> consumedTokens,
                    List<Object> childNodes, Map<String, Object> localData,
                    List<String> expectedTokens, Token actualToken)
        {
            this.matched = matched;
            this.definition = definition;
            this.consumedTokens = consumedTokens;
            this.childNodes = childNodes;
            this.localData = localData;
            this.expectedTokens = expectedTokens;
            this.actualToken = actualToken;
        }
    }

    /// Represents a tree structure for a grammar rule.
    /// The tree is used to optimize rule matching by pre-processing rule definitions
    /// and organizing them based on potential starting tokens.
    private static class RuleTree
    {
        private final Rule rule;
        private final Map<TokenType, List<RuleTreeNode>> tokenToDefinitions = new HashMap<>();

        public RuleTree(Rule rule)
        {
            this.rule = rule;
            buildTree();
        }

        public Rule getRule()
        {
            return rule;
        }

        /// Builds the tree structure by analyzing each definition's first step
        /// and categorizing based on potential matching token types.
        private void buildTree()
        {
            for (Definition definition : rule.getDefinitions())
            {
                // Skip empty definitions
                if (definition.steps.isEmpty())
                    continue;

                // Analyze the first step of the definition to determine potential matching tokens
                Step firstStep = definition.steps.get(0);
                List<TokenType> potentialTokenTypes = getPotentialTokenTypes(firstStep);

                // Add this definition to the tree under all potential token types
                RuleTreeNode node = new RuleTreeNode(definition);
                for (TokenType tokenType : potentialTokenTypes)
                    tokenToDefinitions.computeIfAbsent(tokenType, k -> new ArrayList<>()).add(node);
            }
        }

        /// Determines what token types could potentially match the given step.
        private List<TokenType> getPotentialTokenTypes(Step step)
        {
            List<TokenType> types = new ArrayList<>();

            switch (step)
            {
                case Step.Keyword keyword -> types.add(new TokenType(TokenKind.KEYWORD, keyword.value()));
                case Step.Literal literal -> types.add(new TokenType(TokenKind.LITERAL, literal.type()));
                case Step.Identifier identifier -> types.add(new TokenType(TokenKind.IDENTIFIER, identifier.type()));
                case Step.Operator operator -> types.add(new TokenType(TokenKind.OPERATOR, operator.value()));
                case Step.Delimiter delimiter -> types.add(new TokenType(TokenKind.DELIMITER, delimiter.value()));
                case Step.Optional optional ->
                {
                    // For optional steps, include both the potential token types of its children
                    // and also add a wildcard to represent that this step can be skipped
                    for (Step childStep : optional.children())
                        types.addAll(getPotentialTokenTypes(childStep));

                    // Add special wildcard that matches any token
                    types.add(new TokenType(TokenKind.ANY, "*"));
                }
                case Step.Repeat repeat ->
                {
                    // For repeat steps, include the potential token types of its children
                    // since we might match this step at least once
                    for (Step childStep : repeat.children())
                        types.addAll(getPotentialTokenTypes(childStep));

                    // Also add a wildcard since repeat can match zero times
                    types.add(new TokenType(TokenKind.ANY, "*"));
                }
                case Step.Rule rule1 ->
                    // For subrules, we'd need to analyze the subrule definitions
                    // For simplicity, we'll just use a wildcard for now
                        types.add(new TokenType(TokenKind.ANY, "*"));
                case null, default ->
                {
                }
            }

            return types;
        }

        /// Finds potential matching definitions for the given token.
        public List<RuleTreeNode> findPotentialMatches(Token token)
        {
            List<RuleTreeNode> matches = new ArrayList<>();

            // Check for specific token type matches
            TokenType exactType = tokenTypeFromToken(token);
            if (exactType != null && tokenToDefinitions.containsKey(exactType))
                matches.addAll(tokenToDefinitions.get(exactType));

            // Also include any wildcards that match any token
            TokenType wildcard = new TokenType(TokenKind.ANY, "*");
            if (tokenToDefinitions.containsKey(wildcard))
                matches.addAll(tokenToDefinitions.get(wildcard));

            return matches;
        }

        /// Converts a token to a TokenType for matching in the tree.
        private TokenType tokenTypeFromToken(Token token)
        {
            return switch (token)
            {
                case Token.Keyword keyword -> new TokenType(TokenKind.KEYWORD, token.getValue());
                case Token.Literal literal -> new TokenType(TokenKind.LITERAL, literal.type());
                case Token.Identifier identifier -> new TokenType(TokenKind.IDENTIFIER, identifier.type());
                case Token.Operator operator -> new TokenType(TokenKind.OPERATOR, token.getValue());
                case Token.Delimiter delimiter -> new TokenType(TokenKind.DELIMITER, token.getValue());
                case null, default -> null;
            };
        }
    }

    /// Node in the rule tree representing a definition with its potential matching characteristics.
        private record RuleTreeNode(Definition definition)
        {
        }

    /// Represents a type of token for matching in the rule tree.
    private record TokenType(TokenKind kind, String value)
    {

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TokenType tokenType = (TokenType) o;
            return kind == tokenType.kind && Objects.equals(value, tokenType.value);
        }

    }
}
