int id(int x) {
  return x;
}
int main() {
  int a;
  a = 1;
  {
    int a;
    a = id(2);
    {
      int b;
      (b = id(a));
      return id(id(b));
    }
  }
}
