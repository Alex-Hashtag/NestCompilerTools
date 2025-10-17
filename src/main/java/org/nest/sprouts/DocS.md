# Sprou-T

# 1) Lexical spec

## Character set

- Source is UTF-8; identifiers use ASCII letters, digits, and `_`.
    
- Lines end with `\n` or `\r\n`.
    

## Tokens (with regex-like rules)

Order matters; match the **longest** token, then the **first** rule.

**Whitespace & comments (skip)**

- `WS` : `[ \t\r\n]+`
    
- `LINE_COMMENT` : `//[^\r\n]*`
    

**Literals**

- `INT` : `0|[1-9][0-9]*`  
    Parse as signed 32-bit (`i32`). Reject on overflow during parsing or defer to sema.
    

**Identifiers & keywords**

- `IDENT` : `[A-Za-z_][A-Za-z0-9_]*`
    
- The following are **reserved keywords** (emit dedicated token kinds, not IDENT):
    
    - `let`, `set`, `if`, `else`, `while`, `print`, `exit`
        

**Punctuation**

- `(` `)` `{` `}` `;` `=`
    

**Operators**

- Two-char: `==` `!=` `<=` `>=` `&&` `||`
    
- One-char: `+ - * / % < > !`
    

### Lexing notes

- No string/char literals.
    
- No numeric separators, hex, or unary `+`.
    
- Track `line:column` for diagnostics per token.
    
- Produce an EOF token at the end.
    

---

# 2) Concrete grammar (EBNF)

```
program      := { statement } EOF ;

statement    := let_stmt
              | set_stmt
              | if_stmt
              | while_stmt
              | print_stmt
              | exit_stmt ;

let_stmt     := "let" IDENT "=" expr ";" ;
set_stmt     := "set" IDENT "=" expr ";" ;
if_stmt      := "if" "(" expr ")" block "else" block ;
while_stmt   := "while" "(" expr ")" block ;
print_stmt   := "print" expr ";" ;
exit_stmt    := "exit" expr ";" ;

block        := "{" { statement } "}" ;

expr         := logic_or ;
logic_or     := logic_and { "||" logic_and } ;
logic_and    := equality { "&&" equality } ;
equality     := comparison { ( "==" | "!=" ) comparison } ;
comparison   := term { ( "<" | "<=" | ">" | ">=" ) term } ;
term         := factor { ( "+" | "-" ) factor } ;
factor       := unary { ( "*" | "/" | "%" ) unary } ;
unary        := ( "-" | "!" ) unary | primary ;
primary      := INT | IDENT | "(" expr ")" ;
```

**Associativity & precedence (high → low):**

1. Unary: `- !` (right-assoc)
    
2. `* / %`
    
3. `+ -`
    
4. `< <= > >=`
    
5. `== !=`
    
6. `&&`
    
7. `||` (all binary ops left-assoc)
    

Parsing strategy: recursive-descent or Pratt for `expr`.

---

# 3) Static semantics

## Types

- Single type: `i32` (signed 32-bit).
    
- Truthiness: `0` = false; nonzero = true. `!x` returns `0|1`.
    

## Scopes & symbols

- Each `{ ... }` introduces a new scope.
    
- `let` declares a mutable variable in the current scope.
    
- Shadowing is allowed (inner `let x` hides outer `x` until block end).
    
- **Use-before-def** is an error (check at resolve time).
    
- `set name = expr;` must find a variable in any enclosing scope; assigns to nearest binding.
    

## Operator typing/behavior (all on `i32`)

- Arithmetic: `+ - * / %` → `i32`.
    
    - Division: truncates toward **zero** (C/LLVM semantics).
        
    - Modulo: same sign as dividend (`a % b == a - (a / b) * b` with truncating div).
        
    - **Division/mod by zero**: runtime error (diagnose at runtime; optional static warn if divisor is constant zero).
        
- Comparisons & equality: return `0` or `1`.
    
- Logical: `&&` / `||` short-circuit; return `0` or `1`.
    
- Unary `-` : arithmetic negation (wraps on overflow if you use twos-complement).
    
- Unary `!` : `x == 0 ? 1 : 0`.
    

## Program termination

- `exit expr;` immediately stops execution with **process exit code** = `expr` (masked to `0..255` if you map to OS, or keep full `i32` in a VM).
    
- If no `exit` executed: exit code `0`.
    

## Constant folding (recommended)

- Fold any pure subexpressions of literals (respect overflow rules of target).
    
- Do **not** fold `&&`/`||` in a way that would evaluate short-circuited operands.
    

---

# 4) AST schema (suggested)

Minimal JSON-ish shapes (use enums/variants in your language):

```
Program { stmts: [Stmt] }

Stmt =
  | Let   { name: Ident, init: Expr }
  | Set   { name: Ident, expr: Expr }
  | If    { cond: Expr, then: Block, else_: Block }
  | While { cond: Expr, body: Block }
  | Print { expr: Expr }
  | Exit  { expr: Expr }

Block { stmts: [Stmt] }

Expr =
  | Int   { value: i32 }
  | Var   { name: Ident }
  | Unary { op: '-' | '!', expr: Expr }
  | Bin   { op: '+','-','*','/','%','<','<=','>','>=','==','!=','&&','||',
            left: Expr, right: Expr }
  | Group { expr: Expr }   // optional; can be dropped after parsing
```

Attach `span` (start..end) to all nodes for diagnostics and later tools.

---

# 5) Diagnostics (examples)

- **Lex**: `unexpected character '@' at line 3, col 12`
    
- **Parse**: `expected ';' after expression, found '}' at line 7, col 3`
    
