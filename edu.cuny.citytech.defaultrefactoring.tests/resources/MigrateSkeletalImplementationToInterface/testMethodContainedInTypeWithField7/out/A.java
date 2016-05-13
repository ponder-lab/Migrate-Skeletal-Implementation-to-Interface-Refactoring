package p;

interface I {
	default void m() {
		int f2 = f;
	}
}

public abstract class A implements I {
	static int f;
}
