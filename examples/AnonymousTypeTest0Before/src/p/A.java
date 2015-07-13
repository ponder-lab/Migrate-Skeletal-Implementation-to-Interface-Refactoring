package p;

interface I {}

class A {
	void m() {
		new I() {
			void n() {
			}
		};
	}
}
