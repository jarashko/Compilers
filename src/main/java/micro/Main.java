package micro;

import micro.interp.SemanticAnalyzer;
import micro.llmr.AstToLlmrTranslator;
import micro.llmr.LlmrInterpreter;
import micro.llmr.LlmrOptimizer;
import micro.llmr.LlmrProgram;
import micro.opt.DeadCodeEliminator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Main {
    public static void main(String[] args) throws IOException {
        String source;
        if (args.length > 0) {
            source = Files.readString(Path.of(args[0]));
        } else {
            source = """
                    var nums = [1, 2, 3];
                    print nums[0];
                    nums[1] = 42;
                    print nums[1];
                    print nums;
                    """;
        }

        try {
            Lexer lexer = new Lexer(source);
            var tokens = lexer.tokenize();
            Parser parser = new Parser(tokens);
            var program = parser.parse();
            new SemanticAnalyzer().analyze(program);
            program = new DeadCodeEliminator().eliminate(program);
            LlmrProgram llmr = AstToLlmrTranslator.translate(program);
            llmr = LlmrOptimizer.optimize(llmr);
            new LlmrInterpreter().run(llmr);
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}
