package org.nest.ast;

import org.nest.errors.CompilerError;

import java.util.List;


public interface ASTWrapper
{
    boolean hasErrors();

    Object get(); // or <T> T get(Class<T>) if generic

    List<CompilerError> getErrors();
}
