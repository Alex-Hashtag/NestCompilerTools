package org.nest.ast;

import org.nest.ast.functional.ASTAction;
import org.nest.ast.functional.ASTNodeConsumer;
import org.nest.ast.functional.TokenAction;
import org.nest.ast.state.Step;

import java.util.ArrayList;
import java.util.List;


public class ASTDefinitionTemplateOptional
{
    ASTDefinitionTemplate astDefinitionTemplateCaller;

    List<Step> children;

    ASTDefinitionTemplateOptional(ASTDefinitionTemplate astDefinitionTemplate)
    {
        this.astDefinitionTemplateCaller = astDefinitionTemplate;
        children = new ArrayList<>();
    }

    public ASTDefinitionTemplateOptional keyword(String value, TokenAction action)
    {
        children.add(new Step.Keyword(value, action));
        return this;
    }

    public ASTDefinitionTemplateOptional operator(String value, TokenAction action)
    {
        children.add(new Step.Operator(value, action));
        return this;
    }

    public ASTDefinitionTemplateOptional delimiter(String value, TokenAction action)
    {
        children.add(new Step.Delimiter(value, action));
        return this;
    }

    public ASTDefinitionTemplateOptional identifier(String type, TokenAction action)
    {
        children.add(new Step.Identifier(type, action));
        return this;
    }

    public ASTDefinitionTemplateOptional literal(String type, TokenAction action)
    {
        children.add(new Step.Literal(type, action));
        return this;
    }

    public ASTDefinitionTemplateOptional rule(String ruleName, ASTNodeConsumer consumer)
    {
        children.add(new Step.Rule(ruleName, consumer));
        return this;
    }

    public ASTDefinitionTemplate otherwise(ASTAction fallback)
    {
        this.astDefinitionTemplateCaller.definition.addStep(new Step.Optional(children, fallback));
        return this.astDefinitionTemplateCaller;
    }
}
