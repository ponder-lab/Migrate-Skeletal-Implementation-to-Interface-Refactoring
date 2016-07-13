package p;

interface I {
	default void m() {
		System.out.println(6);
	}
}

abstract class A implements I {
	@Override
	public void m() {
		System.out.println(5);
	}
}

class Main {
	public static void main(String[] args) {
		new A() {}.m(); //prints 5.
		new I() {}.m(); //prints 6.
		new I() {}.m(); //prints 6.
	}
}