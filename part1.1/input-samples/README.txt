README.md for expected testcase input/output and usage instructions - See Usage #1 and Usage #2 below for exact usage!

Four Input Samples. 
- miniCTest1.c and miniCTest2.c are valid c programs, and should compile with an exit code of 0
- miniCTest3.c and miniCTest4.c are invalid c programs, and should compile with an exit code of 1 and 2 respectively

miniCTest1.c — valid, demonstrates complete mini-c language coverage
    Includes:
    Single-line comments //
    Multi-line comments /*
    Helper Function: int g(int,int)
    Main Function: int main()
    Variable and Arrays of types: int/bool/char
    Logical blocks: if/else, while
    Statement, assignment, expressions
        unary -/!
        binary ops,
        array access
        parentheses
        int/char/bool literals
    
    Expected output: exit 0, AST output, then “Parse and semantic checks passed.”


miniCTest2.c — valid, demonstrates scoping/nested function calls
    Program uses nested { } blocks, a small helper function, and nested calls id(id(b))
    
    Expected Output: exit 0, AST output, then "Parse and semantic checks passed."


miniCTest3.c — invalid, lexer/parser failure
    Demonstrates lexical analysis
    Missing ‘;’ before ‘}’
    
    Expected Output: exit 1


miniCTest4.c — invalid, semantic failure only
    Demonstrates semantic analysis/traversal of parse tree
    Errors:
        bool assigned an int value
        return uses an undeclared name.
    
    Expected Output: exit 2

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Usage:

Usage #1 — antlr TestRig (tests lexer/parser only; no AST or semantics)
Run the following four commands from within MC2ILOC/part1.1
    $ antlr4 -no-listener -visitor miniCLexer.g4 miniCParser.g4
    $ CP=$(grep '^CLASSPATH=' "$(which antlr4)" | cut -d= -f2-):.
    $ javac -cp "$CP" miniCLexer.java miniCParser.java miniCParserBaseVisitor.java miniCParserVisitor.java
    
    Either: (for command line -tree output)
    $ java -cp "$CP:." org.antlr.v4.gui.TestRig miniC program -tree < input-samples/miniCTest1.c
    
    OR: (for -gui output)
    $ java -cp "$CP:." org.antlr.v4.gui.TestRig miniC program -gui < input-samples/miniCTest1.c


Usage #2 — Full driver (parse, AST, semantic analysis)
Run the following four commands from within MC2ILOC/part1.1
  $ antlr4 -no-listener -visitor miniCLexer.g4 miniCParser.g4
  $ CP=$(grep '^CLASSPATH=' "$(which antlr4)" | cut -d= -f2-):.
  $ javac -cp "$CP" miniCLexer.java miniCParser.java miniCParserBaseVisitor.java miniCParserVisitor.java SourceSpan.java miniCAstBuilder.java SemType.java VarSymbol.java FuncSymbol.java Scope.java SemanticDiagnostics.java SemanticAnalyzer.java Main.java
  $ java -cp "$CP:." Main input-samples/miniCTest1.c

Exit status:
- 0 = OK
- 1 = parse error
- 2 = semantic error.


~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Cleanup (remove all antlr/java generated files):

$ rm -f *.interp *.tokens miniCLexer.java miniCParser.java miniCParserBaseVisitor.java miniCParserVisitor.java *.class
