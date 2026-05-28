package micro.interp;

import micro.ast.AstStatement;

import java.util.List;

public final class FunctionInfo {
    public final ValueType returnType;
    public final String name;
    public final List<ValueType> paramTypes;
    public final List<String> paramNames;
    public final AstStatement body;

    public FunctionInfo(ValueType returnType, String name, List<ValueType> paramTypes,
                        List<String> paramNames, AstStatement body) {
        this.returnType = returnType;
        this.name = name;
        this.paramTypes = List.copyOf(paramTypes);
        this.paramNames = List.copyOf(paramNames);
        this.body = body;
    }
}
