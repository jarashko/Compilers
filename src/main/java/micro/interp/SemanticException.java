package micro.interp;

import java.util.List;

public final class SemanticException extends RuntimeException {
    public SemanticException(List<String> errors) {
        super(format(errors));
    }

    private static String format(List<String> errors) {
        return String.join("\n", errors);
    }
}
