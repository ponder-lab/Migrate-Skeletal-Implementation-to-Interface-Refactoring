package q;

import java.util.Comparator;

import javax.swing.RowFilter.ComparisonType;

import p.Collections;
import p.List;

public class Main {

	public static void main(String[] args) {
		List<B> l1 = new List<B>() {};
		Collections.binarySearch(l1, new A(), new Comparator<A>() {

			@Override
			public int compare(A o1, A o2) {
				return 0;
			}
		});
	}
}
