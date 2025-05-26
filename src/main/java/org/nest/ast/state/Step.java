package org.nest.ast.state;

import org.nest.ast.functional.ASTAction;
import org.nest.ast.functional.ASTNodeConsumer;
import org.nest.ast.functional.TokenAction;

import java.util.List;


public sealed interface Step permits Step.Keyword, Step.Literal, Step.Identifier, Step.Operator,
        Step.Delimiter, Step.Rule, Step.Repeat, Step.Optional, Step.Choice
{

    record Keyword(String value, TokenAction action) implements Step
    {
    }

    record Literal(String type, TokenAction action) implements Step
    {
    }

    record Identifier(String type, TokenAction action) implements Step
    {
    }

    record Operator(String value, TokenAction action) implements Step
    {
    }

    record Delimiter(String value, TokenAction action) implements Step
    {
    }

    record Rule(String ruleName, ASTNodeConsumer consumer) implements Step
    {
    }

    record Repeat(List<Step> children, ASTAction initializer) implements Step
    {
    }

    record Optional(List<Step> children, ASTAction fallback) implements Step
    {
    }
    
    record Choice(List<List<Step>> alternatives) implements Step
    {
    }
}
