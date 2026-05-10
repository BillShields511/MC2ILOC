// Example 1: full program scope of Mini-C (within project grammar)
// Single-line comment example
/*
  Multi-line comment example
*/

int id(int x) {
  return x;
}

int helper(int a, int b) {
  // bool declaration and assignment
  bool flag;
  (flag = true);

  // char declaration and assignment
  char marker;
  (marker = 'A');

  // array declaration with brace initializer
  int table[2] = {1, 2};

  // int declaration with initializer
  int total = 0;

  // logical and relational operators
  if ((a < b) && flag && ((a != b) || (a == 0))) {
    // while loop with arithmetic and array access
    while (a < b) {
      (a = a + 1);
      (table[0] = (table[1] * 2) / 2 % 2);
      (total = total + table[0]);
      ;
    }
  } else {
    (marker = 'B');
    if (!flag) {
      (total = total + 1);
    }
  }

  // unary minus and additive expression
  return a + (-b) + total + marker;
}

int main() {
  // trace variable for optional p_int instrumentation
  int __mc_trace;

  // declarations with int, bool, and char
  int x = 0;
  bool y;
  char z;

  // function call and literals
  (x = helper(0, 2));
  (y = false);
  (z = 'a');

  // logical OR and equality in condition
  if (y || (z == '\0')) {
    (x = x + 1);
  } else {
    (x = x - 0);
  }

  // merged miniCTest2-style nested scopes and nested calls
  {
    int a;
    (a = id(2));
    {
      int b;
      (b = id(a));
      (__mc_trace = id(id(b)));
    }
  }

  return x;
}
