import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class BPassRegisterAllocator {
    private static final int MAX_USER_REGS = 16;
    private static final int SPILL_BASE = 0x1000;
    private static final int SCRATCH_LEFT = 2;
    private static final int SCRATCH_RIGHT = 3;
    private static final int SCRATCH_DST = 4;
    private static final int SCRATCH_AUX = 5;

    private final List<String> out = new ArrayList<>();
    private final Map<Integer, Integer> spillOffsetByTemp = new HashMap<>();
    private final Map<String, IRFunctionSignature> signatures;

    BPassRegisterAllocator(Map<String, IRFunctionSignature> signatures) {
        this.signatures = signatures;
    }

    List<String> emit(IRProgram ir) {
        out.clear();
        spillOffsetByTemp.clear();
        out.add(".code");
        out.add("loadI 0 => r0");
        out.add("loadI " + SPILL_BASE + " => r1");
        out.add("jumpI => " + ir.entryLabel);

        for (IRFunction fn : ir.functions) {
            emitFunction(fn);
        }
        return List.copyOf(out);
    }

    private void emitFunction(IRFunction fn) {
        for (IRInstr instr : fn.instructions) {
            emitInstr(fn, instr);
        }
    }

    private void emitInstr(IRFunction fn, IRInstr instr) {
        if (instr instanceof IRLabel l) {
            emitLine(l.name() + ": nop");
            return;
        }
        if (instr instanceof IRLoadImm li) {
            emitLine("loadI " + li.value() + " => r" + SCRATCH_DST);
            spillStore(li.dst(), SCRATCH_DST);
            return;
        }
        if (instr instanceof IRLoadSlot ls) {
            emitLine("loadAI r0, " + ls.slotOffset() + " => r" + SCRATCH_DST);
            spillStore(ls.dst(), SCRATCH_DST);
            return;
        }
        if (instr instanceof IRStoreSlot ss) {
            spillLoad(ss.src(), SCRATCH_LEFT);
            emitLine("storeAI r" + SCRATCH_LEFT + " => r0, " + ss.slotOffset());
            return;
        }
        if (instr instanceof IRLoadArrayElem la) {
            spillLoad(la.indexTemp(), SCRATCH_LEFT);
            emitLine("multI r" + SCRATCH_LEFT + ", 8 => r" + SCRATCH_LEFT);
            emitLine("addI r" + SCRATCH_LEFT + ", " + la.baseOffset() + " => r" + SCRATCH_LEFT);
            emitLine("loadAO r0, r" + SCRATCH_LEFT + " => r" + SCRATCH_DST);
            spillStore(la.dst(), SCRATCH_DST);
            return;
        }
        if (instr instanceof IRStoreArrayElem sa) {
            spillLoad(sa.indexTemp(), SCRATCH_LEFT);
            spillLoad(sa.srcTemp(), SCRATCH_RIGHT);
            emitLine("multI r" + SCRATCH_LEFT + ", 8 => r" + SCRATCH_LEFT);
            emitLine("addI r" + SCRATCH_LEFT + ", " + sa.baseOffset() + " => r" + SCRATCH_LEFT);
            emitLine("storeAO r" + SCRATCH_RIGHT + " => r0, r" + SCRATCH_LEFT);
            return;
        }
        if (instr instanceof IRBinary b) {
            spillLoad(b.left(), SCRATCH_LEFT);
            spillLoad(b.right(), SCRATCH_RIGHT);
            emitBinary(b.op(), SCRATCH_LEFT, SCRATCH_RIGHT, SCRATCH_DST);
            spillStore(b.dst(), SCRATCH_DST);
            return;
        }
        if (instr instanceof IRUnary u) {
            spillLoad(u.src(), SCRATCH_LEFT);
            emitUnary(u.op(), SCRATCH_LEFT, SCRATCH_DST);
            spillStore(u.dst(), SCRATCH_DST);
            return;
        }
        if (instr instanceof IRJump j) {
            emitLine("jumpI => " + j.label());
            return;
        }
        if (instr instanceof IRCbr c) {
            spillLoad(c.condTemp(), SCRATCH_LEFT);
            emitLine("cbr r" + SCRATCH_LEFT + " => " + c.trueLabel() + ", " + c.falseLabel());
            return;
        }
        if (instr instanceof IRCall c) {
            IRFunctionSignature callee = signatures.get(c.functionName());
            if (callee == null) {
                emitLine("loadI 0 => r" + SCRATCH_DST);
                spillStore(c.dstTemp(), SCRATCH_DST);
                return;
            }
            int n = Math.min(c.argTemps().size(), callee.paramSlots.size());
            for (int i = 0; i < n; i++) {
                spillLoad(c.argTemps().get(i), SCRATCH_LEFT);
                emitLine("storeAI r" + SCRATCH_LEFT + " => r0, " + callee.paramSlots.get(i));
            }
            emitLine("jsr " + callee.label);
            emitLine("loadAI r0, " + callee.returnSlot + " => r" + SCRATCH_DST);
            spillStore(c.dstTemp(), SCRATCH_DST);
            return;
        }
        if (instr instanceof IRReturn r) {
            spillLoad(r.valueTemp(), SCRATCH_LEFT);
            emitLine("storeAI r" + SCRATCH_LEFT + " => r0, " + fn.returnSlot);
            if ("main".equals(fn.name)) {
                if (fn.traceSlotOffset != null) {
                    emitLine("loadAI r0, " + fn.traceSlotOffset + " => r" + SCRATCH_DST);
                    emitLine("p_int r" + SCRATCH_DST);
                }
                emitLine("halt");
            } else {
                int raSlot = fn.savedRaSlot == null ? 0 : fn.savedRaSlot;
                emitLine("loadAI r0, " + raSlot + " => r" + SCRATCH_AUX);
                emitLine("ret r" + SCRATCH_AUX);
            }
            return;
        }
        if (instr instanceof IRSaveRa s) {
            emitLine("storeAI r_ra => r0, " + s.slotOffset());
            return;
        }
        if (instr instanceof IRHalt) {
            emitLine("halt");
        }
    }

    private void emitBinary(String op, int leftReg, int rightReg, int dstReg) {
        switch (op) {
            case "+" -> emitLine("add r" + leftReg + ", r" + rightReg + " => r" + dstReg);
            case "-" -> emitLine("sub r" + leftReg + ", r" + rightReg + " => r" + dstReg);
            case "*" -> emitLine("mult r" + leftReg + ", r" + rightReg + " => r" + dstReg);
            case "/" -> emitLine("div r" + leftReg + ", r" + rightReg + " => r" + dstReg);
            case "%" -> {
                emitLine("div r" + leftReg + ", r" + rightReg + " => r" + dstReg);
                emitLine("mult r" + dstReg + ", r" + rightReg + " => r" + dstReg);
                emitLine("sub r" + leftReg + ", r" + dstReg + " => r" + dstReg);
            }
            case "<" -> emitLine("cmp_LT r" + leftReg + ", r" + rightReg + " => r" + dstReg);
            case "<=" -> emitLine("cmp_LE r" + leftReg + ", r" + rightReg + " => r" + dstReg);
            case ">" -> emitLine("cmp_GT r" + leftReg + ", r" + rightReg + " => r" + dstReg);
            case ">=" -> emitLine("cmp_GE r" + leftReg + ", r" + rightReg + " => r" + dstReg);
            case "==" -> emitLine("cmp_EQ r" + leftReg + ", r" + rightReg + " => r" + dstReg);
            case "!=" -> emitLine("cmp_NE r" + leftReg + ", r" + rightReg + " => r" + dstReg);
            case "&&" -> emitLine("and r" + leftReg + ", r" + rightReg + " => r" + dstReg);
            case "||" -> emitLine("or r" + leftReg + ", r" + rightReg + " => r" + dstReg);
            default -> emitLine("add r" + leftReg + ", r" + rightReg + " => r" + dstReg);
        }
    }

    private void emitUnary(String op, int srcReg, int dstReg) {
        switch (op) {
            case "neg" -> emitLine("rsubI r" + srcReg + ", 0 => r" + dstReg);
            case "not" -> {
                emitLine("loadI 0 => r" + dstReg);
                emitLine("cmp_EQ r" + srcReg + ", r" + dstReg + " => r" + dstReg);
            }
            case "id" -> emitLine("i2i r" + srcReg + " => r" + dstReg);
            default -> emitLine("i2i r" + srcReg + " => r" + dstReg);
        }
    }

    private void spillLoad(int temp, int dstReg) {
        int off = spillOffset(temp);
        emitLine("loadAI r1, " + off + " => r" + dstReg);
    }

    private void spillStore(int temp, int srcReg) {
        int off = spillOffset(temp);
        emitLine("storeAI r" + srcReg + " => r1, " + off);
    }

    private int spillOffset(int temp) {
        return spillOffsetByTemp.computeIfAbsent(temp, t -> t * 8);
    }

    private void emitLine(String line) {
        out.add(line);
    }
}
