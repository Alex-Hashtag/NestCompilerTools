package org.nest.lisp.ast;

public sealed interface LispAtom extends LispNode permits LispAtom.LispSymbol, LispAtom.LispNumber, LispAtom.LispString, LispAtom.LispBoolean, LispAtom.LispNil, LispAtom.LispCharacter, LispAtom.LispKeyword
{
    record LispSymbol(String name) implements LispAtom
    {
    }

    record LispNumber(String value) implements LispAtom
    {
    }

    record LispString(String value) implements LispAtom
    {
    }

    record LispBoolean(boolean value) implements LispAtom
    {
    }

    record LispCharacter(char value) implements LispAtom
    {
    }

    record LispKeyword(String name) implements LispAtom
    {
    }

    final class LispNil implements LispAtom
    {
        public static final LispNil INSTANCE = new LispNil();

        private LispNil()
        {
        }

        public String toString()
        {
            return "nil";
        }
    }
}
