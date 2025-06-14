package org.nest.ast.state;

import org.nest.ast.functional.ASTNodeSupplier;

import java.util.ArrayList;
import java.util.List;


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

    public void addStep(Step step)
    {
        steps.add(step);
    }
}
