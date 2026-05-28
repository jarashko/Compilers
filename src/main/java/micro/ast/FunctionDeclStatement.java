package micro.ast;

import micro.interp.ValueType;

import java.util.List;

public final class FunctionDeclStatement extends AstStatement {
    public final ValueType returnType;
    public final String name;
    public final List<ValueType> paramTypes;
    public final List<String> paramNames;
    public final AstStatement body;

    public FunctionDeclStatement(ValueType returnType, String name, List<ValueType> paramTypes,
                                 List<String> paramNames, AstStatement body) {
        this.returnType = returnType;
        this.name = name;
        this.paramTypes = List.copyOf(paramTypes);
        this.paramNames = List.copyOf(paramNames);
        this.body = body;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visitFunctionDeclStatement(this);
    }
}
