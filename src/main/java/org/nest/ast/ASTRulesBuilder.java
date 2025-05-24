package org.nest.ast;

import java.util.List;


public interface ASTRulesBuilder
{
    ASTRulesBuilder topRule(List<String> ruleNames); // entry points

    ASTRuleTemplate startRule(String name);
}
