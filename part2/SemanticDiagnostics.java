import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class SemanticDiagnostics {
    private final List<String> errors = new ArrayList<>();

    void error(String msg) {
        errors.add(msg);
    }

    void error(String msg, SourceSpan span) {
        if (span != null) {
            errors.add(span.prefix() + msg);
        } else {
            errors.add(msg);
        }
    }

    List<String> errors() {
        return Collections.unmodifiableList(errors);
    }

    boolean ok() {
        return errors.isEmpty();
    }
}
