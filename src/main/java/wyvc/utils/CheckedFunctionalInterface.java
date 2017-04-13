package wyvc.utils;

public interface CheckedFunctionalInterface {

	@FunctionalInterface
	public interface CheckedFunction<S,T,E extends Exception> {
		public T apply(S s) throws E;
	}

	@FunctionalInterface
	public interface CheckedBiFunction<S,T,U,E extends Exception> {
		public U apply(S s, T t) throws E;
	}

}
