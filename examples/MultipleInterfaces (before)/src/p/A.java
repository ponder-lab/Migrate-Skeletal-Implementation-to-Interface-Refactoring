package p;

public abstract class A implements I,J {
	
	@Override
	public void m() {
		System.out.println("Partial implementation of I.m() in I");
	}
}
