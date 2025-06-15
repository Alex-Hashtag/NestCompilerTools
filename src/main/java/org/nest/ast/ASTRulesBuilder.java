package org.nest.ast;

import java.util.List;


public class ASTRulesBuilder
{
    /// Defines the rules that can be present at the top level of the AST
    ///
    /// example for C:
    /// ```c
    /// struct Point
    ///{
    ///     int x, y;
    ///}
    /// int main (int argc, char *argv[])
    ///{
    ///     return 0;
    ///}
    ///```
    ///
    /// in the above code we'd have the following top rules: `struct`, `function`
    List<String> topRules;

    /// Whether to ignore comment tokens during parsing
    boolean ignoreComments = false;


    ASTRulesBuilder()
    {
    }

    /// Sets the rules that can appear at the top level of the AST. The name of the rule
    /// must match the name of a rule declared with [#startRule(String)].
    ///
    /// @param ruleNames the names of the rules that can appear at the top level.
    /// @return this builder
    public ASTRuleTemplate topRule(List<String> ruleNames)
    {
        this.topRules = ruleNames;
        return new ASTRuleTemplate(this.topRules, this.ignoreComments);
    }

    /// Sets whether comments should be ignored during parsing.
    /// When set to true, comment tokens will be skipped like whitespace.
    ///
    /// @param ignore true to ignore comments, false to treat them as normal tokens
    /// @return this builder
    public ASTRulesBuilder ignoreComments(boolean ignore)
    {
        this.ignoreComments = ignore;
        return this;
    }
}
