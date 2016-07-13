package p;

interface I {
	default void m() {
		System.out.println(5);
	}
}

class Main {
	public static void main(String[] args) {
		new I() {}.m(); //prints 5.
		new I() {}.m(); //prints 5.
	}
}