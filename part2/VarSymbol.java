/** Variable or array binding in a scope. */
final class VarSymbol {
    final String name;
    /** Element type for both scalars and arrays. */
    final SemType elementType;
    final boolean isArray;
    final Long arraySize;

    VarSymbol(String name, SemType elementType, boolean isArray, Long arraySize) {
        this.name = name;
        this.elementType = elementType;
        this.isArray = isArray;
        this.arraySize = arraySize;
    }
}
