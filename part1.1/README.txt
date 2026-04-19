README for mini-c frontend (part1.1)
Bill Shields - jxh608

Core files:
miniCLexer.g4
miniCParser.g4
Main.java
miniCAstBuilder.java
SemType.java
VarSymbol.java
FuncSymbol.java
Scope.java
SemanticDiagnostics.java
SemanticAnalyzer.java
SourceSpan.java

Additional files:
README.txt
checklist.txt
input-samples/


~Prerequisites~
- Java JDK (javac, java)
- ANTLR 4 and an antlr4 helper script that sets CLASSPATH (same style as part1)


~Build and run~
Run commands from within part1.1 directory (~/project/part1.1).

1. $ antlr4 -no-listener -visitor miniCLexer.g4 miniCParser.g4
2. $ CP=$(grep '^CLASSPATH=' "$(which antlr4)" | cut -d= -f2-):.
3. $ javac -cp "$CP" miniCLexer.java miniCParser.java miniCParserBaseVisitor.java miniCParserVisitor.java SourceSpan.java miniCAstBuilder.java SemType.java VarSymbol.java FuncSymbol.java Scope.java SemanticDiagnostics.java SemanticAnalyzer.java Main.java

4.
Option 1: Run with source file:
  java -cp "$CP:." Main input-samples/miniCTest1.c

Option 2: Read input from stdin:
  java -cp "$CP:." Main < input-samples/miniCTest1.c

Exit status:
0 = parse + semantic OK
1 = parse error
2 = semantic error

Output:
- On success: AST on stdout, then the line "Parse and semantic checks passed."
- On parse failure: one line to stderr beginning with "[parse] ".
- On semantic failure: "--- Semantic errors ---" on stderr, then one line per error
  beginning with "[semantic] ", each of the form "line L, column C: <message>"
  (1-based line and column in the source file).


~Cleanup~
Remove ANTLR-generated files and compiled bytecode.
From within part1.1/

$ rm -f *.interp *.tokens miniCLexer.java miniCParser.java miniCParserBaseVisitor.java miniCParserVisitor.java *.class


Notes:
- Do not delete the .g4 files or hand-written .java sources (including semantic analysis .java files).


~Language notes~
- Part 1.1 targets lexer, parser, AST, and semantic analysis (no ILOC / codegen here).
- #include and other preprocessor directives are intentionally invalid syntax for this Mini-C grammar.
