import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds a Mini-C abstract syntax tree from a {@link miniCParser} parse tree.
 */
class MiniCAstBuilder extends miniCParserBaseVisitor<Object> {

    @Override
    public McProgram visitProgram(miniCParser.ProgramContext ctx) {
        List<McFunction> funcs = new ArrayList<>();
        for (miniCParser.FunctionDefinitionContext fd : ctx.functionDefinition()) {
            funcs.add((McFunction) visit(fd));
        }
        return new McProgram(funcs);
    }

    @Override
    public McFunction visitFunctionDefinition(miniCParser.FunctionDefinitionContext ctx) {
        if (ctx.mainFunction() != null) {
            return visitMainFunction(ctx.mainFunction());
        }
        return visitIntFunction(ctx.intFunction());
    }

    @Override
    public McFunction visitMainFunction(miniCParser.MainFunctionContext ctx) {
        McBlock body = visitBlock(ctx.block());
        return new McFunction("main", List.of(), List.of(), body, SourceSpan.from(ctx.MAIN()));
    }

    @Override
    public McFunction visitIntFunction(miniCParser.IntFunctionContext ctx) {
        String name = ctx.ID().getText();
        List<String> params = new ArrayList<>();
        List<SourceSpan> paramSpans = new ArrayList<>();
        if (ctx.parameterList() != null) {
            for (miniCParser.ParameterContext p : ctx.parameterList().parameter()) {
                params.add(p.ID().getText());
                paramSpans.add(SourceSpan.from(p.ID()));
            }
        }
        McBlock body = visitBlock(ctx.block());
        return new McFunction(name, params, paramSpans, body, SourceSpan.from(ctx.ID()));
    }

    @Override
    public McBlock visitBlock(miniCParser.BlockContext ctx) {
        List<McBlockItem> items = new ArrayList<>();
        for (miniCParser.BlockItemContext bi : ctx.blockItem()) {
            items.add((McBlockItem) visit(bi));
        }
        return new McBlock(items);
    }

    @Override
    public McBlockItem visitBlockItem(miniCParser.BlockItemContext ctx) {
        if (ctx.declaration() != null) {
            return new McDeclBlockItem(visitDeclaration(ctx.declaration()));
        }
        return new McStmtBlockItem(visitStatement(ctx.statement()));
    }

    @Override
    public McDecl visitDeclaration(miniCParser.DeclarationContext ctx) {
        McType ty = mcTypeFrom(ctx.typeSpecifier());
        DeclaratorInfo d = parseDeclarator(ctx.declarator());
        McInitializer init = null;
        if (ctx.initializer() != null) {
            init = visitInitializer(ctx.initializer());
        }
        return new McDecl(ty, d.name, d.arraySize, init, SourceSpan.from(ctx.declarator().ID()));
    }

    private static McType mcTypeFrom(miniCParser.TypeSpecifierContext ctx) {
        if (ctx.INT() != null) {
            return McType.INT;
        }
        if (ctx.BOOL() != null) {
            return McType.BOOL;
        }
        if (ctx.CHAR() != null) {
            return McType.CHAR;
        }
        throw new IllegalStateException("unknown type specifier");
    }

    private DeclaratorInfo parseDeclarator(miniCParser.DeclaratorContext ctx) {
        String name = ctx.ID().getText();
        if (ctx.LBRACK() == null) {
            return new DeclaratorInfo(name, null);
        }
        long size = Long.parseLong(ctx.INT_LITERAL().getText());
        return new DeclaratorInfo(name, size);
    }

    @Override
    public McInitializer visitInitializer(miniCParser.InitializerContext ctx) {
        if (ctx.expression() != null) {
            return McInitializer.scalar(asExpr(visit(ctx.expression())));
        }
        List<McExpr> elems = new ArrayList<>();
        if (ctx.expressionList() != null) {
            for (miniCParser.ExpressionContext ex : ctx.expressionList().expression()) {
                elems.add(asExpr(visit(ex)));
            }
        }
        return McInitializer.brace(elems);
    }

