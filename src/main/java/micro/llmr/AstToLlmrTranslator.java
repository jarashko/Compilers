package micro.llmr;

import micro.TokenType;
import micro.ast.AstExpression;
import micro.ast.AstStatement;
import micro.ast.AstVisitor;
import micro.ast.ArrayExpression;
import micro.ast.AssignExpression;
import micro.ast.BinaryExpression;
import micro.ast.IndexAssignExpression;
import micro.ast.IndexExpression;
import micro.ast.BlockStatement;
import micro.ast.BoolExpression;
import micro.ast.ExpressionStatement;
import micro.ast.IfStatement;
import micro.ast.NumberExpression;
import micro.ast.PrintStatement;
import micro.ast.StringExpression;
import micro.ast.UnaryExpression;
import micro.ast.VarStatement;
import micro.ast.VariableExpression;
import micro.ast.WhileStatement;
import micro.interp.Value;

import java.util.ArrayList;
import java.util.List;

public final class AstToLlmrTranslator implements AstVisitor<Void> {
    private final List<LlmrInstr> code = new ArrayList<>();
    private int labelCounter;

    public static LlmrProgram translate(List<AstStatement> program) {
        AstToLlmrTranslator translator = new AstToLlmrTranslator();
        for (AstStatement stmt : program) {
            stmt.accept(translator);
        }
        translator.code.add(new LlmrInstr(LlmrOp.HALT));
        return new LlmrProgram(translator.code);
    }

    @Override
    public Void visitVarStatement(VarStatement stmt) {
        if (stmt.initializer != null) {
            stmt.initializer.accept(this);
            code.add(new LlmrInstr(LlmrOp.STORE_VAR, stmt.name));
        }
        return null;
    }

    @Override
    public Void visitExpressionStatement(ExpressionStatement stmt) {
        stmt.expression.accept(this);
        code.add(new LlmrInstr(LlmrOp.POP));
        return null;
    }

    @Override
    public Void visitPrintStatement(PrintStatement stmt) {
        stmt.expression.accept(this);
        code.add(new LlmrInstr(LlmrOp.PRINT));
        return null;
    }

    @Override
    public Void visitBlockStatement(BlockStatement stmt) {
        for (AstStatement inner : stmt.statements) {
            inner.accept(this);
        }
        return null;
    }

    @Override
    public Void visitIfStatement(IfStatement stmt) {
        stmt.condition.accept(this);
        int elseLabel = newLabel();
        int endLabel = newLabel();
        code.add(new LlmrInstr(LlmrOp.JUMP_IF_FALSE, elseLabel));
        stmt.thenBranch.accept(this);
        code.add(new LlmrInstr(LlmrOp.JUMP, endLabel));
        code.add(new LlmrInstr(LlmrOp.LABEL, elseLabel));
        if (stmt.elseBranch != null) {
            stmt.elseBranch.accept(this);
        }
        code.add(new LlmrInstr(LlmrOp.LABEL, endLabel));
        return null;
    }

    @Override
    public Void visitWhileStatement(WhileStatement stmt) {
        int headLabel = newLabel();
        int endLabel = newLabel();
        code.add(new LlmrInstr(LlmrOp.LABEL, headLabel));
        stmt.condition.accept(this);
        code.add(new LlmrInstr(LlmrOp.JUMP_IF_FALSE, endLabel));
        stmt.body.accept(this);
        code.add(new LlmrInstr(LlmrOp.JUMP, headLabel));
        code.add(new LlmrInstr(LlmrOp.LABEL, endLabel));
        return null;
    }

    @Override
    public Void visitNumberExpression(NumberExpression expr) {
        code.add(new LlmrInstr(LlmrOp.LOAD_CONST, Value.number(expr.value)));
        return null;
    }

    @Override
    public Void visitStringExpression(StringExpression expr) {
        code.add(new LlmrInstr(LlmrOp.LOAD_CONST, Value.string(expr.value)));
        return null;
    }

    @Override
    public Void visitBoolExpression(BoolExpression expr) {
        code.add(new LlmrInstr(LlmrOp.LOAD_CONST, Value.bool(expr.value)));
        return null;
    }

    @Override
    public Void visitVariableExpression(VariableExpression expr) {
        code.add(new LlmrInstr(LlmrOp.LOAD_VAR, expr.name));
        return null;
    }

