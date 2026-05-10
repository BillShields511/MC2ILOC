lexer grammar miniCLexer;

// Keywords
INT: 'int';
BOOL: 'bool';
CHAR: 'char';
IF: 'if';
ELSE: 'else';
WHILE: 'while';
RETURN: 'return';
MAIN: 'main';
TRUE: 'true';
FALSE: 'false';

// Operators
PLUS: '+';
MINUS: '-';
STAR: '*';
SLASH: '/';
PERCENT: '%';
NOT: '!';
ANDAND: '&&';
OROR: '||';
EQEQ: '==';
NEQ: '!=';
LT: '<';
LE: '<=';
GT: '>';
GE: '>=';
ASSIGN: '=';

// Delimiters / punctuation
LPAREN: '(';
RPAREN: ')';
LBRACE: '{';
RBRACE: '}';
LBRACK: '[';
RBRACK: ']';
SEMI: ';';
COMMA: ',';
HASH: '#';

// Literals
INT_LITERAL: [0-9]+;

CHAR_LITERAL
    : '\'' ( EscapeSequence | ~['\\\r\n] ) '\''
    ;

fragment EscapeSequence
    : '\\' [btnfr"'\\]
    | '\\' [0-7] [0-7]? [0-7]?
    | '\\x' [0-9a-fA-F]+
    ;

// Identifiers
ID: [a-zA-Z_][a-zA-Z_0-9]*;

// Comments and whitespace
LINE_COMMENT: '//' ~[\r\n]* -> skip;
BLOCK_COMMENT: '/*' .*? '*/' -> skip;
WS: [ \t\r\n]+ -> skip;