package micro.interp;

import micro.TokenType;
import micro.ast.ArrayExpression;
import micro.ast.AssignExpression;
import micro.ast.AstExpression;
import micro.ast.AstStatement;
import micro.ast.BinaryExpression;
import micro.ast.BlockStatement;
import micro.ast.BoolExpression;
import micro.ast.CallExpression;
import micro.ast.ExpressionStatement;
import micro.ast.FunctionDeclStatement;
import micro.ast.IfStatement;
import micro.ast.IndexAssignExpression;
import micro.ast.IndexExpression;
import micro.ast.NumberExpression;
import micro.ast.PrintStatement;
import micro.ast.ReturnStatement;
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
    private final RuntimeEnvironment env = new RuntimeEnvironment();
    private final Map<String, FunctionInfo> functions = new HashMap<>();

    public void interpret(List<AstStatement> program) {
        functions.clear();
        for (AstStatement stmt : program) {
            if (stmt instanceof FunctionDeclStatement f) {
                functions.put(f.name, new FunctionInfo(f.returnType, f.name, f.paramTypes, f.paramNames, f.body));
            }
        }
        for (AstStatement stmt : program) {
            if (!(stmt instanceof FunctionDeclStatement)) {
                execute(stmt);
            }
        }
    }

    public void execute(AstStatement stmt) {
        if (stmt instanceof VarStatement v) {
            declareVariable(v.name, v.initializer);
        } else if (stmt instanceof ExpressionStatement e) {
            eval(e.expression);
        } else if (stmt instanceof PrintStatement p) {
            System.out.println(Value.stringify(eval(p.expression)));
        } else if (stmt instanceof ReturnStatement r) {
            handleReturn(r);
        } else if (stmt instanceof BlockStatement b) {
            env.pushScope();
            for (AstStatement inner : b.statements) {
                execute(inner);
            }
            env.popScope();
        } else if (stmt instanceof IfStatement i) {
            Value cond = eval(i.condition);
            requireBool(cond, "if condition");
            if (cond.asBool()) {
                execute(i.thenBranch);
            } else if (i.elseBranch != null) {
                execute(i.elseBranch);
            }
        } else if (stmt instanceof WhileStatement w) {
            while (true) {
                Value cond = eval(w.condition);
                requireBool(cond, "while condition");
                if (!cond.asBool()) {
                    break;
                }
                execute(w.body);
            }
        }
    }

    public Value eval(AstExpression expr) {
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
            return env.lookup(v.name);
        }
        if (expr instanceof CallExpression c) {
            return callFunction(c);
        }
        if (expr instanceof IndexExpression idx) {
            return evaluateIndex(idx);
        }
        if (expr instanceof UnaryExpression u) {
            Value r = eval(u.right);
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
            Value v = eval(a.value);
            env.assign(a.name, v);
            return v;
        }
        if (expr instanceof IndexAssignExpression a) {
            Value v = eval(a.value);
            storeIndex(a.array, a.index, v);
            return v;
        }
        throw failure("Unknown expression");
    }

    private void handleReturn(ReturnStatement r) {
        Value value = r.value == null ? null : eval(r.value);
        throw new ReturnException(value);
    }

    private Value callFunction(CallExpression call) {
        FunctionInfo fn = functions.get(call.name);
        if (fn == null) {
            throw failure("Undefined function '" + call.name + "'.");
        }
        if (call.arguments.size() != fn.paramNames.size()) {
            throw failure("Function '" + call.name + "' expects " + fn.paramNames.size()
                    + " arguments, got " + call.arguments.size());
        }
        env.pushScope();
        try {
            for (int i = 0; i < fn.paramNames.size(); i++) {
                Value arg = eval(call.arguments.get(i));
                if (arg.type != fn.paramTypes.get(i)) {
                    throw failure("Argument " + (i + 1) + " of '" + call.name + "' must be "
                            + fn.paramTypes.get(i) + ", got " + arg.type);
                }
                RuntimeEnvironment.Cell cell = new RuntimeEnvironment.Cell();
                cell.type = arg.type;
                cell.elementType = arg.type == ValueType.ARRAY ? arg.elementType() : null;
                cell.value = arg;
                env.declare(fn.paramNames.get(i), cell);
            }
            try {
                execute(fn.body);
                if (fn.returnType != ValueType.VOID) {
                    throw failure("Function '" + call.name + "' did not return a value.");
                }
                return Value.number(0);
            } catch (ReturnException ret) {
                if (fn.returnType == ValueType.VOID) {
                    if (ret.value != null) {
                        throw failure("void function '" + call.name + "' cannot return a value.");
                    }
                    return Value.number(0);
                }
                if (ret.value == null) {
                    throw failure("Function '" + call.name + "' must return " + fn.returnType);
                }
                if (ret.value.type != fn.returnType) {
                    throw failure("Return type mismatch in '" + call.name + "'.");
                }
                return ret.value;
            }
        } finally {
            env.popScope();
        }
    }

    private void declareVariable(String name, AstExpression initializer) {
        RuntimeEnvironment.Cell c = new RuntimeEnvironment.Cell();
        if (initializer != null) {
            Value v = eval(initializer);
            c.type = v.type;
            c.elementType = v.type == ValueType.ARRAY ? v.elementType() : null;
            c.value = v;
        }
        env.declare(name, c);
    }

    private Value evaluateArrayLiteral(ArrayExpression expr) {
        if (expr.elements.isEmpty()) {
            throw failure("array literal must contain at least one element");
        }
        List<Value> values = new ArrayList<>();
        ValueType elementType = null;
        for (AstExpression element : expr.elements) {
            Value v = eval(element);
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
        Value array = eval(expr.array);
        requireArray(array, "indexing");
        int index = resolveIndex(expr.index, array.asArray().size());
        return array.asArray().get(index);
    }

    private void storeIndex(AstExpression arrayExpr, AstExpression indexExpr, Value value) {
        Value array = eval(arrayExpr);
        requireArray(array, "index assignment");
        ArrayValue data = array.asArray();
        if (value.type != data.elementType) {
            throw failure("Type error: cannot assign " + value.type + " to array element (" + data.elementType + ").");
        }
        int index = resolveIndex(indexExpr, data.size());
        data.set(index, value);
    }

    private int resolveIndex(AstExpression indexExpr, int length) {
        Value indexValue = eval(indexExpr);
        requireNumber(indexValue, "array index");
        int index = (int) Math.floor(indexValue.asNumber());
        if (index < 0 || index >= length) {
            throw failure("Array index out of bounds: " + index + " (length " + length + ")");
        }
        return index;
    }

    private Value evaluateBinary(BinaryExpression b) {
        if (b.op == TokenType.AND) {
            Value left = eval(b.left);
            requireBool(left, "left operand of &&");
            if (!left.asBool()) {
                return Value.bool(false);
            }
            Value right = eval(b.right);
            requireBool(right, "right operand of &&");
            return Value.bool(right.asBool());
        }
        if (b.op == TokenType.OR) {
            Value left = eval(b.left);
            requireBool(left, "left operand of ||");
            if (left.asBool()) {
                return Value.bool(true);
            }
            Value right = eval(b.right);
            requireBool(right, "right operand of ||");
            return Value.bool(right.asBool());
        }
        Value left = eval(b.left);
        Value right = eval(b.right);
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
            double l = left.asNumber();
            double r = right.asNumber();
            if (b.op == TokenType.MINUS) {
                return Value.number(l - r);
            }
            if (b.op == TokenType.STAR) {
                return Value.number(l * r);
            }
            return Value.number(l / r);
        }
        if (b.op == TokenType.EQEQ || b.op == TokenType.NEQ) {
            boolean eq = equalValues(left, right);
            return Value.bool(b.op == TokenType.EQEQ ? eq : !eq);
        }
        if (b.op == TokenType.LT || b.op == TokenType.LTEQ || b.op == TokenType.GT || b.op == TokenType.GTEQ) {
            requireNumber(left, "left operand of comparison");
            requireNumber(right, "right operand of comparison");
            double l = left.asNumber();
            double r = right.asNumber();
            boolean res;
            if (b.op == TokenType.LT) {
                res = l < r;
            } else if (b.op == TokenType.LTEQ) {
                res = l <= r;
            } else if (b.op == TokenType.GT) {
                res = l > r;
            } else {
                res = l >= r;
            }
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
