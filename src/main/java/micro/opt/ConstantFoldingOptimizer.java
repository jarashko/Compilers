package micro.opt;

import micro.TokenType;
import micro.ast.AstExpression;
import micro.ast.AstStatement;
import micro.ast.BinaryExpression;
import micro.ast.NumberExpression;
import micro.ast.VariableExpression;
import micro.interp.Value;
import micro.interp.ValueType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ConstantFoldingOptimizer {
    private final ConstantEvaluator constants = new ConstantEvaluator();

    public List<AstStatement> optimize(List<AstStatement> program) {
        List<AstStatement> folded = new ArrayList<>();
        for (AstStatement stmt : program) {
            folded.add(foldStatement(stmt));
        }
        return new DeadCodeEliminator().eliminate(folded);
    }

    private AstStatement foldStatement(AstStatement stmt) {
        return stmt;
    }

    public AstExpression foldExpression(AstExpression expr) {
        if (expr instanceof BinaryExpression b) {
            AstExpression left = foldExpression(b.left);
            AstExpression right = foldExpression(b.right);
            AstExpression simplified = simplifyBinary(new BinaryExpression(left, b.op, right));
            Optional<Value> v = constants.eval(simplified);
            if (v.isPresent()) {
                return toLiteral(v.get());
            }
            return simplified;
        }
        return expr;
    }

    private AstExpression simplifyBinary(BinaryExpression b) {
        if (b.left instanceof NumberExpression ln && b.right instanceof NumberExpression rn) {
            return b;
        }
        if (b.left instanceof VariableExpression && b.right instanceof NumberExpression rn) {
            return simplifyWithNumber(b.op, b.left, rn.value, true);
        }
        if (b.left instanceof NumberExpression ln && b.right instanceof VariableExpression) {
            return simplifyWithNumber(b.op, b.right, ln.value, false);
        }
        return b;
    }

    private AstExpression simplifyWithNumber(TokenType op, AstExpression var, double n, boolean numberOnRight) {
        if (op == TokenType.STAR && n == 0) {
            return new NumberExpression(0);
        }
        if (op == TokenType.STAR && n == 1) {
            return var;
        }
        if (op == TokenType.PLUS && n == 0) {
            return var;
        }
        if (op == TokenType.MINUS && n == 0 && !numberOnRight) {
            return var;
        }
        if (op == TokenType.SLASH && n == 1 && numberOnRight) {
            return var;
        }
        return numberOnRight
                ? new BinaryExpression(var, op, new NumberExpression(n))
                : new BinaryExpression(new NumberExpression(n), op, var);
    }

    private static AstExpression toLiteral(Value value) {
        return switch (value.type) {
            case NUMBER -> new NumberExpression(value.asNumber());
            case BOOL -> new micro.ast.BoolExpression(value.asBool());
            case STRING -> new micro.ast.StringExpression(value.asString());
            case ARRAY, VOID -> throw new IllegalStateException("Cannot fold to literal: " + value.type);
        };
    }
}
