/**
 * Can a method be moved into an empty interface?
 * No.
 */
package p;

interface I {
}

abstract class A implements I {
	public void m() {
	}
}
