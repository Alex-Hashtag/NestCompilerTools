package org.nest.ast.state;

import java.util.HashSet;
import java.util.Set;


public class Rule
{
    private final Set<Definition> definitions = new HashSet<>();
    private final String name;

    public Rule(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public Set<Definition> getDefinitions()
    {
        return definitions;
    }

    public void addDefinition(Definition definition)
    {
        definitions.add(definition);
    }
}
