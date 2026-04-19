import java.util.List;

/** Function signature (Mini-C: int name(int, int, ...)). */
final class FuncSymbol {
    final String name;
    final SemType returnType;
    final List<SemType> paramTypes;

    FuncSymbol(String name, SemType returnType, List<SemType> paramTypes) {
        this.name = name;
        this.returnType = returnType;
        this.paramTypes = paramTypes;
    }
}
