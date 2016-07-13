package p;

interface I {
	default void m() {
		System.out.println("Hello");
	}
}

class Main {
	public static void main(String[] args) {
		new I() {}.m(); //should print Hello.
	}
}