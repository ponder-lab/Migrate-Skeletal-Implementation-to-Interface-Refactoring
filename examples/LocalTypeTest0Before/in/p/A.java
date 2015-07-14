package p;

interface I {
}

class A {
	void m() {
		abstract class B implements I {
			void m() {
			}
		}
	}
}
