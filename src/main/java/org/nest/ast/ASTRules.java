package org.nest.ast;

import org.nest.ast.functional.ASTAction;
import org.nest.ast.functional.ASTNodeConsumer;
import org.nest.ast.functional.TokenAction;
import org.nest.ast.state.Rule;
import org.nest.ast.state.Definition;
import org.nest.ast.state.Step;
import org.nest.errors.ErrorManager;
import org.nest.tokenization.Token;
import org.nest.tokenization.TokenList;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ASTRules {
    List<String> topRules;
    List<Rule> rules;
    boolean ignoreComments;
    TokenList tokenList;
    // Stack of contexts for nested rule processing
    private final Deque<ASTBuildContext> contextStack = new ArrayDeque<>();

    public ASTRules(List<String> topRules, List<Rule> rules, boolean ignoreComments) {
        this.topRules = topRules;
        this.rules = rules;
        this.ignoreComments = ignoreComments;
        this.tokenList = null;
    }

    public static ASTRulesBuilder builder() {
        return new ASTRulesBuilder();
    }

    /// Creates an Abstract Syntax Tree (AST) from a token list.
    ///
    /// @param tokens the token list to parse
    /// @param errors the error manager to report errors to
    /// @return the root of the AST
    public ASTWrapper createAST(TokenList tokens, ErrorManager errors) {
        // 1. Wrap the TokenList in a lookahead-capable cursor
        //    → peek(), consume(), mark()/reset() for backtracking.

        // 2. Build a Map<String, Rule> from your List<Rule> for fast lookup.

        // 3. Create the root ASTWrapper (e.g., named "Program") to collect top-level nodes.

        // 4. While the cursor still has tokens:
        //    a. For each name in topRules:
        //       i.   cursor.mark();  // save position for backtracking
        //       ii.  Attempt to match this Rule:
        //             • For each variant of the Rule:
        //               - Walk its Steps (sequence, choice, optional, repeat).
        //               - On a Step match, consume tokens or recurse for subrules.
        //               - On mismatch, cursor.reset() and try the next variant.
        //             • Track which variant consumed the most tokens (longest match).
        //       iii. If a variant matched >0 tokens:
        //             // --- HERE’S THE KEY SUPPLIER LOGIC ---
        //             1. Identify exactly which tokens (and any child ASTWrappers) were consumed.
        //             2. Create an ASTBuildContext:
        //                  ASTBuildContext ctx = new ASTBuildContext(
        //                      ruleDefinition, consumedTokens, childWrappers, errors
        //                  );
        //             3. Retrieve the supplier for this rule:
        //                  ASTNodeSupplier supplier = ruleDefinition.getSupplier();
        //             4. Build the actual AST node:
        //                  ASTWrapper node = supplier.build(ctx);
        //             5. Add the node to the root’s children:
        //                  root.addChild(node);
        //             6. Advance the cursor past those consumed tokens.
        //             7. break;  // go back to “while tokens remain”
        //
        //       iv.  If no variant matched:
        //             • errors.error("Expected one of " + topRules + " but got " + cursor.current());
        //             • Optionally create a dummy ASTWrapper for recovery.
        //             • Skip to the next sync token (e.g., semicolon, newline).
        //
        // 5. Repeat until cursor.isAtEnd().

        // 6. Return the fully populated root ASTWrapper.
        return null;  // TODO: replace with your constructed root
    }

}
