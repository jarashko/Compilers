package micro.ast;

public final class IndexExpression extends AstExpression {
    public final AstExpression array;
    public final AstExpression index;

    public IndexExpression(AstExpression array, AstExpression index) {
        this.array = array;
        this.index = index;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitIndexExpression(this);
    }
}
