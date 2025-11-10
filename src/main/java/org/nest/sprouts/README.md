# Sprout-S Language Implementation

Complete implementation of the Sprout-S programming language with parser, interpreter, and comprehensive test suite.

## Components

### Core Files

- **`SproutSTokenRules.java`** - Lexical tokenization rules
- **`SproutSASTRules.java`** - AST parsing rules
- **`SproutSInterpreter.java`** - Runtime interpreter with ErrorManager integration
- **`SproutSRunner.java`** - Main entry point for running programs
- **`DocS.md`** - Complete language specification

### AST Nodes (`ast/` directory)

- `Program.java` - Root node
- `Stmt.java` - Statement types (Let, Set, If, While, Print, Exit)
- `Expr.java` - Expression types (Int, Var, Unary, Binary, Group)
- `Block.java` - Statement blocks

## Running Programs

### Using the Runner

```bash
# Run a Sprout-S file
java -cp target/classes org.nest.sprouts.SproutSRunner program.spr

# Run sample Fibonacci program
java -cp target/classes org.nest.sprouts.SproutSRunner --sample
```

### Example Program

```sprout-s
let a = 0;
let b = 1;
print a;
print b;

let i = 0;
while (i < 10) {
    let temp = a + b;
    set a = b;
    set b = temp;
    print temp;
    set i = i + 1;
}
```

## Features

- **Variable scoping** with shadowing support
- **Control flow**: if/else, while loops
- **Operators**: arithmetic (+, -, *, /, %), comparison (<, <=, >, >=, ==, !=), logical (&&, ||)
- **Short-circuit evaluation** for && and ||
- **Error handling** via ErrorManager with detailed messages
- **Exit codes**: 0 (success), 1 (parse errors), 2 (runtime errors)

## Testing

```bash
# Run parser tests (20 tests)
mvn test -Dtest=SproutSParserTest

# Run interpreter tests (21 tests)
mvn test -Dtest=SproutSInterpreterTest
```

All tests pass successfully! ✅
