/**
 * Does PullUp infer the destination type?
 */
package p;

//possible destination.
class C {
}

//possible destination.
class B extends C {
}

class A extends B {
	//what type is selected when we pull up m()?
	public void m() {
	}
}
