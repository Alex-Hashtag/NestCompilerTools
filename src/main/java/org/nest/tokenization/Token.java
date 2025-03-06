package org.nest.tokenization;

public sealed interface Token
        permits Token.Keyword, Token.Delimiter, Token.Operator, Token.Literal, Token.Identifier, Token.Comment,
        Token.Start, Token.End, Token.NewLine, Token.Invalid, Token.IndentIncr, Token.IndentDecr
{
    Coordinates position();

    default String getValue() {
        return switch (this) {
            case Start _ -> "Start";
            case End _ -> "End";
            case NewLine _ -> "NewLine";
            case IndentIncr _ -> "IndentIncr";
            case IndentDecr _ -> "IndentDecr";
            case Invalid invalid -> invalid.value();
            case Keyword keyword -> keyword.value();
            case Delimiter delimiter -> delimiter.value();
            case Operator operator -> operator.value();
            case Comment comment -> comment.value();
            case Literal literal -> literal.value();
            case Identifier identifier -> identifier.value();
        };
    }

    // ===================== //
    // ==== Token Types ==== //
    // ===================== //

    record Keyword(Coordinates position, String value) implements Token {
        @Override public String toString() { return "Keyword: " + value; }
    }

    record Delimiter(Coordinates position, String value) implements Token {
        @Override public String toString() { return "Delimiter: " + value; }
    }

    record Operator(Coordinates position, String value) implements Token {
        @Override public String toString() { return "Operator: " + value; }
    }

    record Literal(Coordinates position, String type, String value) implements Token {
        @Override public String toString() { return "Literal (" + type + "): " + value; }
    }

    record Identifier(Coordinates position, String type, String value) implements Token {
        @Override public String toString() { return "Identifier (" + type + "): " + value; }
    }

    record Comment(Coordinates position, String value) implements Token {
        @Override public String toString() { return "Comment: " + value; }
    }

    record Start(Coordinates position) implements Token {
        @Override public String toString() { return "Start"; }
    }

    record End(Coordinates position) implements Token {
        @Override public String toString() { return "End"; }
    }

    record NewLine(Coordinates position) implements Token {
        @Override public String toString() { return "NewLine"; }
    }

    record IndentIncr(Coordinates position) implements Token {
        @Override public String toString() { return "IndentIncr"; }
    }

    record IndentDecr(Coordinates position) implements Token {
        @Override public String toString() { return "IndentDecr"; }
    }

    record Invalid(Coordinates position, String value) implements Token {
        @Override public String toString() { return "Invalid: " + value; }
    }
}
