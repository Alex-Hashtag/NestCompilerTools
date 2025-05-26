package org.nest.ast;

import org.nest.ast.functional.ASTAction;
import org.nest.ast.functional.ASTNodeConsumer;
import org.nest.ast.functional.TokenAction;
import org.nest.ast.state.Step;

import java.util.ArrayList;
import java.util.List;

/**
 * Template for defining a choice between alternative steps in an AST rule.
 * The Choice template allows defining multiple alternative paths, where the first matching path is chosen.
 */
public class ASTDefinitionTemplateChoice
{
    ASTDefinitionTemplate astDefinitionTemplateCaller;
    List<List<Step>> alternatives;
    List<Step> currentAlternative;

    ASTDefinitionTemplateChoice(ASTDefinitionTemplate astDefinitionTemplate)
    {
        this.astDefinitionTemplateCaller = astDefinitionTemplate;
        this.alternatives = new ArrayList<>();
        this.currentAlternative = new ArrayList<>();
        this.alternatives.add(this.currentAlternative);
    }

    public ASTDefinitionTemplateChoice keyword(String value, TokenAction action)
    {
        currentAlternative.add(new Step.Keyword(value, action));
        return this;
    }

    public ASTDefinitionTemplateChoice operator(String value, TokenAction action)
    {
        currentAlternative.add(new Step.Operator(value, action));
        return this;
    }

    public ASTDefinitionTemplateChoice delimiter(String value, TokenAction action)
    {
        currentAlternative.add(new Step.Delimiter(value, action));
        return this;
    }

    public ASTDefinitionTemplateChoice identifier(String type, TokenAction action)
    {
        currentAlternative.add(new Step.Identifier(type, action));
        return this;
    }

    public ASTDefinitionTemplateChoice literal(String type, TokenAction action)
    {
        currentAlternative.add(new Step.Literal(type, action));
        return this;
    }

    public ASTDefinitionTemplateChoice rule(String ruleName, ASTNodeConsumer consumer)
    {
        currentAlternative.add(new Step.Rule(ruleName, consumer));
        return this;
    }

    public ASTDefinitionTemplateChoice or()
    {
        // Start a new alternative
        this.currentAlternative = new ArrayList<>();
        this.alternatives.add(this.currentAlternative);
        return this;
    }

    public ASTDefinitionTemplate endChoice()
    {
        this.astDefinitionTemplateCaller.definition.addStep(new Step.Choice(alternatives));
        return this.astDefinitionTemplateCaller;
    }
}
