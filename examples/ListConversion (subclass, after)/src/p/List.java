package p;

import java.util.Comparator;

public interface List<E> extends Collection<E> {

	public default <T> int binarySearch(/* List<? extends T> list, */Object key,
			Comparator<? super T> c) {
		System.out.println("Collections.binarySearch(): " + this);
		return 0;
	}
}
