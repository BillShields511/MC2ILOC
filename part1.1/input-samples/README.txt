This file explains how to build and run the system as well as the purpose of each of the test cases
See Usage #1 and Usage #2 below for exact usage!

There are five Input Samples included. 
- miniCTest1.c and miniCTest2.c are valid c programs, and should compile with an exit code of 0
- miniCTest3.c and miniCTest4.c are invalid c programs, and should return errors through stdout

- miniCTest5.c serves as an example so that I can include how the ILOC output will look
  when MC2ILOC is complete. In its current state, MC2ILOC does not generate iloc programs
  The included expected output file: miniCTest5-ILOC.txt is implimented by hand

miniCTest1.c — valid, demonstrates complete mini-c language coverage
    Includes:
    Single-line comments: //
    Multi-line comments: /*
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
    
    Expected output: exit without error, AST output, then “Parse and semantic checks passed.”


miniCTest2.c — valid, demonstrates scoping/nested function calls
    Includes:
        nested { } blocks
        small helper function
        nested calls id(id(b))
    
    Expected Output: exit without error, AST output, then "Parse and semantic checks passed."


miniCTest3.c — invalid, lexer/parser failure
    Demonstrates lexical analysis error catching
    Included Errors:
        Missing ‘;’ before ‘}’
    
    Expected Output: exit with error in lexical stage


miniCTest4.c — invalid, semantic failure only
    Demonstrates semantic analysis/traversal of parse tree
    Included Errors:
        bool assigned an int value
        return uses an undeclared name.
    
    Expected Output: exit with error in parsing stage

miniCTest5.c — valid, for handmade ILOC generation demo
    Includes:
        two int assignments
        int addition
        return zero

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Usage:
There are two main ways to build and run the system:

The first way (Usage #1) uses the antlr4 testrig, by including either the -gui or -tree flag,
antlr4 will provide a representation of the tree.
The second way (Usage #2) uses the MC2ILOC java files to traverse the parse tree, perform scope analysis,
and locate any errors in either the lexing, parsing, or semantic analysis stage.

Usage #1 — antlr TestRig (tests lexer/parser only; no AST or semantics)
Run the following four commands from within MC2ILOC/part1.1/
    $ antlr4 -no-listener -visitor miniCLexer.g4 miniCParser.g4
    $ CP=$(grep '^CLASSPATH=' "$(which antlr4)" | cut -d= -f2-):.
    $ javac -cp "$CP" miniCLexer.java miniCParser.java miniCParserBaseVisitor.java miniCParserVisitor.java
    
    Either: (for command line -tree output)
    $ java -cp "$CP:." org.antlr.v4.gui.TestRig miniC program -tree < input-samples/miniCTest1.c
    
    OR: (for -gui output)
    $ java -cp "$CP:." org.antlr.v4.gui.TestRig miniC program -gui < input-samples/miniCTest1.c


Usage #2 — Full driver (parse, AST, semantic analysis)
Run the following four commands from within MC2ILOC/part1.1/
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
