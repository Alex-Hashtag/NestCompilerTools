package org.nest.ast;

import org.nest.ast.functional.ASTAction;
import org.nest.ast.functional.ASTNodeConsumer;
import org.nest.ast.functional.TokenAction;
import org.nest.ast.state.Step;

import java.util.ArrayList;
import java.util.List;

/// Template for defining a repeated sequence of steps in an AST rule.
///
/// @param <R> The type parameter for this repeat template (self-type)
/// @param <P> The type of the parent template
public class ASTDefinitionTemplateRepeat<R extends ASTDefinitionStepTemplate<R>, P extends ASTDefinitionStepTemplate<P>>
    implements ASTDefinitionStepTemplate<ASTDefinitionTemplateRepeat<R, P>>
{
    P parent;

    List<Step> children;
    ASTAction initializer;

    ASTDefinitionTemplateRepeat(ASTAction initializer, P parent)
    {
        this.parent = parent;
        this.children = new ArrayList<>();
        this.initializer = initializer;
    }

    @Override
    public ASTDefinitionTemplateRepeat<R, P> keyword(String value, TokenAction action)
    {
        children.add(new Step.Keyword(value, action));
        return this;
    }

    @Override
    public ASTDefinitionTemplateRepeat<R, P> operator(String value, TokenAction action)
    {
        children.add(new Step.Operator(value, action));
        return this;
    }

    @Override
    public ASTDefinitionTemplateRepeat<R, P> delimiter(String value, TokenAction action)
    {
        children.add(new Step.Delimiter(value, action));
        return this;
    }

    @Override
    public ASTDefinitionTemplateRepeat<R, P> identifier(String type, TokenAction action)
    {
        children.add(new Step.Identifier(type, action));
        return this;
    }

    @Override
    public ASTDefinitionTemplateRepeat<R, P> literal(String type, TokenAction action)
    {
        children.add(new Step.Literal(type, action));
        return this;
    }

    @Override
    public ASTDefinitionTemplateRepeat<R, P> rule(String ruleName, ASTNodeConsumer consumer)
    {
        children.add(new Step.Rule(ruleName, consumer));
        return this;
    }

    @Override
    public <NR extends ASTDefinitionStepTemplate<NR>> ASTDefinitionTemplateRepeat<NR, ASTDefinitionTemplateRepeat<R, P>> repeat(ASTAction initializer)
    {
        return new ASTDefinitionTemplateRepeat<>(initializer, this);
    }

    @Override
    public <O extends ASTDefinitionStepTemplate<O>> ASTDefinitionTemplateOptional<O, ASTDefinitionTemplateRepeat<R, P>> optional()
    {
        return new ASTDefinitionTemplateOptional<>(this);
    }

    @Override
    public <C extends ASTDefinitionStepTemplate<C>> ASTDefinitionTemplateChoice<C, ASTDefinitionTemplateRepeat<R, P>> choice()
    {
        return new ASTDefinitionTemplateChoice<>(this);
    }

    /// Ends this repeat template and returns to the parent template.
    ///
    /// @return The parent template
    public P stopRepeat()
    {
        switch (parent)
        {
            case ASTDefinitionTemplate astDefTemplate ->
                    astDefTemplate.definition.addStep(new Step.Repeat(children, initializer));
            case ASTDefinitionTemplateRepeat<?, ?> astRepeatTemplate ->
                    astRepeatTemplate.children.add(new Step.Repeat(children, initializer));
            case ASTDefinitionTemplateOptional<?, ?> astOptionalTemplate ->
                    astOptionalTemplate.children.add(new Step.Repeat(children, initializer));
            case ASTDefinitionTemplateChoice<?, ?> astChoiceTemplate ->
                    astChoiceTemplate.currentAlternative.add(new Step.Repeat(children, initializer));
            case null, default ->
            {
            }
        }
        return parent;
    }
}
