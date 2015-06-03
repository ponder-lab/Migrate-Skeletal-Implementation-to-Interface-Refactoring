package p;

public class C extends A {

	@Override
	public void m() {
		System.out.println("In C.m()");
		super.m();
		System.out.println("Concrete implementation of I.m() in C");
	}
}
