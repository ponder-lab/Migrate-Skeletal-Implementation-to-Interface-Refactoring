/**
 * Can an implemented method be pulled up into an interface?
 * No.
 */
package p;

interface I {
	void m();
}

abstract class A implements I {
	public void m() {
	}
}
