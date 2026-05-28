package micro.interp;

import java.util.ArrayList;
import java.util.List;

public final class ArrayValue {
    public final ValueType elementType;
    public final List<Value> elements;

    public ArrayValue(ValueType elementType, List<Value> elements) {
        this.elementType = elementType;
        this.elements = new ArrayList<>(elements);
    }

    public int size() {
        return elements.size();
    }

    public Value get(int index) {
        return elements.get(index);
    }

    public void set(int index, Value value) {
        elements.set(index, value);
    }
}
