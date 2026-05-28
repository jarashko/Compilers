package micro.ast;

import java.util.List;

public final class BlockStatement extends AstStatement {
    public final List<AstStatement> statements;

    public BlockStatement(List<AstStatement> statements) {
        this.statements = statements;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitBlockStatement(this);
    }
}
