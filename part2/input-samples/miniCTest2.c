// Example 2: simple math helpers to validate end-to-end behavior
//expected output: 13

int add(int a, int b) {
  return a + b;
}

int scale(int n) {
  return n * 2 - 1;
}

int main() {
  int __mc_trace;
  int x;
  int y;

  (x = add(3, 4));
  (y = scale(x));
  (__mc_trace = y);

  return y;
}
