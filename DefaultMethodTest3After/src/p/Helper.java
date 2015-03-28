package p;

public abstract class Helper implements Interface1, Interface2 {

	@Override
	public void x() {
		Interface1.super.x();
	}
}
