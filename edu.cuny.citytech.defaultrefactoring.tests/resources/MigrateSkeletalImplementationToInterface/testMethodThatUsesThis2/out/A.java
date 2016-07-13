package p;

interface I {
	default void m() {
		this.getClass();
	}
}