- **Resolve**: `use of undeclared variable 'i' at line 5, col 8`
    
- **Assign**: `cannot set undeclared variable 'x' (did you mean 'let'?)`
    
- **Runtime**: `division by zero at line 9, col 17` (include node span)
    

---

# 6) Reference execution model

Implement either:

## A) LLVM lowering (sketch)

- **Module** has one function: `main()` → `i32`.
    
- Maintain an **SSA value** (alloca) map for variables (reassigned via `store`).
    
- `print e;` → call `printf("%d\n", e)` or a custom `void sprout_print(i32)`.
    
- Short-circuit:
    
    - For `a && b`:
        
        ```
        aBlock:   %a = eval(a)
                  %a_is_true = icmp ne i32 %a, 0
                  br i1 %a_is_true, label %bBlock, label %end
        bBlock:   %b = eval(b)
                  %b_bool = icmp ne i32 %b, 0
                  br label %end
        end:      %phi = phi i32 [0, %aBlock], [%b_bool_zext, %bBlock]
        ```
        
    - For `a || b`: same, but if `a_is_true` → result `1`.
        
- `while (cond) { body }`:
    
    ```
    br cond
    cond:  %c = eval(cond)!=0; br i1 %c, body, end
    body:  ...; br cond
    end:   ...
    ```
    
- `exit e;`:
    
    - Option 1: direct `call void exit(i32 e)` then `unreachable`.
        
    - Option 2: store in a dedicated `ret_slot`, `br epilogue`; in epilogue `ret i32 ret_slot`.
        

## B) Tiny stack VM (bytecode)

### Registers/stack

- Value stack of `i32`.
    
- Environment: vector of **frames**; each frame has a map from symbol id → slot index.
    
- Program is a flat bytecode; blocks compile to labels.
    

### Instructions (minimal)

```
PUSH_I32 imm32
LOAD slot                ; push locals[slot]
STORE slot               ; pop -> locals[slot]
ADD/SUB/MUL/DIV/MOD
LT/LE/GT/GE/EQ/NE        ; push 0/1
NOT                      ; logical not
NEG                      ; arithmetic neg
JMP label
JZ label                 ; pop cond, jump if 0
JNZ label                ; jump if != 0
PRINT                    ; pop, print as decimal + '\n'
EXIT                     ; pop, terminate VM with code
```

**Short-circuit** by codegen:

- `a && b`:
    
    ```
    ... code(a) ...
    DUP
    JZ  L_false
    POP
    ... code(b) ...
    NEZ                 ; (emit EQ/NE against 0)
    JMP L_end
    ```
    

L_false:  
POP  
PUSH_I32 0  
L_end:

```
- `a || b` analogous.

---

# 7) Well-defined edge cases

- **Overflow**: arithmetic wraps in 2’s complement (`i32`) if using VM; LLVM inherits target behavior (usually wrap for `add` unless you mark `nsw`/`nuw`).
- **/ % by 0**: raise runtime error and terminate with non-zero exit (e.g., 2) unless already in an `exit`.
- **Unused variables**: allowed (optionally warn).
- **Multiple `exit`**: the first executed ends the program.
- **Empty program**: valid; exits `0`.

---

# 8) Golden test files (I/O contracts)

### T1: arithmetic.spr
```

let x = 2 + 3 * 4;  
print x;

```
**stdout**
```

14

```
**exit**: `0`

### T2: scope.spr
```

let x = 1;  
{  
let x = 2;  
print x;  
}  
print x;

```
**stdout**
```

2  
1

```

### T3: while.spr
```

let s = 0;  
let i = 1;  
while (i <= 5) {  
set s = s + i;  
set i = i + 1;  
}  
print s;

```
**stdout**
```

15

```

### T4: logic_shortcircuit.spr
```

let a = 0;  
let b = 1;  
print ( (a != 0) && (b / a) ); // must print 0 (no div by zero)  
print ( (b != 0) || (a / b) ); // must print 1

```
**stdout**
```

0  
1

```

### T5: sema_error_before_let.spr (should FAIL)
```

set x = 3;

```
**error**: `cannot assign to undeclared variable 'x'`

### T6: runtime_div0.spr (should RUNTIME-ERROR)
```

let z = 0;  
print 1 / z;

```

---

# 9) Implementation checklist

1. **Lexer**
   - Longest-match, then rule priority.
   - Produce `Token { kind, text, line, column, offset }`.

2. **Parser**
   - Recursive descent for statements & blocks.
   - Pratt or layered precedence functions for expressions.
   - Panic-mode recovery: on error, synchronize at `;` or `}`.

3. **Resolver & sema**
   - Scope stack with hash maps; push on `{`, pop on `}`.
   - On `let`, insert; on `set`/`Var`, lookup or error.
   - Optionally constant-fold and replace subtrees.

4. **Backend**
   - Choose **LLVM** or **VM** above; implement print/exit hooks.
   - Short-circuit via control flow.
   - Division semantics: trunc toward zero; modulo consistent.

5. **Driver**
   - Exit code: propagate VM/LLVM result.
   - Flags: `--ast`, `--ir`, `--run`, `--check` (no run).

---

# 10) Example token stream (sanity)

Input:
```

let x = 2 + 3 * 4;

```
Tokens:
```

LET "let"  
IDENT "x"  
= "="  
INT "2"

- "+"  
    INT "3"
    

- "*"  
    INT "4"  
    ; ";"  
    EOF
    

```

---

If you want, I can hand you a **formal LL(1) FIRST/FOLLOW** table or a **sample bytecode listing** for one of the tests next.
```