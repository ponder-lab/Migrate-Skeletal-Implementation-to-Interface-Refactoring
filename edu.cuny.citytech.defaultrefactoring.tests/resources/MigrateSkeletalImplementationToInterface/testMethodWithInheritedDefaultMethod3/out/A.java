package p;

interface I {
}

interface K extends I {
	default void m() {
	}
}

interface J {
	default void m() {
	}
}