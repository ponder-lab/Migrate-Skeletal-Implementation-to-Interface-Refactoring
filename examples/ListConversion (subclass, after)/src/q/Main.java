package q;

import java.util.Comparator;

import p.List;

public class Main {

	public static void main(String[] args) {
		List<B> l1 = new List<B>() {};
		l1.binarySearch(new A(), new Comparator<A>() {

			@Override
			public int compare(A o1, A o2) {
				return 0;
			}
		});
	}
}
