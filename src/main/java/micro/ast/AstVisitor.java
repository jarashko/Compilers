package micro.ast;

public interface AstVisitor<T> {
    T visitVarStatement(VarStatement stmt);

    T visitExpressionStatement(ExpressionStatement stmt);

    T visitPrintStatement(PrintStatement stmt);

    T visitBlockStatement(BlockStatement stmt);

    T visitIfStatement(IfStatement stmt);

    T visitWhileStatement(WhileStatement stmt);

    T visitNumberExpression(NumberExpression expr);

    T visitStringExpression(StringExpression expr);

    T visitBoolExpression(BoolExpression expr);

    T visitVariableExpression(VariableExpression expr);

    T visitUnaryExpression(UnaryExpression expr);

    T visitBinaryExpression(BinaryExpression expr);

    T visitAssignExpression(AssignExpression expr);

    T visitArrayExpression(ArrayExpression expr);

    T visitIndexExpression(IndexExpression expr);

    T visitIndexAssignExpression(IndexAssignExpression expr);
}
