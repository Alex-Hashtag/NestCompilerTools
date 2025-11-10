package org.nest.tokenization;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;


class TokenPostProcessorTest
{

    @Test
    void testBasicPostProcessor()
    {
        // Create a basic post-processor with no transformations
        TokenPostProcessor noOp = TokenPostProcessor.builder().build();

        // Get processors for a type that doesn't exist - should return empty list
        List<Function<Token, Token>> processors = noOp.getProcessors("nonexistent");
        assertTrue(processors.isEmpty());
    }

    @Test
    void testKeywordProcessor()
    {
        // Create a post processor that transforms keyword tokens
        TokenPostProcessor processor = TokenPostProcessor.builder()
                .keyword("if", kw -> new Token.Keyword(kw.position(), kw.value().toUpperCase()))
                .build();

        // Get the processors for "if" keywords
        List<Function<Token, Token>> processors = processor.getProcessors("if");

        // Should have one processor
        assertEquals(1, processors.size());

        // Test the processor
        Token.Keyword keyword = new Token.Keyword(new Coordinates(1, 1), "if");
        Token transformed = processors.get(0).apply(keyword);

        assertInstanceOf(Token.Keyword.class, transformed);
        assertEquals("IF", ((Token.Keyword) transformed).value());
    }

    @Test
    void testLiteralProcessor()
    {
        // Create a post processor for string literals
        TokenPostProcessor processor = TokenPostProcessor.builder()
                .literal("string", lit -> new Token.Literal(lit.position(), lit.type(), lit.value().replace("\"", "")))
                .build();

        // Get the processors for string literals
        List<Function<Token, Token>> processors = processor.getProcessors("string");

        // Test the processor
        Token.Literal literal = new Token.Literal(new Coordinates(1, 1), "string", "\"hello\"");
        Token transformed = processors.get(0).apply(literal);

        assertInstanceOf(Token.Literal.class, transformed);
        assertEquals("hello", ((Token.Literal) transformed).value());
    }

    @Test
    void testIdentifierProcessor()
    {
        // Create a post processor for identifiers
        TokenPostProcessor processor = TokenPostProcessor.builder()
                .identifier("variable", id -> new Token.Identifier(id.position(), id.type(), id.value().toUpperCase()))
                .build();

        // Get the processors for variable identifiers
        List<Function<Token, Token>> processors = processor.getProcessors("variable");

        // Test the processor
        Token.Identifier identifier = new Token.Identifier(new Coordinates(1, 1), "variable", "count");
        Token transformed = processors.get(0).apply(identifier);

        assertInstanceOf(Token.Identifier.class, transformed);
        assertEquals("COUNT", ((Token.Identifier) transformed).value());
    }

    @Test
    void testCommentProcessor()
    {
        // Create a post processor for comments
        TokenPostProcessor processor = TokenPostProcessor.builder()
                .comment("//", comment -> new Token.Comment(comment.position(), comment.value().substring(2).trim()))
                .build();

        // Get the processors for // comments
        List<Function<Token, Token>> processors = processor.getProcessors("//");

        // Test the processor
        Token.Comment comment = new Token.Comment(new Coordinates(1, 1), "// This is a comment");
        Token transformed = processors.get(0).apply(comment);

        assertInstanceOf(Token.Comment.class, transformed);
        assertEquals("This is a comment", ((Token.Comment) transformed).value());
    }

    @Test
    void testMultipleProcessorsForSameType()
    {
        // Create a post processor with multiple transformations for the same type
        TokenPostProcessor processor = TokenPostProcessor.builder()
                .literal("string", lit -> new Token.Literal(lit.position(), lit.type(), lit.value().replace("\"", "")))
                .literal("string", lit -> new Token.Literal(lit.position(), lit.type(), lit.value().toUpperCase()))
                .build();

        // Get the processors for string literals
        List<Function<Token, Token>> processors = processor.getProcessors("string");

        // Should have two processors
        assertEquals(2, processors.size());

        // Test the combined effect of both processors
        Token.Literal literal = new Token.Literal(new Coordinates(1, 1), "string", "\"hello\"");
        Token transformed1 = processors.get(0).apply(literal);
        Token transformed2 = processors.get(1).apply(transformed1);

        assertInstanceOf(Token.Literal.class, transformed2);
        assertEquals("HELLO", ((Token.Literal) transformed2).value());
    }

    @Test
    void testDifferentTokenTypes()
    {
        // Create a post processor with transformations for different token types
        TokenPostProcessor processor = TokenPostProcessor.builder()
                .keyword("if", kw -> new Token.Keyword(kw.position(), kw.value().toUpperCase()))
                .literal("string", lit -> new Token.Literal(lit.position(), lit.type(), lit.value().replace("\"", "")))
                .identifier("variable", id -> new Token.Identifier(id.position(), id.type(), id.value().toUpperCase()))
                .build();

        // Each token type should have its own processor
        assertEquals(1, processor.getProcessors("if").size());
        assertEquals(1, processor.getProcessors("string").size());
        assertEquals(1, processor.getProcessors("variable").size());

        // Types without processors should return empty lists
        assertTrue(processor.getProcessors("unknown").isEmpty());
    }
}
