package p;

import java.io.Serializable;

class A {
	void m() {
		new Serializable() {
			void n() {
			}
		};
	}
}
