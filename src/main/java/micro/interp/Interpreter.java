package micro.interp;

import micro.TokenType;
import micro.ast.ArrayExpression;
import micro.ast.AssignExpression;
import micro.ast.BinaryExpression;
import micro.ast.BlockStatement;
import micro.ast.BoolExpression;
import micro.ast.AstExpression;
import micro.ast.ExpressionStatement;
import micro.ast.IfStatement;
import micro.ast.IndexAssignExpression;
import micro.ast.IndexExpression;
import micro.ast.NumberExpression;
import micro.ast.PrintStatement;
import micro.ast.AstStatement;
import micro.ast.StringExpression;
import micro.ast.UnaryExpression;
import micro.ast.VarStatement;
import micro.ast.VariableExpression;
import micro.ast.WhileStatement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Interpreter {
    private static final class Cell {
        ValueType type;
        ValueType elementType;
        Value value;

        boolean initialized() {
            return value != null;
        }
    }

    private final Map<String, Cell> env = new HashMap<>();

    public void interpret(List<AstStatement> program) {
        for (AstStatement s : program) {
            execute(s);
        }
    }

    private void execute(AstStatement stmt) {
        if (stmt instanceof VarStatement v) {
            declareVariable(v.name, v.initializer);
        } else if (stmt instanceof ExpressionStatement e) {
            evaluate(e.expression);
        } else if (stmt instanceof PrintStatement p) {
            System.out.println(Value.stringify(evaluate(p.expression)));
        } else if (stmt instanceof BlockStatement b) {
            for (AstStatement inner : b.statements) {
                execute(inner);
            }
        } else if (stmt instanceof IfStatement i) {
            Value cond = evaluate(i.condition);
            requireBool(cond, "if condition");
            if (cond.asBool()) {
                execute(i.thenBranch);
            } else if (i.elseBranch != null) {
                execute(i.elseBranch);
            }
        } else if (stmt instanceof WhileStatement w) {
            while (true) {
                Value cond = evaluate(w.condition);
                requireBool(cond, "while condition");
                if (!cond.asBool()) break;
                execute(w.body);
            }
        }
    }

    private void declareVariable(String name, AstExpression initializer) {
        if (env.containsKey(name)) {
            throw failure("Variable '" + name + "' is already declared.");
        }
        Cell c = new Cell();
        if (initializer != null) {
            Value v = evaluate(initializer);
            c.type = v.type;
            c.elementType = v.type == ValueType.ARRAY ? v.elementType() : null;
            c.value = v;
        }
        env.put(name, c);
    }

    private Value evaluate(AstExpression expr) {
        if (expr instanceof NumberExpression n) {
            return Value.number(n.value);
        }
        if (expr instanceof StringExpression s) {
            return Value.string(s.value);
        }
        if (expr instanceof BoolExpression b) {
            return Value.bool(b.value);
        }
        if (expr instanceof ArrayExpression a) {
            return evaluateArrayLiteral(a);
        }
        if (expr instanceof VariableExpression v) {
            return lookup(v.name);
        }
        if (expr instanceof IndexExpression idx) {
            return evaluateIndex(idx);
        }
        if (expr instanceof UnaryExpression u) {
            Value r = evaluate(u.right);
            if (u.op == TokenType.EXCL) {
                requireBool(r, "operand of '!'");
                return Value.bool(!r.asBool());
            }
            if (u.op == TokenType.MINUS) {
                requireNumber(r, "operand of unary '-'");
                return Value.number(-r.asNumber());
            }
            throw failure("Unsupported unary operator: " + u.op);
        }
        if (expr instanceof BinaryExpression b) {
            return evaluateBinary(b);
        }
        if (expr instanceof AssignExpression a) {
            Value v = evaluate(a.value);
            assign(a.name, v);
            return v;
        }
        if (expr instanceof IndexAssignExpression a) {
            Value v = evaluate(a.value);
            storeIndex(a.array, a.index, v);
            return v;
        }
        throw failure("Unknown expression");
    }

    private Value evaluateArrayLiteral(ArrayExpression expr) {
        if (expr.elements.isEmpty()) {
            throw failure("array literal must contain at least one element");
        }
        List<Value> values = new ArrayList<>();
        ValueType elementType = null;
        for (AstExpression element : expr.elements) {
            Value v = evaluate(element);
            if (elementType == null) {
                elementType = v.type;
            } else if (elementType != v.type) {
                throw failure("array elements must have the same type");
            }
            values.add(v);
        }
        return Value.array(elementType, values);
    }

    private Value evaluateIndex(IndexExpression expr) {
        Value array = evaluate(expr.array);
        requireArray(array, "indexing");
        int index = resolveIndex(expr.index, array.asArray().size());
        return array.asArray().get(index);
    }

    private void storeIndex(AstExpression arrayExpr, AstExpression indexExpr, Value value) {
        Value array = evaluate(arrayExpr);
        requireArray(array, "index assignment");
        ArrayValue data = array.asArray();
        if (value.type != data.elementType) {
            throw failure("Type error: cannot assign " + value.type + " to array element (" + data.elementType + ").");
        }
        int index = resolveIndex(indexExpr, data.size());
        data.set(index, value);
        if (arrayExpr instanceof VariableExpression v) {
            Cell cell = env.get(v.name);
            if (cell != null) {
                cell.value = array;
            }
        }
    }

    private int resolveIndex(AstExpression indexExpr, int length) {
        Value indexValue = evaluate(indexExpr);
        requireNumber(indexValue, "array index");
        int index = (int) Math.floor(indexValue.asNumber());
        if (index < 0 || index >= length) {
            throw failure("Array index out of bounds: " + index + " (length " + length + ")");
        }
        return index;
    }

    private Value evaluateBinary(BinaryExpression b) {
        if (b.op == TokenType.AND) {
            Value left = evaluate(b.left);
            requireBool(left, "left operand of &&");
            if (!left.asBool()) return Value.bool(false);
            Value right = evaluate(b.right);
            requireBool(right, "right operand of &&");
            return Value.bool(right.asBool());
        }
        if (b.op == TokenType.OR) {
            Value left = evaluate(b.left);
            requireBool(left, "left operand of ||");
            if (left.asBool()) return Value.bool(true);
            Value right = evaluate(b.right);
            requireBool(right, "right operand of ||");
            return Value.bool(right.asBool());
        }
        Value left = evaluate(b.left);
        Value right = evaluate(b.right);
        if (b.op == TokenType.PLUS) {
            if (left.type == ValueType.STRING || right.type == ValueType.STRING) {
                return Value.string(Value.stringify(left) + Value.stringify(right));
            }
            requireNumber(left, "left operand of '+'");
            requireNumber(right, "right operand of '+'");
            return Value.number(left.asNumber() + right.asNumber());
        }
        if (b.op == TokenType.MINUS || b.op == TokenType.STAR || b.op == TokenType.SLASH) {
            requireNumber(left, "left operand of arithmetic");
            requireNumber(right, "right operand of arithmetic");
            double L = left.asNumber();
            double R = right.asNumber();
            if (b.op == TokenType.MINUS) return Value.number(L - R);
            if (b.op == TokenType.STAR) return Value.number(L * R);
            return Value.number(L / R);
        }
        if (b.op == TokenType.EQEQ || b.op == TokenType.NEQ) {
            boolean eq = equalValues(left, right);
            boolean res = b.op == TokenType.EQEQ ? eq : !eq;
            return Value.bool(res);
        }
        if (b.op == TokenType.LT || b.op == TokenType.LTEQ || b.op == TokenType.GT || b.op == TokenType.GTEQ) {
            requireNumber(left, "left operand of comparison");
            requireNumber(right, "right operand of comparison");
            double L = left.asNumber();
            double R = right.asNumber();
            boolean res;
            if (b.op == TokenType.LT) res = L < R;
            else if (b.op == TokenType.LTEQ) res = L <= R;
            else if (b.op == TokenType.GT) res = L > R;
            else res = L >= R;
            return Value.bool(res);
        }
        throw failure("Unsupported binary operator: " + b.op);
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

    private void assign(String name, Value value) {
        Cell cell = env.get(name);
        if (cell == null) {
            throw failure("Undefined variable '" + name + "'. Declare with var first.");
        }
        if (!cell.initialized()) {
            cell.type = value.type;
            cell.elementType = value.type == ValueType.ARRAY ? value.elementType() : null;
            cell.value = value;
            return;
        }
        if (cell.type != value.type) {
            throw failure("Type error: cannot assign " + value.type + " to '" + name + "' (" + cell.type + ").");
        }
        if (cell.type == ValueType.ARRAY && cell.elementType != value.elementType()) {
            throw failure("Type error: cannot assign array with element type " + value.elementType()
                    + " to '" + name + "' (" + cell.elementType + ").");
        }
        cell.value = value;
    }

    private Value lookup(String name) {
        Cell cell = env.get(name);
        if (cell == null || !cell.initialized()) {
            throw failure("Variable '" + name + "' is not initialized.");
        }
        return cell.value;
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

    private static void requireArray(Value v, String ctx) {
        if (v.type != ValueType.ARRAY) {
            throw failure("Type error: expected ARRAY in " + ctx + ", got " + v.type);
        }
    }

    private static RuntimeException failure(String msg) {
        return new RuntimeException("[Runtime Error] " + msg);
    }
}
