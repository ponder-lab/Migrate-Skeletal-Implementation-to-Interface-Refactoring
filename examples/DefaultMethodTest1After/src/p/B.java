package p;

public class B implements A {

	//B is now silently overriding the default method A.x(). During
	//the refactoring, the developer may want to know about this. In other
	//words, the developer may want to choose the implementation.
	@Override
	public void x() {
		System.out.println("In B.x()");
	}

}
