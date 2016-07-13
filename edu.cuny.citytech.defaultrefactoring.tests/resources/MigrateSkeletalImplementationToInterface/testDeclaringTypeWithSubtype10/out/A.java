package p;

interface I {
	default void m() {
	}
}

interface J {
	default void m() {
	}
}

class B implements I, J {
	@Override
	public void m() {
	}
}

class C extends B {
}