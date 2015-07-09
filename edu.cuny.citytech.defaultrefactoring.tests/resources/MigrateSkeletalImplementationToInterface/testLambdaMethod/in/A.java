package p;

import java.util.function.Consumer;

class A {
	void m() {
		Consumer<Object> consumer = s -> System.out.println(s);
	}
}
