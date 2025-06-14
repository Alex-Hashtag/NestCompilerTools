package org.nest.tokenization;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Iterator;

class TokenListTest {
    
    @Test
    void testBasicTokenization() {
        // Create simple rules for tokenizing a basic if statement
        TokenRules rules = TokenRules.builder()
                .keyword("if")
                .keyword("else")
                .operator(">")
                .operator("=")
                .delimiter("{")
                .delimiter("}")
                .delimiter(";")
                .literal("number", "\\d+")
                .identifier("variable", "[a-zA-Z_][a-zA-Z0-9_]*")
                .comment("//.*")
                .build();
        
        // No post-processing
        TokenPostProcessor noOp = TokenPostProcessor.builder().build();
        
        // Create token list from source code
        String source = "if count > 0 { // Check if count is positive\n  result = 42;\n}";
        TokenList tokenList = TokenList.create(source, rules, noOp);
        
        // Check token count (start and end tokens included automatically)
        // Expected tokens: Start, if, count, >, 0, {, Comment, NewLine, result, =, 42, ;, }, End
        assertEquals(13, tokenList.size());
        
        // Check first few tokens
        Iterator<Token> iterator = tokenList.iterator();
        Token token;
        
        // Start token
        token = iterator.next();
        assertTrue(token instanceof Token.Start);
        
        // 'if' keyword
        token = iterator.next();
        assertTrue(token instanceof Token.Keyword);
        assertEquals("if", ((Token.Keyword) token).value());
        
        // 'count' identifier
        token = iterator.next();
        assertTrue(token instanceof Token.Identifier);
        assertEquals("count", ((Token.Identifier) token).value());
        
        // '>' operator
        token = iterator.next();
        assertTrue(token instanceof Token.Operator);
        assertEquals(">", ((Token.Operator) token).value());
    }
    
    @Test
    void testTokenizationWithWhitespaceSignificant() {
        TokenRules rules = TokenRules.builder()
                .keyword("if")
                .delimiter("{")
                .delimiter("}")
                .operator(">")
                .literal("number", "\\d+")
                .identifier("variable", "[a-zA-Z_][a-zA-Z0-9_]*")
                .whitespaceMode(WhitespaceMode.SIGNIFICANT) // Significant whitespace
                .build();
        
        TokenPostProcessor noOp = TokenPostProcessor.builder().build();
        
        String source = "if count > 0 {\n  result\n}";
        TokenList tokenList = TokenList.create(source, rules, noOp);
        
        // Count tokens including NewLine tokens that should be generated
        int newLineCount = 0;
        for (Token token : tokenList) {
            if (token instanceof Token.NewLine) {
                newLineCount++;
            }
        }
        
        // Should have 2 NewLine tokens
        assertEquals(2, newLineCount);
    }
    @Test
    void testTokenAccessByIndex() {
        TokenRules rules = TokenRules.builder()
                .keyword("if")
                .delimiter("{")
                .delimiter("}")
                .operator(">")
                .literal("number", "\\d+")
                .identifier("variable", "[a-zA-Z_][a-zA-Z0-9_]*")
                .build();
        
        TokenPostProcessor noOp = TokenPostProcessor.builder().build();
        
        String source = "if count > 0 { result = 42; }";
        TokenList tokenList = TokenList.create(source, rules, noOp);
        
        // Check random access by index
        Token token = tokenList.get(2); // Third token (after Start and if)
        assertTrue(token instanceof Token.Identifier);
        assertEquals("count", ((Token.Identifier) token).value());
        
        // Test bounds checking
        assertThrows(IndexOutOfBoundsException.class, () -> tokenList.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> tokenList.get(tokenList.size()));
    }
    
    @Test
    void testTokenizationWithCaseSensitivity() {
        // Case sensitive rules
        TokenRules caseSensitiveRules = TokenRules.builder()
                .keyword("if")
                .keyword("IF")
                .identifier("variable", "[a-zA-Z_][a-zA-Z0-9_]*")
                .makeCaseSensitive() // Make keywords case sensitive
                .build();
        
        // Case insensitive rules
        TokenRules caseInsensitiveRules = TokenRules.builder()
                .keyword("if")
                .keyword("IF") // This will be treated the same as "if"
                .identifier("variable", "[a-zA-Z_][a-zA-Z0-9_]*")
                .build();
        
        TokenPostProcessor noOp = TokenPostProcessor.builder().build();
        
        // Test with case sensitive rules
        String source1 = "if count IF value";
        TokenList tokenList1 = TokenList.create(source1, caseSensitiveRules, noOp);
        
        // Test with case insensitive rules
        String source2 = "if count IF value";
        TokenList tokenList2 = TokenList.create(source2, caseInsensitiveRules, noOp);
        
        // In case sensitive mode, both "if" and "IF" should be recognized as keywords
        int keywordCount1 = 0;
        for (Token token : tokenList1) {
            if (token instanceof Token.Keyword) {
                keywordCount1++;
            }
        }
        
        // In case insensitive mode, "IF" should be recognized as the same keyword as "if"
        int keywordCount2 = 0;
        for (Token token : tokenList2) {
            if (token instanceof Token.Keyword) {
                keywordCount2++;
            }
        }
        
        // Case sensitive should recognize both "if" and "IF" as keywords
        assertEquals(2, keywordCount1);
        
        // Case insensitive should recognize both "if" and "IF" as the same keyword
        assertEquals(2, keywordCount2);
    }
    
    @Test
    void testLongestMatchFirst() {
        // Rules with longest match first enabled
        TokenRules longestMatchRules = TokenRules.builder()
                .operator("+")
                .operator("++") // Longer operator
                .operator("+=") // Another longer operator
                .enableLongestMatchFirst() // Prioritize longer matches
                .build();
        
        // Rules without longest match first
        TokenRules normalRules = TokenRules.builder()
                .operator("+")
                .operator("++")
                .operator("+=")
                .build();
        
        TokenPostProcessor noOp = TokenPostProcessor.builder().build();
        
        // Test with longest match first
        String source = "x++ y+= z+";
        TokenList tokenList1 = TokenList.create(source, longestMatchRules, noOp);
        
        // Count the tokens
        int plusPlusCount = 0;
        int plusEqualCount = 0;
        int plusCount = 0;
        
        for (Token token : tokenList1) {
            if (token instanceof Token.Operator op) {
                switch (op.value()) {
                    case "++" -> plusPlusCount++;
                    case "+=" -> plusEqualCount++;
                    case "+" -> plusCount++;
                }
            }
        }
        
        // Should prioritize longer operators
        assertEquals(1, plusPlusCount);
        assertEquals(1, plusEqualCount);
        assertEquals(1, plusCount);
    }
    
    @Test
    void testInvalidTokens() {
        TokenRules rules = TokenRules.builder()
                .keyword("if")
                .delimiter("{")
                .delimiter("}")
                .operator(">")
                .literal("number", "\\d+")
                .identifier("variable", "[a-zA-Z_][a-zA-Z0-9_]*")
                .build();
        
        TokenPostProcessor noOp = TokenPostProcessor.builder().build();
        
        // Include an invalid character @
        String source = "if count @ > 0 { result = 42; }";
        TokenList tokenList = TokenList.create(source, rules, noOp);
        
        // Find the invalid token
        boolean foundInvalid = false;
        for (Token token : tokenList) {
            if (token instanceof Token.Invalid) {
                foundInvalid = true;
                assertEquals("@", ((Token.Invalid) token).value());
                break;
            }
        }
        
        assertTrue(foundInvalid, "Should have found an Invalid token");
    }
}
