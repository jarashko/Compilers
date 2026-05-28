package micro.llmr;

import micro.interp.Value;

public final class LlmrInstr {
    public final LlmrOp op;
    public final int label;
    public final String name;
    public final Value constant;

    public LlmrInstr(LlmrOp op) {
        this(op, -1, null, null);
    }

    public LlmrInstr(LlmrOp op, int label) {
        this(op, label, null, null);
    }

    public LlmrInstr(LlmrOp op, String name) {
        this(op, -1, name, null);
    }

    public LlmrInstr(LlmrOp op, Value constant) {
        this(op, -1, null, constant);
    }

    public LlmrInstr(LlmrOp op, int label, String name, Value constant) {
        this.op = op;
        this.label = label;
        this.name = name;
        this.constant = constant;
    }
}
