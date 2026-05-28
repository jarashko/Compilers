package micro.ast;

public final class ReturnStatement extends AstStatement {
    public final AstExpression value;

    public ReturnStatement(AstExpression value) {
        this.value = value;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitReturnStatement(this);
    }
}
