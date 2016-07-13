package p;

interface I {
	default void m() {
	}
}

interface J extends I {
	void m();
}
