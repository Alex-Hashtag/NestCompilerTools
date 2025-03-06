package org.nest.tokenization;

import java.util.*;
import java.util.function.Function;


public final class TokenPostProcessor
{
    private final Map<String, List<Function<Token, Token>>> processingRules;

    private TokenPostProcessor(Map<String, List<Function<Token, Token>>> processingRules)
    {
        this.processingRules = processingRules;
    }

    /**
     * Creates a new builder instance for defining token transformations.
     */
    public static TokenPostProcessorBuilder builder()
    {
        return new TokenPostProcessorBuilder();
    }

    /**
     * Returns the list of processing functions for a given token type.
     * If no transformations exist for the given type, returns an empty list.
     */
    public List<Function<Token, Token>> getProcessors(String tokenType)
    {
        return processingRules.getOrDefault(tokenType, Collections.emptyList());
    }

    /**
     * Builder for TokenPostProcessor that allows chaining transformations.
     */
    public static class TokenPostProcessorBuilder
    {
        private final Map<String, List<Function<Token, Token>>> processingRules = new HashMap<>();

        private void addProcessor(String type, Function<Token, Token> processor)
        {
            processingRules
                    .computeIfAbsent(type, k -> new ArrayList<>())
                    .add(processor);
        }

        public TokenPostProcessorBuilder keyword(String type, Function<Token.Keyword, Token.Keyword> processor)
        {
            addProcessor(type, token -> processor.apply((Token.Keyword) token));
            return this;
        }

        public TokenPostProcessorBuilder operator(String type, Function<Token.Operator, Token.Operator> processor)
        {
            addProcessor(type, token -> processor.apply((Token.Operator) token));
            return this;
        }

        public TokenPostProcessorBuilder delimeter(String type, Function<Token.Delimiter, Token.Delimiter> processor)
        {
            addProcessor(type, token -> processor.apply((Token.Delimiter) token));
            return this;
        }

        public TokenPostProcessorBuilder identifier(String type, Function<Token.Identifier, Token.Identifier> processor)
        {
            addProcessor(type, token -> processor.apply((Token.Identifier) token));
            return this;
        }

        public TokenPostProcessorBuilder literal(String type, Function<Token.Literal, Token.Literal> processor)
        {
            addProcessor(type, token -> processor.apply((Token.Literal) token));
            return this;
        }

        public TokenPostProcessorBuilder comment(String type, Function<Token.Comment, Token.Comment> processor)
        {
            addProcessor(type, token -> processor.apply((Token.Comment) token));
            return this;
        }

        public TokenPostProcessor build()
        {
            return new TokenPostProcessor(Collections.unmodifiableMap(processingRules));
        }
    }
}
