package micro.llmr;

import micro.interp.ArrayValue;
import micro.interp.Value;
import micro.interp.ValueType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LlmrInterpreter {
    private final Deque<Value> stack = new ArrayDeque<>();
    private final Map<String, Value> env = new HashMap<>();

    public void run(LlmrProgram program) {
        List<LlmrInstr> code = program.instructions;
        Map<Integer, Integer> labelPositions = buildLabelIndex(code);
        int pc = 0;
        while (pc < code.size()) {
            LlmrInstr instr = code.get(pc);
            switch (instr.op) {
                case LABEL -> pc++;
                case HALT -> {
                    return;
                }
                case JUMP -> pc = labelPositions.get(instr.label);
                case JUMP_IF_FALSE -> {
                    Value v = pop();
                    requireBool(v, "jump condition");
                    pc = v.asBool() ? pc + 1 : labelPositions.get(instr.label);
                }
                case JUMP_IF_TRUE -> {
                    Value v = pop();
                    requireBool(v, "jump condition");
                    pc = v.asBool() ? labelPositions.get(instr.label) : pc + 1;
                }
                case PRINT -> {
                    System.out.println(Value.stringify(pop()));
                    pc++;
                }
                case POP -> {
                    pop();
                    pc++;
                }
                case LOAD_CONST -> {
                    push(instr.constant);
                    pc++;
                }
                case LOAD_VAR -> {
                    push(lookup(instr.name));
                    pc++;
                }
                case STORE_VAR -> {
                    assign(instr.name, pop());
                    pc++;
                }
                case ADD -> {
                    push(applyAdd(pop(), pop()));
                    pc++;
                }
                case SUB -> {
                    Value right = pop();
                    Value left = pop();
                    push(binaryNumber(left, right, LlmrOp.SUB));
                    pc++;
                }
                case MUL -> {
                    Value right = pop();
                    Value left = pop();
                    push(binaryNumber(left, right, LlmrOp.MUL));
                    pc++;
                }
                case DIV -> {
                    Value right = pop();
                    Value left = pop();
                    push(binaryNumber(left, right, LlmrOp.DIV));
                    pc++;
                }
                case NEG -> {
                    Value v = pop();
                    requireNumber(v, "negation");
                    push(Value.number(-v.asNumber()));
                    pc++;
                }
                case NOT -> {
                    Value v = pop();
                    requireBool(v, "logical not");
                    push(Value.bool(!v.asBool()));
                    pc++;
                }
                case EQ, NEQ, LT, LTE, GT, GTE -> {
                    Value right = pop();
                    Value left = pop();
                    push(compare(left, right, instr.op));
                    pc++;
                }
                case MAKE_ARRAY -> {
                    push(makeArray(instr.label));
                    pc++;
                }
                case ARRAY_GET -> {
                    int index = toIndex(pop());
                    Value array = pop();
                    push(readElement(array, index));
                    pc++;
                }
                case ARRAY_LOAD -> {
                    int index = toIndex(pop());
                    push(readElement(lookup(instr.name), index));
                    pc++;
                }
                case ARRAY_STORE -> {
                    Value value = pop();
                    int index = toIndex(pop());
                    writeElement(instr.name, index, value);
                    pc++;
                }
                case ARRAY_SET -> {
                    Value value = pop();
                    int index = toIndex(pop());
                    Value array = pop();
                    writeElement(array, index, value);
                    push(array);
                    pc++;
                }
            }
        }
    }

    private Value makeArray(int count) {
        if (count <= 0) {
            throw failure("array must contain at least one element");
        }
        List<Value> elements = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            elements.add(0, pop());
        }
        ValueType elementType = elements.get(0).type;
        for (Value element : elements) {
            if (element.type != elementType) {
                throw failure("array elements must have the same type");
            }
        }
        return Value.array(elementType, elements);
    }

    private int toIndex(Value indexValue) {
        requireNumber(indexValue, "array index");
        return (int) Math.floor(indexValue.asNumber());
    }

    private static int boundsCheck(ArrayValue array, int index) {
        if (index < 0 || index >= array.size()) {
            throw failure("Array index out of bounds: " + index + " (length " + array.size() + ")");
        }
        return index;
    }

    private static Value readElement(Value array, int rawIndex) {
        requireArray(array, "indexing");
        ArrayValue data = array.asArray();
        return data.get(boundsCheck(data, rawIndex));
    }

    private void writeElement(String name, int rawIndex, Value value) {
        Value array = lookup(name);
        writeElement(array, rawIndex, value);
        env.put(name, array);
    }

    private static void writeElement(Value array, int rawIndex, Value value) {
        requireArray(array, "index assignment");
        ArrayValue data = array.asArray();
        int index = boundsCheck(data, rawIndex);
        if (value.type != data.elementType) {
            throw failure("Type error: cannot assign " + value.type + " to array element (" + data.elementType + ").");
        }
        data.set(index, value);
    }

    private static void requireArray(Value v, String ctx) {
        if (v.type != ValueType.ARRAY) {
            throw failure("Type error: expected ARRAY in " + ctx + ", got " + v.type);
        }
    }

    private static Map<Integer, Integer> buildLabelIndex(List<LlmrInstr> code) {
        Map<Integer, Integer> positions = new HashMap<>();
        for (int i = 0; i < code.size(); i++) {
            if (code.get(i).op == LlmrOp.LABEL) {
                positions.put(code.get(i).label, i);
            }
        }
        return positions;
    }

    private void push(Value v) {
        stack.push(v);
    }

    private Value pop() {
        if (stack.isEmpty()) {
            throw failure("LLMR stack underflow.");
        }
        return stack.pop();
    }

    private Value lookup(String name) {
        Value v = env.get(name);
        if (v == null) {
            throw failure("Variable '" + name + "' is not initialized.");
        }
        return v;
    }

    private void assign(String name, Value value) {
        Value existing = env.get(name);
        if (existing == null) {
            env.put(name, value);
            return;
        }
        if (existing.type != value.type) {
            throw failure("Type error: cannot assign " + value.type + " to '" + name + "' (" + existing.type + ").");
        }
        if (existing.type == ValueType.ARRAY && existing.elementType() != value.elementType()) {
            throw failure("Type error: cannot assign array with element type " + value.elementType()
                    + " to '" + name + "' (" + existing.elementType() + ").");
        }
        env.put(name, value);
    }

    private static Value applyAdd(Value right, Value left) {
        if (left.type == ValueType.STRING || right.type == ValueType.STRING) {
            return Value.string(Value.stringify(left) + Value.stringify(right));
        }
        requireNumber(left, "left operand of '+'");
        requireNumber(right, "right operand of '+'");
        return Value.number(left.asNumber() + right.asNumber());
    }

    private static Value binaryNumber(Value left, Value right, LlmrOp op) {
        requireNumber(left, "arithmetic operand");
        requireNumber(right, "arithmetic operand");
        double L = left.asNumber();
        double R = right.asNumber();
        return switch (op) {
            case SUB -> Value.number(L - R);
            case MUL -> Value.number(L * R);
            case DIV -> Value.number(L / R);
            default -> throw new IllegalStateException();
        };
    }

    private static Value compare(Value left, Value right, LlmrOp op) {
        boolean res;
        if (op == LlmrOp.EQ || op == LlmrOp.NEQ) {
            res = equalValues(left, right);
            if (op == LlmrOp.NEQ) {
                res = !res;
            }
            return Value.bool(res);
        }
        requireNumber(left, "comparison operand");
        requireNumber(right, "comparison operand");
        double L = left.asNumber();
        double R = right.asNumber();
        res = switch (op) {
            case LT -> L < R;
            case LTE -> L <= R;
            case GT -> L > R;
            case GTE -> L >= R;
            default -> throw new IllegalStateException();
        };
        return Value.bool(res);
    }

    private static boolean equalValues(Value a, Value b) {
        if (a.type != b.type) {
            return false;
        }
        if (a.type == ValueType.NUMBER) {
            return Double.doubleToLongBits(a.asNumber()) == Double.doubleToLongBits(b.asNumber());
        }
        if (a.type == ValueType.BOOL) {
            return a.asBool() == b.asBool();
        }
        if (a.type == ValueType.STRING) {
            return a.asString().equals(b.asString());
        }
        ArrayValue aa = a.asArray();
        ArrayValue bb = b.asArray();
        if (aa.size() != bb.size() || aa.elementType != bb.elementType) {
            return false;
        }
        for (int i = 0; i < aa.size(); i++) {
            if (!equalValues(aa.get(i), bb.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static void requireNumber(Value v, String ctx) {
        if (v.type != ValueType.NUMBER) {
            throw failure("Type error: expected NUMBER in " + ctx + ", got " + v.type);
        }
    }

    private static void requireBool(Value v, String ctx) {
        if (v.type != ValueType.BOOL) {
            throw failure("Type error: expected BOOL in " + ctx + ", got " + v.type);
        }
    }

    private static RuntimeException failure(String msg) {
        return new RuntimeException("[Runtime Error] " + msg);
    }
}
