README for Mini-C to ILOC
Bill Shields - jxh608

This file summarizes the structure and scope of the project. 
For instructions on exactly how to build and run the project, see:
    part2/input-samples/README.txt

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Overview

Mini-C is a small C-like source language described by miniCLexer.g4 / miniCParser.g4. MC2ILOC
implements a compiler front end (parse tree → AST → semantic analysis) and back end that converts files
written in Mini-C into an AST, then into textual ILOC (Intermediate Language for Optimizing Compilers).

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Core files

Grammar / generated parser (via antlr4):
  miniCLexer.g4
  miniCParser.g4

Driver and AST construction:
  Main.java
  miniCAstBuilder.java
  SourceSpan.java

Semantic analysis:
  SemType.java
  VarSymbol.java
  FuncSymbol.java
  Scope.java
  SemanticDiagnostics.java
  SemanticAnalyzer.java

ILOC generation:
  IRDataStructures.java
  Emitter.java
  APassASTtoIR.java              (AST → intermediate form)
  BPassRegisterAllocator.java    (allocation pass)
  IlocGen.java                   (orchestrates passes; produces output lines)

Other layout under part2/
  README.txt                     (this file)
  input-samples/                  Mini-C example files and build/run instructions
    README.txt
    miniCTest*.c
  expected-output/                Expected ILOC output for the two working example files

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Prerequisites
  - Java JDK (javac, java), as used in course/lab setups
  - ANTLR 4 and an antlr4 launcher that configures CLASSPATH (same as installed in class)

