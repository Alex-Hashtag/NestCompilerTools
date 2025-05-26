package org.nest.lisp;

import org.nest.ast.ASTWrapper;
import org.nest.errors.ErrorManager;
import org.nest.lisp.ast.LispAST;
import org.nest.tokenization.TokenList;


/// Main entry point for parsing Lisp code.
/// This class simplifies the process of parsing Lisp code by encapsulating the tokenization
/// and AST creation steps.
public class LispParser
{

    /// Parses the given Lisp code and returns a LispAST representation.
    ///
    /// @param code         The Lisp code to parse
    /// @param errorManager The error manager to collect any parsing errors
    /// @return A LispAST representation of the parsed code
    public static LispAST parse(String code, ErrorManager errorManager)
    {
        // Set the context for error reporting
        errorManager.setContext("lisp", code);

        // Create token list
        TokenList tokens = TokenList.create(code, LispTokenRules.create(), LispTokenProcessor.create());

        // Create AST
        ASTWrapper astWrapper = LispASTRules.create().createAST(tokens, errorManager);

        // Convert to LispAST
        return LispAST.fromASTWrapper(astWrapper);
    }

    /// Parses the given Lisp code and returns a LispAST representation.
    /// Creates a new error manager for this parsing operation.
    ///
    /// @param code The Lisp code to parse
    /// @return A LispAST representation of the parsed code
    public static LispAST parse(String code)
    {
        ErrorManager errorManager = new ErrorManager();
        return parse(code, errorManager);
    }

    /// Parses the given Lisp code and returns a LispAST representation.
    /// Prints any errors to the specified output.
    ///
    /// @param code          The Lisp code to parse
    /// @param printErrorsTo Where to print errors (e.g., System.out)
    /// @return A LispAST representation of the parsed code, or null if there were errors
    public static LispAST parseWithErrorOutput(String code, java.io.PrintStream printErrorsTo)
    {
        ErrorManager errorManager = new ErrorManager();
        LispAST ast = parse(code, errorManager);

        if (errorManager.hasErrors())
        {
            errorManager.printReports(printErrorsTo);
            return null;
        }

        return ast;
    }
}
