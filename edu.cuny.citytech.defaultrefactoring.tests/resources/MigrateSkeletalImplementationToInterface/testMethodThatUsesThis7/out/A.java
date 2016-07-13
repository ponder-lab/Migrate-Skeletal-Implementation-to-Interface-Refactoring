package p;

interface I {
	default void m() {
		I i = this;
	}
}