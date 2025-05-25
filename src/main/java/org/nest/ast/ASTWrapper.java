package org.nest.ast;

import java.util.List;


/**
 *
 */
public class ASTWrapper
{
    private final List<Object> astNodes;
    private final boolean hasErrors;

    ASTWrapper(List<Object> astNode)
    {
        this.astNodes = astNode;
        this.hasErrors = false;
    }

    ASTWrapper()
    {
        this.astNodes = null;
        this.hasErrors = true;
    }

    public boolean hasErrors()
    {
        return hasErrors;
    }

    public List<Object> get()
    {
        return this.astNodes;
    }

}
