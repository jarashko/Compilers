package micro.ast;

public final class BoolExpression extends AstExpression {
    public final boolean value;

    public BoolExpression(boolean value) {
        this.value = value;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitBoolExpression(this);
    }
}
