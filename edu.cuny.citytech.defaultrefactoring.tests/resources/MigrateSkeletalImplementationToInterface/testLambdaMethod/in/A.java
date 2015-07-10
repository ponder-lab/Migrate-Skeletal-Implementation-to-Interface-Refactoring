package p;

import java.util.function.Consumer;

interface I {}

abstract class A implements I {
	void m() {
		Consumer<Object> consumer = s -> System.out.println(s);
	}
}
