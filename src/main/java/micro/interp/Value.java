package micro.interp;

import java.util.List;

public final class Value {
    public final ValueType type;
    public final Object data;

    private Value(ValueType type, Object data) {
        this.type = type;
        this.data = data;
    }

    public static Value number(double d) {
        return new Value(ValueType.NUMBER, d);
    }

    public static Value bool(boolean b) {
        return new Value(ValueType.BOOL, b);
    }

    public static Value string(String s) {
        return new Value(ValueType.STRING, s);
    }

    public static Value array(ValueType elementType, List<Value> elements) {
        return new Value(ValueType.ARRAY, new ArrayValue(elementType, elements));
    }

    public double asNumber() {
        return (Double) data;
    }

    public boolean asBool() {
        return (Boolean) data;
    }

    public String asString() {
        return (String) data;
    }

    public ArrayValue asArray() {
        return (ArrayValue) data;
    }

    public ValueType elementType() {
        if (type != ValueType.ARRAY) {
            throw new IllegalStateException("Not an array value");
        }
        return asArray().elementType;
    }

    public static String stringify(Value v) {
        return switch (v.type) {
            case NUMBER -> formatNumber(v.asNumber());
            case BOOL -> Boolean.toString(v.asBool());
            case STRING -> v.asString();
            case ARRAY -> formatArray(v.asArray());
            case VOID -> "void";
        };
    }

    private static String formatArray(ArrayValue array) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(stringify(array.get(i)));
        }
        sb.append(']');
        return sb.toString();
    }

    private static String formatNumber(double d) {
        if (!Double.isFinite(d)) return Double.toString(d);
        long r = Math.round(d);
        if (Math.abs(d - r) < 1e-9) return Long.toString(r);
        return Double.toString(d);
    }
}
