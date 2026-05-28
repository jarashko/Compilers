package micro;

public enum TokenType {
    NUMBER,
    STRING,
    ID,
    VAR,
    FUNCTION,
    RETURN,
    TYPE_NUMBER,
    TYPE_BOOL,
    TYPE_STRING,
    TYPE_VOID,
    PRINT,
    IF, ELSE,
    WHILE,
    TRUE, FALSE,

    PLUS, MINUS, STAR, SLASH,
    EQ, EQEQ, EXCL, NEQ,
    LT, GT, LTEQ, GTEQ,
    AND, OR,

    LPAREN, RPAREN,
    LBRACKET, RBRACKET,
    LBRACE, RBRACE,
    SEMICOLON,
    COMMA,

    EOF
}
