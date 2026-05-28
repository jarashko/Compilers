package micro.ast;

public final class NumberExpression extends AstExpression {
    public final double value;

    public NumberExpression(double value) {
        this.value = value;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitNumberExpression(this);
    }
}
