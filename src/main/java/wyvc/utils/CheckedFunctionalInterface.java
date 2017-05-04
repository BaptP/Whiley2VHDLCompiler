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

	@FunctionalInterface
	public interface CheckedConsumer<S,E extends Exception> {
		public void apply(S s) throws E;
	}

	@FunctionalInterface
	public interface CheckedBiConsumer<S,T,E extends Exception> {
		public void apply(S s, T t) throws E;
	}

	@FunctionalInterface
	public interface CheckedSupplier<S,E extends Exception> {
		public S get() throws E;
	}

}