    @Override
    public McStmt visitStatement(miniCParser.StatementContext ctx) {
        if (ctx.block() != null) {
            return new McBlockStmt(visitBlock(ctx.block()));
        }
        if (ctx.ifStatement() != null) {
            return visitIfStatement(ctx.ifStatement());
        }
        if (ctx.whileStatement() != null) {
            return visitWhileStatement(ctx.whileStatement());
        }
        if (ctx.returnStatement() != null) {
            return visitReturnStatement(ctx.returnStatement());
        }
        return visitExpressionStatement(ctx.expressionStatement());
    }

    @Override
    public McIfStmt visitIfStatement(miniCParser.IfStatementContext ctx) {
        McExpr cond = asExpr(visit(ctx.expression()));
        McStmt thenStmt = visitStatement(ctx.statement(0));
        McStmt elseStmt = null;
        if (ctx.ELSE() != null) {
            elseStmt = visitStatement(ctx.statement(1));
        }
        return new McIfStmt(cond, thenStmt, elseStmt, SourceSpan.from(ctx.IF()));
    }

    @Override
    public McWhileStmt visitWhileStatement(miniCParser.WhileStatementContext ctx) {
        McExpr cond = asExpr(visit(ctx.expression()));
        McStmt body = visitStatement(ctx.statement());
        return new McWhileStmt(cond, body, SourceSpan.from(ctx.WHILE()));
    }

    @Override
    public McReturnStmt visitReturnStatement(miniCParser.ReturnStatementContext ctx) {
        McExpr value = asExpr(visit(ctx.expression()));
        return new McReturnStmt(value, SourceSpan.from(ctx.RETURN()));
    }

    @Override
    public McExprStmt visitExpressionStatement(miniCParser.ExpressionStatementContext ctx) {
        McExpr expr = null;
        if (ctx.expression() != null) {
            expr = asExpr(visit(ctx.expression()));
        }
        return new McExprStmt(expr);
    }

    @Override
    public McExpr visitExpression(miniCParser.ExpressionContext ctx) {
        return asExpr(visit(ctx.assignmentExpression()));
    }

    @Override
    public McExpr visitAssignmentExpression(miniCParser.AssignmentExpressionContext ctx) {
        if (ctx.lvalue() != null) {
            McLvalue lhs = visitLvalue(ctx.lvalue());
            McExpr rhs = asExpr(visit(ctx.assignmentExpression()));
            return new McAssignExpr(lhs, rhs, SourceSpan.from(ctx.ASSIGN()));
        }
        return asExpr(visit(ctx.logicalOrExpression()));
    }

    @Override
    public McLvalue visitLvalue(miniCParser.LvalueContext ctx) {
        String name = ctx.ID().getText();
        if (ctx.LBRACK() == null) {
            return new McIdLvalue(name, SourceSpan.from(ctx.ID()));
        }
        McExpr index = asExpr(visit(ctx.expression()));
        return new McArrayLvalue(name, index, SourceSpan.from(ctx.ID()));
    }

    @Override
    public McExpr visitLogicalOrExpression(miniCParser.LogicalOrExpressionContext ctx) {
        McExpr node = asExpr(visit(ctx.logicalAndExpression(0)));
        for (int i = 1; i < ctx.logicalAndExpression().size(); i++) {
            String op = ctx.OROR(i - 1).getText();
            McExpr right = asExpr(visit(ctx.logicalAndExpression(i)));
            node = new McBinaryExpr(op, node, right, SourceSpan.from(ctx.OROR(i - 1)));
        }
        return node;
    }

    @Override
    public McExpr visitLogicalAndExpression(miniCParser.LogicalAndExpressionContext ctx) {
        McExpr node = asExpr(visit(ctx.equalityExpression(0)));
        for (int i = 1; i < ctx.equalityExpression().size(); i++) {
            String op = ctx.ANDAND(i - 1).getText();
            McExpr right = asExpr(visit(ctx.equalityExpression(i)));
            node = new McBinaryExpr(op, node, right, SourceSpan.from(ctx.ANDAND(i - 1)));
        }
        return node;
    }

    @Override
    public McExpr visitEqualityExpression(miniCParser.EqualityExpressionContext ctx) {
        McExpr node = asExpr(visit(ctx.relationalExpression(0)));
        for (int i = 1; i < ctx.relationalExpression().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            McExpr right = asExpr(visit(ctx.relationalExpression(i)));
            TerminalNode opNode = (TerminalNode) ctx.getChild(2 * i - 1);
            node = new McBinaryExpr(op, node, right, SourceSpan.from(opNode));
        }
        return node;
    }

