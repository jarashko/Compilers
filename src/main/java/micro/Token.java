package micro;

public record Token(TokenType type, String lexeme, int position, int line, int column) {
    @Override
    public String toString() {
        return "[" + line + ":" + column + "] Token(" + type + ", '" + lexeme + "')";
    }
}
