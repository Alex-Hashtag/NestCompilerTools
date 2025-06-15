package org.nest.lisp;

import org.nest.errors.ErrorManager;
import org.nest.lisp.ast.LispAST;
import org.nest.lisp.parser.LispParser;
import org.nest.tokenization.TokenList;


/// Example demonstrating how to use the Lisp parser.
public class LispParserExample
{

    public static void main(String[] args)
    {
        // Example Lisp code
        String lispCode = """
                ; Test with a simple missing closing parenthesis
                ((define x 10)
                
                ; Test with a misplaced closing parenthesis
                (foo bar) baz)
                
                ; Test with balanced parentheses for comparison
                (alpha (beta) gamma)
                """;

        System.out.println("=== Original Lisp Code ===");
        System.out.println(lispCode);

        // Generate tokens using our token rules
        TokenList tokens = TokenList.create(lispCode, LispTokenRules.create(), LispTokenProcessor.create());

        System.out.println("\n=== Tokens ===");
        System.out.println(tokens);

        // Create error manager for collecting errors
        ErrorManager errorManager = new ErrorManager();
        errorManager.setContext("lisp", lispCode);

        // Parse using the LispParser
        LispAST ast = LispParser.parse(lispCode, errorManager);

        // Handle errors or display results
        if (errorManager.hasErrors())
        {
            System.out.println("\n=== Parse Errors ===");
            errorManager.printReports(System.out);
        }
        else
        {
            System.out.println("\n=== AST Tree Structure ===");
            System.out.println(ast.printTree(0));

            System.out.println("\n=== Regenerated Lisp Code ===");
            System.out.println(ast.generateCode());
        }

        // Alternative simplified usage
        System.out.println("\n=== Using Simplified Parser API ===");
        String simpleCode = "(define (add x y) (+ x y))";
        LispAST simpleAst = LispParser.parseWithErrorOutput(simpleCode, System.out);
        if (simpleAst != null)
        {
            System.out.println("Parsed successfully: " + simpleAst.generateCode());
        }
    }
}
