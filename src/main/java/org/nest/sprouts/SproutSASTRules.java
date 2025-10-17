package org.nest.sprouts;

import org.nest.ast.ASTRules;
import org.nest.sprouts.ast.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines the AST rules for Sprout-S language parsing.
 * Based on the grammar specification in DocS.md
 */
public class SproutSASTRules
{
    /**
     * Creates and returns the standard AST rules for Sprout-S.
     *
     * @return ASTRules configured for Sprout-S syntax
     */
    public static ASTRules rules()
    {
        return ASTRules.builder()
                .ignoreComments(true)
                .topRule(List.of("program"))
                
                // program := { statement } EOF
                .startRule("program")
                .addDefinition("program")
                .repeat(ctx -> ctx.put("stmts", new ArrayList<Stmt>()))
                    .rule("statement", ctx -> stmt -> ctx.<List<Stmt>>get("stmts").add((Stmt) stmt))
                .stopRepeat()
                .endDefinition(ctx -> () -> new Program(ctx.get("stmts", List.class)))
                
                // statement := let_stmt | set_stmt | if_stmt | while_stmt | print_stmt | exit_stmt
                .startRule("statement")
                .addDefinition("let")
                    .rule("let_stmt", ctx -> stmt -> ctx.put("result", stmt))
                .endDefinition(ctx -> () -> ctx.get("result"))
                .addDefinition("set")
                    .rule("set_stmt", ctx -> stmt -> ctx.put("result", stmt))
                .endDefinition(ctx -> () -> ctx.get("result"))
                .addDefinition("if")
                    .rule("if_stmt", ctx -> stmt -> ctx.put("result", stmt))
                .endDefinition(ctx -> () -> ctx.get("result"))
                .addDefinition("while")
                    .rule("while_stmt", ctx -> stmt -> ctx.put("result", stmt))
                .endDefinition(ctx -> () -> ctx.get("result"))
                .addDefinition("print")
                    .rule("print_stmt", ctx -> stmt -> ctx.put("result", stmt))
                .endDefinition(ctx -> () -> ctx.get("result"))
                .addDefinition("exit")
                    .rule("exit_stmt", ctx -> stmt -> ctx.put("result", stmt))
                .endDefinition(ctx -> () -> ctx.get("result"))
                
                // let_stmt := "let" IDENT "=" expr ";"
                .startRule("let_stmt")
                .addDefinition()
                .keyword("let", null)
                .identifier("IDENT", ctx -> token -> ctx.put("name", token.getValue()))
                .delimiter("=", null)
                .rule("expr", ctx -> expr -> ctx.put("init", expr))
                .delimiter(";", null)
                .endDefinition(ctx -> () -> new Stmt.Let(
                        ctx.get("name", String.class),
                        ctx.get("init", Expr.class)
                ))
                
                // set_stmt := "set" IDENT "=" expr ";"
                .startRule("set_stmt")
                .addDefinition()
                .keyword("set", null)
                .identifier("IDENT", ctx -> token -> ctx.put("name", token.getValue()))
                .delimiter("=", null)
                .rule("expr", ctx -> expr -> ctx.put("expr", expr))
                .delimiter(";", null)
                .endDefinition(ctx -> () -> new Stmt.Set(
                        ctx.get("name", String.class),
                        ctx.get("expr", Expr.class)
                ))
                
                // if_stmt := "if" "(" expr ")" block "else" block
                .startRule("if_stmt")
                .addDefinition()
                .keyword("if", null)
                .delimiter("(", null)
                .rule("expr", ctx -> expr -> ctx.put("cond", expr))
                .delimiter(")", null)
                .rule("block", ctx -> block -> ctx.put("then", block))
                .keyword("else", null)
                .rule("block", ctx -> block -> ctx.put("else", block))
                .endDefinition(ctx -> () -> new Stmt.If(
                        ctx.get("cond", Expr.class),
                        ctx.get("then", Block.class),
                        ctx.get("else", Block.class)
                ))
                
                // while_stmt := "while" "(" expr ")" block
                .startRule("while_stmt")
                .addDefinition()
                .keyword("while", null)
                .delimiter("(", null)
                .rule("expr", ctx -> expr -> ctx.put("cond", expr))
                .delimiter(")", null)
                .rule("block", ctx -> block -> ctx.put("body", block))
                .endDefinition(ctx -> () -> new Stmt.While(
                        ctx.get("cond", Expr.class),
                        ctx.get("body", Block.class)
                ))
                
                // print_stmt := "print" expr ";"
                .startRule("print_stmt")
                .addDefinition()
                .keyword("print", null)
                .rule("expr", ctx -> expr -> ctx.put("expr", expr))
                .delimiter(";", null)
                .endDefinition(ctx -> () -> new Stmt.Print(ctx.get("expr", Expr.class)))
                
                // exit_stmt := "exit" expr ";"
                .startRule("exit_stmt")
                .addDefinition()
                .keyword("exit", null)
                .rule("expr", ctx -> expr -> ctx.put("expr", expr))
                .delimiter(";", null)
                .endDefinition(ctx -> () -> new Stmt.Exit(ctx.get("expr", Expr.class)))
                
                // block := "{" { statement } "}"
                .startRule("block")
                .addDefinition()
                .delimiter("{", null)
                .repeat(ctx -> ctx.put("stmts", new ArrayList<Stmt>()))
                    .rule("statement", ctx -> stmt -> ctx.<List<Stmt>>get("stmts").add((Stmt) stmt))
                .stopRepeat()
                .delimiter("}", null)
                .endDefinition(ctx -> () -> new Block(ctx.get("stmts", List.class)))
                
                // expr := logic_or
                .startRule("expr")
                .addDefinition()
                .rule("logic_or", ctx -> expr -> ctx.put("result", expr))
                .endDefinition(ctx -> () -> ctx.get("result"))
                
                // logic_or := logic_and { "||" logic_and }
                .startRule("logic_or")
                .addDefinition()
                .rule("logic_and", ctx -> expr -> ctx.put("left", expr))
                .repeat(ctx -> {
                    ctx.put("ops", new ArrayList<String>());
                    return ctx.put("rights", new ArrayList<Expr>());
                })
                    .operator("||", ctx -> token -> ctx.<List<String>>get("ops").add(token.getValue()))
                    .rule("logic_and", ctx -> expr -> ctx.<List<Expr>>get("rights").add((Expr) expr))
                .stopRepeat()
                .endDefinition(ctx -> () -> {
                    Expr left = ctx.get("left", Expr.class);
                    List<String> ops = ctx.get("ops", List.class);
                    List<Expr> rights = ctx.get("rights", List.class);
                    for (int i = 0; i < ops.size(); i++) {
                        left = new Expr.Binary(ops.get(i), left, rights.get(i));
                    }
                    return left;
                })
                
                // logic_and := equality { "&&" equality }
                .startRule("logic_and")
                .addDefinition()
                .rule("equality", ctx -> expr -> ctx.put("left", expr))
                .repeat(ctx -> {
                    ctx.put("ops", new ArrayList<String>());
                    return ctx.put("rights", new ArrayList<Expr>());
                })
                    .operator("&&", ctx -> token -> ctx.<List<String>>get("ops").add(token.getValue()))
                    .rule("equality", ctx -> expr -> ctx.<List<Expr>>get("rights").add((Expr) expr))
                .stopRepeat()
                .endDefinition(ctx -> () -> {
                    Expr left = ctx.get("left", Expr.class);
                    List<String> ops = ctx.get("ops", List.class);
                    List<Expr> rights = ctx.get("rights", List.class);
                    for (int i = 0; i < ops.size(); i++) {
                        left = new Expr.Binary(ops.get(i), left, rights.get(i));
                    }
                    return left;
                })
                
                // equality := comparison { ( "==" | "!=" ) comparison }
                .startRule("equality")
                .addDefinition()
                .rule("comparison", ctx -> expr -> ctx.put("left", expr))
                .repeat(ctx -> {
                    ctx.put("ops", new ArrayList<String>());
                    return ctx.put("rights", new ArrayList<Expr>());
                })
                    .choice()
                        .operator("==", ctx -> token -> ctx.<List<String>>get("ops").add(token.getValue()))
                    .or()
                        .operator("!=", ctx -> token -> ctx.<List<String>>get("ops").add(token.getValue()))
                    .endChoice()
                    .rule("comparison", ctx -> expr -> ctx.<List<Expr>>get("rights").add((Expr) expr))
                .stopRepeat()
                .endDefinition(ctx -> () -> {
                    Expr left = ctx.get("left", Expr.class);
                    List<String> ops = ctx.get("ops", List.class);
                    List<Expr> rights = ctx.get("rights", List.class);
                    for (int i = 0; i < ops.size(); i++) {
                        left = new Expr.Binary(ops.get(i), left, rights.get(i));
                    }
                    return left;
                })
                
                // comparison := term { ( "<" | "<=" | ">" | ">=" ) term }
                .startRule("comparison")
                .addDefinition()
                .rule("term", ctx -> expr -> ctx.put("left", expr))
                .repeat(ctx -> {
                    ctx.put("ops", new ArrayList<String>());
                    return ctx.put("rights", new ArrayList<Expr>());
                })
                    .choice()
                        .operator("<=", ctx -> token -> ctx.<List<String>>get("ops").add(token.getValue()))
                    .or()
                        .operator(">=", ctx -> token -> ctx.<List<String>>get("ops").add(token.getValue()))
                    .or()
                        .operator("<", ctx -> token -> ctx.<List<String>>get("ops").add(token.getValue()))
                    .or()
                        .operator(">", ctx -> token -> ctx.<List<String>>get("ops").add(token.getValue()))
                    .endChoice()
                    .rule("term", ctx -> expr -> ctx.<List<Expr>>get("rights").add((Expr) expr))
                .stopRepeat()
                .endDefinition(ctx -> () -> {
                    Expr left = ctx.get("left", Expr.class);
                    List<String> ops = ctx.get("ops", List.class);
                    List<Expr> rights = ctx.get("rights", List.class);
                    for (int i = 0; i < ops.size(); i++) {
                        left = new Expr.Binary(ops.get(i), left, rights.get(i));
                    }
                    return left;
                })
                
                // term := factor { ( "+" | "-" ) factor }
                .startRule("term")
                .addDefinition()
                .rule("factor", ctx -> expr -> ctx.put("left", expr))
                .repeat(ctx -> {
                    ctx.put("ops", new ArrayList<String>());
                    return ctx.put("rights", new ArrayList<Expr>());
                })
                    .choice()
                        .operator("+", ctx -> token -> ctx.<List<String>>get("ops").add(token.getValue()))
                    .or()
                        .operator("-", ctx -> token -> ctx.<List<String>>get("ops").add(token.getValue()))
                    .endChoice()
                    .rule("factor", ctx -> expr -> ctx.<List<Expr>>get("rights").add((Expr) expr))
                .stopRepeat()
                .endDefinition(ctx -> () -> {
                    Expr left = ctx.get("left", Expr.class);
                    List<String> ops = ctx.get("ops", List.class);
                    List<Expr> rights = ctx.get("rights", List.class);
                    for (int i = 0; i < ops.size(); i++) {
                        left = new Expr.Binary(ops.get(i), left, rights.get(i));
                    }
                    return left;
                })
                
                // factor := unary { ( "*" | "/" | "%" ) unary }
                .startRule("factor")
                .addDefinition()
                .rule("unary", ctx -> expr -> ctx.put("left", expr))
                .repeat(ctx -> {
                    ctx.put("ops", new ArrayList<String>());
                    return ctx.put("rights", new ArrayList<Expr>());
                })
                    .choice()
                        .operator("*", ctx -> token -> ctx.<List<String>>get("ops").add(token.getValue()))
                    .or()
                        .operator("/", ctx -> token -> ctx.<List<String>>get("ops").add(token.getValue()))
                    .or()
                        .operator("%", ctx -> token -> ctx.<List<String>>get("ops").add(token.getValue()))
                    .endChoice()
                    .rule("unary", ctx -> expr -> ctx.<List<Expr>>get("rights").add((Expr) expr))
                .stopRepeat()
                .endDefinition(ctx -> () -> {
                    Expr left = ctx.get("left", Expr.class);
                    List<String> ops = ctx.get("ops", List.class);
                    List<Expr> rights = ctx.get("rights", List.class);
                    for (int i = 0; i < ops.size(); i++) {
                        left = new Expr.Binary(ops.get(i), left, rights.get(i));
                    }
                    return left;
                })
                
                // unary := ( "-" | "!" ) unary | primary
                .startRule("unary")
                .addDefinition("unary_minus")
                .operator("-", ctx -> token -> ctx.put("op", token.getValue()))
                .rule("unary", ctx -> expr -> ctx.put("expr", expr))
                .endDefinition(ctx -> () -> new Expr.Unary(
                        ctx.get("op", String.class),
                        ctx.get("expr", Expr.class)
                ))
                .addDefinition("unary_not")
                .operator("!", ctx -> token -> ctx.put("op", token.getValue()))
                .rule("unary", ctx -> expr -> ctx.put("expr", expr))
                .endDefinition(ctx -> () -> new Expr.Unary(
                        ctx.get("op", String.class),
                        ctx.get("expr", Expr.class)
                ))
                .addDefinition("primary")
                .rule("primary", ctx -> expr -> ctx.put("result", expr))
                .endDefinition(ctx -> () -> ctx.get("result"))
                
                // primary := INT | IDENT | "(" expr ")"
                .startRule("primary")
                .addDefinition("int")
                .literal("INT", ctx -> token -> ctx.put("value", token.getValue()))
                .endDefinition(ctx -> () -> new Expr.Int(
                        Integer.parseInt(ctx.get("value", String.class))
                ))
                .addDefinition("ident")
                .identifier("IDENT", ctx -> token -> ctx.put("name", token.getValue()))
                .endDefinition(ctx -> () -> new Expr.Var(
                        ctx.get("name", String.class)
                ))
                .addDefinition("grouped")
                .delimiter("(", null)
                .rule("expr", ctx -> expr -> ctx.put("expr", expr))
                .delimiter(")", null)
                .endDefinition(ctx -> () -> new Expr.Group(
                        ctx.get("expr", Expr.class)
                ))
                
                .build();
    }
}
