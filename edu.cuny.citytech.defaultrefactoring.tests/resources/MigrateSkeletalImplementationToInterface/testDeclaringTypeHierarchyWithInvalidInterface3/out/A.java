package p;

interface I extends J {
}

interface J {
	default void m() {
	}
}
