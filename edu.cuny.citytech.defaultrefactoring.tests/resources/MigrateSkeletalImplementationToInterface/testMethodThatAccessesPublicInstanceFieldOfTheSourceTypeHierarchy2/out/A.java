package p;

interface I {
	default void m() {
		int g = new B().f;
	}
}

class B {
	int f;
}