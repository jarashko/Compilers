package micro.interp;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public final class SymbolTable {
    public static final class VarInfo {
        public ValueType type;
        public ValueType elementType;
        public boolean initialized;
    }

    private final Deque<Map<String, VarInfo>> scopes = new ArrayDeque<>();
    private final Map<String, FunctionInfo> functions = new HashMap<>();

    public SymbolTable() {
        pushScope();
    }

    public void pushScope() {
        scopes.push(new HashMap<>());
    }

    public void popScope() {
        if (scopes.size() <= 1) {
            throw new IllegalStateException("Cannot pop global scope.");
        }
        scopes.pop();
    }

    public void declareVar(String name, VarInfo info) {
        Map<String, VarInfo> scope = scopes.peek();
        if (scope.containsKey(name)) {
            throw new IllegalStateException("Variable '" + name + "' is already declared in this scope.");
        }
        scope.put(name, info);
    }

    public VarInfo lookupVar(String name) {
        for (Map<String, VarInfo> scope : scopes) {
            VarInfo info = scope.get(name);
            if (info != null) {
                return info;
            }
        }
        return null;
    }

    public void registerFunction(FunctionInfo info) {
        if (functions.containsKey(info.name)) {
            throw new IllegalStateException("Function '" + info.name + "' is already declared.");
        }
        functions.put(info.name, info);
    }

    public FunctionInfo lookupFunction(String name) {
        return functions.get(name);
    }

    public Map<String, FunctionInfo> functions() {
        return functions;
    }
}
