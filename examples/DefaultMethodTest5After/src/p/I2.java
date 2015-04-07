package p;

public interface I2 extends I1 {
	public default void x() {
		System.out.println("In C.x()");
	}
}
