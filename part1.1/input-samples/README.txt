Input samples (four files: miniCTest1.c … miniCTest4.c)

miniCTest1.c — valid, language coverage
  Single-line // and multi-line /* */ comments each contain fake declarations to show
  comments are skipped. One helper int g(int,int) plus int main(): types int/bool/char,
  arrays and brace initializers, scalar initializer, parameters, if/else, while, empty
  statement, assignment and all expression classes (including unary -/!, binary ops,
  calls, array access, parentheses, int/char/bool literals). Expect: exit 0, AST then
  “Parse and semantic checks passed.”

miniCTest2.c — valid, nesting
  Program uses nested { } blocks (shadowing), a small helper function,
  and nested calls id(id(b)). Expect: exit 0.

miniCTest3.c — lexer/parser failure
  Missing ‘;’ before ‘}’. Expect: exit 1; stderr includes “[parse]” (ANTLR may also
  print a line about the syntax error).

miniCTest4.c — semantic failure only
  Parses; bool assigned an int, and return uses an undeclared name. Expect: exit 2;
  “[semantic]” lines with line/column.


Usage #1 — TestRig (lexer/parser only; no AST or semantics)
  From part1.1:

  $ antlr4 -no-listener -visitor miniCLexer.g4 miniCParser.g4
  $ CP=$(grep '^CLASSPATH=' "$(which antlr4)" | cut -d= -f2-):.
  $ javac -cp "$CP" miniCLexer.java miniCParser.java miniCParserBaseVisitor.java miniCParserVisitor.java
  $ java -cp "$CP:." org.antlr.v4.gui.TestRig miniC program -tree < input-samples/miniCTest1.c


Usage #2 — Full driver (parse, AST, semantic analysis)
  From part1.1:

  $ antlr4 -no-listener -visitor miniCLexer.g4 miniCParser.g4
  $ CP=$(grep '^CLASSPATH=' "$(which antlr4)" | cut -d= -f2-):.
  $ javac -cp "$CP" miniCLexer.java miniCParser.java miniCParserBaseVisitor.java miniCParserVisitor.java SourceSpan.java miniCAstBuilder.java SemType.java VarSymbol.java FuncSymbol.java Scope.java SemanticDiagnostics.java SemanticAnalyzer.java Main.java
  $ java -cp "$CP:." Main input-samples/miniCTest1.c

Exit status for Main: 0 = OK, 1 = parse error, 2 = semantic error.
