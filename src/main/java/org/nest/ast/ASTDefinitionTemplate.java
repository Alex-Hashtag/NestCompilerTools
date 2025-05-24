package org.nest.ast;

public interface ASTDefinitionTemplate {
    ASTDefinitionTemplate keyword(String value, TokenAction action);
    ASTDefinitionTemplate operator(String value, TokenAction action);
    ASTDefinitionTemplate delimeter(String value, TokenAction action);
    ASTDefinitionTemplate identifier(String type, TokenAction action);
    ASTDefinitionTemplate literal(String type, TokenAction action);
    ASTDefinitionTemplate rule(String ruleName, ASTNodeConsumer consumer);
    ASTDefinitionTemplate definition(String key, ASTNodeConsumer consumer);
    ASTDefinitionTemplateRepeat repeat(ASTAction initializer);
    ASTDefinitionTemplateOptional optional();
    ASTRuleTemplate endDefinition(ASTNodeSupplier supplier);
}
