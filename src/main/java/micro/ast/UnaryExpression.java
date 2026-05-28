package micro.ast;

import micro.TokenType;

public final class UnaryExpression extends AstExpression {
    public final TokenType op;
    public final AstExpression right;

    public UnaryExpression(TokenType op, AstExpression right) {
        this.op = op;
        this.right = right;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitUnaryExpression(this);
    }
}
