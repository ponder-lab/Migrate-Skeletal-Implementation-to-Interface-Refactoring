package p;

public abstract class B implements I {

	@Override
	public void m() {
		System.out.println("In B.m()");
		System.out.println("Partial implementation of I.m() in B");
	}
}
