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

abstract class C implements J, I {
	@Override
	public T m() {
		return null;
	}
}