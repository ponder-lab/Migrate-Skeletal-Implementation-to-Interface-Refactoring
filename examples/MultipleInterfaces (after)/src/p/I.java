package p;

public interface I {
	
	public default void m() {
		System.out.println("Partial implementation of I.m() in I");
	}
}
