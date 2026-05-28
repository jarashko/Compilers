package micro.ast;

import micro.TokenType;

public final class BinaryExpression extends AstExpression {
    public final AstExpression left;
    public final TokenType op;
    public final AstExpression right;

    public BinaryExpression(AstExpression left, TokenType op, AstExpression right) {
        this.left = left;
        this.op = op;
        this.right = right;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitBinaryExpression(this);
    }
}
