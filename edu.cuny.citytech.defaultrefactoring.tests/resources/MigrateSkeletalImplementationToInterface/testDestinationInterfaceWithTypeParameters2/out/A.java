package p;

interface I<T> {
	default void m() {
		T e = null;
	}
}
