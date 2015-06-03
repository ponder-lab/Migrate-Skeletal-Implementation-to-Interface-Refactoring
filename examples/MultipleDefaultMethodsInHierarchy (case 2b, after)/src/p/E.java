package p;

public class E extends B {

	@Override
	public void m() {
		System.out.println("In E.m()");
		super.m();
		System.out.println("Concrete implementation of I.m() in E");
	}

}
