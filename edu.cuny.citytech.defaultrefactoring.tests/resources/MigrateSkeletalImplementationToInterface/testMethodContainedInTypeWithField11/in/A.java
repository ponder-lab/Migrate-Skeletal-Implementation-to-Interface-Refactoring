package p;

interface I {
	void m();
}

public abstract class A implements I {
	
	class B {
		static final int f = 5;
	}

	@Override
	public void m() {
		int f2 = B.f;
	}
}
