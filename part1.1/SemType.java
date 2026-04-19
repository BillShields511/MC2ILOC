/** Semantic (checked) types for Mini-C expressions and declarations. */
enum SemType {
    INT,
    BOOL,
    CHAR,
    /** Invalid expression; analysis continues to collect more errors. */
    ERROR
}
