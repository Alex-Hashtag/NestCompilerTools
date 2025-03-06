package org.nest.tokenization;

import java.util.List;


public final class TokenRules
{
    List<TokenPrototype> tokenPrototypes;
    WhitespaceMode whitespaceMode;
    boolean longestMatchFirst;
    boolean caseSensitive;

    protected TokenRules(List<TokenPrototype> tokenPrototypes, WhitespaceMode whitespaceMode, boolean longestMatchFirst, boolean caseSensitive)
    {
        this.tokenPrototypes = tokenPrototypes;
        this.whitespaceMode = whitespaceMode;
        this.longestMatchFirst = longestMatchFirst;
        this.caseSensitive = caseSensitive;
    }

    public static TokenRulesBuilder builder()
    {
        return new TokenRulesBuilder();
    }

    public static class TokenRulesBuilder
    {
        private final List<TokenPrototype> tokenPrototypes = new java.util.ArrayList<>();
        private WhitespaceMode whitespaceMode = WhitespaceMode.IGNORE;
        private boolean longestMatchFirst = false;
        private boolean caseSensitive = false;

        public TokenRulesBuilder keyword(String value)
        {
            tokenPrototypes.add(new TokenPrototype.Keyword(value));
            return this;
        }

        public TokenRulesBuilder operator(String value)
        {
            tokenPrototypes.add(new TokenPrototype.Operator(value));
            return this;
        }

        public TokenRulesBuilder delimeter(String value)
        {
            tokenPrototypes.add(new TokenPrototype.Delimiter(value));
            return this;
        }

        public TokenRulesBuilder literal(String type, String regex)
        {
            tokenPrototypes.add(new TokenPrototype.Literal(type, regex));
            return this;
        }

        public TokenRulesBuilder identifier(String type, String regex)
        {
            tokenPrototypes.add(new TokenPrototype.Identifier(type, regex));
            return this;
        }

        public TokenRulesBuilder comment(String regex)
        {
            tokenPrototypes.add(new TokenPrototype.Comment(regex));
            return this;
        }

        public TokenRulesBuilder whitespaceMode(WhitespaceMode mode)
        {
            this.whitespaceMode = mode;
            return this;
        }

        public TokenRulesBuilder enableLongestMatchFirst()
        {
            this.longestMatchFirst = true;
            return this;
        }

        public TokenRulesBuilder makeCaseSensitive()
        {
            this.caseSensitive = true;
            return this;
        }

        public TokenRules build()
        {
            tokenPrototypes.add(new TokenPrototype.Start());
            tokenPrototypes.add(new TokenPrototype.End());
            return new TokenRules(tokenPrototypes, whitespaceMode, longestMatchFirst, caseSensitive);
        }
    }
}
