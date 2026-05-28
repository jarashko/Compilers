package micro.ast;

public final class VariableExpression extends AstExpression {
    public final String name;

    public VariableExpression(String name) {
        this.name = name;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitVariableExpression(this);
    }
}
