package org.nest.lisp;

import org.nest.ast.ASTRules;
import org.nest.lisp.ast.LispAtom;
import org.nest.lisp.ast.LispList;
import org.nest.lisp.ast.LispNode;

import java.util.ArrayList;
import java.util.List;


/**
 * Defines the AST rules for Lisp language parsing.
 */
public class LispASTRules
{

    /// Creates and returns the standard AST rules for Lisp.
    ///
    /// @return ASTRules configured for Lisp syntax
    public static ASTRules create()
    {

        return ASTRules.builder()
                .ignoreComments(true) // Enable comment skipping to avoid errors with comments
                .topRule(List.of("expr")) // Top Rule defines the types of elements that can be at the top layer of the AST.
                .startRule("expr") // ASTRuleTemplate — top-level expression

                // ----- List Form -----
                .addDefinition("list")
                .delimiter("(", _ -> _ ->
                {
                })
                .repeat(self -> self.put("elements", new ArrayList<LispNode>())) // init empty list
                .rule("expr", self -> expr -> self.<List<LispNode>>get("elements").add((LispNode) expr))
                .stopRepeat()
                .delimiter(")", _ -> _ ->
                {
                })
                .endDefinition(self -> () -> new LispList(
                        self.get("elements", List.class)
                ), "Expected a properly formed S-expression: open parenthesis, expressions, close parenthesis")

                // ----- Quote Form -----
                .addDefinition("quote")
                .operator("'", self -> token ->
                {
                })
                .rule("expr", self -> expr -> self.put("quoted", expr))
                .endDefinition(self -> () ->
                {
                    // Create a quote list: (quote <expr>)
                    List<LispNode> elements = new ArrayList<>();
                    elements.add(new LispAtom.LispSymbol("quote"));
                    elements.add(self.get("quoted"));
                    return new LispList(elements);
                }, "Expected a quoted expression: 'expression")

                // ----- Quasiquote Form -----
                .addDefinition("quasiquote")
                .operator("`", self -> token ->
                {
                })
                .rule("expr", self -> expr -> self.put("quoted", expr))
                .endDefinition(self -> () ->
                {
                    // Create a quasiquote list: (quasiquote <expr>)
                    List<LispNode> elements = new ArrayList<>();
                    elements.add(new LispAtom.LispSymbol("quasiquote"));
                    elements.add(self.get("quoted"));
                    return new LispList(elements);
                }, "Expected a quasiquoted expression: `expression")

                // ----- Unquote Form -----
                .addDefinition("unquote")
                .operator(",", self -> token ->
                {
                })
                .rule("expr", self -> expr -> self.put("unquoted", expr))
                .endDefinition(self -> () ->
                {
                    // Create an unquote list: (unquote <expr>)
                    List<LispNode> elements = new ArrayList<>();
                    elements.add(new LispAtom.LispSymbol("unquote"));
                    elements.add(self.get("unquoted"));
                    return new LispList(elements);
                }, "Expected an unquoted expression: ,expression (must be inside a quasiquoted context)")

                // ----- Unquote-splicing Form -----
                .addDefinition("unquote-splicing")
                .operator(",@", self -> token ->
                {
                })
                .rule("expr", self -> expr -> self.put("unquoted", expr))
                .endDefinition(self -> () ->
                {
                    // Create an unquote-splicing list: (unquote-splicing <expr>)
                    List<LispNode> elements = new ArrayList<>();
                    elements.add(new LispAtom.LispSymbol("unquote-splicing"));
                    elements.add(self.get("unquoted"));
                    return new LispList(elements);
                }, "Expected an unquote-splicing expression: ,@expression (must be inside a quasiquoted context and evaluate to a list)")

                // ----- String Literal -----
                .addDefinition("string")
                .literal("string", self -> token ->
                {
                    self.put("value", token.getValue());
                })
                .endDefinition(self -> () -> new LispAtom.LispString(
                        self.get("value", String.class)
                ), "Expected a string literal: \"text\"")

                // ----- Integer Literal -----
                .addDefinition("int")
                .literal("integer", self -> token ->
                {
                    self.put("value", token.getValue());
                })
                .endDefinition(self -> () -> new LispAtom.LispNumber(
                        self.get("value", String.class)
                ), "Expected an integer number: 42")

                // ----- Float Literal -----
                .addDefinition("float")
                .literal("float", self -> token ->
                {
                    self.put("value", token.getValue());
                })
                .endDefinition(self -> () -> new LispAtom.LispNumber(
                        self.get("value", String.class)
                ), "Expected a floating-point number: 3.14")

                // ----- Boolean -----
                .addDefinition("boolean")
                .literal("boolean", self -> token ->
                {
                    self.put("value", token.getValue());
                })
                .endDefinition(self -> () -> new LispAtom.LispBoolean(
                        self.get("value", String.class).equals("#t")
                ), "Expected a boolean literal: #t or #f")

                // ----- Character Literal -----
                .addDefinition("character")
                .literal("character", self -> token ->
                {
                    self.put("value", token.getValue());
                })
                .endDefinition(self -> () -> new LispAtom.LispCharacter(
                        self.get("value", String.class).charAt(2)
                ), "Expected a character literal: #\\a")

                // ----- Symbol -----
                .addDefinition("symbol")
                .identifier("symbol", self -> token ->
                {
                    self.put("value", token.getValue());
                })
                .endDefinition(self -> () -> new LispAtom.LispSymbol(
                        self.get("value", String.class)
                ), "Expected a valid symbol name")

                // ----- Keyword -----
                .addDefinition("keyword")
                .identifier("keyword", self -> token ->
                {
                    self.put("value", token.getValue());
                })
                .endDefinition(self -> () -> new LispAtom.LispKeyword(
                        self.get("value", String.class).substring(1)
                ), "Expected a keyword: :keyword-name")

                // ----- Nil -----
                .addDefinition("nil")
                .literal("nil", self -> token ->
                {
                })
                .endDefinition(self -> () ->
                {
                    return LispAtom.LispNil.INSTANCE;
                }, "Expected nil value: nil")

                .build();
    }
}
