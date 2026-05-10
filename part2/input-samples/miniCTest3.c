// Example 3: lexical error — '@' is not a Mini-C token.
// Putting '@' alone (outside any recoverable grammar context) yields a lexer error;
// EOF then fails the grammar (program needs at least one function), so the driver exits 1.
@
