package org.nest.lisp.ast;

public record LispList(List<LispNode> elements) implements LispNode {}