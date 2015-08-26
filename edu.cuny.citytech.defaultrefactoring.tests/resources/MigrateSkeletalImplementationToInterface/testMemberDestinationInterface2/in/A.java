package p;

class B {
	static interface I {
		void m();
	}
}

abstract class A implements B.I {
	public void m() {
	}
}
