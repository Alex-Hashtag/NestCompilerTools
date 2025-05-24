package org.nest.ast;

public interface ASTWrapper {
    boolean hasErrors();
    Object get(); // or <T> T get(Class<T>) if generic
    List<CompilerError> getErrors();
}
