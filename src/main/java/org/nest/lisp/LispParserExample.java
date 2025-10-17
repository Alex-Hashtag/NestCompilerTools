package org.nest.lisp;

import org.nest.errors.ErrorManager;
import org.nest.lisp.ast.LispAST;
import org.nest.lisp.parser.LispParser;
import org.nest.tokenization.TokenList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;

/**
 * Robust parser runner for the Lisp parser.
 *
 * Usage:
 *   java org.nest.lisp.LispParserExample [--sample | --stdin | <path/to/file.lisp>] [--show-tokens] [--no-regen]
 *
 * Exit codes:
 *   0 = success (parsed without errors)
 *   1 = parse/tokenization errors
 *   2 = I/O or unexpected failure
 */
public final class LispParserExample {

    private LispParserExample() {}

    public static void main(String[] args) {
        Locale.setDefault(Locale.ROOT);

        boolean useSample = false;
        boolean useStdin = false;
        boolean showTokens = false;
        boolean noRegen = false;
        Path file = null;

        // Parse flags/args
        for (String arg : args) {
            switch (arg) {
                case "--sample" -> useSample = true;
                case "--stdin" -> useStdin = true;
                case "--show-tokens" -> showTokens = true;
                case "--no-regen" -> noRegen = true;
                default -> {
                    if (file == null) {
                        file = Path.of(arg);
                    } else {
                        logErr("Unexpected argument: " + arg);
                        printUsageAndExit(2);
                    }
                }
            }
        }
        // Validate arg combinations
        int sources = (useSample ? 1 : 0) + (useStdin ? 1 : 0) + (file != null ? 1 : 0);
        if (sources == 0) useSample = true; // default to sample
        if (sources > 1) {
            logErr("Choose exactly one source: --sample, --stdin, or a file path.");
            printUsageAndExit(2);
        }

        // 1) Load code
        String lispCode;
        try {
            if (useSample) {
                lispCode = sample();
            } else if (useStdin) {
                lispCode = readAllFromStdin();
            } else {
                lispCode = Files.readString(file, StandardCharsets.UTF_8);
            }
        } catch (IOException ioe) {
            logErr("Failed to read input: " + ioe.getMessage());
            System.exit(2);
            return;
        }

        section("Original Lisp Code");
        System.out.println(lispCode);

        // 2) Tokenize
        TokenList tokens;
        Instant t0 = Instant.now();
        try {
            tokens = TokenList.create(
                    lispCode,
                    LispTokenRules.create(),
                    LispTokenProcessor.create()
            );
        } catch (Throwable t) {
            section("Tokenizer Crash");
            t.printStackTrace(System.err);
            System.exit(2);
            return;
        }
        Instant t1 = Instant.now();

        section("Tokens (" + tokens.size() + ") in " + pretty(Duration.between(t0, t1)));
        if (showTokens) {
            System.out.println(tokens);
        } else {
            // Show a short preview to avoid flooding output
            String tokStr = tokens.toString();
            System.out.println(tokStr.length() > 1000 ? tokStr.substring(0, 1000) + "\n... (use --show-tokens to print all)" : tokStr);
        }

        // 3) Parse
        ErrorManager errorManager = new ErrorManager();
        errorManager.setContext("lisp", lispCode);

        LispAST ast;
        Instant p0 = Instant.now();
        try {
            ast = LispParser.parse(lispCode, errorManager);
        } catch (Throwable t) {
            section("Parser Crash");
            t.printStackTrace(System.err);
            System.exit(2);
            return;
        }
        Instant p1 = Instant.now();

        if (errorManager.hasErrors() || ast == null) {
            section("Parse Errors (" + errorManager.getErrors().size() + ") in " + pretty(Duration.between(p0, p1)));
            errorManager.printReports(System.out);
            System.exit(1);
            return;
        }

        section("AST Tree Structure in " + pretty(Duration.between(p0, p1)));
        try {
            System.out.println(ast.printTree(0));
        } catch (Throwable t) {
            logErr("AST printTree failed: " + t.getMessage());
        }

        // 4) Regenerate code
        if (!noRegen) {
            section("Regenerated Lisp Code");
            try {
                System.out.println(ast.generateCode());
            } catch (Throwable t) {
                logErr("Code generation failed: " + t.getMessage());
                System.exit(1);
                return;
            }
        }

        // 5) Simplified API demo (kept, but hardened)
        section("Using Simplified Parser API");
        String simpleCode = "(define (add x y) (+ x y))";
        try {
            LispAST simpleAst = LispParser.parseWithErrorOutput(simpleCode, System.out);
            if (simpleAst != null) {
                System.out.println("Parsed successfully: " + simpleAst.generateCode());
            } else {
                System.out.println("Failed to parse simple program.");
            }
        } catch (Throwable t) {
            logErr("Simplified API failed: " + t.getMessage());
            System.exit(1);
            return;
        }

        System.exit(0);
    }

    // ----------------- helpers -----------------

    private static String sample() {
        return """
                ; Test with a simple missing closing parenthesis
                ((define x 10)
                """;
    }

    private static String readAllFromStdin() throws IOException {
        StringBuilder sb = new StringBuilder(8 * 1024);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            char[] buf = new char[4096];
            int r;
            while ((r = br.read(buf)) != -1) sb.append(buf, 0, r);
        }
        return sb.toString();
    }

    private static void section(String title) {
        System.out.println("\n=== " + title + " ===");
    }

    private static void logErr(String msg) {
        System.err.println("[LispParserExample] " + msg);
    }

    private static void printUsageAndExit(int code) {
        System.err.println("Usage:\n" +
                "  java " + LispParserExample.class.getName() + " [--sample | --stdin | <file>] [--show-tokens] [--no-regen]\n" +
                "\nExamples:\n" +
                "  # Use built-in sample (default)\n" +
                "  java " + LispParserExample.class.getName() + "\n\n" +
                "  # Parse a file and show all tokens\n" +
                "  java " + LispParserExample.class.getName() + " program.lisp --show-tokens\n\n" +
                "  # Read from stdin and skip regeneration\n" +
                "  cat program.lisp | java " + LispParserExample.class.getName() + " --stdin --no-regen\n");
        System.err.println("Args: " + Arrays.toString(java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toArray()));
        System.exit(code);
    }

    private static String pretty(Duration d) {
        long ms = d.toMillis();
        if (ms < 1000) return ms + " ms";
        long s = ms / 1000;
        long rem = ms % 1000;
        return s + "." + String.format(Locale.ROOT, "%03d", rem) + " s";
    }
}
