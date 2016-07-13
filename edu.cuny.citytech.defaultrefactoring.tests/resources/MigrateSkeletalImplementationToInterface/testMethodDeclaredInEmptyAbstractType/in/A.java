package p;

import java.io.Serializable;

import p.A;

interface I {
	void m();
}

public abstract class A implements I {

	@Override
	public void m() {
		A a;
		p.A a2;
	}
}

/**
 * 
 * @author <a href="mailto:rkhatchadourian@citytech.cuny.edu">Raffi
 *         Khatchadourian</a>
 * @see p.A
 * @see A
 */
class B<E> extends A {
	void n() {
		A a = new A() {
		};
		A[] a = new A[0];
	}
}

class C extends p.A implements Serializable {
	private static final long serialVersionUID = 1L;
}
