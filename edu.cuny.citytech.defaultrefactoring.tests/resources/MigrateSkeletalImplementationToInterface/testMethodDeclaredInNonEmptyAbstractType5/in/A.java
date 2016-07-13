package p;

interface I {
	void m();
}

abstract class A implements I {
	static {
	}

	@Override
	public void m() {
	}
}
