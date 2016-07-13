package p;

interface I {
	default void m(I i) {
		m(this);
	}
}