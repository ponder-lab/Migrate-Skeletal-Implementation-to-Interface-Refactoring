package p;

import java.io.Serializable;

interface I {}

abstract class A implements I {
	void m() {
		new Serializable() {
			void n() {
			}
		};
	}
}
