package micro.llmr;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LlmrOptimizer {
    public static LlmrProgram optimize(LlmrProgram program) {
        List<LlmrInstr> code = program.mutableInstructions();
        code = removeUnreachable(code);
        code = removeDeadStores(code);
        return program.withInstructions(code);
    }

    private static List<LlmrInstr> removeUnreachable(List<LlmrInstr> code) {
        if (code.isEmpty()) {
            return code;
        }
        boolean[] reachable = new boolean[code.size()];
        Deque<Integer> work = new ArrayDeque<>();
        reachable[0] = true;
        work.add(0);
        while (!work.isEmpty()) {
            int pc = work.removeFirst();
            LlmrInstr instr = code.get(pc);
            switch (instr.op) {
                case JUMP -> enqueue(work, reachable, labelIndex(code, instr.label));
                case JUMP_IF_FALSE, JUMP_IF_TRUE -> {
                    enqueue(work, reachable, pc + 1);
                    enqueue(work, reachable, labelIndex(code, instr.label));
                }
                case HALT -> {
                }
                default -> enqueue(work, reachable, pc + 1);
            }
        }
        List<LlmrInstr> kept = new ArrayList<>();
        for (int i = 0; i < code.size(); i++) {
            if (reachable[i]) {
                kept.add(code.get(i));
            }
        }
        return remapLabels(kept);
    }

    private static void enqueue(Deque<Integer> work, boolean[] reachable, int index) {
        if (index < 0 || index >= reachable.length) {
            return;
        }
        if (!reachable[index]) {
            reachable[index] = true;
            work.add(index);
        }
    }

    private static int labelIndex(List<LlmrInstr> code, int label) {
        for (int i = 0; i < code.size(); i++) {
            if (code.get(i).op == LlmrOp.LABEL && code.get(i).label == label) {
                return i;
            }
        }
        throw new IllegalStateException("Missing label " + label);
    }

    private static List<LlmrInstr> remapLabels(List<LlmrInstr> code) {
        Map<Integer, Integer> remap = new HashMap<>();
        int next = 0;
        for (LlmrInstr instr : code) {
            if (instr.op == LlmrOp.LABEL) {
                remap.put(instr.label, next++);
            }
        }
        List<LlmrInstr> out = new ArrayList<>();
        for (LlmrInstr instr : code) {
            out.add(remapInstruction(instr, remap));
        }
        return out;
    }

    private static LlmrInstr remapInstruction(LlmrInstr instr, Map<Integer, Integer> remap) {
        return switch (instr.op) {
            case LABEL -> new LlmrInstr(LlmrOp.LABEL, remap.get(instr.label));
            case JUMP, JUMP_IF_FALSE, JUMP_IF_TRUE -> new LlmrInstr(instr.op, remap.get(instr.label));
            case LOAD_VAR, STORE_VAR, ARRAY_LOAD, ARRAY_STORE -> new LlmrInstr(instr.op, instr.name);
            case LOAD_CONST -> new LlmrInstr(LlmrOp.LOAD_CONST, instr.constant);
            case MAKE_ARRAY -> new LlmrInstr(LlmrOp.MAKE_ARRAY, instr.label);
            default -> new LlmrInstr(instr.op);
        };
    }

    private static List<LlmrInstr> removeDeadStores(List<LlmrInstr> code) {
        Set<String> loaded = new HashSet<>();
        boolean[] remove = new boolean[code.size()];
        for (int i = code.size() - 1; i >= 0; i--) {
            LlmrInstr instr = code.get(i);
            if (instr.op == LlmrOp.LOAD_VAR) {
                loaded.add(instr.name);
            } else if (instr.op == LlmrOp.STORE_VAR) {
                if (!loaded.contains(instr.name)) {
                    remove[i] = true;
                } else {
                    loaded.remove(instr.name);
                }
            }
        }
        List<LlmrInstr> out = new ArrayList<>();
        for (int i = 0; i < code.size(); i++) {
            if (!remove[i]) {
                out.add(code.get(i));
            }
        }
        return out;
    }
}