    @Override
    public McExpr visitRelationalExpression(miniCParser.RelationalExpressionContext ctx) {
        McExpr node = asExpr(visit(ctx.additiveExpression(0)));
        for (int i = 1; i < ctx.additiveExpression().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            McExpr right = asExpr(visit(ctx.additiveExpression(i)));
            TerminalNode opNode = (TerminalNode) ctx.getChild(2 * i - 1);
            node = new McBinaryExpr(op, node, right, SourceSpan.from(opNode));
        }
        return node;
    }

    @Override
    public McExpr visitAdditiveExpression(miniCParser.AdditiveExpressionContext ctx) {
        McExpr node = asExpr(visit(ctx.multiplicativeExpression(0)));
        for (int i = 1; i < ctx.multiplicativeExpression().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            McExpr right = asExpr(visit(ctx.multiplicativeExpression(i)));
            TerminalNode opNode = (TerminalNode) ctx.getChild(2 * i - 1);
            node = new McBinaryExpr(op, node, right, SourceSpan.from(opNode));
        }
        return node;
    }

    @Override
    public McExpr visitMultiplicativeExpression(miniCParser.MultiplicativeExpressionContext ctx) {
        McExpr node = asExpr(visit(ctx.unaryExpression(0)));
        for (int i = 1; i < ctx.unaryExpression().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText();
            McExpr right = asExpr(visit(ctx.unaryExpression(i)));
            TerminalNode opNode = (TerminalNode) ctx.getChild(2 * i - 1);
            node = new McBinaryExpr(op, node, right, SourceSpan.from(opNode));
        }
        return node;
    }

    @Override
    public McExpr visitUnaryExpression(miniCParser.UnaryExpressionContext ctx) {
        if (ctx.MINUS() != null) {
            return new McUnaryExpr("-", asExpr(visit(ctx.unaryExpression())), SourceSpan.from(ctx.MINUS()));
        }
        if (ctx.NOT() != null) {
            return new McUnaryExpr("!", asExpr(visit(ctx.unaryExpression())), SourceSpan.from(ctx.NOT()));
        }
        return asExpr(visit(ctx.primaryExpression()));
    }

    @Override
    public McExpr visitPrimaryExpression(miniCParser.PrimaryExpressionContext ctx) {
        if (ctx.INT_LITERAL() != null) {
            return new McIntLit(Long.parseLong(ctx.INT_LITERAL().getText()));
        }
        if (ctx.CHAR_LITERAL() != null) {
            return new McCharLit(ctx.CHAR_LITERAL().getText());
        }
        if (ctx.TRUE() != null) {
            return new McBoolLit(true);
        }
        if (ctx.FALSE() != null) {
            return new McBoolLit(false);
        }
        if (ctx.functionCall() != null) {
            return visitFunctionCall(ctx.functionCall());
        }
        if (ctx.arrayAccess() != null) {
            return visitArrayAccess(ctx.arrayAccess());
        }
        if (ctx.LPAREN() != null) {
            return asExpr(visit(ctx.expression()));
        }
        return new McVarExpr(ctx.ID().getText(), SourceSpan.from(ctx.ID()));
    }

    @Override
    public McCallExpr visitFunctionCall(miniCParser.FunctionCallContext ctx) {
        String name = ctx.ID().getText();
        List<McExpr> args = new ArrayList<>();
        if (ctx.argumentList() != null) {
            for (miniCParser.ExpressionContext ex : ctx.argumentList().expression()) {
                args.add(asExpr(visit(ex)));
            }
        }
        return new McCallExpr(name, args, SourceSpan.from(ctx.ID()));
    }

    @Override
    public McArrayAccessExpr visitArrayAccess(miniCParser.ArrayAccessContext ctx) {
        String name = ctx.ID().getText();
        McExpr index = asExpr(visit(ctx.expression()));
        return new McArrayAccessExpr(name, index, SourceSpan.from(ctx.ID()));
    }

    private static McExpr asExpr(Object o) {
        return (McExpr) o;
    }

