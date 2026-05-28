package micro.opt;

import micro.ast.AstExpression;
import micro.ast.AstStatement;
import micro.ast.AstVisitor;
import micro.ast.ArrayExpression;
import micro.ast.AssignExpression;
import micro.ast.IndexAssignExpression;
import micro.ast.IndexExpression;
import micro.ast.BlockStatement;
import micro.ast.BoolExpression;
import micro.ast.ExpressionStatement;
import micro.ast.IfStatement;
import micro.ast.PrintStatement;
import micro.ast.VarStatement;
import micro.ast.WhileStatement;
import micro.interp.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class DeadCodeEliminator implements AstVisitor<AstStatement> {
    private final ConstantEvaluator constants = new ConstantEvaluator();
    private final SideEffectVisitor sideEffects = new SideEffectVisitor();
    private final ReadVariableVisitor readVars = new ReadVariableVisitor();
    private Set<String> usedReads = Set.of();

    public List<AstStatement> eliminate(List<AstStatement> program) {
        List<AstStatement> current = new ArrayList<>(program);
        while (true) {
            usedReads = readVars.collectReads(current);
            List<AstStatement> next = new ArrayList<>();
            boolean changed = false;
            for (AstStatement stmt : current) {
                AstStatement optimized = stmt.accept(this);
                if (optimized == null) {
                    changed = true;
                } else {
                    next.add(optimized);
                }
            }
            if (!changed && next.size() == current.size()) {
                break;
            }
            current = next;
        }
        return current;
    }

    @Override
    public AstStatement visitVarStatement(VarStatement stmt) {
        if (!usedReads.contains(stmt.name)) {
            if (stmt.initializer == null || !sideEffects.hasSideEffects(stmt.initializer)) {
                return null;
            }
            return new ExpressionStatement(stmt.initializer);
        }
        AstExpression init = stmt.initializer == null ? null : foldExpr(stmt.initializer);
        return new VarStatement(stmt.name, init);
    }

    @Override
    public AstStatement visitExpressionStatement(ExpressionStatement stmt) {
        AstExpression expr = foldExpr(stmt.expression);
        if (expr instanceof AssignExpression assign && !usedReads.contains(assign.name)
                && !sideEffects.hasSideEffects(assign.value)) {
            return null;
        }
        if (!sideEffects.hasSideEffects(expr)) {
            return null;
        }
        return new ExpressionStatement(expr);
    }

    @Override
    public AstStatement visitPrintStatement(PrintStatement stmt) {
        return new PrintStatement(foldExpr(stmt.expression));
    }

    @Override
    public AstStatement visitBlockStatement(BlockStatement stmt) {
        List<AstStatement> kept = new ArrayList<>();
        for (AstStatement inner : stmt.statements) {
            AstStatement optimized = inner.accept(this);
            if (optimized != null) {
                kept.add(optimized);
            }
        }
        if (kept.isEmpty()) {
            return null;
        }
        return new BlockStatement(kept);
    }

    @Override
    public AstStatement visitIfStatement(IfStatement stmt) {
        Optional<Boolean> cond = constants.evalBool(stmt.condition);
        if (cond.isPresent()) {
            if (cond.get()) {
                return stmt.thenBranch.accept(this);
            }
            if (stmt.elseBranch != null) {
                return stmt.elseBranch.accept(this);
            }
            return null;
        }
        AstStatement thenBranch = stmt.thenBranch.accept(this);
        AstStatement elseBranch = stmt.elseBranch == null ? null : stmt.elseBranch.accept(this);
        if (thenBranch == null && elseBranch == null) {
            return null;
        }
        if (thenBranch == null) {
            return elseBranch;
        }
        if (elseBranch == null) {
            return new IfStatement(foldExpr(stmt.condition), thenBranch, null);
        }
        return new IfStatement(foldExpr(stmt.condition), thenBranch, elseBranch);
    }

    @Override
    public AstStatement visitWhileStatement(WhileStatement stmt) {
        Optional<Boolean> cond = constants.evalBool(stmt.condition);
        if (cond.isPresent() && !cond.get()) {
            return null;
        }
        AstStatement body = stmt.body.accept(this);
        if (body == null) {
            return null;
        }
        return new WhileStatement(foldExpr(stmt.condition), body);
    }

    @Override
    public AstStatement visitNumberExpression(micro.ast.NumberExpression expr) {
        throw new IllegalStateException();
    }

    @Override
    public AstStatement visitStringExpression(micro.ast.StringExpression expr) {
        throw new IllegalStateException();
    }

    @Override
    public AstStatement visitBoolExpression(micro.ast.BoolExpression expr) {
        throw new IllegalStateException();
    }

    @Override
    public AstStatement visitVariableExpression(micro.ast.VariableExpression expr) {
        throw new IllegalStateException();
    }

    @Override
    public AstStatement visitUnaryExpression(micro.ast.UnaryExpression expr) {
        throw new IllegalStateException();
    }

    @Override
    public AstStatement visitBinaryExpression(micro.ast.BinaryExpression expr) {
        throw new IllegalStateException();
    }

    @Override
    public AstStatement visitAssignExpression(AssignExpression expr) {
        throw new IllegalStateException();
    }

    @Override
    public AstStatement visitArrayExpression(ArrayExpression expr) {
        throw new IllegalStateException();
    }

    @Override
    public AstStatement visitIndexExpression(IndexExpression expr) {
        throw new IllegalStateException();
    }

    @Override
    public AstStatement visitIndexAssignExpression(IndexAssignExpression expr) {
        throw new IllegalStateException();
    }

    @Override
    public AstStatement visitCallExpression(micro.ast.CallExpression expr) {
        throw new IllegalStateException();
    }

    @Override
    public AstStatement visitFunctionDeclStatement(micro.ast.FunctionDeclStatement stmt) {
        return stmt;
    }

    @Override
    public AstStatement visitReturnStatement(micro.ast.ReturnStatement stmt) {
        return stmt;
    }

    private AstExpression foldExpr(AstExpression expr) {
        Optional<Value> v = constants.eval(expr);
        if (v.isEmpty()) {
            return expr;
        }
        Value value = v.get();
        return switch (value.type) {
            case NUMBER -> new micro.ast.NumberExpression(value.asNumber());
            case BOOL -> new BoolExpression(value.asBool());
            case STRING -> new micro.ast.StringExpression(value.asString());
            case ARRAY -> expr;
            case VOID -> expr;
        };
    }
}
