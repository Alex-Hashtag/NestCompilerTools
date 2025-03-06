package org.nest.errors;

/// Simple concrete implementation of a compiler error.
public record SimpleCompilerError(
        String message,
        int line,
        int column,
        String token,
        String hint
) implements CompilerError {
    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getLine() {
        return line;
    }

    @Override
    public int getColumn() {
        return column;
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public String getHint() {
        return hint;
    }
}
