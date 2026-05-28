package micro.ast;

public final class WhileStatement extends AstStatement {
    public final AstExpression condition;
    public final AstStatement body;

    public WhileStatement(AstExpression condition, AstStatement body) {
        this.condition = condition;
        this.body = body;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitWhileStatement(this);
    }
}
