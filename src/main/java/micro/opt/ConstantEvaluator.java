package micro.opt;

import micro.TokenType;
import micro.ast.AstExpression;
import micro.ast.AstVisitor;
import micro.ast.BinaryExpression;
import micro.ast.BoolExpression;
import micro.ast.NumberExpression;
import micro.ast.StringExpression;
import micro.ast.UnaryExpression;
import micro.ast.VariableExpression;
import micro.interp.Value;
import micro.interp.ValueType;

import java.util.Optional;

public final class ConstantEvaluator implements AstVisitor<Optional<Value>> {
    public Optional<Value> eval(AstExpression expr) {
        return expr.accept(this);
    }

    public Optional<Boolean> evalBool(AstExpression expr) {
        Optional<Value> v = expr.accept(this);
        if (v.isEmpty() || v.get().type != ValueType.BOOL) {
            return Optional.empty();
        }
        return Optional.of(v.get().asBool());
    }

    @Override
    public Optional<Value> visitNumberExpression(NumberExpression expr) {
        return Optional.of(Value.number(expr.value));
    }

    @Override
    public Optional<Value> visitStringExpression(StringExpression expr) {
        return Optional.of(Value.string(expr.value));
    }

    @Override
    public Optional<Value> visitBoolExpression(BoolExpression expr) {
        return Optional.of(Value.bool(expr.value));
    }

    @Override
    public Optional<Value> visitVariableExpression(VariableExpression expr) {
        return Optional.empty();
    }

    @Override
    public Optional<Value> visitUnaryExpression(UnaryExpression expr) {
        Optional<Value> r = expr.right.accept(this);
        if (r.isEmpty()) {
            return Optional.empty();
        }
        if (expr.op == TokenType.EXCL) {
            if (r.get().type != ValueType.BOOL) {
                return Optional.empty();
            }
            return Optional.of(Value.bool(!r.get().asBool()));
        }
        if (expr.op == TokenType.MINUS) {
            if (r.get().type != ValueType.NUMBER) {
                return Optional.empty();
            }
            return Optional.of(Value.number(-r.get().asNumber()));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Value> visitBinaryExpression(BinaryExpression expr) {
        Optional<Value> l = expr.left.accept(this);
        Optional<Value> r = expr.right.accept(this);
        if (l.isEmpty() || r.isEmpty()) {
            return Optional.empty();
        }
        Value left = l.get();
        Value right = r.get();
        if (expr.op == TokenType.AND) {
            if (left.type != ValueType.BOOL || right.type != ValueType.BOOL) {
                return Optional.empty();
            }
            return Optional.of(Value.bool(left.asBool() && right.asBool()));
        }
        if (expr.op == TokenType.OR) {
            if (left.type != ValueType.BOOL || right.type != ValueType.BOOL) {
                return Optional.empty();
            }
            return Optional.of(Value.bool(left.asBool() || right.asBool()));
        }
        if (expr.op == TokenType.PLUS) {
            if (left.type == ValueType.STRING || right.type == ValueType.STRING) {
                return Optional.of(Value.string(Value.stringify(left) + Value.stringify(right)));
            }
            if (left.type != ValueType.NUMBER || right.type != ValueType.NUMBER) {
                return Optional.empty();
            }
            return Optional.of(Value.number(left.asNumber() + right.asNumber()));
        }
        if (expr.op == TokenType.MINUS || expr.op == TokenType.STAR || expr.op == TokenType.SLASH) {
            if (left.type != ValueType.NUMBER || right.type != ValueType.NUMBER) {
                return Optional.empty();
            }
            double L = left.asNumber();
            double R = right.asNumber();
            if (expr.op == TokenType.MINUS) {
                return Optional.of(Value.number(L - R));
            }
            if (expr.op == TokenType.STAR) {
                return Optional.of(Value.number(L * R));
            }
            return Optional.of(Value.number(L / R));
        }
        if (expr.op == TokenType.EQEQ || expr.op == TokenType.NEQ) {
            boolean eq = equalValues(left, right);
            boolean res = expr.op == TokenType.EQEQ ? eq : !eq;
            return Optional.of(Value.bool(res));
        }
        if (expr.op == TokenType.LT || expr.op == TokenType.LTEQ || expr.op == TokenType.GT || expr.op == TokenType.GTEQ) {
            if (left.type != ValueType.NUMBER || right.type != ValueType.NUMBER) {
                return Optional.empty();
            }
            double L = left.asNumber();
            double R = right.asNumber();
            boolean res;
            if (expr.op == TokenType.LT) {
                res = L < R;
            } else if (expr.op == TokenType.LTEQ) {
                res = L <= R;
            } else if (expr.op == TokenType.GT) {
                res = L > R;
            } else {
                res = L >= R;
            }
            return Optional.of(Value.bool(res));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Value> visitAssignExpression(micro.ast.AssignExpression expr) {
        return Optional.empty();
    }

    @Override
    public Optional<Value> visitArrayExpression(micro.ast.ArrayExpression expr) {
        return Optional.empty();
    }

    @Override
    public Optional<Value> visitIndexExpression(micro.ast.IndexExpression expr) {
        return Optional.empty();
    }

    @Override
    public Optional<Value> visitIndexAssignExpression(micro.ast.IndexAssignExpression expr) {
        return Optional.empty();
    }

    @Override
    public Optional<Value> visitCallExpression(micro.ast.CallExpression expr) {
        return Optional.empty();
    }

    @Override
    public Optional<Value> visitFunctionDeclStatement(micro.ast.FunctionDeclStatement stmt) {
        throw new IllegalStateException();
    }

    @Override
    public Optional<Value> visitReturnStatement(micro.ast.ReturnStatement stmt) {
        throw new IllegalStateException();
    }

    @Override
    public Optional<Value> visitVarStatement(micro.ast.VarStatement stmt) {
        throw new IllegalStateException();
    }

    @Override
    public Optional<Value> visitExpressionStatement(micro.ast.ExpressionStatement stmt) {
        throw new IllegalStateException();
    }

    @Override
    public Optional<Value> visitPrintStatement(micro.ast.PrintStatement stmt) {
        throw new IllegalStateException();
    }

    @Override
    public Optional<Value> visitBlockStatement(micro.ast.BlockStatement stmt) {
        throw new IllegalStateException();
    }

    @Override
    public Optional<Value> visitIfStatement(micro.ast.IfStatement stmt) {
        throw new IllegalStateException();
    }

    @Override
    public Optional<Value> visitWhileStatement(micro.ast.WhileStatement stmt) {
        throw new IllegalStateException();
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
        return a.asString().equals(b.asString());
    }
}
