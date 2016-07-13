package p;

import java.io.Serializable;

interface I {
	default void m() {
		I a;
		p.I a2;
	}
}

/**
 * 
 * @author <a href="mailto:rkhatchadourian@citytech.cuny.edu">Raffi
 *         Khatchadourian</a>
 */
class B<E> implements I {
	void n() {
		I a = new I() {
		};
		I[] a = new I[0];
	}
}

class C implements Serializable, I {
	private static final long serialVersionUID = 1L;
}
