package org.nest.lisp.ast;

import java.util.List;


public record LispList(List<LispNode> elements) implements LispNode {}