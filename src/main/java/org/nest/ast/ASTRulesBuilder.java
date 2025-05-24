package org.nest.ast;

public interface ASTRulesBuilder {
    ASTRulesBuilder topRule(List<String> ruleNames); // entry points
    ASTRuleTemplate startRule(String name);
}
