package org.nest.ast;

import org.nest.ast.functional.ASTAction;
import org.nest.ast.functional.ASTNodeConsumer;
import org.nest.ast.functional.ASTNodeSupplier;
import org.nest.ast.functional.TokenAction;

/// Common interface for all AST definition template types.
/// This enables nesting of different template types (Choice, Optional, Repeat)
/// while maintaining the fluent API style.
///
/// @param <T> The implementing type for method chaining
public interface ASTDefinitionStepTemplate<T extends ASTDefinitionStepTemplate<T>> {
    
    /// Adds a keyword step to this template.
    ///
    /// @param value The keyword string
    /// @param action The action to perform on token match
    /// @return This template for method chaining
    T keyword(String value, TokenAction action);
    
    /// Adds an operator step to this template.
    ///
    /// @param value The operator string
    /// @param action The action to perform on token match
    /// @return This template for method chaining
    T operator(String value, TokenAction action);
    
    /// Adds a delimiter step to this template.
    ///
    /// @param value The delimiter string
    /// @param action The action to perform on token match
    /// @return This template for method chaining
    T delimiter(String value, TokenAction action);
    
    /// Adds an identifier step to this template.
    ///
    /// @param type The identifier type
    /// @param action The action to perform on token match
    /// @return This template for method chaining
    T identifier(String type, TokenAction action);
    
    /// Adds a literal step to this template.
    ///
    /// @param type The literal type
    /// @param action The action to perform on token match
    /// @return This template for method chaining
    T literal(String type, TokenAction action);
    
    /// Adds a rule step to this template.
    ///
    /// @param ruleName The name of the rule to apply
    /// @param consumer The consumer to handle the rule match
    /// @return This template for method chaining
    T rule(String ruleName, ASTNodeConsumer consumer);
    
    /// Creates a new repeat template with this template as its parent.
    ///
    /// @param initializer The initializer action for the repeat
    /// @return A new repeat template
    <R extends ASTDefinitionStepTemplate<R>> ASTDefinitionTemplateRepeat<R, T> repeat(ASTAction initializer);
    
    /// Creates a new optional template with this template as its parent.
    ///
    /// @return A new optional template
    <O extends ASTDefinitionStepTemplate<O>> ASTDefinitionTemplateOptional<O, T> optional();
    
    /// Creates a new choice template with this template as its parent.
    ///
    /// @return A new choice template
    <C extends ASTDefinitionStepTemplate<C>> ASTDefinitionTemplateChoice<C, T> choice();
}
