package micro.ast;

public abstract class AstStatement {
    public abstract <T> T accept(AstVisitor<T> visitor);
}
