package org.nest.lisp.ast;

import org.nest.errors.ErrorManager;
import org.nest.lisp.LispInterpreter;
import org.nest.lisp.LispPrinter;

import java.util.*;
import java.util.function.BiFunction;


/**
 * Manages variable bindings, function definitions, and macros for a Lisp environment.
 * Provides scoping and a hierarchical environment structure.
 */
public class LispDefinitions
{
    // Parent scope for hierarchical lookups
    private final LispDefinitions parent;

    // Various definition types
    private final Map<String, LispNode> variables = new HashMap<>();
    private final Map<String, LispFunction> functions = new HashMap<>();
    private final Map<String, LispMacro> macros = new HashMap<>();
    private final Map<String, BuiltinFunction> builtins = new HashMap<>();
    private final Random rng = new Random();

    /**
     * Creates a top-level definitions environment with no parent.
     */
    public LispDefinitions()
    {
        this(null);
    }

    /**
     * Creates a definitions environment with the specified parent.
     *
     * @param parent The parent environment for hierarchical lookups
     */
    public LispDefinitions(LispDefinitions parent)
    {
        this.parent = parent;
        if (parent == null)
        {
            // Initialize built-in functions only at the root level
            initializeBuiltins();
        }
    }

    /**
     * Creates a new child scope with this as the parent.
     *
     * @return A new definitions environment with this as the parent
     */
    public LispDefinitions createChildScope()
    {
        return new LispDefinitions(this);
    }

    /**
     * Defines a variable in the current scope.
     *
     * @param name  The variable name
     * @param value The variable value
     */
    public void defineVariable(String name, LispNode value)
    {
        variables.put(name, value);
    }

    /**
     * Looks up a variable in the current scope or parent scopes.
     *
     * @param name The variable name
     * @return The variable value or null if not found
     */
    public LispNode lookupVariable(String name)
    {
        if (variables.containsKey(name))
        {
            return variables.get(name);
        }

        if (parent != null)
        {
            return parent.lookupVariable(name);
        }

        return null;
    }

    /**
     * Sets the value of an existing variable in the current scope or parent scopes.
     *
     * @param name         The variable name
     * @param value        The new variable value
     * @param errorManager Error manager for reporting errors
     * @return true if the variable was found and set, false otherwise
     */
    public boolean setVariable(String name, LispNode value, ErrorManager errorManager)
    {
        if (variables.containsKey(name))
        {
            variables.put(name, value);
            return true;
        }

        if (parent != null)
        {
            return parent.setVariable(name, value, errorManager);
        }

        errorManager.error("Cannot set undefined variable: " + name, 0, 0, name, "Define the variable before setting it");
        return false;
    }

    /**
     * Defines a function in the current scope.
     *
     * @param name   The function name
     * @param params The parameter list
     * @param body   The function body
     */
    public void defineFunction(String name, List<String> params, LispNode body)
    {
        functions.put(name, new LispFunction(params, body));
    }

    /**
     * Looks up a function in the current scope or parent scopes.
     *
     * @param name The function name
     * @return The function or null if not found
     */
    public LispFunction lookupFunction(String name)
    {
        if (functions.containsKey(name))
        {
            return functions.get(name);
        }

        if (parent != null)
        {
            return parent.lookupFunction(name);
        }

        return null;
    }

    /**
     * Defines a macro in the current scope.
     *
     * @param name   The macro name
     * @param params The parameter list
     * @param body   The macro body
     */
    public void defineMacro(String name, List<String> params, LispNode body)
    {
        macros.put(name, new LispMacro(params, body));
    }

    /**
     * Looks up a macro in the current scope or parent scopes.
     *
     * @param name The macro name
     * @return The macro or null if not found
     */
    public LispMacro lookupMacro(String name)
    {
        if (macros.containsKey(name))
        {
            return macros.get(name);
        }

        if (parent != null)
        {
            return parent.lookupMacro(name);
        }

        return null;
    }

    /**
     * Looks up a built-in function by name.
     *
     * @param name The built-in function name
     * @return The built-in function or null if not found
     */
    public BuiltinFunction lookupBuiltin(String name)
    {
        if (builtins.containsKey(name))
        {
            return builtins.get(name);
        }

        if (parent != null)
        {
            return parent.lookupBuiltin(name);
        }

        return null;
    }

