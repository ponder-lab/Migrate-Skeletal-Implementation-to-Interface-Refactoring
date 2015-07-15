/**
 * Can a method be pulled up into an empty interface?
 */
package p;

interface I {
}

abstract class A implements I {
	public void m() {
	}
}
