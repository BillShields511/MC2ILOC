import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.InputStreamReader;
import java.nio.file.Path;

/**
 * Mini-C compiler driver: lex, parse, AST, semantic checks, and ILOC generation.
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

        if (!semDiag.ok()) {
            System.err.println("--- Semantic errors ---");
            for (String msg : semDiag.errors()) {
                System.err.println("[semantic] " + msg);
            }
            System.exit(2);
        }

        IlocGen gen = new IlocGen();
        for (String line : gen.generate(ast)) {
            System.out.println(line);
        }
    }
}