    /**
     * Initializes the built-in functions for the Lisp environment.
     */
    private void initializeBuiltins()
    {
        // Arithmetic operations
        builtins.put("+", (interpreter, args, errorManager) ->
        {
            double result = 0;
            for (LispNode arg : args)
            {
                if (arg instanceof LispAtom.LispNumber)
                {
                    result += Double.parseDouble(((LispAtom.LispNumber) arg).value());
                }
                else
                {
                    errorManager.error("'+' expects numeric arguments", 0, 0, arg.toString(), "Make sure all arguments are numbers");
                    return null;
                }
            }
            return new LispAtom.LispNumber(String.valueOf(result));
        });

        builtins.put("-", (interpreter, args, errorManager) ->
        {
            if (args.isEmpty())
            {
                errorManager.error("'-' expects at least one argument", 0, 0, "-", "Provide at least one argument");
                return null;
            }

            LispNode first = args.getFirst();
            if (!(first instanceof LispAtom.LispNumber))
            {
                errorManager.error("'-' expects numeric arguments", 0, 0, first.toString(), "Make sure all arguments are numbers");
                return null;
            }

            double result = Double.parseDouble(((LispAtom.LispNumber) first).value());

            if (args.size() == 1)
            {
                // Unary minus
                return new LispAtom.LispNumber(String.valueOf(-result));
            }

            // Binary subtraction
            for (int i = 1; i < args.size(); i++)
            {
                LispNode arg = args.get(i);
                if (arg instanceof LispAtom.LispNumber)
                {
                    result -= Double.parseDouble(((LispAtom.LispNumber) arg).value());
                }
                else
                {
                    errorManager.error("'-' expects numeric arguments", 0, 0, arg.toString(), "Make sure all arguments are numbers");
                    return null;
                }
            }
            return new LispAtom.LispNumber(String.valueOf(result));
        });

        builtins.put("*", (interpreter, args, errorManager) ->
        {
            double result = 1;
            for (LispNode arg : args)
            {
                if (arg instanceof LispAtom.LispNumber)
                {
                    result *= Double.parseDouble(((LispAtom.LispNumber) arg).value());
                }
                else
                {
                    errorManager.error("'*' expects numeric arguments", 0, 0, arg.toString(), "Make sure all arguments are numbers");
                    return null;
                }
            }
            return new LispAtom.LispNumber(String.valueOf(result));
        });

        builtins.put("/", (interpreter, args, errorManager) ->
        {
            if (args.isEmpty())
            {
                errorManager.error("'/' expects at least one argument", 0, 0, "/", "Provide at least one argument");
                return null;
            }

            LispNode first = args.getFirst();
            if (!(first instanceof LispAtom.LispNumber))
            {
                errorManager.error("'/' expects numeric arguments", 0, 0, first.toString(), "Make sure all arguments are numbers");
                return null;
            }

            double result = Double.parseDouble(((LispAtom.LispNumber) first).value());

            if (args.size() == 1)
            {
                // Unary division (reciprocal)
                if (result == 0)
                {
                    errorManager.error("Division by zero", 0, 0, "0", "Cannot divide by zero");
                    return null;
                }
                return new LispAtom.LispNumber(String.valueOf(1.0 / result));
            }

            // Binary division
            for (int i = 1; i < args.size(); i++)
            {
                LispNode arg = args.get(i);
                if (arg instanceof LispAtom.LispNumber)
                {
                    double divisor = Double.parseDouble(((LispAtom.LispNumber) arg).value());
                    if (divisor == 0)
                    {
                        errorManager.error("Division by zero", 0, 0, "0", "Cannot divide by zero");
                        return null;
                    }
                    result /= divisor;
                }
                else
                {
                    errorManager.error("'/' expects numeric arguments", 0, 0, arg.toString(), "Make sure all arguments are numbers");
                    return null;
                }
            }
            return new LispAtom.LispNumber(String.valueOf(result));
        });

        builtins.put("<", (interpreter, args, errorManager) -> compareNumbers(args, errorManager, (a, b) -> a < b));
        builtins.put(">", (interpreter, args, errorManager) -> compareNumbers(args, errorManager, (a, b) -> a > b));
        builtins.put("<=", (interpreter, args, errorManager) -> compareNumbers(args, errorManager, (a, b) -> a <= b));
        builtins.put(">=", (interpreter, args, errorManager) -> compareNumbers(args, errorManager, (a, b) -> a >= b));

        builtins.put("eq?", (interpreter, args, errorManager) ->
        {
            if (args.size() != 2)
            {
                errorManager.error("'eq?' expects two arguments", 0, 0, args.toString(), "Provide two expressions");
                return null;
            }
            return new LispAtom.LispBoolean(args.get(0) == args.get(1));
        });

        builtins.put("random", (interpreter, args, errorManager) ->
        {
            try
            {
                if (args.isEmpty())
                {
                    return new LispAtom.LispNumber(String.valueOf(rng.nextDouble()));
                }
                else if (args.size() == 1 && args.get(0) instanceof LispAtom.LispNumber maxArg)
                {
                    int max = (int) Double.parseDouble(maxArg.value());
                    return new LispAtom.LispNumber(String.valueOf(rng.nextInt(max)));
                }
                else if (args.size() == 2 &&
                        args.get(0) instanceof LispAtom.LispNumber minArg &&
                        args.get(1) instanceof LispAtom.LispNumber maxArg)
                {
                    int min = (int) Double.parseDouble(minArg.value());
                    int max = (int) Double.parseDouble(maxArg.value());
                    return new LispAtom.LispNumber(String.valueOf(rng.nextInt(max - min) + min));
                }
                else
                {
                    errorManager.error("'random' expects 0, 1, or 2 numeric arguments", 0, 0, args.toString(), "Pass zero, one, or two numbers");
                    return null;
                }
            } catch (Exception e)
            {
                errorManager.error("Error in 'random' function", 0, 0, e.getMessage(), "Check arguments");
                return null;
            }
        });

        builtins.put("loop", (interpreter, args, errorManager) ->
        {
            LispNode result = LispAtom.LispNil.INSTANCE;
            try
            {
                for (; ; )
                {
                    for (LispNode arg : args)
                    {
                        result = interpreter.evaluate(arg);
                    }
                }
            } catch (ReturnSignal signal)
            {
                return signal.value;
            }
        });

        builtins.put("return", (interpreter, args, errorManager) ->
        {
            LispNode value = args.isEmpty() ? LispAtom.LispNil.INSTANCE : interpreter.evaluate(args.get(0));
            throw new ReturnSignal(value);
        });


        // Comparison operations
        builtins.put("=", (interpreter, args, errorManager) ->
        {
            if (args.size() < 2)
            {
                errorManager.error("'=' expects at least two arguments", 0, 0, "=", "Provide at least two arguments");
                return null;
            }

            // Only numbers can be compared with =
            for (LispNode arg : args)
            {
                if (!(arg instanceof LispAtom.LispNumber))
                {
                    errorManager.error("'=' expects numeric arguments", 0, 0, arg.toString(), "Make sure all arguments are numbers");
                    return null;
                }
            }

            // Check if each number is equal to the next
            for (int i = 0; i < args.size() - 1; i++)
            {
                double current = Double.parseDouble(((LispAtom.LispNumber) args.get(i)).value());
                double next = Double.parseDouble(((LispAtom.LispNumber) args.get(i + 1)).value());
                if (current != next)
                {
                    return new LispAtom.LispBoolean(false);
                }
            }

            return new LispAtom.LispBoolean(true);
        });

        builtins.put("car", (interpreter, args, errorManager) ->
        {
            if (args.size() != 1 || !(args.get(0) instanceof LispList list))
            {
                errorManager.error("'car' expects a single list argument", 0, 0, args.toString(), "Pass exactly one list");
                return null;
            }
            return list.elements().isEmpty() ? LispAtom.LispNil.INSTANCE : list.elements().getFirst();
        });

        builtins.put("cdr", (interpreter, args, errorManager) ->
        {
            if (args.size() != 1 || !(args.get(0) instanceof LispList list))
            {
                errorManager.error("'cdr' expects a single list argument", 0, 0, args.toString(), "Pass exactly one list");
                return null;
            }
            return new LispList(list.elements().subList(1, list.elements().size()));
        });

        builtins.put("cons", (interpreter, args, errorManager) ->
        {
            if (args.size() != 2 || !(args.get(1) instanceof LispList list))
            {
                errorManager.error("'cons' expects two arguments: an element and a list", 0, 0, args.toString(), "Pass an element and a list");
                return null;
            }
            List<LispNode> newList = new ArrayList<>();
            newList.add(args.get(0));
            newList.addAll(list.elements());
            return new LispList(newList);
        });


        builtins.put("list", (interpreter, args, errorManager) -> new LispList(args));

        builtins.put("length", (interpreter, args, errorManager) ->
        {
            if (args.size() != 1 || !(args.get(0) instanceof LispList list))
            {
                errorManager.error("'length' expects one list argument", 0, 0, args.toString(), "Pass a single list");
                return null;
            }
            return new LispAtom.LispNumber(String.valueOf(list.elements().size()));
        });

        builtins.put("null?", (interpreter, args, errorManager) ->
        {
            if (args.size() != 1)
            {
                errorManager.error("'null?' expects one argument", 0, 0, args.toString(), "Pass a single list");
                return null;
            }
            return new LispAtom.LispBoolean(args.get(0) instanceof LispList list && list.elements().isEmpty());
        });

        builtins.put("number?", (interpreter, args, errorManager) ->
        {
            return new LispAtom.LispBoolean(args.size() == 1 && args.get(0) instanceof LispAtom.LispNumber);
        });

        builtins.put("list?", (interpreter, args, errorManager) ->
        {
            return new LispAtom.LispBoolean(args.size() == 1 && args.get(0) instanceof LispList);
        });

        builtins.put("symbol?", (interpreter, args, errorManager) ->
        {
            return new LispAtom.LispBoolean(args.size() == 1 && args.get(0) instanceof LispAtom.LispSymbol);
        });

        builtins.put("not", (interpreter, args, errorManager) ->
        {
            if (args.size() != 1)
            {
                errorManager.error("'not' expects one argument", 0, 0, args.toString(), "Provide a single expression");
                return null;
            }
            return new LispAtom.LispBoolean(!interpreter.getDefinitions().isTruthy(args.get(0)));
        });

        // Special form: (defun name (params...) body)
        builtins.put("defun", (interpreter, args, errorManager) ->
        {
            if (args.size() < 3 || !(args.get(0) instanceof LispAtom.LispSymbol name)
                    || !(args.get(1) instanceof LispList paramList))
            {
                errorManager.error("Malformed defun", 0, 0, args.toString(), "Use: (defun name (params) body)");
                return null;
            }

            List<String> params = new ArrayList<>();
            for (LispNode p : ((LispList) paramList).elements())
            {
                if (!(p instanceof LispAtom.LispSymbol s))
                {
                    errorManager.error("Parameters must be symbols", 0, 0, p.toString(), "Use simple names");
                    return null;
                }
                params.add(s.name());
            }

            LispNode body = new LispList(args.subList(2, args.size()));
            interpreter.getDefinitions().defineFunction(name.name(), params, body);
            return name;
        });

        builtins.put("format", (interpreter, args, errorManager) ->
        {
            if (args.size() < 2)
            {
                errorManager.error("'format' expects at least 2 arguments: destination and format string", 0, 0, args.toString(), "");
                return null;
            }

            LispNode dest = args.get(0);
            LispNode formatStr = args.get(1);

            if (!(formatStr instanceof LispAtom.LispString fmt))
            {
                errorManager.error("Second argument to 'format' must be a string", 0, 0, formatStr.toString(), "");
                return null;
            }

            String javaFmt = interpreter.getDefinitions().convertLispFormatToJavaFormat(fmt.value(), errorManager);
            List<Object> values = new ArrayList<>();
            for (int i = 2; i < args.size(); i++)
            {
                values.add(interpreter.getDefinitions().renderLispNodeAsString(args.get(i)));
            }

            String result = String.format(javaFmt.replace("~%", "%n"), values.toArray());

            if (dest instanceof LispAtom.LispSymbol symbol && symbol.name().equalsIgnoreCase("t"))
            {
                System.out.print(result);
            }

            return new LispAtom.LispString(result);
        });
        builtins.put("setf", (interpreter, args, errorManager) ->
        {
            if (args.size() != 2 || !(args.get(0) instanceof LispAtom.LispSymbol sym))
            {
                errorManager.error("'setf' expects (setf symbol value)", 0, 0, args.toString(), "");
                return null;
            }

            LispNode value = interpreter.evaluate(args.get(1));
            interpreter.getDefinitions().setVariable(sym.name(), value, errorManager);
            return value;
        });

        builtins.put("incf", (interpreter, args, errorManager) ->
        {
            if (args.size() != 1 || !(args.get(0) instanceof LispAtom.LispSymbol sym))
            {
                errorManager.error("'incf' expects (incf symbol)", 0, 0, args.toString(), "");
                return null;
            }

            LispNode val = interpreter.getDefinitions().lookupVariable(sym.name());
            if (!(val instanceof LispAtom.LispNumber n))
            {
                errorManager.error("'incf' requires the variable to be a number", 0, 0, val.toString(), "");
                return null;
            }

            double incremented = Double.parseDouble(n.value()) + 1;
            LispNode newVal = new LispAtom.LispNumber(String.valueOf(incremented));
            interpreter.getDefinitions().setVariable(sym.name(), newVal, errorManager);
            return newVal;
        });


        builtins.put("read-line", (interpreter, args, errorManager) ->
        {
            try
            {
                String line = new java.util.Scanner(System.in).nextLine();
                return new LispAtom.LispString(line);
            } catch (Exception e)
            {
                errorManager.error("Failed to read line", 0, 0, "", "");
                return LispAtom.LispNil.INSTANCE;
            }
        });

        builtins.put("parse-integer", (interpreter, args, errorManager) ->
        {
            if (args.isEmpty() || !(args.get(0) instanceof LispAtom.LispString str))
            {
                errorManager.error("'parse-integer' expects a string as the first argument", 0, 0, args.toString(), "");
                return LispAtom.LispNil.INSTANCE;
            }

            try
            {
                return new LispAtom.LispNumber(String.valueOf(Integer.parseInt(str.value())));
            } catch (Exception e)
            {
                return LispAtom.LispNil.INSTANCE; // Support :junk-allowed
            }
        });


    }

