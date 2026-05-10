This file explains how to build and run the system as well as the purpose of each of the test cases
See Usage #1 and Usage #2 below for exact usage!

There are five input samples included.
- miniCTest1.c and miniCTest2.c are valid Mini-C programs and should exit with code 0.
- miniCTest3.c and miniCTest4.c are invalid: the driver exits with code 1 (parse/lexical errors) before codegen; errors are written to stderr.
- miniCTest5.c is invalid semantically: the driver parses OK but rejects the program during analysis (exit 2); diagnostics are written to stderr.

miniCTest1.c — Example 1: full program scope of Mini-C
    Includes:
    Single-line comments: //
    Multi-line comments: /*
    Helper Functions: int id(int), int helper(int,int)
    Main Function: int main()
    Variable and Arrays of types: int/bool/char
    Logical blocks: if/else, while
    Statement, assignment, expressions
        unary -/!
        binary ops,
        array access
        parentheses
        int/char/bool literals
    Nested scopes and nested calls id(id(b))
    int __mc_trace in main’s outer block for optional p_int tracing

    Expected output: exit 0, ILOC on stdout.


miniCTest2.c — Example 2: simple math functions to validate functionality
    Includes:
        int add(int,int) and int scale(int) (addition, multiply, subtract literal)
        chaining results in main and int __mc_trace for optional p_int tracing

    Expected output: exit 0, ILOC on stdout
        (Nickle prints 13 and a newline from __mc_trace; return value is 13).


miniCTest3.c — Example 3: lexical error
    The lone character '@' does not match any lexer rule (token recognition error on stderr).

    Important: placing an invalid token *inside* a well-formed body (for example inside main's `{ }`)
    can still recover to a syntactically valid `program`; this sample avoids that — there is only the
    bad token plus comments, so the grammar cannot match `program` and you also see a parse error.

    Expected output: exit 1 (no ILOC). Lexer error plus ensuing parse failure; the driver treats this as fatal.


miniCTest4.c — Example 4: syntactical error
    Missing ';' after `return 0` so the token stream is valid but the parser cannot build `program`.

    Expected output: exit 1 (no ILOC). Parser reports a syntax error; the driver prints a parse failure message.


miniCTest5.c — Example 5: semantic error
    `bool b = 'a';` initializes a `bool` with a character literal (`char`), which violates Mini-C typing rules.

    Expected output: exit 2 (no ILOC). Semantic analyzer reports an initializer type mismatch; errors are grouped under "--- Semantic errors ---".

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Optional trace output for Nickle ./a.out or ILOC pipelines:
Declare a scalar integer named __mc_trace directly in main’s outermost block,
assign whatever int you want echoed, then set your real return expression as usual.
Immediately before halt, the emitter issues loadAI (from __mc_trace’s slot) and p_int
so stdout shows that value (Nickle prints a newline after the integer). Omitting
the variable skips this extra instrumentation.

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Usage:
There are two main ways to build and run the system:

The first way (Usage #1) uses the antlr4 testrig, by including either the -gui or -tree flag,
antlr4 will provide a representation of the tree.
The second way (Usage #2) uses the MC2ILOC java files to traverse the parse tree, perform scope analysis,
detect semantic errors, and on success lower the AST to ILOC (suitable for feeding into Nickle).

Usage #1 — antlr TestRig (tests lexer/parser only; no AST or semantics)
Run the following four commands from within MC2ILOC/part2/
    $ antlr4 -no-listener -visitor miniCLexer.g4 miniCParser.g4
    $ CP=$(grep '^CLASSPATH=' "$(which antlr4)" | cut -d= -f2-):.
    $ javac -cp "$CP" miniCLexer.java miniCParser.java miniCParserBaseVisitor.java miniCParserVisitor.java

    Either: (for command line -tree output)
    $ java -cp "$CP:." org.antlr.v4.gui.TestRig miniC program -tree < input-samples/miniCTest1.c

    OR: (for -gui output)
    $ java -cp "$CP:." org.antlr.v4.gui.TestRig miniC program -gui < input-samples/miniCTest1.c


Usage #2 — Full driver (parse, semantic analysis, ILOC generation)
Run the following four commands from within MC2ILOC/part2/
  $ antlr4 -no-listener -visitor miniCLexer.g4 miniCParser.g4
  $ CP=$(grep '^CLASSPATH=' "$(which antlr4)" | cut -d= -f2-):.
  $ javac -cp "$CP" miniCLexer.java miniCParser.java miniCParserBaseVisitor.java miniCParserVisitor.java SourceSpan.java miniCAstBuilder.java SemType.java VarSymbol.java FuncSymbol.java Scope.java SemanticDiagnostics.java SemanticAnalyzer.java Emitter.java IRDataStructures.java APassASTtoIR.java BPassRegisterAllocator.java IlocGen.java Main.java
  $ java -cp "$CP:." Main input-samples/miniCTest1.c

Exit status:
- 0 = OK
- 1 = parse error
- 2 = semantic error.

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Piping ILOC outputs to nickle:

To pipe the ILOC programs to nickle, first follow Usage #2 above, using any mini-c input file. The exact commands are written again below for clarity.
  $ antlr4 -no-listener -visitor miniCLexer.g4 miniCParser.g4
  $ CP=$(grep '^CLASSPATH=' "$(which antlr4)" | cut -d= -f2-):.
  $ javac -cp "$CP" miniCLexer.java miniCParser.java miniCParserBaseVisitor.java miniCParserVisitor.java SourceSpan.java miniCAstBuilder.java SemType.java VarSymbol.java FuncSymbol.java Scope.java SemanticDiagnostics.java SemanticAnalyzer.java Emitter.java IRDataStructures.java APassASTtoIR.java BPassRegisterAllocator.java IlocGen.java Main.java

Then pipe the output of Main.java into an iloc file:
  $ java -cp "$CP:." Main input-samples/miniCTest1.c > a.iloc

Then use nickle to convert the iloc file into a c file:
  $ java -jar ../nickle/nickle/nickle-0.2.jar a.iloc > a.c

Finally, use gcc to convet the c file into an out file:
  $ gcc -I ../nickle/nickle/ a.c ../nickle/nickle/*.c

The out file can be run normally:
  $ ./a.out

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Cleanup (remove all antlr/java generated files):

$ rm -f *.interp *.tokens miniCLexer.java miniCParser.java miniCParserBaseVisitor.java miniCParserVisitor.java *.class
