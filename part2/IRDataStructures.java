import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class IRProgram {
    final List<IRFunction> functions;
    final String entryLabel;

    IRProgram(List<IRFunction> functions, String entryLabel) {
        this.functions = functions;
        this.entryLabel = entryLabel;
    }
}

final class IRFunction {
    final String name;
    final String label;
    final List<IRInstr> instructions;
    final List<Integer> paramSlots;
    final int returnSlot;
    final Integer savedRaSlot;
    final int localCount;
    /**
     * If non-null ({@code main} only): r0 slot (bytes) of {@code int __mc_trace}
     * declared directly in {@code main}'s body; emits {@code loadAI}+{@code p_int} before halt.
     */
    final Integer traceSlotOffset;

    IRFunction(
            String name,
            String label,
            List<IRInstr> instructions,
            List<Integer> paramSlots,
            int returnSlot,
            Integer savedRaSlot,
            int localCount,
            Integer traceSlotOffset
    ) {
        this.name = name;
        this.label = label;
        this.instructions = instructions;
        this.paramSlots = paramSlots;
        this.returnSlot = returnSlot;
        this.savedRaSlot = savedRaSlot;
        this.localCount = localCount;
        this.traceSlotOffset = traceSlotOffset;
    }
}

interface IRInstr {
}

record IRLabel(String name) implements IRInstr {
}

record IRLoadImm(int dst, long value) implements IRInstr {
}

record IRLoadSlot(int dst, int slotOffset) implements IRInstr {
}

record IRStoreSlot(int slotOffset, int src) implements IRInstr {
}

record IRLoadArrayElem(int dst, int baseOffset, int indexTemp) implements IRInstr {
}

record IRStoreArrayElem(int baseOffset, int indexTemp, int srcTemp) implements IRInstr {
}

record IRBinary(String op, int left, int right, int dst) implements IRInstr {
}

record IRUnary(String op, int src, int dst) implements IRInstr {
}

record IRJump(String label) implements IRInstr {
}

record IRCbr(int condTemp, String trueLabel, String falseLabel) implements IRInstr {
}

record IRCall(String functionName, List<Integer> argTemps, int dstTemp) implements IRInstr {
}

record IRReturn(int valueTemp) implements IRInstr {
}

record IRSaveRa(int slotOffset) implements IRInstr {
}

record IRHalt() implements IRInstr {
}

final class IRFunctionSignature {
    final String name;
    final String label;
    final List<Integer> paramSlots = new ArrayList<>();
    int returnSlot;
    Integer savedRaSlot;

    IRFunctionSignature(String name, String label) {
        this.name = name;
        this.label = label;
    }
}

final class IRLoweringResult {
    final IRProgram program;
    final Map<String, IRFunctionSignature> signatures;

    IRLoweringResult(IRProgram program, Map<String, IRFunctionSignature> signatures) {
        this.program = program;
        this.signatures = signatures;
    }
}

final class VRegPool {
    private int nextId = 0;

    int next() {
        return nextId++;
    }

    int count() {
        return nextId;
    }
}

final class TempList {
    private final List<Integer> values = new ArrayList<>();

    void add(int v) {
        values.add(v);
    }

    List<Integer> immutable() {
        return Collections.unmodifiableList(values);
    }
}