    private LispAtom.LispBoolean compareNumbers(List<LispNode> args, ErrorManager errorManager, BiFunction<Double, Double, Boolean> comp)
    {
        if (args.size() < 2)
        {
            errorManager.error("Comparison expects at least two arguments", 0, 0, args.toString(), "Provide at least two numbers");
            return null;
        }
        for (int i = 0; i < args.size() - 1; i++)
        {
            if (!(args.get(i) instanceof LispAtom.LispNumber num1) || !(args.get(i + 1) instanceof LispAtom.LispNumber num2))
            {
                errorManager.error("Comparison expects only numbers", 0, 0, args.toString(), "Ensure all arguments are numbers");
                return null;
            }
            double a = Double.parseDouble(num1.value());
            double b = Double.parseDouble(num2.value());
            if (!comp.apply(a, b)) return new LispAtom.LispBoolean(false);
        }
        return new LispAtom.LispBoolean(true);
    }

    /**
     * Converts Lisp-style format specifiers to Java style.
     *
     * @param formatStr    The format string to convert
     * @param errorManager Error manager for reporting errors
     * @return The converted format string or null if error
     */
    public String convertLispFormatToJavaFormat(String formatStr, ErrorManager errorManager)
    {
        // Implementation details
        return formatStr; // Simplified implementation
    }

