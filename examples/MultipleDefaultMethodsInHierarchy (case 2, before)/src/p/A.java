package p;

public abstract class A implements I {

	@Override
	public void m() {
		System.out.println("In A.m()");
		System.out.println("Partial implementation of I.m() in A");
	}
}
