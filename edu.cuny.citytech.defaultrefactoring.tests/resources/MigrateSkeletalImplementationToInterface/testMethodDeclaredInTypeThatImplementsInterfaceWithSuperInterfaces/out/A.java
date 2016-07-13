package p;

interface I {
}

interface J extends I {
	default void m() {
	}
}
