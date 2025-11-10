package org.nest.ast;

import org.nest.ast.functional.ASTAction;
import org.nest.ast.functional.ASTNodeConsumer;
import org.nest.ast.functional.TokenAction;
import org.nest.ast.state.Step;

import java.util.ArrayList;
import java.util.List;


/// Template for defining optional steps in an AST rule.
///
/// @param <O> The type parameter for this optional template (self-type)
/// @param <P> The type of the parent template
public final class ASTDefinitionTemplateOptional<O extends ASTDefinitionStepTemplate<O>, P extends ASTDefinitionStepTemplate<P>>
        implements ASTDefinitionStepTemplate<ASTDefinitionTemplateOptional<O, P>>
{
    P parent;
    List<Step> children;

    ASTDefinitionTemplateOptional(P parent)
    {
        this.parent = parent;
        children = new ArrayList<>();
    }

    @Override
    public ASTDefinitionTemplateOptional<O, P> keyword(String value, TokenAction action)
    {
        children.add(new Step.Keyword(value, action));
        return this;
    }

    @Override
    public ASTDefinitionTemplateOptional<O, P> operator(String value, TokenAction action)
    {
        children.add(new Step.Operator(value, action));
        return this;
    }

    @Override
    public ASTDefinitionTemplateOptional<O, P> delimiter(String value, TokenAction action)
    {
        children.add(new Step.Delimiter(value, action));
        return this;
    }

    @Override
    public ASTDefinitionTemplateOptional<O, P> identifier(String type, TokenAction action)
    {
        children.add(new Step.Identifier(type, action));
        return this;
    }

    @Override
    public ASTDefinitionTemplateOptional<O, P> literal(String type, TokenAction action)
    {
        children.add(new Step.Literal(type, action));
        return this;
    }

    @Override
    public ASTDefinitionTemplateOptional<O, P> rule(String ruleName, ASTNodeConsumer consumer)
    {
        children.add(new Step.Rule(ruleName, consumer));
        return this;
    }

    @Override
    public <R extends ASTDefinitionStepTemplate<R>> ASTDefinitionTemplateRepeat<R, ASTDefinitionTemplateOptional<O, P>> repeat(ASTAction initializer)
    {
        return new ASTDefinitionTemplateRepeat<>(initializer, this);
    }

    @Override
    public <NO extends ASTDefinitionStepTemplate<NO>> ASTDefinitionTemplateOptional<NO, ASTDefinitionTemplateOptional<O, P>> optional()
    {
        return new ASTDefinitionTemplateOptional<>(this);
    }

    @Override
    public <C extends ASTDefinitionStepTemplate<C>> ASTDefinitionTemplateChoice<C, ASTDefinitionTemplateOptional<O, P>> choice()
    {
        return new ASTDefinitionTemplateChoice<>(this);
    }

    /// Ends this optional template and returns to the parent template.
    ///
    /// @param fallback The action to perform if the optional steps don't match
    /// @return The parent template
    public P otherwise(ASTAction fallback)
    {
        switch (parent)
        {
            case ASTDefinitionTemplate astDefTemplate ->
                    astDefTemplate.definition.addStep(new Step.Optional(children, fallback));
            case ASTDefinitionTemplateRepeat<?, ?> astRepeatTemplate ->
                    astRepeatTemplate.children.add(new Step.Optional(children, fallback));
            case ASTDefinitionTemplateOptional<?, ?> astOptionalTemplate ->
                    astOptionalTemplate.children.add(new Step.Optional(children, fallback));
            case ASTDefinitionTemplateChoice<?, ?> astChoiceTemplate ->
                    astChoiceTemplate.currentAlternative.add(new Step.Optional(children, fallback));
            case null, default ->
            {
            }
        }
        return parent;
    }
}
