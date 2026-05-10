import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Semantic analysis: symbol tables, type checking, and multi-error collection.
 */
final class SemanticAnalyzer {

    private final Map<String, FuncSymbol> functions = new HashMap<>();
    private final SemanticDiagnostics diag = new SemanticDiagnostics();

    SemanticDiagnostics analyze(McProgram program) {
        functions.clear();
        List<McFunction> registered = new ArrayList<>();
        for (McFunction f : program.functions) {
            if (functions.containsKey(f.name)) {
                diag.error("duplicate function: " + f.name, f.nameSpan);
                continue;
            }
            List<SemType> params = new ArrayList<>();
            for (int i = 0; i < f.paramNames.size(); i++) {
                params.add(SemType.INT);
            }
            functions.put(f.name, new FuncSymbol(f.name, SemType.INT, params));
            registered.add(f);
        }
        for (McFunction f : registered) {
            checkFunctionBody(f);
        }
        return diag;
    }

    private void checkFunctionBody(McFunction f) {
        FuncSymbol sig = functions.get(f.name);
        if (sig == null) {
            return;
        }
        Scope scope = new Scope(null);
        for (int i = 0; i < f.paramNames.size(); i++) {
            String p = f.paramNames.get(i);
            SourceSpan pspan = i < f.paramNameSpans.size() ? f.paramNameSpans.get(i) : null;
            if (scope.declaresLocally(p)) {
                diag.error("duplicate parameter: " + p, pspan);
            } else {
                scope.declare(new VarSymbol(p, SemType.INT, false, null));
            }
        }
        processBlockItems(f.body.items, scope, sig.returnType);
    }

    private void processBlockItems(List<McBlockItem> items, Scope scope, SemType returnType) {
        for (McBlockItem item : items) {
            if (item instanceof McDeclBlockItem) {
                checkDecl(((McDeclBlockItem) item).decl, scope);
            } else if (item instanceof McStmtBlockItem) {
                checkStmt(((McStmtBlockItem) item).stmt, scope, returnType);
            }
        }
    }

    private void checkDecl(McDecl d, Scope scope) {
        if (scope.declaresLocally(d.name)) {
            diag.error("duplicate declaration in same scope: " + d.name, d.nameSpan);
        }
        SemType elem = mcTypeToSem(d.type);
        scope.declare(new VarSymbol(d.name, elem, d.arraySize != null, d.arraySize));

        if (d.initializer == null) {
            return;
        }

        McInitializer init = d.initializer;
        if (d.arraySize == null) {
            if (!init.isScalarInitializer()) {
                diag.error("scalar declaration cannot use brace initializer: " + d.name, d.nameSpan);
                return;
            }
            SemType t = evalExpr(init.elements.get(0), scope);
            if (!assignableToVar(elem, t)) {
                diag.error("initializer type mismatch for '" + d.name + "'", d.nameSpan);
            }
        } else {
            if (init.isScalarInitializer()) {
                diag.error("array '" + d.name + "' requires a brace initializer { ... }", d.nameSpan);
                return;
            }
            long expected = d.arraySize;
            if (init.elements.size() != expected) {
                diag.error("array '" + d.name + "' initializer count " + init.elements.size()
                        + " does not match size " + expected, d.nameSpan);
            }
            for (McExpr e : init.elements) {
                SemType t = evalExpr(e, scope);
                if (!assignableToVar(elem, t)) {
                    diag.error("array '" + d.name + "' element type mismatch", d.nameSpan);
                }
            }
        }
    }

    private void checkStmt(McStmt stmt, Scope scope, SemType returnType) {
        if (stmt instanceof McBlockStmt) {
            Scope inner = new Scope(scope);
            processBlockItems(((McBlockStmt) stmt).block.items, inner, returnType);
        } else if (stmt instanceof McIfStmt) {
            McIfStmt s = (McIfStmt) stmt;
            SemType cond = evalExpr(s.condition, scope);
            if (cond != SemType.BOOL && cond != SemType.ERROR) {
                diag.error("if condition must be bool", s.span);
            }
            checkStmt(s.thenBranch, scope, returnType);
            if (s.elseBranch != null) {
                checkStmt(s.elseBranch, scope, returnType);
            }
        } else if (stmt instanceof McWhileStmt) {
            McWhileStmt s = (McWhileStmt) stmt;
            SemType cond = evalExpr(s.condition, scope);
            if (cond != SemType.BOOL && cond != SemType.ERROR) {
                diag.error("while condition must be bool", s.span);
            }
            checkStmt(s.body, scope, returnType);
        } else if (stmt instanceof McReturnStmt) {
            McReturnStmt rs = (McReturnStmt) stmt;
            SemType t = evalExpr(rs.value, scope);
            if (returnType == SemType.INT) {
                if (!isIntLike(t) && t != SemType.ERROR) {
                    diag.error("return type mismatch (expected int)", rs.span);
                }
            }
        } else if (stmt instanceof McExprStmt) {
            McExpr e = ((McExprStmt) stmt).expr;
            if (e != null) {
                evalExpr(e, scope);
            }
        }
    }

