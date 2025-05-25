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

/**
 * Main class for the Nest compiler.
 */
public class Main
{
    public static void main(String[] args)
    {

        TokenRules lispRules = TokenRules.builder()
                // Lisp uses parentheses as structure
                .delimeter("(")
                .delimeter(")")

                // Identifiers (symbols, function names)
                .identifier("symbol", "[^\\s()]+") // Anything that's not space or parentheses

                // Numbers
                .literal("integer", "[+-]?[0-9]+")
                .literal("float", "[+-]?[0-9]*\\.[0-9]+")
                .literal("boolean", "#t|#f")
                .literal("nil", "nil")

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

        String lispCode = """
      (define (square x)
        (* x x)) ; This computes the square
      (print (square 5))
    """;

        TokenList lispTokens = TokenList.create(lispCode, lispRules, lispPost);
        
        // Debug print the token list
        System.out.println("\n[DEBUG] ====== TOKEN LIST ======");
        System.out.println("Total tokens: " + lispTokens.size());
        int i = 0;
        for (Token token : lispTokens) {
            System.out.println("[DEBUG] Token " + i + ": " + token.getValue() + 
                " (" + token.getClass().getSimpleName() + ")");
            i++;
        }
        System.out.println("[DEBUG] =======================\n");

        ErrorManager errorManager = new ErrorManager();

        ASTRules rules = ASTRules.builder()
                .topRule(List.of("node")) // Top Rule defines the types of elements that can be at the top layer of the AST.
                .ignoreComments(true) // Enable comment skipping to avoid errors with comments
                .startRule("node") // ASTRuleTemplate — top-level expression

                // ----- List Form -----
                .addDefinition("list")
                    .delimiter("(", self -> token -> {})
                    .repeat(self -> self.put("elements", new ArrayList<LispNode>())) // init empty list
                    .rule("node", self -> node -> self.<List<LispNode>>get("elements").add((LispNode) node))
                    .stopRepeat()
                    .delimiter(")", self -> token -> {})
                    .endDefinition(self -> () -> new LispList(self.get("elements", List.class)))

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

                // ----- Nil -----
                .addDefinition("nil")
                    .literal("nil", self -> token -> {})
                    .endDefinition(self -> () -> LispAtom.LispNil.INSTANCE)

                // ----- Symbol -----
                .addDefinition("symbol")
                    .identifier("symbol", self -> token -> self.put("value", token.getValue()))
                    .endDefinition(self -> () -> new LispAtom.LispSymbol(self.get("value", String.class)))

                .build();

        ASTWrapper astWrapper = rules.createAST(lispTokens, errorManager);

        if (astWrapper.hasErrors())
            errorManager.printReports(System.err);
        else {
            LispAST ast = LispAST.fromASTWrapper(astWrapper);

                // Print the AST as a tree structure
                System.out.println("\n=== AST Tree Structure ===");
                System.out.println(ast.printTree(0));

                // Print the regenerated code
                System.out.println("\n=== Regenerated Lisp Code ===");
                System.out.println(ast.generateCode());

                // Print the original code for comparison
                System.out.println("\n=== Original Code ===");
                System.out.println(lispCode);
            }
}

public static final java.util.function.Function<Token.Comment, Token.Comment> stripLispCommentMarker =
        (Token.Comment comment) -> {
            String value = comment.value();
            String stripped = value.startsWith(";")
                    ? value.substring(1).strip()
                    : value.strip(); // fallback
            return new Token.Comment(comment.position(), stripped);
            };
}