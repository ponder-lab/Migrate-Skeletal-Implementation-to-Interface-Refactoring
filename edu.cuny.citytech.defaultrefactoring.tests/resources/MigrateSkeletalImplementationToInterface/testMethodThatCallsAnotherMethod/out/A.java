package p;

interface I {
	default void m() {
		n();
	}

	void n();
}
