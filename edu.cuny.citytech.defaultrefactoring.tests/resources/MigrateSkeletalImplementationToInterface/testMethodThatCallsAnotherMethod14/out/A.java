package p;

interface I {
	default void m() {
		B.n();
	}
}

class B {
	public static void n() {
	}
}