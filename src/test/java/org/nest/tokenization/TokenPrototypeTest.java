package org.nest.tokenization;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TokenPrototypeTest {
    
    @Test
    void testKeywordPrototype() {
        TokenPrototype.Keyword keyword = new TokenPrototype.Keyword("if");
        assertEquals("if", keyword.value());
    }
    
    @Test
    void testDelimiterPrototype() {
        TokenPrototype.Delimiter delimiter = new TokenPrototype.Delimiter("{");
        assertEquals("{", delimiter.value());
    }
    
    @Test
    void testOperatorPrototype() {
        TokenPrototype.Operator operator = new TokenPrototype.Operator("+");
        assertEquals("+", operator.value());
    }
    
    @Test
    void testLiteralPrototype() {
        TokenPrototype.Literal literal = new TokenPrototype.Literal("string", "\"[^\"]*\"");
        assertEquals("string", literal.type());
        assertEquals("\"[^\"]*\"", literal.regex());
    }
    
    @Test
    void testIdentifierPrototype() {
        TokenPrototype.Identifier identifier = new TokenPrototype.Identifier("variable", "[a-zA-Z_][a-zA-Z0-9_]*");
        assertEquals("variable", identifier.type());
        assertEquals("[a-zA-Z_][a-zA-Z0-9_]*", identifier.regex());
    }
    
    @Test
    void testCommentPrototype() {
        TokenPrototype.Comment comment = new TokenPrototype.Comment("//.*");
        assertEquals("//.*", comment.regex());
    }
    
    @Test
    void testStartPrototype() {
        TokenPrototype.Start start = new TokenPrototype.Start();
        assertNotNull(start);
    }
    
    @Test
    void testEndPrototype() {
        TokenPrototype.End end = new TokenPrototype.End();
        assertNotNull(end);
    }
    
    @Test
    void testNewLinePrototype() {
        TokenPrototype.NewLine newLine = new TokenPrototype.NewLine();
        assertNotNull(newLine);
    }
}
