package org.nest.tokenization;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TokenRulesTest {
    
    @Test
    void testTokenRulesBuilder() {
        TokenRules rules = TokenRules.builder()
                .keyword("if")
                .keyword("else")
                .operator("+")
                .operator("-")
                .delimiter("{")
                .delimiter("}")
                .literal("string", "\"[^\"]*\"")
                .literal("number", "\\d+")
                .identifier("variable", "[a-zA-Z_][a-zA-Z0-9_]*")
                .comment("//.*")
                .whitespaceMode(WhitespaceMode.SIGNIFICANT)
                .enableLongestMatchFirst()
                .makeCaseSensitive()
                .build();
        
        // Rules should include Start and End tokens added automatically
        assertEquals(12, rules.tokenPrototypes.size());
        assertTrue(rules.caseSensitive);
        assertTrue(rules.longestMatchFirst);
        assertEquals(WhitespaceMode.SIGNIFICANT, rules.whitespaceMode);
    }
    
    @Test
    void testDefaultRulesConfiguration() {
        TokenRules rules = TokenRules.builder()
                .keyword("if")
                .build();
        
        // Default configuration should be case-insensitive, not use longest match first,
        // and ignore whitespace
        assertFalse(rules.caseSensitive);
        assertFalse(rules.longestMatchFirst);
        assertEquals(WhitespaceMode.IGNORE, rules.whitespaceMode);
    }
    
    @Test
    void testWhitespaceModeIndentation() {
        TokenRules rules = TokenRules.builder()
                .keyword("if")
                .whitespaceMode(WhitespaceMode.INDENTATION)
                .build();
        
        assertEquals(WhitespaceMode.INDENTATION, rules.whitespaceMode);
    }
    
    @Test
    void testTokenPrototypesAddedCorrectly() {
        TokenRules rules = TokenRules.builder()
                .keyword("if")
                .operator("+")
                .delimiter("{")
                .literal("string", "\"[^\"]*\"")
                .identifier("variable", "[a-zA-Z_][a-zA-Z0-9_]*")
                .comment("//.*")
                .build();
        
        // Verify the prototypes by checking their type (we need to count the automatic Start and End)
        long keywordCount = rules.tokenPrototypes.stream()
                .filter(tp -> tp instanceof TokenPrototype.Keyword)
                .count();
        
        long operatorCount = rules.tokenPrototypes.stream()
                .filter(tp -> tp instanceof TokenPrototype.Operator)
                .count();
                
        long delimiterCount = rules.tokenPrototypes.stream()
                .filter(tp -> tp instanceof TokenPrototype.Delimiter)
                .count();
                
        long literalCount = rules.tokenPrototypes.stream()
                .filter(tp -> tp instanceof TokenPrototype.Literal)
                .count();
                
        long identifierCount = rules.tokenPrototypes.stream()
                .filter(tp -> tp instanceof TokenPrototype.Identifier)
                .count();
                
        long commentCount = rules.tokenPrototypes.stream()
                .filter(tp -> tp instanceof TokenPrototype.Comment)
                .count();
                
        long startCount = rules.tokenPrototypes.stream()
                .filter(tp -> tp instanceof TokenPrototype.Start)
                .count();
                
        long endCount = rules.tokenPrototypes.stream()
                .filter(tp -> tp instanceof TokenPrototype.End)
                .count();
        
        assertEquals(1, keywordCount);
        assertEquals(1, operatorCount);
        assertEquals(1, delimiterCount);
        assertEquals(1, literalCount);
        assertEquals(1, identifierCount);
        assertEquals(1, commentCount);
        assertEquals(1, startCount);
        assertEquals(1, endCount);
    }
}
