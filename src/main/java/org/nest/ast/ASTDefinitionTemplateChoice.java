package org.nest.ast;

import org.nest.ast.functional.ASTAction;
import org.nest.ast.functional.ASTNodeConsumer;
import org.nest.ast.functional.TokenAction;
import org.nest.ast.state.Step;

import java.util.ArrayList;
import java.util.List;

/// Template for defining a choice between alternative steps in an AST rule.
/// The Choice template allows defining multiple alternative paths, where the first matching path is chosen.
///
/// @param <C> The type parameter for this choice template (self-type)
/// @param <P> The type of the parent template
public class ASTDefinitionTemplateChoice<C extends ASTDefinitionStepTemplate<C>, P extends ASTDefinitionStepTemplate<P>>
    implements ASTDefinitionStepTemplate<ASTDefinitionTemplateChoice<C, P>>
{
    P parent;
    List<List<Step>> alternatives;
    List<Step> currentAlternative;

    ASTDefinitionTemplateChoice(P parent)
    {
        this.parent = parent;
        this.alternatives = new ArrayList<>();
        this.currentAlternative = new ArrayList<>();
        this.alternatives.add(this.currentAlternative);
    }

    @Override
    public ASTDefinitionTemplateChoice<C, P> keyword(String value, TokenAction action)
    {
        currentAlternative.add(new Step.Keyword(value, action));
        return this;
    }

    @Override
    public ASTDefinitionTemplateChoice<C, P> operator(String value, TokenAction action)
    {
        currentAlternative.add(new Step.Operator(value, action));
        return this;
    }

    @Override
    public ASTDefinitionTemplateChoice<C, P> delimiter(String value, TokenAction action)
    {
        currentAlternative.add(new Step.Delimiter(value, action));
        return this;
    }

    @Override
    public ASTDefinitionTemplateChoice<C, P> identifier(String type, TokenAction action)
    {
        currentAlternative.add(new Step.Identifier(type, action));
        return this;
    }

    @Override
    public ASTDefinitionTemplateChoice<C, P> literal(String type, TokenAction action)
    {
        currentAlternative.add(new Step.Literal(type, action));
        return this;
    }

    @Override
    public ASTDefinitionTemplateChoice<C, P> rule(String ruleName, ASTNodeConsumer consumer)
    {
        currentAlternative.add(new Step.Rule(ruleName, consumer));
        return this;
    }

    @Override
    public <R extends ASTDefinitionStepTemplate<R>> ASTDefinitionTemplateRepeat<R, ASTDefinitionTemplateChoice<C, P>> repeat(ASTAction initializer)
    {
        return new ASTDefinitionTemplateRepeat<>(initializer, this);
    }

    @Override
    public <O extends ASTDefinitionStepTemplate<O>> ASTDefinitionTemplateOptional<O, ASTDefinitionTemplateChoice<C, P>> optional()
    {
        return new ASTDefinitionTemplateOptional<>(this);
    }

    @Override
    public <NC extends ASTDefinitionStepTemplate<NC>> ASTDefinitionTemplateChoice<NC, ASTDefinitionTemplateChoice<C, P>> choice()
    {
        return new ASTDefinitionTemplateChoice<>(this);
    }

    /// Starts a new alternative in this choice template.
    ///
    /// @return This choice template
    public ASTDefinitionTemplateChoice<C, P> or()
    {
        // Start a new alternative
        this.currentAlternative = new ArrayList<>();
        this.alternatives.add(this.currentAlternative);
        return this;
    }

    /// Ends this choice template and returns to the parent template.
    ///
    /// @return The parent template
    public P endChoice()
    {
        switch (parent)
        {
            case ASTDefinitionTemplate astDefTemplate ->
                    astDefTemplate.definition.addStep(new Step.Choice(alternatives));
            case ASTDefinitionTemplateRepeat<?, ?> astRepeatTemplate ->
                    astRepeatTemplate.children.add(new Step.Choice(alternatives));
            case ASTDefinitionTemplateOptional<?, ?> astOptionalTemplate ->
                    astOptionalTemplate.children.add(new Step.Choice(alternatives));
            case ASTDefinitionTemplateChoice<?, ?> astChoiceTemplate ->
                    astChoiceTemplate.currentAlternative.add(new Step.Choice(alternatives));
            case null, default ->
            {
            }
        }
        return parent;
    }
}
