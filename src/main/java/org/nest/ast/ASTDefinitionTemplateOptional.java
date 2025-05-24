package org.nest.ast;

import org.nest.ast.functional.ASTAction;
import org.nest.ast.functional.ASTNodeConsumer;
import org.nest.ast.functional.TokenAction;


public interface ASTDefinitionTemplateOptional
{
    ASTDefinitionTemplateOptional keyword(String value, TokenAction action);

    ASTDefinitionTemplateOptional operator(String value, TokenAction action);

    ASTDefinitionTemplateOptional delimeter(String value, TokenAction action);

    ASTDefinitionTemplateOptional identifier(String type, TokenAction action);

    ASTDefinitionTemplateOptional literal(String type, TokenAction action);

    ASTDefinitionTemplateOptional rule(String ruleName, ASTNodeConsumer consumer);

    ASTDefinitionTemplate otherwise(ASTAction fallback);
}
