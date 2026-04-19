README for mini-c to ILOC frontend (part1.1)
Bill Shields - jxh608

This file explains the structure and scope of the project

FOR INSTRUCTIONS ON HOW TO BUILD AND RUN THE SYSTEM, SEE:
part1.1/input-samples/README.txt

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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
input-samples/
expected-output/

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

~Prerequisites~
- Java JDK (javac, java) - same as the one installed in class
- ANTLR 4 and an antlr4 helper script that sets CLASSPATH - same as installed in class

~General Notes~:
- Do not delete the .g4 files or hand-written .java sources (including semantic analysis .java files).

~Language notes~
- Part 1.1 targets lexer, parser, AST, and semantic analysis (no ILOC / codegen yet).
- #include and other preprocessor directives are intentionally invalid syntax in Mini-C grammar

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
