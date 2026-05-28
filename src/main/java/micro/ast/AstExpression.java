package micro.ast;

public abstract class AstExpression {
    public abstract <T> T accept(AstVisitor<T> visitor);
}
