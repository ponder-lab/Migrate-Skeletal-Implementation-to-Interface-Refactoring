package p;

interface I {
	void m();
}

abstract class A extends B implements I {
	public void m() {
	}
}

class B {
}