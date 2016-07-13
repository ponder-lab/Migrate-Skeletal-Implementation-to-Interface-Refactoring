package p;

interface I<T> {
	default void m() {
		T t = null;
	}
}
