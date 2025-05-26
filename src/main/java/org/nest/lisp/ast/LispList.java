package org.nest.lisp.ast;

import java.util.ArrayList;
import java.util.List;


public record LispList(List<LispNode> elements) implements LispNode
{
    public LispList
    {
        // Ensure elements is never null, using an empty list instead
        if (elements == null)
        {
            elements = new ArrayList<>();
        }
    }

    // Convenience constructor for empty list
    public LispList()
    {
        this(new ArrayList<>());
    }
}