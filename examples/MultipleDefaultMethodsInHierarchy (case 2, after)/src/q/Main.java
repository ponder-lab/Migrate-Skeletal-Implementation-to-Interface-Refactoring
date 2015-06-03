package q;

import java.util.Arrays;

import p.C;
import p.D;
import p.E;
import p.I;

public class Main {

	public static void main(String[] args) {
		Arrays.asList(new C(), new D(), new E()).forEach(I::m);
	}

}
