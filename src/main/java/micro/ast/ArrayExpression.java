package micro.ast;

import java.util.List;

public final class ArrayExpression extends AstExpression {
    public final List<AstExpression> elements;

    public ArrayExpression(List<AstExpression> elements) {
        this.elements = List.copyOf(elements);
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitArrayExpression(this);
    }
}
