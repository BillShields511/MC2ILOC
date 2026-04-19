parser grammar miniCParser;

options { tokenVocab=miniCLexer; }

program
    : functionDefinition+ EOF
    ;

functionDefinition
    : mainFunction
    | intFunction
    ;

mainFunction
    : INT MAIN LPAREN RPAREN block
    ;

intFunction
    : INT ID LPAREN parameterList? RPAREN block
    ;

parameterList
    : parameter (COMMA parameter)*
    ;

parameter
    : INT ID
    ;

block
    : LBRACE blockItem* RBRACE
    ;

blockItem
    : declaration
    | statement
    ;

declaration
    : typeSpecifier declarator (ASSIGN initializer)? SEMI
    ;

typeSpecifier
    : INT
    | BOOL
    | CHAR
    ;

declarator
    : ID
    | ID LBRACK INT_LITERAL RBRACK
    ;

initializer
    : expression
    | LBRACE expressionList? RBRACE
    ;

expressionList
    : expression (COMMA expression)*
    ;

statement
    : block
    | ifStatement
    | whileStatement
    | returnStatement
    | expressionStatement
    ;

ifStatement
    : IF LPAREN expression RPAREN statement (ELSE statement)?
    ;

whileStatement
    : WHILE LPAREN expression RPAREN statement
    ;

returnStatement
    : RETURN expression SEMI
    ;

expressionStatement
    : expression? SEMI
    ;

expression
    : assignmentExpression
    ;

assignmentExpression
    : logicalOrExpression
    | lvalue ASSIGN assignmentExpression
    ;

lvalue
    : ID
    | ID LBRACK expression RBRACK
    ;

logicalOrExpression
    : logicalAndExpression (OROR logicalAndExpression)*
    ;

logicalAndExpression
    : equalityExpression (ANDAND equalityExpression)*
    ;

equalityExpression
    : relationalExpression ((EQEQ | NEQ) relationalExpression)*
    ;

relationalExpression
    : additiveExpression ((LT | LE | GT | GE) additiveExpression)*
    ;

additiveExpression
    : multiplicativeExpression ((PLUS | MINUS) multiplicativeExpression)*
    ;

multiplicativeExpression
    : unaryExpression ((STAR | SLASH | PERCENT) unaryExpression)*
    ;

unaryExpression
    : MINUS unaryExpression
    | NOT unaryExpression
    | primaryExpression
    ;

primaryExpression
    : INT_LITERAL
    | CHAR_LITERAL
    | TRUE
    | FALSE
    | functionCall
    | arrayAccess
    | ID
    | LPAREN expression RPAREN
    ;

functionCall
    : ID LPAREN argumentList? RPAREN
    ;

argumentList
    : expression (COMMA expression)*
    ;

arrayAccess
    : ID LBRACK expression RBRACK
    ;
