package org.nest.ast;

import org.nest.errors.ErrorManager;

import java.io.PrintStream;
import java.util.List;


/**
 *
 */
public class ASTWrapper
{
    private final List<Object> astNodes;
    private final ErrorManager errorManager;

    ASTWrapper(List<Object> astNode, ErrorManager errorManager)
    {
        this.astNodes = astNode;
        this.errorManager = errorManager;
    }

    public boolean hasErrors()
    {
        return errorManager.hasErrors();
    }

    public void printErrors(PrintStream out)
    {
        errorManager.printReports(out);
    }

    public List<Object> get()
    {
        return this.astNodes;
    }

}
