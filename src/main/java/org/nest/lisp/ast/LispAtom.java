package org.nest.lisp.ast;


public sealed interface LispAtom extends LispNode permits LispAtom.LispBoolean, LispAtom.LispCharacter, LispAtom.LispKeyword, LispAtom.LispNil, LispAtom.LispNumber, LispAtom.LispString, LispAtom.LispSymbol
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
        public static final LispBoolean TRUE = new LispBoolean(true);
        public static final LispBoolean FALSE = new LispBoolean(false);
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
