package org.nest.ast;

import org.nest.ast.functional.ASTAction;
import org.nest.ast.functional.ASTNodeConsumer;
import org.nest.ast.functional.ASTNodeSupplier;
import org.nest.ast.functional.TokenAction;
import org.nest.ast.state.Definition;
import org.nest.ast.state.Step;


public class ASTDefinitionTemplate
{
    ASTRuleTemplate astRuleTemplateCaller;

    Definition definition;

    ASTDefinitionTemplate(String name, ASTRuleTemplate astRuleTemplate)
    {
        this.astRuleTemplateCaller = astRuleTemplate;
        definition = new Definition(name);
    }

    public ASTDefinitionTemplate keyword(String value, TokenAction action)
    {
        definition.addStep(new Step.Keyword(value, action));
        return this;
    }

    public ASTDefinitionTemplate operator(String value, TokenAction action)
    {
        definition.addStep(new Step.Operator(value, action));
        return this;
    }

    public ASTDefinitionTemplate delimiter(String value, TokenAction action)
    {
        definition.addStep(new Step.Delimiter(value, action));
        return this;
    }

    public ASTDefinitionTemplate identifier(String type, TokenAction action)
    {
        definition.addStep(new Step.Identifier(type, action));
        return this;
    }

    public ASTDefinitionTemplate literal(String type, TokenAction action)
    {
        definition.addStep(new Step.Literal(type, action));
        return this;
    }

    public ASTDefinitionTemplate rule(String ruleName, ASTNodeConsumer consumer)
    {
        definition.addStep(new Step.Rule(ruleName, consumer));
        return this;
    }

    public ASTDefinitionTemplateRepeat repeat(ASTAction initializer)
    {
        return new ASTDefinitionTemplateRepeat(initializer, this);
    }

    public ASTDefinitionTemplateOptional optional()
    {
        return new ASTDefinitionTemplateOptional(this);
    }
    
    public ASTDefinitionTemplateChoice choice()
    {
        return new ASTDefinitionTemplateChoice(this);
    }

    public ASTRuleTemplate endDefinition(ASTNodeSupplier supplier)
    {
        definition.builder = supplier;
        astRuleTemplateCaller.currentRuleBeingBuilt.addDefinition(definition);
        return astRuleTemplateCaller;
    }
}
