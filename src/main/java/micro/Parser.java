package micro;

import micro.ast.ArrayExpression;
import micro.ast.AssignExpression;
import micro.ast.AstExpression;
import micro.ast.IndexAssignExpression;
import micro.ast.IndexExpression;
import micro.ast.BinaryExpression;
import micro.ast.BlockStatement;
import micro.ast.BoolExpression;
import micro.ast.ExpressionStatement;
import micro.ast.IfStatement;
import micro.ast.NumberExpression;
import micro.ast.PrintStatement;
import micro.ast.AstStatement;
import micro.ast.StringExpression;
import micro.ast.UnaryExpression;
import micro.ast.VarStatement;
import micro.ast.VariableExpression;
import micro.ast.WhileStatement;

import java.util.ArrayList;
import java.util.List;

public final class Parser {
    private final List<Token> tokens;
    private int pos;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<AstStatement> parse() {
        List<AstStatement> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(parseDeclaration());
        }
        return statements;
    }

    private AstStatement parseDeclaration() {
        if (match(TokenType.VAR)) {
            return parseVarDeclaration();
        }
        return parseStatement();
    }

    private AstStatement parseStatement() {
        if (match(TokenType.IF)) return parseIfStatement();
        if (match(TokenType.WHILE)) return parseWhileStatement();
        if (match(TokenType.PRINT)) return parsePrintStatement();
        if (match(TokenType.LBRACE)) return new BlockStatement(parseBlock());
        return parseExpressionStatement();
    }

    private AstStatement parseVarDeclaration() {
        Token name = consume(TokenType.ID, "Expected variable name.");
        AstExpression initializer = null;
        if (match(TokenType.EQ)) {
            initializer = parseExpression();
        }
        consume(TokenType.SEMICOLON, "Expected ';' after variable declaration.");
        return new VarStatement(name.lexeme(), initializer);
    }

    private AstStatement parseIfStatement() {
        consume(TokenType.LPAREN, "Expected '(' after 'if'.");
        AstExpression condition = parseExpression();
        consume(TokenType.RPAREN, "Expected ')' after if condition.");
        AstStatement thenBranch = parseStatement();
        AstStatement elseBranch = null;
        if (match(TokenType.ELSE)) {
            elseBranch = parseStatement();
        }
        return new IfStatement(condition, thenBranch, elseBranch);
    }

    private AstStatement parseWhileStatement() {
        consume(TokenType.LPAREN, "Expected '(' after 'while'.");
        AstExpression condition = parseExpression();
        consume(TokenType.RPAREN, "Expected ')' after while condition.");
        AstStatement body = parseStatement();
        return new WhileStatement(condition, body);
    }

    private AstStatement parsePrintStatement() {
        AstExpression value = parseExpression();
        consume(TokenType.SEMICOLON, "Expected ';' after value.");
        return new PrintStatement(value);
    }

    private AstStatement parseExpressionStatement() {
        AstExpression expr = parseExpression();
        consume(TokenType.SEMICOLON, "Expected ';' after expression.");
        return new ExpressionStatement(expr);
    }

    private List<AstStatement> parseBlock() {
        List<AstStatement> statements = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            statements.add(parseDeclaration());
        }
        consume(TokenType.RBRACE, "Expected '}' after block.");
        return statements;
    }

    private AstExpression parseExpression() {
        return parseAssignment();
    }

    private AstExpression parseAssignment() {
        AstExpression expr = parseLogicalOr();
        if (match(TokenType.EQ)) {
            Token equals = previous();
            AstExpression value = parseAssignment();
            if (expr instanceof VariableExpression ve) {
                return new AssignExpression(ve.name, value);
            }
            if (expr instanceof IndexExpression idx) {
                return new IndexAssignExpression(idx.array, idx.index, value);
            }
            throw error(equals, "Invalid assignment target.");
        }
        return expr;
    }

    private AstExpression parseLogicalOr() {
        AstExpression expr = parseLogicalAnd();
        while (match(TokenType.OR)) {
            TokenType op = previous().type();
            AstExpression right = parseLogicalAnd();
            expr = new BinaryExpression(expr, op, right);
        }
        return expr;
    }

    private AstExpression parseLogicalAnd() {
        AstExpression expr = parseEquality();
        while (match(TokenType.AND)) {
            TokenType op = previous().type();
            AstExpression right = parseEquality();
            expr = new BinaryExpression(expr, op, right);
        }
        return expr;
    }

    private AstExpression parseEquality() {
        AstExpression expr = parseComparison();
        while (match(TokenType.EQEQ, TokenType.NEQ)) {
            TokenType op = previous().type();
            AstExpression right = parseComparison();
            expr = new BinaryExpression(expr, op, right);
        }
        return expr;
    }

    private AstExpression parseComparison() {
        AstExpression expr = parseTerm();
        while (match(TokenType.LT, TokenType.LTEQ, TokenType.GT, TokenType.GTEQ)) {
            TokenType op = previous().type();
            AstExpression right = parseTerm();
            expr = new BinaryExpression(expr, op, right);
        }
        return expr;
    }

    private AstExpression parseTerm() {
        AstExpression expr = parseFactor();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            TokenType op = previous().type();
            AstExpression right = parseFactor();
            expr = new BinaryExpression(expr, op, right);
        }
        return expr;
    }

    private AstExpression parseFactor() {
        AstExpression expr = parseUnary();
        while (match(TokenType.STAR, TokenType.SLASH)) {
            TokenType op = previous().type();
            AstExpression right = parseUnary();
            expr = new BinaryExpression(expr, op, right);
        }
        return expr;
    }

    private AstExpression parseUnary() {
        if (match(TokenType.EXCL, TokenType.MINUS)) {
            TokenType op = previous().type();
            AstExpression right = parseUnary();
            return new UnaryExpression(op, right);
        }
        return parsePostfix(parsePrimary());
    }

    private AstExpression parsePostfix(AstExpression expr) {
        while (match(TokenType.LBRACKET)) {
            AstExpression index = parseExpression();
            consume(TokenType.RBRACKET, "Expected ']' after index.");
            expr = new IndexExpression(expr, index);
        }
        return expr;
    }

    private AstExpression parsePrimary() {
        if (match(TokenType.TRUE)) return new BoolExpression(true);
        if (match(TokenType.FALSE)) return new BoolExpression(false);
        if (match(TokenType.STRING)) return new StringExpression(previous().lexeme());
        if (match(TokenType.NUMBER)) {
            double value = Double.parseDouble(previous().lexeme());
            return new NumberExpression(value);
        }
        if (match(TokenType.ID)) {
            return new VariableExpression(previous().lexeme());
        }
        if (match(TokenType.LBRACKET)) {
            return parseArrayLiteral();
        }
        if (match(TokenType.LPAREN)) {
            AstExpression expr = parseExpression();
            consume(TokenType.RPAREN, "Expected ')' after expression.");
            return expr;
        }
        throw error(peek(), "Expected expression.");
    }

    private AstExpression parseArrayLiteral() {
        List<AstExpression> elements = new ArrayList<>();
        if (!check(TokenType.RBRACKET)) {
            do {
                elements.add(parseExpression());
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RBRACKET, "Expected ']' after array elements.");
        return new ArrayExpression(elements);
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type() == type;
    }

    private Token advance() {
        if (!isAtEnd()) pos++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type() == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(pos);
    }

    private Token previous() {
        return tokens.get(pos - 1);
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private RuntimeException error(Token token, String message) {
        return new RuntimeException("[Parser Error] Line " + token.line() + ", Col " + token.column() + ": " + message);
    }
}
