package org.nest.lisp.ast;

public sealed interface LispAtom extends LispNode permits LispAtom.LispSymbol, LispAtom.LispNumber, LispAtom.LispString, LispAtom.LispBoolean, LispAtom.LispNil
{
    public record LispSymbol(String name) implements LispAtom {}
    public record LispNumber(String value) implements LispAtom {}
    public record LispString(String value) implements LispAtom {}
    public record LispBoolean(boolean value) implements LispAtom {}
    public final class LispNil implements LispAtom {
        public static final LispNil INSTANCE = new LispNil();
        private LispNil() {}
        public String toString() { return "nil"; }
    }
}
