package micro.ast;

public final class ExpressionStatement extends AstStatement {
    public final AstExpression expression;

    public ExpressionStatement(AstExpression expression) {
        this.expression = expression;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitExpressionStatement(this);
    }
}
