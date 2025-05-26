package org.nest.tokenization;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TokenTest {
    
    private final Coordinates testPosition = new Coordinates(1, 1);
    
    @Test
    void testKeywordToken() {
        Token token = new Token.Keyword(testPosition, "if");
        
        assertEquals(testPosition, token.position());
        assertEquals("if", ((Token.Keyword) token).value());
        assertEquals("if", token.getValue());
        assertEquals("Keyword: if", token.toString());
    }
    
    @Test
    void testDelimiterToken() {
        Token token = new Token.Delimiter(testPosition, "{");
        
        assertEquals(testPosition, token.position());
        assertEquals("{", ((Token.Delimiter) token).value());
        assertEquals("{", token.getValue());
        assertEquals("Delimiter: {", token.toString());
    }
    
    @Test
    void testOperatorToken() {
        Token token = new Token.Operator(testPosition, "+");
        
        assertEquals(testPosition, token.position());
        assertEquals("+", ((Token.Operator) token).value());
        assertEquals("+", token.getValue());
        assertEquals("Operator: +", token.toString());
    }
    
    @Test
    void testLiteralToken() {
        Token token = new Token.Literal(testPosition, "string", "hello");
        
        assertEquals(testPosition, token.position());
        assertEquals("string", ((Token.Literal) token).type());
        assertEquals("hello", ((Token.Literal) token).value());
        assertEquals("hello", token.getValue());
        assertEquals("Literal (string): hello", token.toString());
    }
    
    @Test
    void testIdentifierToken() {
        Token token = new Token.Identifier(testPosition, "variable", "myVar");
        
        assertEquals(testPosition, token.position());
        assertEquals("variable", ((Token.Identifier) token).type());
        assertEquals("myVar", ((Token.Identifier) token).value());
        assertEquals("myVar", token.getValue());
        assertEquals("Identifier (variable): myVar", token.toString());
    }
    
    @Test
    void testCommentToken() {
        Token token = new Token.Comment(testPosition, "// This is a comment");
        
        assertEquals(testPosition, token.position());
        assertEquals("// This is a comment", ((Token.Comment) token).value());
        assertEquals("// This is a comment", token.getValue());
        assertEquals("Comment: // This is a comment", token.toString());
    }
    
    @Test
    void testStartToken() {
        Token token = new Token.Start(testPosition);
        
        assertEquals(testPosition, token.position());
        assertEquals("Start", token.getValue());
        assertEquals("Start", token.toString());
    }
    
    @Test
    void testEndToken() {
        Token token = new Token.End(testPosition);
        
        assertEquals(testPosition, token.position());
        assertEquals("End", token.getValue());
        assertEquals("End", token.toString());
    }
    
    @Test
    void testNewLineToken() {
        Token token = new Token.NewLine(testPosition);
        
        assertEquals(testPosition, token.position());
        assertEquals("NewLine", token.getValue());
        assertEquals("NewLine", token.toString());
    }
    
    @Test
    void testIndentIncrToken() {
        Token token = new Token.IndentIncr(testPosition);
        
        assertEquals(testPosition, token.position());
        assertEquals("IndentIncr", token.getValue());
        assertEquals("IndentIncr", token.toString());
    }
    
    @Test
    void testIndentDecrToken() {
        Token token = new Token.IndentDecr(testPosition);
        
        assertEquals(testPosition, token.position());
        assertEquals("IndentDecr", token.getValue());
        assertEquals("IndentDecr", token.toString());
    }
    
    @Test
    void testInvalidToken() {
        Token token = new Token.Invalid(testPosition, "@#$");
        
        assertEquals(testPosition, token.position());
        assertEquals("@#$", ((Token.Invalid) token).value());
        assertEquals("@#$", token.getValue());
        assertEquals("Invalid: @#$", token.toString());
    }
}
