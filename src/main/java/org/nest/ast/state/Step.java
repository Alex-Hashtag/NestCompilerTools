package org.nest.ast.state;

import org.nest.ast.functional.ASTAction;
import org.nest.ast.functional.ASTNodeConsumer;
import org.nest.ast.functional.TokenAction;

import java.util.List;
import java.util.Set;


/// A `Step` is a single unit of parsing logic within a rule definition.
/// Each variant defines a different strategy for matching and handling tokens during parsing.
public sealed interface Step
        permits Step.Keyword, Step.Literal, Step.Identifier, Step.Operator,
        Step.Delimiter, Step.Rule, Step.Repeat, Step.Optional, Step.Choice {

    /// Matches a specific keyword in the token stream.
    /// Parsing succeeds if the next token is of type "keyword" and has the exact value `value`.
    /// If matched, the associated `TokenAction` is executed.
    record Keyword(String value, TokenAction action) implements Step {}

    /// Matches a literal token of a specific type (e.g., "number", "string").
    /// Parsing succeeds if the next token has the specified `type`, regardless of its content.
    /// If matched, the `TokenAction` is executed with the token.
    record Literal(String type, TokenAction action) implements Step {}

    /// Matches an identifier token.
    /// Parsing succeeds if the next token is of the specific type, and the `TokenAction` is applied.
    record Identifier(String type, TokenAction action) implements Step {}

    /// Matches a specific operator (e.g., "+", "==", "*").
    /// Parsing succeeds if the next token is an operator with the exact value `value`.
    /// If matched, the `TokenAction` is triggered.
    record Operator(String value, TokenAction action) implements Step {}

    /// Matches a delimiter character (e.g., ";", ",", ")", "}").
    /// Parsing succeeds if the next token is a delimiter with the exact value `value`.
    /// On success, the `TokenAction` is executed.
    record Delimiter(String value, TokenAction action) implements Step {}

    /// Recursively parses a named rule.
    /// Parsing succeeds if the rule identified by `ruleName` matches at the current position.
    /// If so, its result is passed to the `ASTNodeConsumer`.
    record Rule(String ruleName, ASTNodeConsumer consumer) implements Step {}

    /// Matches the enclosed steps repeatedly (one or more times).
    /// The parser keeps matching `children` until it no longer succeeds.
    /// All successful matches are collected and passed to the `ASTAction` for aggregation.
    record Repeat(List<Step> children, ASTAction initializer) implements Step {}

    /// Attempts to match the enclosed steps once.
    /// If they match, parsing proceeds normally.
    /// If they fail, the parser still considers this a successful match, and the `fallback` is triggered.
    record Optional(List<Step> children, ASTAction fallback) implements Step {}

    /// Attempts to match one of several alternative step sequences.
    /// Each alternative is tried in order; the first one that fully matches is used.
    /// If none match, parsing fails at this step.
    record Choice(Set<List<Step>> alternatives) implements Step {}
}
