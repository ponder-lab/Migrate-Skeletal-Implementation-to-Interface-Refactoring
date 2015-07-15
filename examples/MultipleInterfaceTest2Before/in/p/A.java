package p;

interface I {
}

interface J extends I {
}

abstract class B implements J {
}

abstract class A extends B {
	void m() {
	}
}
