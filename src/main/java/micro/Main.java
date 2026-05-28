package micro;

import micro.ast.AstStatement;
import micro.ast.FunctionDeclStatement;
import micro.interp.Interpreter;
import micro.interp.SemanticAnalyzer;
import micro.llmr.AstToLlmrTranslator;
import micro.llmr.LlmrInterpreter;
import micro.llmr.LlmrOptimizer;
import micro.llmr.LlmrProgram;
import micro.opt.ConstantFoldingOptimizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class Main {
    public static void main(String[] args) throws IOException {
        String source;
        String title = "Built-in demo";
        if (args.length > 0) {
            source = Files.readString(Path.of(args[0]));
            title = args[0];
        } else {
            source = """
                    function number add(number a, number b) {
                        return a + b;
                    }

                    function number factorial(number n) {
                        if (n <= 1) {
                            return 1;
                        }
                        return n * factorial(n - 1);
                    }

                    var folded = 3 * 4 + 2;
                    print folded;
                    print add(10, 4);
                    print factorial(6);

                    var nums = [1, 2, 3];
                    print nums[0];
                    nums[1] = 42;
                    print nums;
                    """;
        }

        System.out.println("=".repeat(60));
        System.out.println("  " + title);
        System.out.println("=".repeat(60));

        try {
            System.out.println("\n[1] Lexical Analysis...");
            Lexer lexer = new Lexer(source);
            var tokens = lexer.tokenize();
            System.out.println("    Tokens: " + tokens.size());

            System.out.println("[2] Parsing...");
            Parser parser = new Parser(tokens);
            List<AstStatement> program = parser.parse();
            System.out.println("    AST built.");

            System.out.println("[3] Type Checking...");
            new SemanticAnalyzer().analyze(program);
            System.out.println("    Type check passed.");

            System.out.println("[4] Optimizing (Constant Folding + Dead Code Elimination)...");
            program = new ConstantFoldingOptimizer().optimize(program);
            System.out.println("    Optimization done.");

            System.out.println("[5] Interpreting optimized AST...");
            System.out.println("    -- Output ------------------------------");
            new Interpreter().interpret(program);
            System.out.println("    ----------------------------------------");

            if (!containsFunctions(program)) {
                System.out.println("[6] Generating LLMR...");
                LlmrProgram llmr = AstToLlmrTranslator.translate(program);
                llmr = LlmrOptimizer.optimize(llmr);
                Path out = Path.of("demo_outputs/output.llmr");
                Files.createDirectories(out.getParent());
                dumpLlmr(llmr, out);
                System.out.println("    Written to: " + out);
            } else {
                System.out.println("[6] LLMR generation skipped (program contains functions).");
            }
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private static boolean containsFunctions(List<AstStatement> program) {
        for (AstStatement stmt : program) {
            if (stmt instanceof FunctionDeclStatement) {
                return true;
            }
        }
        return false;
    }

    private static void dumpLlmr(LlmrProgram program, Path path) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < program.instructions.size(); i++) {
            var instr = program.instructions.get(i);
            sb.append(i).append(": ").append(instr.op);
            if (instr.label >= 0) {
                sb.append(" L").append(instr.label);
            }
            if (instr.name != null) {
                sb.append(" ").append(instr.name);
            }
            if (instr.constant != null) {
                sb.append(" ").append(micro.interp.Value.stringify(instr.constant));
            }
            sb.append('\n');
        }
        Files.writeString(path, sb.toString());
    }
}
