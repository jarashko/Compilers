package micro.ast;

public final class IfStatement extends AstStatement {
    public final AstExpression condition;
    public final AstStatement thenBranch;
    public final AstStatement elseBranch;

    public IfStatement(AstExpression condition, AstStatement thenBranch, AstStatement elseBranch) {
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitIfStatement(this);
    }
}
