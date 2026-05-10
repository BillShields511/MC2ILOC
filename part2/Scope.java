import java.util.HashMap;
import java.util.Map;

/** Lexical scopes for variables (functions hold parameters in the outer scope). */
final class Scope {
    private final Scope parent;
    private final Map<String, VarSymbol> vars = new HashMap<>();

    Scope(Scope parent) {
        this.parent = parent;
    }

    /** Declare in this scope only; caller checks duplicate. */
    void declare(VarSymbol sym) {
        vars.put(sym.name, sym);
    }

    boolean declaresLocally(String name) {
        return vars.containsKey(name);
    }

    VarSymbol lookup(String name) {
        Scope s = this;
        while (s != null) {
            VarSymbol v = s.vars.get(name);
            if (v != null) {
                return v;
            }
            s = s.parent;
        }
        return null;
    }
}
