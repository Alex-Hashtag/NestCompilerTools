package org.nest.ast;

import org.nest.ast.state.Rule;

import java.util.ArrayList;
import java.util.List;

/// ASTRuleTemplate is used to build a rule chain.
/// It is used to keep track of the top rules while the pipeline is being built.
public class ASTRuleTemplate
{
    /// Used to keep track of the top rules while the pipleline is being built
    List<String> topRules;
    /// Flag to ignore comments during parsing
    boolean ignoreComments;

    List<Rule> rules = new ArrayList<>();
    Rule currentRuleBeingBuilt;

    ASTRuleTemplate(String name, List<String> topRules, boolean ignoreComments)
    {
        currentRuleBeingBuilt = new Rule(name);
        this.topRules = topRules;
        this.ignoreComments = ignoreComments;
    }

    public ASTDefinitionTemplate addDefinition() // unnamed
    {
        return addDefinition("");
    }

    public ASTDefinitionTemplate addDefinition(String name) // named
    {
        return new ASTDefinitionTemplate(name, this);
    }

    public ASTRuleTemplate startRule(String name) // ✅ allows defining next rule
    {
        rules.add(currentRuleBeingBuilt);
        currentRuleBeingBuilt = new Rule(name);
        return this;
    }

    public ASTRules build() // finishes the rule chain
    {
        rules.add(currentRuleBeingBuilt);
        return new ASTRules(topRules, rules, ignoreComments);
    }
}