package org.nest;

import org.nest.ast.ASTRules;
import org.nest.ast.ASTWrapper;
import org.nest.errors.ErrorManager;
import org.nest.lisp.ast.LispAST;
import org.nest.lisp.ast.LispAtom;
import org.nest.lisp.ast.LispList;
import org.nest.lisp.ast.LispNode;
import org.nest.tokenization.*;

import java.util.ArrayList;
import java.util.List;


public class Main
{
    public static final java.util.function.Function<Token.Comment, Token.Comment> stripLispCommentMarker =
            (Token.Comment comment) ->
            {
                String value = comment.value();
                String stripped = value.startsWith(";")
                        ? value.substring(1).strip()
                        : value.strip(); // fallback
                return new Token.Comment(comment.position(), stripped);
            };

    public static void main(String[] args)
    {

        TokenRules lispRules = TokenRules.builder()
                // Lisp uses parentheses as structure
                .delimeter("(")
                .delimeter(")")

                // Quote characters
                .operator("'")   // Quote
                .operator("`")   // Quasiquote
                .operator(",")   // Unquote
                .operator(",@")  // Unquote-splicing

                // Identifiers (symbols, function names)
                .identifier("symbol", "[^\\s(),'`,@:]+") // Updated to exclude colon and quote characters
                .identifier("keyword", ":[^\\s(),'`,@]+") // Keywords start with colon

                // Numbers
                .literal("integer", "[+-]?[0-9]+")
                .literal("float", "[+-]?[0-9]*\\.[0-9]+")
                .literal("boolean", "#t|#f")
                .literal("nil", "nil")

                // Character literals
                .literal("character", "#\\\\.")

                // Strings
                .literal("string", "^\"(?:\\\\.|[^\"\\\\])*\"")

                // Comments (Lisp often uses ';' for line comments)
                .comment(";.*")

                // Settings
                .whitespaceMode(WhitespaceMode.IGNORE)
                .enableLongestMatchFirst()
                .makeCaseSensitive()
                .build();


        TokenPostProcessor lispPost = TokenPostProcessor.builder()
                .literal("string", TokenTransformations::processEscapeSequences)
                .literal("string", TokenTransformations::unquoteAndTrimIndentation)
                .literal("integer", TokenTransformations::normalizeInteger)
                .literal("float", TokenTransformations::normalizeFloat)
                .comment("comment", Main.stripLispCommentMarker)
                .build();


        ASTRules rules = ASTRules.builder()
                .topRule(List.of("expr")) // Top Rule defines the types of elements that can be at the top layer of the AST.
                .ignoreComments(true) // Enable comment skipping to avoid errors with comments
                .startRule("expr") // ASTRuleTemplate — top-level expression

                // ----- List Form -----
                .addDefinition("list")
                .delimiter("(", self -> token ->
                {
                })
                .repeat(self -> self.put("elements", new ArrayList<LispNode>())) // init empty list
                .rule("expr", self -> expr -> self.<List<LispNode>>get("elements").add((LispNode) expr))
                .stopRepeat()
                .delimiter(")", self -> token ->
                {
                })
                .endDefinition(self -> () -> new LispList(self.get("elements", List.class)))

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
                })

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
                })

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
                })

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
                })

                // ----- String Literal -----
                .addDefinition("string")
                .literal("string", self -> token -> self.put("value", token.getValue()))
                .endDefinition(self -> () -> new LispAtom.LispString(self.get("value", String.class)))

                // ----- Integer Literal -----
                .addDefinition("int")
                .literal("integer", self -> token -> self.put("value", token.getValue()))
                .endDefinition(self -> () -> new LispAtom.LispNumber(self.get("value", String.class)))

                // ----- Float Literal -----
                .addDefinition("float")
                .literal("float", self -> token -> self.put("value", token.getValue()))
                .endDefinition(self -> () -> new LispAtom.LispNumber(self.get("value", String.class)))

                // ----- Boolean -----
                .addDefinition("boolean")
                .literal("boolean", self -> token -> self.put("value", token.getValue()))
                .endDefinition(self -> () -> new LispAtom.LispBoolean(self.get("value", String.class).equals("#t")))

                // ----- Character Literal -----
                .addDefinition("character")
                .literal("character", self -> token -> self.put("value", token.getValue()))
                .endDefinition(self -> () -> new LispAtom.LispCharacter(self.get("value", String.class).charAt(2)))


                // ----- Symbol -----
                .addDefinition("symbol")
                .identifier("symbol", self -> token -> self.put("value", token.getValue()))
                .endDefinition(self -> () -> new LispAtom.LispSymbol(self.get("value", String.class)))

                // ----- Keyword -----
                .addDefinition("keyword")
                .identifier("keyword", self -> token -> self.put("value", token.getValue()))
                .endDefinition(self -> () -> new LispAtom.LispKeyword(self.get("value", String.class).substring(1)))

                // ----- Nil -----
                .addDefinition("nil")
                .literal("nil", self -> token ->
                {
                })
                .endDefinition(self -> () -> LispAtom.LispNil.INSTANCE)


                .build();

        String lispCode = """
                ; Test with a simple missing closing parenthesis
                ((define x 10
                
                ; Test with a misplaced closing parenthesis
                (foo bar) baz)
                
                ; Test with balanced parentheses for comparison
                (alpha (beta) gamma)
                """;

        TokenList lispTokens = TokenList.create(lispCode, lispRules, lispPost);

        System.out.println("\n=== Tokens ===");
        System.out.println(lispTokens);

        ErrorManager errorManager = new ErrorManager();

        errorManager.setContext("lisp", lispCode);

        ASTWrapper astWrapper = rules.createAST(lispTokens, errorManager);


        if (errorManager.hasErrors())
            errorManager.printReports(System.out);
        else
        {
            LispAST ast = LispAST.fromASTWrapper(astWrapper);

            System.out.println("\n=== AST Tree Structure ===");
            System.out.println(ast.printTree(0));

            System.out.println("\n=== Regenerated Lisp Code ===");
            System.out.println(ast.generateCode());

            System.out.println("\n=== Original Code ===");
            System.out.println(lispCode);

        }
    }
}