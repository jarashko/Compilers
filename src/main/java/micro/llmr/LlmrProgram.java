package micro.llmr;

import java.util.ArrayList;
import java.util.List;

public final class LlmrProgram {
    public final List<LlmrInstr> instructions;

    public LlmrProgram(List<LlmrInstr> instructions) {
        this.instructions = List.copyOf(instructions);
    }

    public LlmrProgram copy(List<LlmrInstr> instructions) {
        return new LlmrProgram(instructions);
    }

    public static LlmrProgram empty() {
        return new LlmrProgram(List.of());
    }

    public LlmrProgram withInstructions(List<LlmrInstr> instructions) {
        return new LlmrProgram(instructions);
    }

    public List<LlmrInstr> mutableInstructions() {
        return new ArrayList<>(instructions);
    }
}
