package p;

public interface A {
	public default void x() {
		System.out.println("In Helper.x()");
	}

	public void y();
}
