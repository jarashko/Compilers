package micro.opt;

import micro.ast.AstExpression;
import micro.ast.AstVisitor;
import micro.ast.AssignExpression;
import micro.ast.BinaryExpression;
import micro.ast.BoolExpression;
import micro.ast.NumberExpression;
import micro.ast.StringExpression;
import micro.ast.UnaryExpression;
import micro.ast.VariableExpression;

public final class SideEffectVisitor implements AstVisitor<Boolean> {
    public boolean hasSideEffects(AstExpression expr) {
        return expr.accept(this);
    }

    @Override
    public Boolean visitNumberExpression(NumberExpression expr) {
        return false;
    }

    @Override
    public Boolean visitStringExpression(StringExpression expr) {
        return false;
    }

    @Override
    public Boolean visitBoolExpression(BoolExpression expr) {
        return false;
    }

    @Override
    public Boolean visitVariableExpression(VariableExpression expr) {
        return false;
    }

    @Override
    public Boolean visitUnaryExpression(UnaryExpression expr) {
        return expr.right.accept(this);
    }

    @Override
    public Boolean visitBinaryExpression(BinaryExpression expr) {
        return expr.left.accept(this) || expr.right.accept(this);
    }

    @Override
    public Boolean visitAssignExpression(AssignExpression expr) {
        return true;
    }

    @Override
    public Boolean visitArrayExpression(micro.ast.ArrayExpression expr) {
        for (AstExpression element : expr.elements) {
            if (element.accept(this)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean visitIndexExpression(micro.ast.IndexExpression expr) {
        return expr.array.accept(this) || expr.index.accept(this);
    }

    @Override
    public Boolean visitIndexAssignExpression(micro.ast.IndexAssignExpression expr) {
        return true;
    }

    @Override
    public Boolean visitCallExpression(micro.ast.CallExpression expr) {
        for (AstExpression arg : expr.arguments) {
            if (arg.accept(this)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean visitFunctionDeclStatement(micro.ast.FunctionDeclStatement stmt) {
        throw new IllegalStateException();
    }

    @Override
    public Boolean visitReturnStatement(micro.ast.ReturnStatement stmt) {
        throw new IllegalStateException();
    }

    @Override
    public Boolean visitVarStatement(micro.ast.VarStatement stmt) {
        throw new IllegalStateException();
    }

    @Override
    public Boolean visitExpressionStatement(micro.ast.ExpressionStatement stmt) {
        throw new IllegalStateException();
    }

    @Override
    public Boolean visitPrintStatement(micro.ast.PrintStatement stmt) {
        throw new IllegalStateException();
    }

    @Override
    public Boolean visitBlockStatement(micro.ast.BlockStatement stmt) {
        throw new IllegalStateException();
    }

    @Override
    public Boolean visitIfStatement(micro.ast.IfStatement stmt) {
        throw new IllegalStateException();
    }

    @Override
    public Boolean visitWhileStatement(micro.ast.WhileStatement stmt) {
        throw new IllegalStateException();
    }
}
