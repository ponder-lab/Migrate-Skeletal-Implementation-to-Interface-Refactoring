/**
 * Can an implementing method be moved into its corresponding interface?
 */
package p;

interface I {
	public void m();
}

abstract class A implements I {
	public void m() {
	}
}
