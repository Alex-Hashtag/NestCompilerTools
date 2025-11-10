package org.nest.lisp;

import org.nest.lisp.ast.LispAtom;
import org.nest.lisp.ast.LispList;
import org.nest.lisp.ast.LispNode;


/// Utility class for formatting Lisp values in a user-friendly,
/// Lisp-like representation for display in the REPL and examples.
public class LispPrinter
{

    /// Formats a Lisp value as a string in a more readable format.
    ///
    /// @param value The Lisp value to format
    /// @return A human-readable string representation
    public static String format(LispNode value)
    {
        return switch (value)
        {
            case null -> "nil";
            case LispAtom.LispNil _ -> "nil";
            case LispAtom.LispNumber lispNumber ->
            {
                String numStr = lispNumber.value();
                yield numStr.endsWith(".0") ? numStr.substring(0, numStr.length() - 2) : numStr;
            }
            case LispAtom.LispString lispString -> "\"" + lispString.value() + "\"";
            case LispAtom.LispBoolean lispBoolean -> lispBoolean.value() ? "#t" : "#f";
            case LispAtom.LispSymbol lispSymbol -> lispSymbol.name();
            case LispAtom.LispCharacter lispChar -> "#\\" + lispChar.value();
            case LispAtom.LispKeyword lispKeyword -> ":" + lispKeyword.name();
            case LispList lispList ->
            {
                StringBuilder sb = new StringBuilder("(");
                boolean first = true;
                for (LispNode element : lispList.elements())
                {
                    if (!first)
                    {
                        sb.append(" ");
                    }
                    sb.append(format(element));
                    first = false;
                }
                sb.append(")");
                yield sb.toString();
            }
            default -> value.toString();
        };
    }
}
