package micro.interp;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public final class RuntimeEnvironment {
    public static final class Cell {
        public ValueType type;
        public ValueType elementType;
        public Value value;

        public boolean initialized() {
            return value != null;
        }
    }

    private final Deque<Map<String, Cell>> scopes = new ArrayDeque<>();

    public RuntimeEnvironment() {
        pushScope();
    }

    public void pushScope() {
        scopes.push(new HashMap<>());
    }

    public void popScope() {
        if (scopes.size() <= 1) {
            throw new RuntimeException("[Runtime Error] Cannot pop global scope.");
        }
        scopes.pop();
    }

    public void declare(String name, Cell cell) {
        Map<String, Cell> scope = scopes.peek();
        if (scope.containsKey(name)) {
            throw new RuntimeException("[Runtime Error] Variable '" + name + "' is already declared in this scope.");
        }
        scope.put(name, cell);
    }

    public Cell lookupCell(String name) {
        for (Map<String, Cell> scope : scopes) {
            Cell cell = scope.get(name);
            if (cell != null) {
                return cell;
            }
        }
        return null;
    }

    public Value lookup(String name) {
        Cell cell = lookupCell(name);
        if (cell == null || !cell.initialized()) {
            throw new RuntimeException("[Runtime Error] Variable '" + name + "' is not initialized.");
        }
        return cell.value;
    }

    public void assign(String name, Value value) {
        Cell cell = lookupCell(name);
        if (cell == null) {
            throw new RuntimeException("[Runtime Error] Undefined variable '" + name + "'. Declare with var first.");
        }
        if (!cell.initialized()) {
            cell.type = value.type;
            cell.elementType = value.type == ValueType.ARRAY ? value.elementType() : null;
            cell.value = value;
            return;
        }
        if (cell.type != value.type) {
            throw new RuntimeException("[Runtime Error] Type error: cannot assign " + value.type
                    + " to '" + name + "' (" + cell.type + ").");
        }
        if (cell.type == ValueType.ARRAY && cell.elementType != value.elementType()) {
            throw new RuntimeException("[Runtime Error] Type error: cannot assign array with element type "
                    + value.elementType() + " to '" + name + "' (" + cell.elementType + ").");
        }
        cell.value = value;
    }
}
