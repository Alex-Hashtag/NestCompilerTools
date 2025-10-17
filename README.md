# NestCompilerTools

A flexible toolkit for creating lexers, parsers, and abstract syntax trees (AST) for language processing in Java. The library includes specialized support for Lisp-like languages.

## Overview

NestCompilerTools provides a highly configurable framework for building custom language processors. It follows a pipeline approach:

1. **Tokenization** - Convert source text into a stream of tokens
2. **AST Building** - Transform tokens into an abstract syntax tree
3. **Processing** - Work with the AST for interpretation or compilation

## Features

- **Declarative Rules**: Define your language's syntax with a fluent builder API
- **Customizable Tokenization**: Configure tokens including keywords, operators, identifiers, literals, and more
- **Flexible AST Construction**: Build ASTs with a powerful rule-based system
- **Error Handling**: Comprehensive error reporting with contextual hints
- **Lisp Support**: Built-in support for parsing and working with Lisp syntax

## Getting Started

### Prerequisites

- Java 23 or higher
- Maven 3.8+

### Installation

1. Clone the repository
2. Build with Maven:
   ```
   mvn clean install
   ```

## Usage Examples

### Creating a Tokenizer for Lisp

```java
// Define token rules for Lisp
TokenRules lispRules = TokenRules.builder()
    // Lisp uses parentheses as structure
    .delimiter("(")
    .delimiter(")")

    // Quote characters
    .operator("'")   // Quote
    .operator("`")   // Quasiquote
    .operator(",")   // Unquote
    .operator(",@")  // Unquote-splicing

    // Identifiers (symbols, function names)
    .identifier("symbol", "[^\\s(),'`,@:]+") // Exclude colon and quote characters
    .identifier("keyword", ":[^\\s(),'`,@]+") // Keywords start with colon

    // Numbers
    .literal("integer", "[+-]?[0-9]+")
    .literal("float", "[+-]?[0-9]*\\.[0-9]+")
    .literal("boolean", "#t|#f")
    .literal("nil", "nil")

    // Character literals
    .literal("character", "#\\\\.")

    // Strings
    .literal("string", "^\"(?:\\\\.|[^\"\\\\])*\"")

    // Comments (Lisp often uses ';' for line comments)
    .comment(";.*")

    // Settings
    .whitespaceMode(WhitespaceMode.IGNORE)
    .enableLongestMatchFirst()
    .makeCaseSensitive()
    .build();

// Create token list from Lisp code
String lispCode = "(define (add x y) (+ x y))";
TokenList tokens = TokenList.create(lispCode, lispRules, LispTokenProcessor.create());

System.out.println(tokens);
```

// You can also use the prebuilt Lisp token rules:
```java
// Using the prebuilt Lisp token rules
TokenRules lispRules = LispTokenRules.create();
TokenList tokens = TokenList.create(lispCode, lispRules, LispTokenProcessor.create());
```

### Parsing Lisp Code

```java
// Simple parsing with error output to console
String lispCode = "(define (factorial n) (if (= n 0) 1 (* n (factorial (- n 1)))))";
LispAST ast = LispParser.parseWithErrorOutput(lispCode, System.out);

if (ast != null) {
    // Print regenerated code
    System.out.println(ast.generateCode());
    
    // Print tree structure
    System.out.println(ast.printTree(0));
}

// Parsing with custom error handling
ErrorManager errorManager = new ErrorManager();
errorManager.setContext("lisp", lispCode);

LispAST ast2 = LispParser.parse(lispCode, errorManager);
if (errorManager.hasErrors()) {
    errorManager.printReports(System.out);
} else {
    // Work with the AST
    System.out.println(ast2.generateCode());
}
```

### Defining Custom AST Rules

```java
ASTRules rules = ASTRules.builder()
    .topRule(List.of("expr"))
    .ignoreComments(true)
    .startRule("expr")
    
    // Define a list form
    .addDefinition("list")
    .delimiter("(", self -> token -> {})
    .repeat(self -> self.put("elements", new ArrayList<>()))
    .rule("expr", self -> expr -> self.<List<Object>>get("elements").add(expr))
    .stopRepeat()
    .delimiter(")", self -> token -> {})
    .endDefinition(self -> () -> new CustomList(self.get("elements", List.class)))
    
    // Define an identifier
    .addDefinition("symbol")
    .identifier("symbol", self -> token -> self.put("value", token.getValue()))
    .endDefinition(self -> () -> new CustomSymbol(self.get("value", String.class)))
    
    // Define more rules as needed
    
    .build();

// Create AST from tokens
ASTWrapper astWrapper = rules.createAST(tokens, errorManager);
```

### Complete Lisp Parsing Example

```java
// Example Lisp code
String lispCode = """
        (define x 10)
        (define (square n) (* n n))
        (square x)
        """;

// Set up error manager
ErrorManager errorManager = new ErrorManager();
errorManager.setContext("lisp", lispCode);

// Parse the code
LispAST ast = LispParser.parse(lispCode, errorManager);

// Check for errors
if (errorManager.hasErrors()) {
    errorManager.printReports(System.out);
} else {
    // Print the AST structure
    System.out.println(ast.printTree(0));
    
    // Print regenerated code
    System.out.println(ast.generateCode());
}
```

## Project Structure

- `tokenization`: Token definitions, rules, and tokenizer implementation
- `ast`: AST building framework including rules, templates, and context management
- `errors`: Error reporting and handling
- `lisp`: Specialized support for Lisp language processing

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
