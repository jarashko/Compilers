package micro.ast;

public final class VarStatement extends AstStatement {
    public final String name;
    public final AstExpression initializer;

    public VarStatement(String name, AstExpression initializer) {
        this.name = name;
        this.initializer = initializer;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitVarStatement(this);
    }
}
