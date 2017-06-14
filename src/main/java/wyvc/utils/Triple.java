package wyvc.utils;

public class Triple<S,T,U> {
	public S first;
	public T second;
	public U third;

	public Triple(S first, T second, U third) {
		this.first = first;
		this.second = second;
		this.third = third;
	}

	public S getFirst() {
		return first;
	}

	public T getSecond() {
		return second;
	}

	public U getThird() {
		return third;
	}
}