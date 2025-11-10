package org.nest.ast.state;

import org.nest.ast.StandaloneDefinitionTemplate;
import org.nest.ast.functional.ASTNodeSupplier;

import java.util.ArrayList;
import java.util.List;


/**
 * Represents a definition in the AST.
 */
public class Definition
{
    public final String name;
    public final List<Step> steps = new ArrayList<>();
    public ASTNodeSupplier builder;
    public String hint; // Custom hint to display on parse failure

    public Definition(String name)
    {
        this.name = name;
    }

    /// Creates a standalone definition template that can be used to build a Definition
    /// outside of the normal rule building pipeline.
    ///
    /// @param name The name for the definition
    /// @return A StandaloneDefinitionTemplate that can be used to build a Definition
    public static StandaloneDefinitionTemplate standalone(String name)
    {
        return new StandaloneDefinitionTemplate(name);
    }

    public void addStep(Step step)
    {
        steps.add(step);
    }
}
