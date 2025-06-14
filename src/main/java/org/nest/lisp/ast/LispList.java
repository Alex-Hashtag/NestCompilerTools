package org.nest.lisp.ast;

import org.nest.tokenization.Coordinates;

import java.util.ArrayList;
import java.util.List;

/**
 * A LispList is a list of LispNodes.
 */
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
}