package p;

interface I {
	default void m() {
		System.out.println("Hello");
	}
}

interface J extends I {
	default void m() {
		System.out.println("Goodbye");
	}
}

abstract class C implements J, I {
	@Override
	public void m() {
		super.m();
	}
}

class B extends C {
}

class Main {
	public static void main(String[] args) {
		// Should print hello.
		new B().m();
	}
}