package p;

interface I {
	default void m() {
		n();
	}

	void n();
}

abstract class B implements I {
	public void n() {
		System.out.println("B.n()");
	}
}