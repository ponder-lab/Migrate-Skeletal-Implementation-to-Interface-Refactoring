package p;

public interface I {
	public default void x() {
		System.out.println("In I.x()");
	}
}
