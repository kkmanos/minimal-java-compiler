
class Main {
	public static void main(String[] args) {
		A beta;


		beta = new B();
		System.out.println(beta.foo());
		System.out.println(beta.printAll());
		if (1 < 2) {
			System.out.println(500);
		}
		else {
			System.out.println(600);
		}
	}
}

class A {
		int i;
		boolean flag;
		int j;
		public int foo() {
			i = 200;
			flag = false;
			j = 300;
			return 1;
		}

}

class B extends A {
		A type;
		int k;

		public boolean bla() { return true; }
		public int printAll() {
			System.out.println(i);
			System.out.println(j);
			return 1;
		}
  }