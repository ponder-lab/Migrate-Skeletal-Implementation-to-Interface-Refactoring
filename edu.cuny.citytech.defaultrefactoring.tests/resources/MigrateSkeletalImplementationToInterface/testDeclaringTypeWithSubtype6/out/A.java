package p;

interface I {
	default void m() {
	}
}

interface J extends I {
	void m();
}

abstract class C implements J, I {
	@Override
	public void m() {
		super.m();
	}
}

class B extends C {
}