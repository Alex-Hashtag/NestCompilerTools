package org.nest.tokenization;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TokenTransformationsTest {
    
    private final Coordinates testPosition = new Coordinates(1, 1);
    
    @Test
    void testUnquoteAndTrimIndentation_SingleLineString() {
        Token.Literal lit = new Token.Literal(testPosition, "string", "\"hello world\"");
        Token.Literal transformed = TokenTransformations.unquoteAndTrimIndentation(lit);
        
        assertEquals("hello world", transformed.value());
        assertEquals("string", transformed.type());
        assertEquals(testPosition, transformed.position());
    }

    
    @Test
    void testProcessEscapeSequences_BasicEscapes() {
        Token.Literal lit = new Token.Literal(testPosition, "string", "Line 1\\nLine 2\\tTabbed\\\"Quoted\\\"");
        Token.Literal transformed = TokenTransformations.processEscapeSequences(lit);
        
        String expected = "Line 1\nLine 2\tTabbed\"Quoted\"";
        assertEquals(expected, transformed.value());
    }
    
    @Test
    void testProcessEscapeSequences_AllEscapeTypes() {
        Token.Literal lit = new Token.Literal(testPosition, "string", 
                "\\n\\t\\r\\b\\f\\0\\\\\\\"\\u0041");
        Token.Literal transformed = TokenTransformations.processEscapeSequences(lit);
        
        // \n, \t, \r, \b, \f, \0, \\, \", \u0041 (Unicode 'A')
        String expected = "\n\t\r\b\f\0\\\"A";
        assertEquals(expected, transformed.value());
    }
    
    @Test
    void testProcessEscapeSequences_NoEscapeSequences() {
        Token.Literal lit = new Token.Literal(testPosition, "string", "Regular string without escapes");
        Token.Literal transformed = TokenTransformations.processEscapeSequences(lit);
        
        // Should be unchanged
        assertEquals("Regular string without escapes", transformed.value());
    }
    
    @Test
    void testRemoveCommonIndentation_EqualIndentation() {
        String multiline = "    Line 1\n    Line 2\n    Line 3";
        String result = TokenTransformations.removeCommonIndentation(multiline);
        
        assertEquals("Line 1\nLine 2\nLine 3", result);
    }
    
    @Test
    void testRemoveCommonIndentation_VaryingIndentation() {
        String multiline = "    Line 1\n      Line 2\n  Line 3";
        String result = TokenTransformations.removeCommonIndentation(multiline);
        
        assertEquals("  Line 1\n    Line 2\nLine 3", result);
    }
    
    @Test
    void testRemoveCommonIndentation_BlankLines() {
        String multiline = "    Line 1\n\n    Line 3";
        String result = TokenTransformations.removeCommonIndentation(multiline);
        
        assertEquals("Line 1\n\nLine 3", result);
    }
    
    @Test
    void testStripCommentMarkers_SingleLineComment() {
        Token.Comment comment = new Token.Comment(testPosition, "// This is a comment");
        Token.Comment transformed = TokenTransformations.stripCommentMarkers(comment);
        
        assertEquals("This is a comment", transformed.value());
    }
    
    @Test
    void testStripCommentMarkers_MultiLineComment() {
        Token.Comment comment = new Token.Comment(testPosition, "/* This is a\nmulti-line comment */");
        Token.Comment transformed = TokenTransformations.stripCommentMarkers(comment);
        
        assertEquals("This is a\nmulti-line comment", transformed.value());
    }
    
    @Test
    void testStripCommentMarkers_UnrecognizedFormat() {
        Token.Comment comment = new Token.Comment(testPosition, "# This is a Python-style comment");
        Token.Comment transformed = TokenTransformations.stripCommentMarkers(comment);
        
        // Should be unchanged as it doesn't match known formats
        assertEquals("# This is a Python-style comment", transformed.value());
    }
    
    @Test
    void testNormalizeInteger_DecimalInteger() {
        Token.Literal lit = new Token.Literal(testPosition, "integer", "12345");
        Token.Literal transformed = TokenTransformations.normalizeInteger(lit);
        
        assertEquals("12345", transformed.value());
    }
    
    @Test
    void testNormalizeInteger_HexInteger() {
        Token.Literal lit = new Token.Literal(testPosition, "integer", "0xFF");
        Token.Literal transformed = TokenTransformations.normalizeInteger(lit);
        
        assertEquals("255", transformed.value()); // 0xFF = 255 in decimal
    }
    
    @Test
    void testNormalizeInteger_BinaryInteger() {
        Token.Literal lit = new Token.Literal(testPosition, "integer", "0b1010");
        Token.Literal transformed = TokenTransformations.normalizeInteger(lit);
        
        assertEquals("10", transformed.value()); // 0b1010 = 10 in decimal
    }
    
    @Test
    void testNormalizeInteger_OctalInteger() {
        Token.Literal lit = new Token.Literal(testPosition, "integer", "0o17");
        Token.Literal transformed = TokenTransformations.normalizeInteger(lit);
        
        assertEquals("15", transformed.value()); // 0o17 = 15 in decimal
    }
    
    @Test
    void testNormalizeInteger_WithUnderscores() {
        Token.Literal lit = new Token.Literal(testPosition, "integer", "1_000_000");
        Token.Literal transformed = TokenTransformations.normalizeInteger(lit);
        
        assertEquals("1000000", transformed.value()); // Underscores should be removed
    }
    
    @Test
    void testNormalizeFloat_BasicFloat() {
        Token.Literal lit = new Token.Literal(testPosition, "float", "123.45");
        Token.Literal transformed = TokenTransformations.normalizeFloat(lit);
        
        // Result should be in scientific notation
        assertEquals("1.234500e+02", transformed.value());
    }
    
    @Test
    void testNormalizeFloat_ScientificNotation() {
        Token.Literal lit = new Token.Literal(testPosition, "float", "1.2345e+2");
        Token.Literal transformed = TokenTransformations.normalizeFloat(lit);
        
        // Should normalize to consistent scientific notation
        assertEquals("1.234500e+02", transformed.value());
    }
    
    @Test
    void testNormalizeFloat_WithUnderscores() {
        Token.Literal lit = new Token.Literal(testPosition, "float", "1_000.5");
        Token.Literal transformed = TokenTransformations.normalizeFloat(lit);
        
        // Underscores should be removed and converted to scientific notation
        assertEquals("1.000500e+03", transformed.value());
    }
}
