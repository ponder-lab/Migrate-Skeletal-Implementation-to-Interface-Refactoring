package p;

interface I {
	default <E extends B> void m() {
		E e;
	}
}

class B {
}
