package org.nest.ast;

import org.nest.ast.functional.ASTAction;
import org.nest.ast.functional.ASTNodeConsumer;
import org.nest.ast.functional.TokenAction;
import org.nest.ast.state.Step;

import java.util.ArrayList;
import java.util.List;


public class ASTDefinitionTemplateRepeat
{
    ASTDefinitionTemplate astDefinitionTemplateCaller;

    List<Step> children;
    ASTAction initializer;

    ASTDefinitionTemplateRepeat(ASTAction initializer, ASTDefinitionTemplate astDefinitionTemplateCaller)
    {
        this.astDefinitionTemplateCaller = astDefinitionTemplateCaller;
        this.children = new ArrayList<>();
        this.initializer = initializer;
    }


    public ASTDefinitionTemplateRepeat keyword(String value, TokenAction action)
    {
        children.add(new Step.Keyword(value, action));
        return this;
    }

    public ASTDefinitionTemplateRepeat operator(String value, TokenAction action)
    {
        children.add(new Step.Operator(value, action));
        return this;
    }

    public ASTDefinitionTemplateRepeat delimiter(String value, TokenAction action)
    {
        children.add(new Step.Delimiter(value, action));
        return this;
    }

    public ASTDefinitionTemplateRepeat identifier(String type, TokenAction action)
    {
        children.add(new Step.Identifier(type, action));
        return this;
    }

    public ASTDefinitionTemplateRepeat literal(String type, TokenAction action)
    {
        children.add(new Step.Literal(type, action));
        return this;
    }

    public ASTDefinitionTemplateRepeat rule(String ruleName, ASTNodeConsumer consumer)
    {
        children.add(new Step.Rule(ruleName, consumer));
        return this;
    }

    public ASTDefinitionTemplate stopRepeat()
    {
        astDefinitionTemplateCaller.definition.addStep(new Step.Repeat(children, initializer));
        return astDefinitionTemplateCaller;
    }
}