    private static final class DeclaratorInfo {
        final String name;
        final Long arraySize;

        DeclaratorInfo(String name, Long arraySize) {
            this.name = name;
            this.arraySize = arraySize;
        }
    }
}

// --- AST types ---

enum McType {
    INT, BOOL, CHAR
}

final class McProgram {
    final List<McFunction> functions;

    McProgram(List<McFunction> functions) {
        this.functions = functions;
    }

    @Override
    public String toString() {
        return functions.stream().map(McFunction::toString).collect(Collectors.joining("\n"));
    }
}

final class McFunction {
    final String name;
    final List<String> paramNames;
    /** Parallel to {@link #paramNames}; same length when built from the parser. */
    final List<SourceSpan> paramNameSpans;
    final McBlock body;
    /** Span of the function name token (or {@code main} keyword for main). */
    final SourceSpan nameSpan;

    McFunction(String name, List<String> paramNames, List<SourceSpan> paramNameSpans, McBlock body, SourceSpan nameSpan) {
        this.name = name;
        this.paramNames = paramNames;
        this.paramNameSpans = paramNameSpans;
        this.body = body;
        this.nameSpan = nameSpan;
    }

    @Override
    public String toString() {
        String params = paramNames.stream().map(p -> "int " + p).collect(Collectors.joining(", "));
        return "int " + name + "(" + params + ") " + body;
    }
}

final class McBlock {
    final List<McBlockItem> items;

    McBlock(List<McBlockItem> items) {
        this.items = items;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (McBlockItem it : items) {
            sb.append("  ").append(it).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }
}

interface McBlockItem {}

final class McDeclBlockItem implements McBlockItem {
    final McDecl decl;

    McDeclBlockItem(McDecl decl) {
        this.decl = decl;
    }

    @Override
    public String toString() {
        return decl.toString();
    }
}

final class McStmtBlockItem implements McBlockItem {
    final McStmt stmt;

    McStmtBlockItem(McStmt stmt) {
        this.stmt = stmt;
    }

    @Override
    public String toString() {
        return stmt.toString();
    }
}

final class McDecl {
    final McType type;
    final String name;
    final Long arraySize;
    final McInitializer initializer;
    /** Span of the declarator identifier. */
    final SourceSpan nameSpan;

    McDecl(McType type, String name, Long arraySize, McInitializer initializer, SourceSpan nameSpan) {
        this.type = type;
        this.name = name;
        this.arraySize = arraySize;
        this.initializer = initializer;
        this.nameSpan = nameSpan;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type.name().toLowerCase()).append(" ");
        sb.append(name);
        if (arraySize != null) {
            sb.append("[").append(arraySize).append("]");
        }
        if (initializer != null) {
            sb.append(" = ").append(initializer);
        }
        sb.append(";");
        return sb.toString();
    }
}

final class McInitializer {
    final List<McExpr> elements;
    private final boolean scalarForm;

    private McInitializer(List<McExpr> elements, boolean scalarForm) {
        this.elements = elements;
        this.scalarForm = scalarForm;
    }

    static McInitializer scalar(McExpr e) {
        return new McInitializer(List.of(e), true);
    }

    static McInitializer brace(List<McExpr> elems) {
        return new McInitializer(elems, false);
    }

    /** {@code true} for {@code = expr}; {@code false} for {@code = { ... }}. */
    boolean isScalarInitializer() {
        return scalarForm;
    }

    @Override
    public String toString() {
        if (scalarForm) {
            return elements.get(0).toString();
        }
        return "{" + elements.stream().map(McExpr::toString).collect(Collectors.joining(", ")) + "}";
    }
}

interface McStmt {}

final class McBlockStmt implements McStmt {
    final McBlock block;

    McBlockStmt(McBlock block) {
        this.block = block;
    }

    @Override
    public String toString() {
        return block.toString();
    }
}

final class McIfStmt implements McStmt {
    final McExpr condition;
    final McStmt thenBranch;
    final McStmt elseBranch;
    final SourceSpan span;

    McIfStmt(McExpr condition, McStmt thenBranch, McStmt elseBranch, SourceSpan span) {
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
        this.span = span;
    }

