package micro.ast;

public final class PrintStatement extends AstStatement {
    public final AstExpression expression;

    public PrintStatement(AstExpression expression) {
        this.expression = expression;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitPrintStatement(this);
    }
}
