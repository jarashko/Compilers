package micro.interp;

public final class ReturnException extends RuntimeException {
    public final Value value;

    public ReturnException(Value value) {
        super(null, null, false, false);
        this.value = value;
    }
}
