package p;

public interface I {
	public default void m() {
		System.out.println("Helper for I.m()");
	}
}
