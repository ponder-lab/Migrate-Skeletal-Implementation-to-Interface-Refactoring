package p;

interface I<E extends B> {
	default void m() {
		E e;
	}
}

class B {
}