    @Override
    public String toString() {
        String s = "if (" + condition + ") " + thenBranch;
        if (elseBranch != null) {
            s += " else " + elseBranch;
        }
        return s;
    }
}

final class McWhileStmt implements McStmt {
    final McExpr condition;
    final McStmt body;
    final SourceSpan span;

    McWhileStmt(McExpr condition, McStmt body, SourceSpan span) {
        this.condition = condition;
        this.body = body;
        this.span = span;
    }

    @Override
    public String toString() {
        return "while (" + condition + ") " + body;
    }
}

final class McReturnStmt implements McStmt {
    final McExpr value;
    final SourceSpan span;

    McReturnStmt(McExpr value, SourceSpan span) {
        this.value = value;
        this.span = span;
    }

    @Override
    public String toString() {
        return "return " + value + ";";
    }
}

final class McExprStmt implements McStmt {
    final McExpr expr;

    McExprStmt(McExpr expr) {
        this.expr = expr;
    }

    @Override
    public String toString() {
        if (expr == null) {
            return ";";
        }
        return expr + ";";
    }
}

interface McExpr {}

final class McBinaryExpr implements McExpr {
    final String op;
    final McExpr left;
    final McExpr right;
    final SourceSpan opSpan;

    McBinaryExpr(String op, McExpr left, McExpr right, SourceSpan opSpan) {
        this.op = op;
        this.left = left;
        this.right = right;
        this.opSpan = opSpan;
    }

    @Override
    public String toString() {
        return "(" + left + " " + op + " " + right + ")";
    }
}

final class McUnaryExpr implements McExpr {
    final String op;
    final McExpr expr;
    final SourceSpan span;

    McUnaryExpr(String op, McExpr expr, SourceSpan span) {
        this.op = op;
        this.expr = expr;
        this.span = span;
    }

    @Override
    public String toString() {
        return "(" + op + expr + ")";
    }
}

final class McIntLit implements McExpr {
    final long value;

    McIntLit(long value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}

final class McCharLit implements McExpr {
    final String raw;

    McCharLit(String raw) {
        this.raw = raw;
    }

    @Override
    public String toString() {
        return raw;
    }
}

final class McBoolLit implements McExpr {
    final boolean value;

    McBoolLit(boolean value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value ? "true" : "false";
    }
}

final class McVarExpr implements McExpr {
    final String name;
    final SourceSpan span;

    McVarExpr(String name, SourceSpan span) {
        this.name = name;
        this.span = span;
    }

    @Override
    public String toString() {
        return name;
    }
}

final class McArrayAccessExpr implements McExpr {
    final String name;
    final McExpr index;
    final SourceSpan span;

    McArrayAccessExpr(String name, McExpr index, SourceSpan span) {
        this.name = name;
        this.index = index;
        this.span = span;
    }

    @Override
    public String toString() {
        return name + "[" + index + "]";
    }
}

final class McCallExpr implements McExpr {
    final String name;
    final List<McExpr> args;
    final SourceSpan span;

    McCallExpr(String name, List<McExpr> args, SourceSpan span) {
        this.name = name;
        this.args = args;
        this.span = span;
    }

    @Override
    public String toString() {
        return name + "(" + args.stream().map(McExpr::toString).collect(Collectors.joining(", ")) + ")";
    }
}

interface McLvalue {}

final class McIdLvalue implements McLvalue {
    final String name;
    final SourceSpan span;

    McIdLvalue(String name, SourceSpan span) {
        this.name = name;
        this.span = span;
    }

    @Override
    public String toString() {
        return name;
    }
}

final class McArrayLvalue implements McLvalue {
    final String name;
    final McExpr index;
    final SourceSpan span;

    McArrayLvalue(String name, McExpr index, SourceSpan span) {
        this.name = name;
        this.index = index;
        this.span = span;
    }

    @Override
    public String toString() {
        return name + "[" + index + "]";
    }
}

final class McAssignExpr implements McExpr {
    final McLvalue lhs;
    final McExpr rhs;
    final SourceSpan span;

    McAssignExpr(McLvalue lhs, McExpr rhs, SourceSpan span) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.span = span;
    }

    @Override
    public String toString() {
        return "(" + lhs + " = " + rhs + ")";
    }
}
