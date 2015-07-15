/**
 * Can a final field be pulled up into an interface?
 */
package p;

interface I {
}

class A implements I {
	public static final int CONSTANT = 0;
}
