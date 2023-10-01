package util;

public class Tuple<T1, T2> {
	private final T1 v1;
	private final T2 v2;

	public Tuple(T1 v1, T2 v2) {
		this.v1 = v1;
		this.v2 = v2;
	}

	public T1 v1() {
		return v1;
	}

	public T2 v2() {
		return v2;
	}

}