    private SemType evalExpr(McExpr e, Scope scope) {
        if (e instanceof McIntLit) {
            return SemType.INT;
        }
        if (e instanceof McBoolLit) {
            return SemType.BOOL;
        }
        if (e instanceof McCharLit) {
            return SemType.CHAR;
        }
        if (e instanceof McVarExpr) {
            McVarExpr ve = (McVarExpr) e;
            VarSymbol v = scope.lookup(ve.name);
            if (v == null) {
                diag.error("undeclared identifier: " + ve.name, ve.span);
                return SemType.ERROR;
            }
            if (v.isArray) {
                diag.error("array '" + v.name + "' used without index", ve.span);
                return SemType.ERROR;
            }
            return v.elementType;
        }
        if (e instanceof McArrayAccessExpr) {
            McArrayAccessExpr a = (McArrayAccessExpr) e;
            VarSymbol v = scope.lookup(a.name);
            if (v == null) {
                diag.error("undeclared identifier: " + a.name, a.span);
                return SemType.ERROR;
            }
            if (!v.isArray) {
                diag.error("'" + a.name + "' is not an array", a.span);
                return SemType.ERROR;
            }
            SemType idx = evalExpr(a.index, scope);
            if (idx != SemType.INT && idx != SemType.ERROR) {
                diag.error("array index must be int", a.span);
            }
            return v.elementType;
        }
        if (e instanceof McCallExpr) {
            return checkCall((McCallExpr) e, scope);
        }
        if (e instanceof McBinaryExpr) {
            return evalBinary((McBinaryExpr) e, scope);
        }
        if (e instanceof McUnaryExpr) {
            return evalUnary((McUnaryExpr) e, scope);
        }
        if (e instanceof McAssignExpr) {
            return evalAssign((McAssignExpr) e, scope);
        }
        diag.error("internal: unknown expression node");
        return SemType.ERROR;
    }

    private SemType evalAssign(McAssignExpr e, Scope scope) {
        SemType lhsType = lvalueTypeForAssign(e.lhs, scope);
        SemType rhsType = evalExpr(e.rhs, scope);
        if (lhsType != SemType.ERROR && rhsType != SemType.ERROR && !assignableToVar(lhsType, rhsType)) {
            diag.error("assignment type mismatch", e.span);
        }
        return lhsType == SemType.ERROR ? SemType.ERROR : lhsType;
    }

    /** Type of value stored in the location (for assignment target). */
    private SemType lvalueTypeForAssign(McLvalue lhs, Scope scope) {
        if (lhs instanceof McIdLvalue) {
            McIdLvalue id = (McIdLvalue) lhs;
            String name = id.name;
            VarSymbol v = scope.lookup(name);
            if (v == null) {
                diag.error("undeclared identifier: " + name, id.span);
                return SemType.ERROR;
            }
            if (v.isArray) {
                diag.error("array '" + name + "' requires index in assignment", id.span);
                return SemType.ERROR;
            }
            return v.elementType;
        }
        if (lhs instanceof McArrayLvalue) {
            McArrayLvalue a = (McArrayLvalue) lhs;
            VarSymbol v = scope.lookup(a.name);
            if (v == null) {
                diag.error("undeclared identifier: " + a.name, a.span);
                return SemType.ERROR;
            }
            if (!v.isArray) {
                diag.error("'" + a.name + "' is not an array", a.span);
                return SemType.ERROR;
            }
            SemType idx = evalExpr(a.index, scope);
            if (idx != SemType.INT && idx != SemType.ERROR) {
                diag.error("array index must be int", a.span);
            }
            return v.elementType;
        }
        return SemType.ERROR;
    }

