package micro.opt;

import micro.ast.AstExpression;
import micro.ast.AstStatement;
import micro.ast.AstVisitor;
import micro.ast.AssignExpression;
import micro.ast.BinaryExpression;
import micro.ast.BlockStatement;
import micro.ast.ExpressionStatement;
import micro.ast.IfStatement;
import micro.ast.PrintStatement;
import micro.ast.UnaryExpression;
import micro.ast.VarStatement;
import micro.ast.VariableExpression;
import micro.ast.WhileStatement;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ReadVariableVisitor implements AstVisitor<Void> {
    private final Set<String> reads = new HashSet<>();

    public Set<String> collectReads(List<AstStatement> program) {
        reads.clear();
        for (AstStatement stmt : program) {
            stmt.accept(this);
        }
        return Set.copyOf(reads);
    }

    @Override
    public Void visitVarStatement(VarStatement stmt) {
        if (stmt.initializer != null) {
            stmt.initializer.accept(this);
        }
        return null;
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatement stmt) {
        stmt.expression.accept(this);
        return null;
    }

    @Override
    public Void visitPrintStatement(PrintStatement stmt) {
        stmt.expression.accept(this);
        return null;
    }

    @Override
    public Void visitBlockStatement(BlockStatement stmt) {
        for (AstStatement inner : stmt.statements) {
            inner.accept(this);
        }
        return null;
    }

    @Override
    public Void visitIfStatement(IfStatement stmt) {
        stmt.condition.accept(this);
        stmt.thenBranch.accept(this);
        if (stmt.elseBranch != null) {
            stmt.elseBranch.accept(this);
        }
        return null;
    }

    @Override
    public Void visitWhileStatement(WhileStatement stmt) {
        stmt.condition.accept(this);
        stmt.body.accept(this);
        return null;
    }

    @Override
    public Void visitNumberExpression(micro.ast.NumberExpression expr) {
        return null;
    }

    @Override
    public Void visitStringExpression(micro.ast.StringExpression expr) {
        return null;
    }

    @Override
    public Void visitBoolExpression(micro.ast.BoolExpression expr) {
        return null;
    }

    @Override
    public Void visitVariableExpression(VariableExpression expr) {
        reads.add(expr.name);
        return null;
    }

    @Override
    public Void visitUnaryExpression(UnaryExpression expr) {
        expr.right.accept(this);
        return null;
    }

    @Override
    public Void visitBinaryExpression(BinaryExpression expr) {
        expr.left.accept(this);
        expr.right.accept(this);
        return null;
    }

    @Override
    public Void visitAssignExpression(AssignExpression expr) {
        expr.value.accept(this);
        return null;
    }

    @Override
    public Void visitArrayExpression(micro.ast.ArrayExpression expr) {
        for (AstExpression element : expr.elements) {
            element.accept(this);
        }
        return null;
    }

    @Override
    public Void visitIndexExpression(micro.ast.IndexExpression expr) {
        expr.array.accept(this);
        expr.index.accept(this);
        return null;
    }

    @Override
    public Void visitIndexAssignExpression(micro.ast.IndexAssignExpression expr) {
        expr.array.accept(this);
        expr.index.accept(this);
        expr.value.accept(this);
        return null;
    }

    @Override
    public Void visitCallExpression(micro.ast.CallExpression expr) {
        for (AstExpression arg : expr.arguments) {
            arg.accept(this);
        }
        return null;
    }

    @Override
    public Void visitFunctionDeclStatement(micro.ast.FunctionDeclStatement stmt) {
        stmt.body.accept(this);
        return null;
    }

    @Override
    public Void visitReturnStatement(micro.ast.ReturnStatement stmt) {
        if (stmt.value != null) {
            stmt.value.accept(this);
        }
        return null;
    }
}
