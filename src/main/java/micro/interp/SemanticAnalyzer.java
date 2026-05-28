package micro.interp;

import micro.TokenType;
import micro.ast.ArrayExpression;
import micro.ast.AssignExpression;
import micro.ast.AstExpression;
import micro.ast.AstStatement;
import micro.ast.BinaryExpression;
import micro.ast.BlockStatement;
import micro.ast.BoolExpression;
import micro.ast.ExpressionStatement;
import micro.ast.IfStatement;
import micro.ast.IndexAssignExpression;
import micro.ast.IndexExpression;
import micro.ast.NumberExpression;
import micro.ast.PrintStatement;
import micro.ast.StringExpression;
import micro.ast.UnaryExpression;
import micro.ast.VarStatement;
import micro.ast.VariableExpression;
import micro.ast.WhileStatement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SemanticAnalyzer {
    private static final class VarInfo {
        ValueType type;
        ValueType elementType;
        boolean initialized;
    }

    private static final class TypeResult {
        final ValueType type;
        final ValueType elementType;

        TypeResult(ValueType type, ValueType elementType) {
            this.type = type;
            this.elementType = elementType;
        }

        static TypeResult scalar(ValueType type) {
            return new TypeResult(type, null);
        }

        static TypeResult array(ValueType elementType) {
            return new TypeResult(ValueType.ARRAY, elementType);
        }
    }

    private final Map<String, VarInfo> symbols = new HashMap<>();
    private final List<String> errors = new ArrayList<>();

    public void analyze(List<AstStatement> program) {
        errors.clear();
        symbols.clear();
        for (AstStatement s : program) {
            analyzeStatement(s);
        }
        if (!errors.isEmpty()) {
            List<String> prefixed = new ArrayList<>();
            for (String e : errors) {
                prefixed.add("[Semantic Error] " + e);
            }
            throw new SemanticException(prefixed);
        }
    }

    private void analyzeStatement(AstStatement stmt) {
        if (stmt instanceof VarStatement v) {
            analyzeVarDeclaration(v.name, v.initializer);
        } else if (stmt instanceof ExpressionStatement e) {
            inferExpr(e.expression);
        } else if (stmt instanceof PrintStatement p) {
            inferExpr(p.expression);
        } else if (stmt instanceof BlockStatement b) {
            for (AstStatement inner : b.statements) {
                analyzeStatement(inner);
            }
        } else if (stmt instanceof IfStatement i) {
            TypeResult ct = inferExpr(i.condition);
            if (ct.type != ValueType.BOOL) {
                error("if condition must be BOOL, got " + ct.type);
            }
            analyzeStatement(i.thenBranch);
            if (i.elseBranch != null) {
                analyzeStatement(i.elseBranch);
            }
        } else if (stmt instanceof WhileStatement w) {
            TypeResult ct = inferExpr(w.condition);
            if (ct.type != ValueType.BOOL) {
                error("while condition must be BOOL, got " + ct.type);
            }
            analyzeStatement(w.body);
        }
    }

    private void analyzeVarDeclaration(String name, AstExpression initializer) {
        if (symbols.containsKey(name)) {
            error("Variable '" + name + "' is already declared.");
            return;
        }
        VarInfo info = new VarInfo();
        if (initializer != null) {
            TypeResult tr = inferExpr(initializer);
            info.type = tr.type;
            info.elementType = tr.elementType;
            info.initialized = true;
        }
        symbols.put(name, info);
    }

    private TypeResult inferExpr(AstExpression expr) {
        if (expr instanceof NumberExpression) {
            return TypeResult.scalar(ValueType.NUMBER);
        }
        if (expr instanceof StringExpression) {
            return TypeResult.scalar(ValueType.STRING);
        }
        if (expr instanceof BoolExpression) {
            return TypeResult.scalar(ValueType.BOOL);
        }
        if (expr instanceof ArrayExpression a) {
            return inferArrayLiteral(a);
        }
        if (expr instanceof VariableExpression v) {
            VarInfo info = symbols.get(v.name);
            if (info == null) {
                error("Undefined variable '" + v.name + "'.");
                return TypeResult.scalar(ValueType.NUMBER);
            }
            if (!info.initialized) {
                error("Variable '" + v.name + "' is not initialized.");
                return info.type != null ? TypeResult.scalar(info.type) : TypeResult.scalar(ValueType.NUMBER);
            }
            if (info.type == ValueType.ARRAY) {
                return TypeResult.array(info.elementType);
            }
            return TypeResult.scalar(info.type);
        }
        if (expr instanceof IndexExpression idx) {
            return inferIndex(idx);
        }
        if (expr instanceof UnaryExpression u) {
            TypeResult r = inferExpr(u.right);
            if (u.op == TokenType.EXCL) {
                if (r.type != ValueType.BOOL) {
                    error("operand of '!' must be BOOL, got " + r.type);
                }
                return TypeResult.scalar(ValueType.BOOL);
            }
            if (u.op == TokenType.MINUS) {
                if (r.type != ValueType.NUMBER) {
                    error("operand of unary '-' must be NUMBER, got " + r.type);
                }
                return TypeResult.scalar(ValueType.NUMBER);
            }
            error("Unsupported unary operator: " + u.op);
            return TypeResult.scalar(ValueType.NUMBER);
        }
        if (expr instanceof BinaryExpression b) {
            return inferBinary(b);
        }
        if (expr instanceof AssignExpression a) {
            TypeResult vt = inferExpr(a.value);
            applyAssign(a.name, vt);
            return vt;
        }
        if (expr instanceof IndexAssignExpression a) {
            TypeResult vt = inferExpr(a.value);
            applyIndexAssign(a.array, a.index, vt);
            return vt;
        }
        error("Unknown expression type");
        return TypeResult.scalar(ValueType.NUMBER);
    }

    private TypeResult inferArrayLiteral(ArrayExpression expr) {
        if (expr.elements.isEmpty()) {
            error("array literal must contain at least one element");
            return TypeResult.array(ValueType.NUMBER);
        }
        ValueType elementType = null;
        for (AstExpression element : expr.elements) {
            TypeResult tr = inferExpr(element);
            if (tr.type == ValueType.ARRAY) {
                error("nested arrays are not supported");
                continue;
            }
            if (elementType == null) {
                elementType = tr.type;
            } else if (elementType != tr.type) {
                error("array elements must have the same type");
            }
        }
        return TypeResult.array(elementType);
    }

    private TypeResult inferIndex(IndexExpression expr) {
        TypeResult arrayType = inferExpr(expr.array);
        if (arrayType.type != ValueType.ARRAY) {
            error("indexing requires an array, got " + arrayType.type);
            return TypeResult.scalar(ValueType.NUMBER);
        }
        TypeResult indexType = inferExpr(expr.index);
        if (indexType.type != ValueType.NUMBER) {
            error("array index must be NUMBER, got " + indexType.type);
        }
        return TypeResult.scalar(arrayType.elementType);
    }

    private void applyIndexAssign(AstExpression array, AstExpression index, TypeResult valueType) {
        TypeResult arrayType = inferExpr(array);
        if (arrayType.type != ValueType.ARRAY) {
            error("index assignment requires an array, got " + arrayType.type);
            return;
        }
        TypeResult indexType = inferExpr(index);
        if (indexType.type != ValueType.NUMBER) {
            error("array index must be NUMBER, got " + indexType.type);
        }
        if (valueType.type != arrayType.elementType) {
            error("cannot assign " + valueType.type + " to array element (" + arrayType.elementType + ")");
        }
    }

    private TypeResult inferBinary(BinaryExpression b) {
        if (b.op == TokenType.AND || b.op == TokenType.OR) {
            TypeResult l = inferExpr(b.left);
            TypeResult r = inferExpr(b.right);
            if (l.type != ValueType.BOOL) {
                error("left operand of logical operator must be BOOL, got " + l.type);
            }
            if (r.type != ValueType.BOOL) {
                error("right operand of logical operator must be BOOL, got " + r.type);
            }
            return TypeResult.scalar(ValueType.BOOL);
        }
        TypeResult left = inferExpr(b.left);
        TypeResult right = inferExpr(b.right);
        if (b.op == TokenType.PLUS) {
            if (left.type == ValueType.STRING || right.type == ValueType.STRING) {
                return TypeResult.scalar(ValueType.STRING);
            }
            if (left.type != ValueType.NUMBER) {
                error("left operand of '+' must be NUMBER when concatenation does not apply, got " + left.type);
            }
            if (right.type != ValueType.NUMBER) {
                error("right operand of '+' must be NUMBER when concatenation does not apply, got " + right.type);
            }
            return TypeResult.scalar(ValueType.NUMBER);
        }
        if (b.op == TokenType.MINUS || b.op == TokenType.STAR || b.op == TokenType.SLASH) {
            if (left.type != ValueType.NUMBER) {
                error("left operand of arithmetic must be NUMBER, got " + left.type);
            }
            if (right.type != ValueType.NUMBER) {
                error("right operand of arithmetic must be NUMBER, got " + right.type);
            }
            return TypeResult.scalar(ValueType.NUMBER);
        }
        if (b.op == TokenType.EQEQ || b.op == TokenType.NEQ) {
            return TypeResult.scalar(ValueType.BOOL);
        }
        if (b.op == TokenType.LT || b.op == TokenType.LTEQ || b.op == TokenType.GT || b.op == TokenType.GTEQ) {
            if (left.type != ValueType.NUMBER) {
                error("left operand of comparison must be NUMBER, got " + left.type);
            }
            if (right.type != ValueType.NUMBER) {
                error("right operand of comparison must be NUMBER, got " + right.type);
            }
            return TypeResult.scalar(ValueType.BOOL);
        }
        error("Unsupported binary operator: " + b.op);
        return TypeResult.scalar(ValueType.NUMBER);
    }

    private void applyAssign(String name, TypeResult valueType) {
        VarInfo cell = symbols.get(name);
        if (cell == null) {
            error("Undefined variable '" + name + "'. Declare with var first.");
            return;
        }
        if (!cell.initialized) {
            cell.type = valueType.type;
            cell.elementType = valueType.elementType;
            cell.initialized = true;
            return;
        }
        if (cell.type != valueType.type) {
            error("cannot assign " + valueType.type + " to '" + name + "' (" + cell.type + ")");
            return;
        }
        if (cell.type == ValueType.ARRAY && cell.elementType != valueType.elementType) {
            error("cannot assign array with element type " + valueType.elementType + " to '" + name + "' ("
                    + cell.elementType + ")");
        }
    }

    private void error(String msg) {
        errors.add(msg);
    }
}
