package org.nest.lisp.ast;

import org.nest.tokenization.Coordinates;

public sealed interface LispNode permits LispAtom, LispList
{
}
