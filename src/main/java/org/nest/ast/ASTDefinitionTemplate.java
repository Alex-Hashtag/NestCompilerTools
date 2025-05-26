package org.nest.ast;

import org.nest.ast.functional.ASTAction;
import org.nest.ast.functional.ASTNodeConsumer;
import org.nest.ast.functional.ASTNodeSupplier;
import org.nest.ast.functional.TokenAction;
import org.nest.ast.state.Definition;
import org.nest.ast.state.Step;

/// ASTDefinitionTemplate is a template class used to define a definition in the Abstract Syntax Tree (AST).
/// It provides methods to add steps to the definition, such as keywords, operators, delimiters, identifiers, literals, and rules.
/// It also provides methods to create nested template constructs, such as repeats, optionals, and choices.
public class ASTDefinitionTemplate implements ASTDefinitionStepTemplate<ASTDefinitionTemplate>
{
    /// The ASTRuleTemplate that this definition belongs to.
    ASTRuleTemplate astRuleTemplateCaller;

    /// The definition being built.
    Definition definition;

    /// Constructs a new ASTDefinitionTemplate with the given name and ASTRuleTemplate.
    ///
    /// @param name The name of the definition.
    /// @param astRuleTemplate The ASTRuleTemplate that this definition belongs to.
    ASTDefinitionTemplate(String name, ASTRuleTemplate astRuleTemplate)
    {
        this.astRuleTemplateCaller = astRuleTemplate;
        definition = new Definition(name);
    }

    /// Adds a keyword step to the definition.
    ///
    /// @param value The value of the keyword.
    /// @param action The action to perform when the keyword is encountered.
    /// @return This ASTDefinitionTemplate.
    @Override
    public ASTDefinitionTemplate keyword(String value, TokenAction action)
    {
        definition.addStep(new Step.Keyword(value, action));
        return this;
    }

    /// Adds an operator step to the definition.
    ///
    /// @param value The value of the operator.
    /// @param action The action to perform when the operator is encountered.
    /// @return This ASTDefinitionTemplate.
    @Override
    public ASTDefinitionTemplate operator(String value, TokenAction action)
    {
        definition.addStep(new Step.Operator(value, action));
        return this;
    }

    /// Adds a delimiter step to the definition.
    ///
    /// @param value The value of the delimiter.
    /// @param action The action to perform when the delimiter is encountered.
    /// @return This ASTDefinitionTemplate.
    @Override
    public ASTDefinitionTemplate delimiter(String value, TokenAction action)
    {
        definition.addStep(new Step.Delimiter(value, action));
        return this;
    }

    /// Adds an identifier step to the definition.
    ///
    /// @param type The type of the identifier.
    /// @param action The action to perform when the identifier is encountered.
    /// @return This ASTDefinitionTemplate.
    @Override
    public ASTDefinitionTemplate identifier(String type, TokenAction action)
    {
        definition.addStep(new Step.Identifier(type, action));
        return this;
    }

    /// Adds a literal step to the definition.
    ///
    /// @param type The type of the literal.
    /// @param action The action to perform when the literal is encountered.
    /// @return This ASTDefinitionTemplate.
    @Override
    public ASTDefinitionTemplate literal(String type, TokenAction action)
    {
        definition.addStep(new Step.Literal(type, action));
        return this;
    }

    /// Adds a rule step to the definition.
    ///
    /// @param ruleName The name of the rule.
    /// @param consumer The consumer to use when the rule is encountered.
    /// @return This ASTDefinitionTemplate.
    @Override
    public ASTDefinitionTemplate rule(String ruleName, ASTNodeConsumer consumer)
    {
        definition.addStep(new Step.Rule(ruleName, consumer));
        return this;
    }

    /// Creates a new repeat template construct.
    ///
    /// @param initializer The initializer for the repeat.
    /// @return A new ASTDefinitionTemplateRepeat.
    @Override
    public <R extends ASTDefinitionStepTemplate<R>> ASTDefinitionTemplateRepeat<R, ASTDefinitionTemplate> repeat(ASTAction initializer)
    {
        return new ASTDefinitionTemplateRepeat<>(initializer, this);
    }

    /// Creates a new optional template construct.
    ///
    /// @return A new ASTDefinitionTemplateOptional.
    @Override
    public <O extends ASTDefinitionStepTemplate<O>> ASTDefinitionTemplateOptional<O, ASTDefinitionTemplate> optional()
    {
        return new ASTDefinitionTemplateOptional<>(this);
    }
    
    /// Creates a new choice template construct.
    ///
    /// @return A new ASTDefinitionTemplateChoice.
    @Override
    public <C extends ASTDefinitionStepTemplate<C>> ASTDefinitionTemplateChoice<C, ASTDefinitionTemplate> choice()
    {
        return new ASTDefinitionTemplateChoice<>(this);
    }

    /// Ends the definition and adds it to the ASTRuleTemplate.
    ///
    /// @param supplier The supplier to use when the definition is encountered.
    /// @return The ASTRuleTemplate.
    public ASTRuleTemplate endDefinition(ASTNodeSupplier supplier)
    {
        definition.builder = supplier;
        astRuleTemplateCaller.currentRuleBeingBuilt.addDefinition(definition);
        return astRuleTemplateCaller;
    }
}
