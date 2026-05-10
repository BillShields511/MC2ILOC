import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class APassASTtoIR {
    private int slotCursor = 0;
    private int labelCursor = 0;
    private final Map<String, IRFunctionSignature> signatures = new HashMap<>();

    IRLoweringResult lower(McProgram program) {
        List<IRFunction> funcs = new ArrayList<>();

        // Predeclare signatures so calls can reference functions declared later.
        for (McFunction fn : program.functions) {
            IRFunctionSignature sig = new IRFunctionSignature(fn.name, funcLabel(fn.name));
            signatures.put(fn.name, sig);
            for (int i = 0; i < fn.paramNames.size(); i++) {
                sig.paramSlots.add(allocSlot());
            }
            sig.returnSlot = allocSlot();
            if (!"main".equals(fn.name)) {
                sig.savedRaSlot = allocSlot();
            }
        }

        for (McFunction fn : program.functions) {
            funcs.add(lowerFunction(fn));
        }

        String entry = signatures.containsKey("main")
                ? signatures.get("main").label
                : (!funcs.isEmpty() ? funcs.get(0).label : "L_missing_entry");
        return new IRLoweringResult(new IRProgram(funcs, entry), signatures);
    }

    private IRFunction lowerFunction(McFunction fn) {
        IRFunctionSignature sig = signatures.get(fn.name);
        FunctionLowerCtx ctx = new FunctionLowerCtx(sig);
        ctx.emit(new IRLabel(sig.label));

        if (sig.savedRaSlot != null) {
            ctx.emit(new IRSaveRa(sig.savedRaSlot));
        }

        // Parameter names are addressable in this function using pre-allocated slots.
        for (int i = 0; i < fn.paramNames.size(); i++) {
            ctx.declare(fn.paramNames.get(i), new VarLoc(sig.paramSlots.get(i), false, 1));
        }

        lowerBlock(fn.body, ctx);

        // Implicit fallback when execution falls off the end of the function body.
        if (!endsWithIrReturn(ctx.instructions)) {
            int zero = ctx.newTemp();
            ctx.emit(new IRLoadImm(zero, 0));
            ctx.emit(new IRReturn(zero));
        }

        return new IRFunction(
                fn.name,
                sig.label,
                ctx.instructions,
                List.copyOf(sig.paramSlots),
                sig.returnSlot,
                sig.savedRaSlot,
                ctx.localCount,
                "main".equals(fn.name) ? ctx.mainTraceSlotOffset : null
        );
    }

    private void lowerBlock(McBlock block, FunctionLowerCtx ctx) {
        ctx.pushScope();
        for (McBlockItem item : block.items) {
            if (item instanceof McDeclBlockItem declItem) {
                lowerDecl(declItem.decl, ctx);
            } else if (item instanceof McStmtBlockItem stmtItem) {
                lowerStmt(stmtItem.stmt, ctx);
            }
        }
        ctx.popScope();
    }

    private void lowerDecl(McDecl decl, FunctionLowerCtx ctx) {
        boolean isArray = decl.arraySize != null;
        int len = isArray ? Math.toIntExact(decl.arraySize) : 1;
        int base = allocSlot();
        for (int i = 1; i < len; i++) {
            allocSlot();
        }
        ctx.localCount += len;
        VarLoc loc = new VarLoc(base, isArray, len);
        ctx.declare(decl.name, loc);

        if ("main".equals(ctx.signature.name)
                && "__mc_trace".equals(decl.name)
                && decl.type == McType.INT
                && decl.arraySize == null
                && ctx.scopeDepth() == 2
                && ctx.mainTraceSlotOffset == null) {
            ctx.mainTraceSlotOffset = loc.baseOffset;
        }

        if (decl.initializer == null) {
            return;
        }
        McInitializer init = decl.initializer;
        if (!loc.isArray) {
            int value = lowerExpr(init.elements.get(0), ctx);
            ctx.emit(new IRStoreSlot(loc.baseOffset, value));
            return;
        }

        for (int i = 0; i < init.elements.size() && i < loc.length; i++) {
            int v = lowerExpr(init.elements.get(i), ctx);
            ctx.emit(new IRStoreSlot(loc.baseOffset + (i * 8), v));
        }
    }

    private void lowerStmt(McStmt stmt, FunctionLowerCtx ctx) {
        if (stmt instanceof McBlockStmt b) {
            lowerBlock(b.block, ctx);
            return;
        }
        if (stmt instanceof McIfStmt s) {
            int cond = lowerExpr(s.condition, ctx);
            String lThen = newLabel("if_then");
            String lElse = newLabel("if_else");
            String lJoin = newLabel("if_join");
            ctx.emit(new IRCbr(cond, lThen, lElse));
            ctx.emit(new IRLabel(lThen));
            lowerStmt(s.thenBranch, ctx);
            ctx.emit(new IRJump(lJoin));
            ctx.emit(new IRLabel(lElse));
            if (s.elseBranch != null) {
                lowerStmt(s.elseBranch, ctx);
            }
            ctx.emit(new IRLabel(lJoin));
            return;
        }
        if (stmt instanceof McWhileStmt s) {
            String lHead = newLabel("while_head");
            String lBody = newLabel("while_body");
            String lDone = newLabel("while_done");
            ctx.emit(new IRLabel(lHead));
            int cond = lowerExpr(s.condition, ctx);
            ctx.emit(new IRCbr(cond, lBody, lDone));
            ctx.emit(new IRLabel(lBody));
            lowerStmt(s.body, ctx);
            ctx.emit(new IRJump(lHead));
            ctx.emit(new IRLabel(lDone));
            return;
        }
        if (stmt instanceof McReturnStmt s) {
            int value = lowerExpr(s.value, ctx);
            ctx.emit(new IRReturn(value));
            return;
        }
        if (stmt instanceof McExprStmt s && s.expr != null) {
            lowerExpr(s.expr, ctx);
        }
    }

    private int lowerExpr(McExpr expr, FunctionLowerCtx ctx) {
        if (expr instanceof McIntLit lit) {
            int t = ctx.newTemp();
            ctx.emit(new IRLoadImm(t, lit.value));
            return t;
        }
        if (expr instanceof McBoolLit lit) {
            int t = ctx.newTemp();
            ctx.emit(new IRLoadImm(t, lit.value ? 1 : 0));
            return t;
        }
        if (expr instanceof McCharLit lit) {
            int t = ctx.newTemp();
            ctx.emit(new IRLoadImm(t, parseCharLiteral(lit.raw)));
            return t;
        }
        if (expr instanceof McVarExpr ve) {
            VarLoc loc = ctx.lookup(ve.name);
            int t = ctx.newTemp();
            ctx.emit(new IRLoadSlot(t, loc.baseOffset));
            return t;
        }
        if (expr instanceof McArrayAccessExpr ae) {
            VarLoc loc = ctx.lookup(ae.name);
            int idx = lowerExpr(ae.index, ctx);
            int t = ctx.newTemp();
            ctx.emit(new IRLoadArrayElem(t, loc.baseOffset, idx));
            return t;
        }
        if (expr instanceof McUnaryExpr ue) {
            int src = lowerExpr(ue.expr, ctx);
            int dst = ctx.newTemp();
            if ("-".equals(ue.op)) {
                ctx.emit(new IRUnary("neg", src, dst));
            } else if ("!".equals(ue.op)) {
                ctx.emit(new IRUnary("not", src, dst));
            } else {
                ctx.emit(new IRUnary("id", src, dst));
            }
            return dst;
        }
        if (expr instanceof McBinaryExpr be) {
            int l = lowerExpr(be.left, ctx);
            int r = lowerExpr(be.right, ctx);
            int dst = ctx.newTemp();
            ctx.emit(new IRBinary(be.op, l, r, dst));
            return dst;
        }
        if (expr instanceof McAssignExpr ae) {
            int v = lowerExpr(ae.rhs, ctx);
            if (ae.lhs instanceof McIdLvalue id) {
                VarLoc loc = ctx.lookup(id.name);
                ctx.emit(new IRStoreSlot(loc.baseOffset, v));
                return v;
            }
            if (ae.lhs instanceof McArrayLvalue arr) {
                VarLoc loc = ctx.lookup(arr.name);
                int idx = lowerExpr(arr.index, ctx);
                ctx.emit(new IRStoreArrayElem(loc.baseOffset, idx, v));
                return v;
            }
            return v;
        }
        if (expr instanceof McCallExpr call) {
            TempList args = new TempList();
            for (McExpr argExpr : call.args) {
                args.add(lowerExpr(argExpr, ctx));
            }
            int dst = ctx.newTemp();
            ctx.emit(new IRCall(call.name, args.immutable(), dst));
            return dst;
        }

        int fallback = ctx.newTemp();
        ctx.emit(new IRLoadImm(fallback, 0));
        return fallback;
    }

    private int parseCharLiteral(String raw) {
        if (raw == null || raw.length() < 2) {
            return 0;
        }
        String body = raw.substring(1, raw.length() - 1);
        if (body.isEmpty()) {
            return 0;
        }
        if (body.charAt(0) != '\\') {
            return body.charAt(0);
        }
        if (body.length() == 1) {
            return '\\';
        }
        char esc = body.charAt(1);
        return switch (esc) {
            case 'n' -> '\n';
            case 't' -> '\t';
            case 'r' -> '\r';
            case '0' -> '\0';
            case '\\' -> '\\';
            case '\'' -> '\'';
            case '"' -> '"';
            default -> esc;
        };
    }

    private int allocSlot() {
        int slot = slotCursor;
        slotCursor += 8;
        return slot;
    }

    private String newLabel(String stem) {
        return stem + "_" + (labelCursor++);
    }

    private String funcLabel(String name) {
        return "fn_" + name;
    }

    private static final class VarLoc {
        final int baseOffset;
        final boolean isArray;
        final int length;

        VarLoc(int baseOffset, boolean isArray, int length) {
            this.baseOffset = baseOffset;
            this.isArray = isArray;
            this.length = length;
        }
    }

    private static final class FunctionLowerCtx {
        final IRFunctionSignature signature;
        final List<IRInstr> instructions = new ArrayList<>();
        final Deque<Map<String, VarLoc>> scopes = new ArrayDeque<>();
        final VRegPool regs = new VRegPool();
        int localCount = 0;
        Integer mainTraceSlotOffset;

        FunctionLowerCtx(IRFunctionSignature signature) {
            this.signature = signature;
            pushScope();
        }

        /** Number of lexical scopes nested (same as scope stack depth). Params live at depth 1. */
        int scopeDepth() {
            return scopes.size();
        }

        void pushScope() {
            scopes.push(new HashMap<>());
        }

        void popScope() {
            scopes.pop();
        }

        void declare(String name, VarLoc loc) {
            scopes.peek().put(name, loc);
        }

        VarLoc lookup(String name) {
            for (Map<String, VarLoc> scope : scopes) {
                if (scope.containsKey(name)) {
                    return scope.get(name);
                }
            }
            // Semantic analyzer already rejects unresolved identifiers.
            return new VarLoc(0, false, 1);
        }

        int newTemp() {
            return regs.next();
        }

        void emit(IRInstr instr) {
            instructions.add(instr);
        }
    }

    private static boolean endsWithIrReturn(List<IRInstr> instructions) {
        for (int i = instructions.size() - 1; i >= 0; i--) {
            IRInstr ins = instructions.get(i);
            if (ins instanceof IRLabel) {
                continue;
            }
            return ins instanceof IRReturn;
        }
        return false;
    }
}
