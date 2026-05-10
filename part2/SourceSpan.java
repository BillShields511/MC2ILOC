import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * 1-based line, 1-based column (human-friendly), from ANTLR tokens.
 */
final class SourceSpan {
    final int line;
    final int column;

    SourceSpan(int line, int column) {
        this.line = line;
        this.column = column;
    }

    static SourceSpan from(Token t) {
        if (t == null) {
            return null;
        }
        return new SourceSpan(t.getLine(), t.getCharPositionInLine() + 1);
    }

    static SourceSpan from(TerminalNode n) {
        return n == null ? null : from(n.getSymbol());
    }

    String prefix() {
        return "line " + line + ", column " + column + ": ";
    }
}
