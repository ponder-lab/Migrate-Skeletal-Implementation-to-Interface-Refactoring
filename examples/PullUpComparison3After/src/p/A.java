/**
 * Can a method be pulled up into an empty interface?
 * Yes, but not default. Not really what I was expecting.
 */
package p;

interface I {

	void m();
}

abstract class A implements I {
	@Override
	public void m() {
	}
}
