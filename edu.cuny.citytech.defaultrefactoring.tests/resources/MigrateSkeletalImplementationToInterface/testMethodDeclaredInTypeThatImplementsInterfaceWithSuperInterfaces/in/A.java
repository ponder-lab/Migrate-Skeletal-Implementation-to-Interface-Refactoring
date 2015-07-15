package p;

interface I {
}

interface J extends I {
}

abstract class A implements J {
	void m() {
	}
}
