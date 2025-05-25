package org.nest.ast;

import org.nest.ast.functional.ASTAction;
import org.nest.ast.functional.ASTNodeConsumer;
import org.nest.ast.functional.TokenAction;


public interface ASTDefinitionTemplateOptional
{
    ASTDefinitionTemplate keyword(String value, TokenAction action);

    ASTDefinitionTemplate operator(String value, TokenAction action);

    ASTDefinitionTemplate delimeter(String value, TokenAction action);

    ASTDefinitionTemplate identifier(String type, TokenAction action);

    ASTDefinitionTemplate literal(String type, TokenAction action);

    ASTDefinitionTemplate rule(String ruleName, ASTNodeConsumer consumer);

    ASTDefinitionTemplate otherwise(ASTAction fallback);
}