    private SemType checkCall(McCallExpr c, Scope scope) {
        FuncSymbol fn = functions.get(c.name);
        if (fn == null) {
            diag.error("call to undefined function: " + c.name, c.span);
            return SemType.ERROR;
        }
        if (c.args.size() != fn.paramTypes.size()) {
            diag.error("wrong argument count for '" + c.name + "' (expected " + fn.paramTypes.size()
                    + ", got " + c.args.size() + ")", c.span);
        }
        int n = Math.min(c.args.size(), fn.paramTypes.size());
        for (int i = 0; i < n; i++) {
            SemType argT = evalExpr(c.args.get(i), scope);
            if (!isIntLike(argT) && argT != SemType.ERROR) {
                diag.error("argument " + (i + 1) + " to '" + c.name + "' must be int-compatible", c.span);
            }
        }
        return fn.returnType;
    }

    private SemType evalUnary(McUnaryExpr e, Scope scope) {
        SemType inner = evalExpr(e.expr, scope);
        if ("!".equals(e.op)) {
            if (inner != SemType.BOOL && inner != SemType.ERROR) {
                diag.error("operator ! expects bool operand", e.span);
            }
            return SemType.BOOL;
        }
        if ("-".equals(e.op)) {
            if (!isNumeric(inner) && inner != SemType.ERROR) {
                diag.error("unary - expects numeric operand", e.span);
            }
            return SemType.INT;
        }
        diag.error("unknown unary operator: " + e.op, e.span);
        return SemType.ERROR;
    }

    private SemType evalBinary(McBinaryExpr e, Scope scope) {
        SemType l = evalExpr(e.left, scope);
        SemType r = evalExpr(e.right, scope);
        String op = e.op;

        if ("&&".equals(op) || "||".equals(op)) {
            if (l != SemType.BOOL && l != SemType.ERROR) {
                diag.error("logical operator '" + op + "' requires bool left operand", e.opSpan);
            }
            if (r != SemType.BOOL && r != SemType.ERROR) {
                diag.error("logical operator '" + op + "' requires bool right operand", e.opSpan);
            }
            return SemType.BOOL;
        }

        if ("==".equals(op) || "!=".equals(op)) {
            if (!compatibleForEquality(l, r)) {
                diag.error("incompatible operands for " + op, e.opSpan);
            }
            return SemType.BOOL;
        }

        if ("<".equals(op) || "<=".equals(op) || ">".equals(op) || ">=".equals(op)) {
            if (!isNumeric(l) && l != SemType.ERROR) {
                diag.error("comparison '" + op + "' requires numeric left operand", e.opSpan);
            }
            if (!isNumeric(r) && r != SemType.ERROR) {
                diag.error("comparison '" + op + "' requires numeric right operand", e.opSpan);
            }
            return SemType.BOOL;
        }

        if ("+".equals(op) || "-".equals(op) || "*".equals(op) || "/".equals(op) || "%".equals(op)) {
            if (!isNumeric(l) && l != SemType.ERROR) {
                diag.error("arithmetic '" + op + "' requires numeric left operand", e.opSpan);
            }
            if (!isNumeric(r) && r != SemType.ERROR) {
                diag.error("arithmetic '" + op + "' requires numeric right operand", e.opSpan);
            }
            return SemType.INT;
        }

        diag.error("unknown binary operator: " + op, e.opSpan);
        return SemType.ERROR;
    }

    private static boolean compatibleForEquality(SemType l, SemType r) {
        if (l == SemType.ERROR || r == SemType.ERROR) {
            return true;
        }
        if (l == SemType.BOOL || r == SemType.BOOL) {
            return l == r;
        }
        return isNumeric(l) && isNumeric(r);
    }

    private static boolean isNumeric(SemType t) {
        return t == SemType.INT || t == SemType.CHAR;
    }

    private static boolean isIntLike(SemType t) {
        return t == SemType.INT || t == SemType.CHAR || t == SemType.ERROR;
    }

    private static boolean assignableToVar(SemType target, SemType value) {
        if (value == SemType.ERROR) {
            return true;
        }
        if (target == value) {
            return true;
        }
        if (target == SemType.INT && value == SemType.CHAR) {
            return true;
        }
        if (target == SemType.CHAR && value == SemType.INT) {
            return true;
        }
        return false;
    }

    private static SemType mcTypeToSem(McType t) {
        switch (t) {
            case INT:
                return SemType.INT;
            case BOOL:
                return SemType.BOOL;
            case CHAR:
                return SemType.CHAR;
            default:
                return SemType.ERROR;
        }
    }
}
