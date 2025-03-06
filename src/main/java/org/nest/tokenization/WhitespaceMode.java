package org.nest.tokenization;

public enum WhitespaceMode
{
    IGNORE,         // Whitespace is ignored (C, Java)
    SIGNIFICANT,    // Whitespace is a valid separator (Haskell, Lisp)
    INDENTATION     // Whitespace is indentation-based (Python, YAML)
}
