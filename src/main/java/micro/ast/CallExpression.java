package micro.ast;

import java.util.List;

public final class CallExpression extends AstExpression {
    public final String name;
    public final List<AstExpression> arguments;

    public CallExpression(String name, List<AstExpression> arguments) {
        this.name = name;
        this.arguments = List.copyOf(arguments);
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitCallExpression(this);
    }
}
