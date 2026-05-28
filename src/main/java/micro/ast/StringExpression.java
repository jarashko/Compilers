package micro.ast;

public final class StringExpression extends AstExpression {
    public final String value;

    public StringExpression(String value) {
        this.value = value;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitStringExpression(this);
    }
}
