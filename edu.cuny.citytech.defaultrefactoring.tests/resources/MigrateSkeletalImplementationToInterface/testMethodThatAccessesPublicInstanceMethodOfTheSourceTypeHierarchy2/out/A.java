package p;

interface I {
	default void m() {
		new B().n();
	}
}

class B {
	void n() {
	}
}