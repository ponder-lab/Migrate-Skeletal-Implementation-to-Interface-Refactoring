package p;

interface I {
	default void m() {
		new B().f = 5;
	}
}

class B {
	int f;
}