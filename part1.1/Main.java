import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.InputStreamReader;
import java.nio.file.Path;

/**
 * Mini-C front-end driver: lex, parse, AST, and semantic checks (no ILOC / codegen).
 * Usage:
 *   java -cp ... Main              (read source from stdin)
 *   java -cp ... Main path/to/file.c
 */
public class Main {
    public static void main(String[] args) throws Exception {
        CharStream input = args.length == 0
                ? CharStreams.fromReader(new InputStreamReader(System.in))
                : CharStreams.fromPath(Path.of(args[0]));

        miniCLexer lexer = new miniCLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        miniCParser parser = new miniCParser(tokens);

        miniCParser.ProgramContext tree = parser.program();

        int errors = parser.getNumberOfSyntaxErrors();
        if (errors > 0) {
            System.err.println("[parse] Parse failed with " + errors + " syntax error(s).");
            System.exit(1);
        }

        MiniCAstBuilder builder = new MiniCAstBuilder();
        McProgram ast = (McProgram) builder.visitProgram(tree);

        SemanticAnalyzer sem = new SemanticAnalyzer();
        SemanticDiagnostics semDiag = sem.analyze(ast);

        System.out.println("--- AST ---");
        System.out.println(ast);

        if (!semDiag.ok()) {
            System.err.println("--- Semantic errors ---");
            for (String msg : semDiag.errors()) {
                System.err.println("[semantic] " + msg);
            }
            System.exit(2);
        }

        System.out.println("Parse and semantic checks passed.");
    }
}
