package org.nest.ast;

public interface ASTDefinitionTemplateOptional {
    ASTDefinitionTemplateOptional keyword(String value, TokenAction action);
    ASTDefinitionTemplateOptional operator(String value, TokenAction action);
    ASTDefinitionTemplateOptional delimeter(String value, TokenAction action);
    ASTDefinitionTemplateOptional identifier(String type, TokenAction action);
    ASTDefinitionTemplateOptional literal(String type, TokenAction action);
    ASTDefinitionTemplateOptional rule(String ruleName, ASTNodeConsumer consumer);
}