    /**
     * Determines if a Lisp node is truthy (anything except #f or nil).
     *
     * @param node The node to check
     * @return true if the node is truthy, false otherwise
     */
    public boolean isTruthy(LispNode node)
    {
        if (node instanceof LispAtom.LispBoolean)
        {
            return ((LispAtom.LispBoolean) node).value();
        }
        return node != LispAtom.LispNil.INSTANCE;
    }

    /**
     * Renders a Lisp node as a string for display.
     *
     * @param node The node to render
     * @return A string representation of the node
     */
    public String renderLispNodeAsString(LispNode node)
    {
        return LispPrinter.format(node);
    }

    @Override
    public String toString()
    {
        return "LispEnvironment{variables=" + variables.size() + ", functions=" + functions.size() + "}";
    }

    /**
     * Represents a built-in function.
     */
    @FunctionalInterface
    public interface BuiltinFunction
    {
        /**
         * Executes the built-in function with the given arguments.
         *
         * @param interpreter  The Lisp interpreter
         * @param args         The function arguments
         * @param errorManager Error manager for reporting errors
         * @return The result of the function or null if an error occurred
         */
        LispNode apply(LispInterpreter interpreter, List<LispNode> args, ErrorManager errorManager);
    }

    private static class ReturnSignal extends RuntimeException
    {
        public final LispNode value;

        public ReturnSignal(LispNode value)
        {
            this.value = value;
        }
    }

    /**
     * Represents a user-defined function.
     */
    public record LispFunction(List<String> params, LispNode body)
    {
    }

    /**
     * Represents a user-defined macro.
     */
    public record LispMacro(List<String> params, LispNode body)
    {
    }
}
