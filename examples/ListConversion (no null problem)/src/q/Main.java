package q;

import java.util.Comparator;

import p.List;

public class Main {

	public static void main(String[] args) {
		List<String> l1 = new p.List<String>() {
		};
		
		l1.binarySearch("Hi", new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				return Integer.compare(o1.length(), o2.length());
			}
		});
	}
}