    @Override
    public Void visitUnaryExpression(UnaryExpression expr) {
        expr.right.accept(this);
        if (expr.op == TokenType.EXCL) {
            code.add(new LlmrInstr(LlmrOp.NOT));
        } else if (expr.op == TokenType.MINUS) {
            code.add(new LlmrInstr(LlmrOp.NEG));
        }
        return null;
    }

    @Override
    public Void visitBinaryExpression(BinaryExpression expr) {
        if (expr.op == TokenType.AND) {
            emitLogicalAnd(expr.left, expr.right);
            return null;
        }
        if (expr.op == TokenType.OR) {
            emitLogicalOr(expr.left, expr.right);
            return null;
        }
        expr.left.accept(this);
        expr.right.accept(this);
        code.add(new LlmrInstr(mapBinary(expr.op)));
        return null;
    }

    @Override
    public Void visitAssignExpression(AssignExpression expr) {
        expr.value.accept(this);
        code.add(new LlmrInstr(LlmrOp.STORE_VAR, expr.name));
        code.add(new LlmrInstr(LlmrOp.LOAD_VAR, expr.name));
        return null;
    }

    @Override
    public Void visitArrayExpression(ArrayExpression expr) {
        for (AstExpression element : expr.elements) {
            element.accept(this);
        }
        code.add(new LlmrInstr(LlmrOp.MAKE_ARRAY, expr.elements.size()));
        return null;
    }

    @Override
    public Void visitIndexExpression(IndexExpression expr) {
        if (expr.array instanceof VariableExpression v) {
            expr.index.accept(this);
            code.add(new LlmrInstr(LlmrOp.ARRAY_LOAD, v.name));
            return null;
        }
        expr.array.accept(this);
        expr.index.accept(this);
        code.add(new LlmrInstr(LlmrOp.ARRAY_GET));
        return null;
    }

    @Override
    public Void visitIndexAssignExpression(IndexAssignExpression expr) {
        if (expr.array instanceof VariableExpression v) {
            expr.index.accept(this);
            expr.value.accept(this);
            code.add(new LlmrInstr(LlmrOp.ARRAY_STORE, v.name));
            expr.index.accept(this);
            code.add(new LlmrInstr(LlmrOp.ARRAY_LOAD, v.name));
            return null;
        }
        expr.array.accept(this);
        expr.index.accept(this);
        expr.value.accept(this);
        code.add(new LlmrInstr(LlmrOp.ARRAY_SET));
        return null;
    }

    private void emitLogicalAnd(AstExpression left, AstExpression right) {
        int falseLabel = newLabel();
        int endLabel = newLabel();
        left.accept(this);
        code.add(new LlmrInstr(LlmrOp.JUMP_IF_FALSE, falseLabel));
        right.accept(this);
        code.add(new LlmrInstr(LlmrOp.JUMP, endLabel));
        code.add(new LlmrInstr(LlmrOp.LABEL, falseLabel));
        code.add(new LlmrInstr(LlmrOp.LOAD_CONST, Value.bool(false)));
        code.add(new LlmrInstr(LlmrOp.LABEL, endLabel));
    }

    private void emitLogicalOr(AstExpression left, AstExpression right) {
        int trueLabel = newLabel();
        int endLabel = newLabel();
        left.accept(this);
        code.add(new LlmrInstr(LlmrOp.JUMP_IF_TRUE, trueLabel));
        right.accept(this);
        code.add(new LlmrInstr(LlmrOp.JUMP, endLabel));
        code.add(new LlmrInstr(LlmrOp.LABEL, trueLabel));
        code.add(new LlmrInstr(LlmrOp.LOAD_CONST, Value.bool(true)));
        code.add(new LlmrInstr(LlmrOp.LABEL, endLabel));
    }

    private static LlmrOp mapBinary(TokenType op) {
        return switch (op) {
            case PLUS -> LlmrOp.ADD;
            case MINUS -> LlmrOp.SUB;
            case STAR -> LlmrOp.MUL;
            case SLASH -> LlmrOp.DIV;
            case EQEQ -> LlmrOp.EQ;
            case NEQ -> LlmrOp.NEQ;
            case LT -> LlmrOp.LT;
            case LTEQ -> LlmrOp.LTE;
            case GT -> LlmrOp.GT;
            case GTEQ -> LlmrOp.GTE;
            default -> throw new IllegalStateException("Unsupported binary op: " + op);
        };
    }

    private int newLabel() {
        return labelCounter++;
    }
}
