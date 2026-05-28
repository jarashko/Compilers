package micro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Lexer {
    private final String input;
    private int pos;
    private int line = 1;
    private int column = 1;

    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put("var", TokenType.VAR);
        KEYWORDS.put("function", TokenType.FUNCTION);
        KEYWORDS.put("return", TokenType.RETURN);
        KEYWORDS.put("number", TokenType.TYPE_NUMBER);
        KEYWORDS.put("bool", TokenType.TYPE_BOOL);
        KEYWORDS.put("string", TokenType.TYPE_STRING);
        KEYWORDS.put("void", TokenType.TYPE_VOID);
        KEYWORDS.put("print", TokenType.PRINT);
        KEYWORDS.put("if", TokenType.IF);
        KEYWORDS.put("else", TokenType.ELSE);
        KEYWORDS.put("while", TokenType.WHILE);
        KEYWORDS.put("true", TokenType.TRUE);
        KEYWORDS.put("false", TokenType.FALSE);
    }

    public Lexer(String input) {
        this.input = input != null ? input : "";
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (pos < input.length()) {
            char c = peek();
            if (Character.isWhitespace(c)) {
                advance();
                continue;
            }
            if (c == '"') {
                tokens.add(readString());
                continue;
            }
            if (Character.isDigit(c)) {
                tokens.add(readNumber());
                continue;
            }
            if (Character.isLetter(c) || c == '_') {
                tokens.add(readWord());
                continue;
            }
            tokens.add(readOperatorOrPunctuation());
        }
        tokens.add(new Token(TokenType.EOF, "\0", pos, line, column));
        return tokens;
    }

    private Token readString() {
        int startPos = pos;
        int startLine = line;
        int startCol = column;
        advance();
        StringBuilder sb = new StringBuilder();
        while (peek() != '"' && peek() != '\0') {
            if (peek() == '\\') {
                advance();
                char esc = peek();
                switch (esc) {
                    case 'n' -> {
                        sb.append('\n');
                        advance();
                    }
                    case 't' -> {
                        sb.append('\t');
                        advance();
                    }
                    case 'r' -> {
                        sb.append('\r');
                        advance();
                    }
                    case '"' -> {
                        sb.append('"');
                        advance();
                    }
                    case '\\' -> {
                        sb.append('\\');
                        advance();
                    }
                    default -> {
                        sb.append(esc);
                        advance();
                    }
                }
            } else {
                sb.append(advance());
            }
        }
        if (peek() == '\0') {
            throw new RuntimeException("[Lexer Error] Unterminated string at Line " + startLine + ", Column " + startCol);
        }
        advance();
        return new Token(TokenType.STRING, sb.toString(), startPos, startLine, startCol);
    }

    private Token readNumber() {
        int startPos = pos;
        int startLine = line;
        int startCol = column;
        while (Character.isDigit(peek())) {
            advance();
        }
        String text = input.substring(startPos, pos);
        return new Token(TokenType.NUMBER, text, startPos, startLine, startCol);
    }

    private Token readWord() {
        int startPos = pos;
        int startLine = line;
        int startCol = column;
        while (Character.isLetterOrDigit(peek()) || peek() == '_') {
            advance();
        }
        String text = input.substring(startPos, pos);
        TokenType type = KEYWORDS.getOrDefault(text, TokenType.ID);
        return new Token(type, text, startPos, startLine, startCol);
    }

    private Token readOperatorOrPunctuation() {
        int startPos = pos;
        int startLine = line;
        int startCol = column;
        if (pos + 1 < input.length()) {
            String two = input.substring(pos, pos + 2);
            TokenType t2 = switch (two) {
                case "==" -> TokenType.EQEQ;
                case "!=" -> TokenType.NEQ;
                case "<=" -> TokenType.LTEQ;
                case ">=" -> TokenType.GTEQ;
                case "&&" -> TokenType.AND;
                case "||" -> TokenType.OR;
                default -> null;
            };
            if (t2 != null) {
                advance();
                advance();
                return new Token(t2, two, startPos, startLine, startCol);
            }
        }
        char ch = input.charAt(pos);
        String one = String.valueOf(ch);
        TokenType t1 = switch (ch) {
            case '+' -> TokenType.PLUS;
            case '-' -> TokenType.MINUS;
            case '*' -> TokenType.STAR;
            case '/' -> TokenType.SLASH;
            case '=' -> TokenType.EQ;
            case '<' -> TokenType.LT;
            case '>' -> TokenType.GT;
            case '!' -> TokenType.EXCL;
            case '(' -> TokenType.LPAREN;
            case ')' -> TokenType.RPAREN;
            case '[' -> TokenType.LBRACKET;
            case ']' -> TokenType.RBRACKET;
            case '{' -> TokenType.LBRACE;
            case '}' -> TokenType.RBRACE;
            case ';' -> TokenType.SEMICOLON;
            case ',' -> TokenType.COMMA;
            default -> null;
        };
        if (t1 != null) {
            advance();
            return new Token(t1, one, startPos, startLine, startCol);
        }
        throw new RuntimeException("[Lexer Error] Unexpected character '" + ch + "' at Line " + startLine + ", Column " + startCol);
    }

    private char peek() {
        if (pos >= input.length()) return '\0';
        return input.charAt(pos);
    }

    private char advance() {
        if (pos >= input.length()) return '\0';
        char c = input.charAt(pos++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }
}
