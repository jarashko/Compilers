package micro.ast;

public final class AssignExpression extends AstExpression {
    public final String name;
    public final AstExpression value;

    public AssignExpression(String name, AstExpression value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitAssignExpression(this);
    }
}
