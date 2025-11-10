package org.nest.lisp.ast;

import org.nest.ast.ASTWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/// Root of the Lisp Abstract Syntax Tree.
/// Provides methods to convert from ASTWrapper, print as tree, and generate code.
public record LispAST(List<LispNode> nodes)
{
    /// Creates a LispAST from an ASTWrapper containing parsed nodes
    ///
    /// @param wrapper The ASTWrapper containing parsed AST nodes
    /// @return A new LispAST containing the converted nodes
    public static LispAST fromASTWrapper(ASTWrapper wrapper)
    {
        if (wrapper.hasErrors() || wrapper.get() == null)
        {
            return new LispAST(new ArrayList<>());
        }

        List<LispNode> nodes = new ArrayList<>();
        List<Object> rawObjects = wrapper.get();

        // Process all objects in the wrapper
        for (int i = 0; i < rawObjects.size(); i++)
        {
            Object obj = rawObjects.get(i);
            if (!(obj instanceof LispNode)) continue;

            // Special case for 'define' expressions: (define (name args...) body)
            if (obj instanceof LispAtom.LispSymbol(String name) && name.equals("define"))
            {
                LispAtom.LispSymbol symbol = (LispAtom.LispSymbol) obj;
                // Get the next object which should be either a symbol (for variable definitions)
                // or a list (for function definitions)
                if (i + 1 < rawObjects.size())
                {
                    Object nextObj = rawObjects.get(i + 1);

                    if (nextObj instanceof LispList functionNameAndArgs)
                    {
                        // This is a function definition: (define (name args...) body)
                        List<LispNode> defineElements = new ArrayList<>();
                        defineElements.add(symbol); // 'define'
                        defineElements.add(functionNameAndArgs); // '(name args...)'

                        // Find the function body
                        // We need to collect all expressions that form the function body
                        // until we get to another top-level expression or end of input
                        int bodyStart = i + 2;
                        if (bodyStart < rawObjects.size())
                        {
                            // Add the function body (may be multiple expressions)
                            defineElements.add((LispNode) rawObjects.get(bodyStart));

                            // Skip over the elements we've processed
                            i = bodyStart;
                        }
                        else
                        {
                            // No body found, just a function signature
                            i++; // Skip just the function name/args
                        }

                        // Add the complete define expression to the nodes list
                        nodes.add(new LispList(defineElements));
                    }
                    else if (nextObj instanceof LispNode nameNode && i + 2 < rawObjects.size())
                    {
                        // This is a variable definition: (define name value)
                        Object valueObj = rawObjects.get(i + 2);
                        if (valueObj instanceof LispNode valueNode)
                        {
                            List<LispNode> defineElements = new ArrayList<>();
                            defineElements.add(symbol); // 'define'
                            defineElements.add(nameNode); // name
                            defineElements.add(valueNode); // value

                            nodes.add(new LispList(defineElements));
                            i += 2; // Skip over the elements we've processed
                        }
                        else
                        {
                            // If value is not a LispNode, just add the symbol alone
                            nodes.add((LispNode) obj);
                        }
                    }
                    else
                    {
                        // If the next object is not as expected, just add the symbol
                        nodes.add((LispNode) obj);
                    }
                }
                else
                {
                    // If there's no next object, just add the symbol
                    nodes.add((LispNode) obj);
                }
            }
            // For any other top-level expression
            else
            {
                nodes.add((LispNode) obj);
            }
        }

        return new LispAST(nodes);
    }

    /// Prints the AST as a tree structure
    ///
    /// @param indent The indentation level (used for recursive calls)
    /// @return A string representation of the AST as a tree
    public String printTree(int indent)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("LispAST {\n");

        for (LispNode node : nodes)
        {
            sb.append("  ".repeat(indent + 1));
            if (node instanceof LispList(List<LispNode> elements))
            {
                sb.append("List [").append("\n");
                if (elements != null)
                {
                    for (LispNode element : elements)
                    {
                        sb.append("  ".repeat(indent + 2));
                        sb.append(printNodeTree(element, indent + 2));
                        sb.append("\n");
                    }
                }
                sb.append("  ".repeat(indent + 1)).append("]");
            }
            else
            {
                sb.append(printNodeTree(node, indent + 1));
            }
            sb.append("\n");
        }

        sb.append("  ".repeat(indent)).append("}");
        return sb.toString();
    }

    /**
     * Helper method to print a node in the tree
     */
    private String printNodeTree(LispNode node, int indent)
    {
        switch (node)
        {
            case LispAtom.LispSymbol(String name) ->
            {
                return "Symbol(" + name + ")";
            }
            case LispAtom.LispString(String value) ->
            {
                return "String(\"" + value + "\")";
            }
            case LispAtom.LispNumber(String value) ->
            {
                return "Number(" + value + ")";
            }
            case LispAtom.LispBoolean(boolean value) ->
            {
                return "Boolean(" + value + ")";
            }
            case LispAtom.LispCharacter(char value) ->
            {
                return "Character('" + value + "')";
            }
            case LispAtom.LispKeyword(String name) ->
            {
                return "Keyword(:" + name + ")";
            }
            case LispAtom.LispNil lispNil ->
            {
                return "Nil";
            }
            case LispList(List<LispNode> elements) ->
            {
                StringBuilder sb = new StringBuilder("List [").append("\n");
                if (elements != null)
                {
                    for (LispNode element : elements)
                    {
                        sb.append("  ".repeat(indent + 1));
                        sb.append(printNodeTree(element, indent + 1));
                        sb.append("\n");
                    }
                }
                sb.append("  ".repeat(indent)).append("]");
                return sb.toString();
            }
            case null, default ->
            {
            }
        }
        return node.toString();
    }

    /**
     * Regenerates the original Lisp code from this AST
     *
     * @return A string of the regenerated Lisp code
     */
    public String generateCode()
    {
        return nodes.stream()
                .map(this::generateNodeCode)
                .collect(Collectors.joining("\n"));
    }

    /**
     * Helper method to generate code for a specific node
     */
    private String generateNodeCode(LispNode node)
    {
        switch (node)
        {
            case LispAtom.LispSymbol symbol ->
            {
                return symbol.name();
            }
            case LispAtom.LispString str ->
            {
                return "\"" + str.value() + "\"";
            }
            case LispAtom.LispNumber num ->
            {
                return num.value();
            }
            case LispAtom.LispBoolean bool ->
            {
                return bool.value() ? "#t" : "#f";
            }
            case LispAtom.LispCharacter ch ->
            {
                return "#\\" + ch.value();
            }
            case LispAtom.LispKeyword keyword ->
            {
                return ":" + keyword.name();
            }
            case LispAtom.LispNil lispNil ->
            {
                return "nil";
            }
            case LispList list ->
            {
                List<LispNode> elements = list.elements();
                if (elements == null)
                {
                    return "()";
                }
                return "(" + elements.stream()
                        .map(this::generateNodeCode)
                        .collect(Collectors.joining(" ")) + ")";
            }
            case null, default ->
            {
            }
        }
        return node.toString();
    }

    @Override
    public String toString()
    {
        return printTree(0);
    }
}
