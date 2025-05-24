package org.nest.lisp.ast;

public sealed interface LispNode permits LispAtom, LispList {}
