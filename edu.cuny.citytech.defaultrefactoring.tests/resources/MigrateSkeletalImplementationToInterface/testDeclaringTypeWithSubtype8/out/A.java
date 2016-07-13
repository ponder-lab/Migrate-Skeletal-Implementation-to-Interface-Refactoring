package p;

class S {
}

class T extends S {
}

interface I {
	default S m() {
		return null;
	}
}

interface J {
	T m();
}

abstract class C implements I, J {
	@Override
	public T m() {
		return null;
	}
}