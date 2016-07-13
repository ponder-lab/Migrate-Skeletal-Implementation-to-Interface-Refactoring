package p;

import javax.annotation.Generated;

interface I {
	@Generated("hello")
	default
	void m() {
	}
}