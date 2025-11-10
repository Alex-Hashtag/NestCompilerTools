package org.nest.ast;

import org.nest.ast.functional.ASTAction;
import org.nest.ast.functional.ASTNodeConsumer;
import org.nest.ast.functional.ASTNodeSupplier;
import org.nest.ast.functional.TokenAction;
import org.nest.ast.state.Definition;
import org.nest.ast.state.Step;

/**
 * A standalone version of ASTDefinitionTemplate that can be used to build a Definition
 * outside of the normal rule building pipeline.
 */
public final class StandaloneDefinitionTemplate implements ASTDefinitionStepTemplate<StandaloneDefinitionTemplate>
{
    /** The definition being built. */
    private final Definition definition;

    /**
     * Constructs a new StandaloneDefinitionTemplate with the given name.
     *
     * @param name The name of the definition.
     */
    public StandaloneDefinitionTemplate(String name)
    {
        definition = new Definition(name);
    }

    /**
     * Adds a keyword step to the definition.
     *
     * @param value The value of the keyword.
     * @param action The action to perform when the keyword is encountered.
     * @return This StandaloneDefinitionTemplate.
     */
    @Override
    public StandaloneDefinitionTemplate keyword(String value, TokenAction action)
    {
        definition.addStep(new Step.Keyword(value, action));
        return this;
    }

    /**
     * Adds an operator step to the definition.
     *
     * @param value The value of the operator.
     * @param action The action to perform when the operator is encountered.
     * @return This StandaloneDefinitionTemplate.
     */
    @Override
    public StandaloneDefinitionTemplate operator(String value, TokenAction action)
    {
        definition.addStep(new Step.Operator(value, action));
        return this;
    }

    /**
     * Adds a delimiter step to the definition.
     *
     * @param value The value of the delimiter.
     * @param action The action to perform when the delimiter is encountered.
     * @return This StandaloneDefinitionTemplate.
     */
    @Override
    public StandaloneDefinitionTemplate delimiter(String value, TokenAction action)
    {
        definition.addStep(new Step.Delimiter(value, action));
        return this;
    }

    /**
     * Adds an identifier step to the definition.
     *
     * @param type The type of the identifier.
     * @param action The action to perform when the identifier is encountered.
     * @return This StandaloneDefinitionTemplate.
     */
    @Override
    public StandaloneDefinitionTemplate identifier(String type, TokenAction action)
    {
        definition.addStep(new Step.Identifier(type, action));
        return this;
    }

    /**
     * Adds a literal step to the definition.
     *
     * @param type The type of the literal.
     * @param action The action to perform when the literal is encountered.
     * @return This StandaloneDefinitionTemplate.
     */
    @Override
    public StandaloneDefinitionTemplate literal(String type, TokenAction action)
    {
        definition.addStep(new Step.Literal(type, action));
        return this;
    }

    /**
     * Adds a rule step to the definition.
     *
     * @param ruleName The name of the rule.
     * @param consumer The consumer to use when the rule is encountered.
     * @return This StandaloneDefinitionTemplate.
     */
    @Override
    public StandaloneDefinitionTemplate rule(String ruleName, ASTNodeConsumer consumer)
    {
        definition.addStep(new Step.Rule(ruleName, consumer));
        return this;
    }

    /**
     * Creates a new repeat template construct.
     *
     * @param initializer The initializer for the repeat.
     * @return A new ASTDefinitionTemplateRepeat.
     */
    @Override
    public <R extends ASTDefinitionStepTemplate<R>> ASTDefinitionTemplateRepeat<R, StandaloneDefinitionTemplate> repeat(ASTAction initializer)
    {
        return new ASTDefinitionTemplateRepeat<>(initializer, this);
    }

    /**
     * Creates a new optional template construct.
     *
     * @return A new ASTDefinitionTemplateOptional.
     */
    @Override
    public <O extends ASTDefinitionStepTemplate<O>> ASTDefinitionTemplateOptional<O, StandaloneDefinitionTemplate> optional()
    {
        return new ASTDefinitionTemplateOptional<>(this);
    }
    
    /**
     * Creates a new choice template construct.
     *
     * @return A new ASTDefinitionTemplateChoice.
     */
    @Override
    public <C extends ASTDefinitionStepTemplate<C>> ASTDefinitionTemplateChoice<C, StandaloneDefinitionTemplate> choice()
    {
        return new ASTDefinitionTemplateChoice<>(this);
    }

    /**
     * Builds and returns the definition.
     *
     * @param supplier The supplier to use when the definition is encountered.
     * @return The built Definition object.
     */
    public Definition buildDefinition(ASTNodeSupplier supplier)
    {
        definition.builder = supplier;
        return definition;
    }

    /**
     * Builds and returns the definition with a custom error hint.
     *
     * @param supplier The supplier to use when the definition is encountered.
     * @param hint A custom hint to display when parsing fails for this definition.
     * @return The built Definition object.
     */
    public Definition buildDefinition(ASTNodeSupplier supplier, String hint)
    {
        definition.builder = supplier;
        definition.hint = hint;
        return definition;
    }
}
