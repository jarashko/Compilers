package micro.ast;

public final class IndexAssignExpression extends AstExpression {
    public final AstExpression array;
    public final AstExpression index;
    public final AstExpression value;

    public IndexAssignExpression(AstExpression array, AstExpression index, AstExpression value) {
        this.array = array;
        this.index = index;
        this.value = value;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitIndexAssignExpression(this);
    }
}
