package wyvc.utils;

/**
 * Functional interfaces with checked exception handling.
 *
 * @author Baptiste Pauget
 *
 * See java.util.function
 */
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
		public void accept(S s) throws E;
	}

	@FunctionalInterface
	public interface CheckedBiConsumer<S,T,E extends Exception> {
		public void accept(S s, T t) throws E;
	}

	@FunctionalInterface
	public interface CheckedSupplier<S,E extends Exception> {
		public S get() throws E;
	}

	@FunctionalInterface
	public interface CheckedPredicate<S,E extends Exception> {
		public boolean test(S s) throws E;
	}

	@FunctionalInterface
	public interface CheckedBiPredicate<S,T,E extends Exception> {
		public boolean test(S s, T t) throws E;
	}
}
