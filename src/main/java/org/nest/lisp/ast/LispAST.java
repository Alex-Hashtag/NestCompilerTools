package org.nest.lisp.ast;

import org.nest.ast.ASTWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/// Root of the Lisp Abstract Syntax Tree.
/// Provides methods to convert from ASTWrapper, print as tree, and generate code.
public record LispAST(List<LispNode> nodes) {
    /// Creates a LispAST from an ASTWrapper containing parsed nodes
    /// @param wrapper The ASTWrapper containing parsed AST nodes
    /// @return A new LispAST containing the converted nodes
    public static LispAST fromASTWrapper(ASTWrapper wrapper) {
        if (wrapper.hasErrors() || wrapper.get() == null) {
            return new LispAST(new ArrayList<>());
        }
        
        List<LispNode> nodes = new ArrayList<>();
        for (Object obj : wrapper.get()) {
            if (obj instanceof LispNode node) {
                nodes.add(node);
            } else {
            }
        }
        return new LispAST(nodes);
    }
    
    /// Prints the AST as a tree structure
    /// @param indent The indentation level (used for recursive calls)
    /// @return A string representation of the AST as a tree
    public String printTree(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append("LispAST {\n");
        
        for (LispNode node : nodes) {
            sb.append("  ".repeat(indent + 1));
            if (node instanceof LispList list) {
                sb.append("List [\n");
                List<LispNode> elements = list.elements();
                if (elements != null) {
                    for (LispNode element : elements) {
                        sb.append("  ".repeat(indent + 2));
                        sb.append(printNodeTree(element, indent + 2));
                        sb.append("\n");
                    }
                }
                sb.append("  ".repeat(indent + 1)).append("]");
            } else {
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
    private String printNodeTree(LispNode node, int indent) {
        if (node instanceof LispAtom.LispSymbol symbol) {
            return "Symbol(" + symbol.name() + ")";
        } else if (node instanceof LispAtom.LispString str) {
            return "String(\"" + str.value() + "\")";
        } else if (node instanceof LispAtom.LispNumber num) {
            return "Number(" + num.value() + ")";
        } else if (node instanceof LispAtom.LispBoolean bool) {
            return "Boolean(" + bool.value() + ")";
        } else if (node instanceof LispAtom.LispNil) {
            return "Nil";
        } else if (node instanceof LispList list) {
            StringBuilder sb = new StringBuilder("List [\n");
            List<LispNode> elements = list.elements();
            if (elements != null) {
                for (LispNode element : elements) {
                    sb.append("  ".repeat(indent + 1));
                    sb.append(printNodeTree(element, indent + 1));
                    sb.append("\n");
                }
            }
            sb.append("  ".repeat(indent)).append("]");
            return sb.toString();
        }
        return node.toString();
    }
    
    /**
     * Regenerates the original Lisp code from this AST
     * @return A string of the regenerated Lisp code
     */
    public String generateCode() {
        return nodes.stream()
                .map(this::generateNodeCode)
                .collect(Collectors.joining("\n"));
    }
    
    /**
     * Helper method to generate code for a specific node
     */
    private String generateNodeCode(LispNode node) {
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
                return bool.value() ? "true" : "false";
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
    public String toString() {
        return printTree(0);
    }
}
