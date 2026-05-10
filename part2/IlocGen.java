import java.util.List;

final class IlocGen implements Emitter {
    @Override
    public void emit(String line) {
        // This backend currently accumulates output in pass B.
    }

    List<String> generate(McProgram program) {
        APassASTtoIR passA = new APassASTtoIR();
        IRLoweringResult lowered = passA.lower(program);

        BPassRegisterAllocator passB = new BPassRegisterAllocator(lowered.signatures);
        return passB.emit(lowered.program);
    }
}
