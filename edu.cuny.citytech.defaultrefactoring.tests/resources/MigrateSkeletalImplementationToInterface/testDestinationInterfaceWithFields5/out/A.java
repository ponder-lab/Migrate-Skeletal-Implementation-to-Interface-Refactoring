package p;

interface I {
	int f = 5;

	default void m() {
		int f = 0;
		System.out.println(f);
	}
}

class Main {
	public static void main(String[] args) {
		new I() {
		}.m(); // prints 0.
	}
}
