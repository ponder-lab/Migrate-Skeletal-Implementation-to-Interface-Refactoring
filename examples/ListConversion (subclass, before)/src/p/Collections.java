package p;

import java.util.Comparator;


public class Collections {
	public static <T> int binarySearch(List<? extends T> list, T key, Comparator<? super T> c) {
		System.out.println("Collections.binarySearch(): " + list);
		return 0;
	}
}
