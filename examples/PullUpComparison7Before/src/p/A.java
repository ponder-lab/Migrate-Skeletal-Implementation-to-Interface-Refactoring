package p;

class I {
}

class A extends I {
	void n() {
	}

	public void m() {
		A a = new A();
		a.n();
	}
}
