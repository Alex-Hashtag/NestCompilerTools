package org.nest.ast;

public interface ASTRuleTemplate
{
    ASTDefinitionTemplate addDefinition();                  // unnamed

    ASTDefinitionTemplate addDefinition(String name);       // named

    ASTRuleTemplate startRule(String name);                 // ✅ allows defining next rule

    ASTRules build();                                       // finishes the rule chain
}