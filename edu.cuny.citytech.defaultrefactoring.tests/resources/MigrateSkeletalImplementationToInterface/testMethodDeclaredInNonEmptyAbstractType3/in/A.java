package p;

interface I {
	void m();
}

abstract class A implements I {
	class C {
	}

	@Override
	public void m() {
	}
}
