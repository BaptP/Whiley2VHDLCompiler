package wyvc.utils;

/**
 * Functional interfaces with checked exception handling.
 *
 * @author Baptiste Pauget
 *
 * See java.util.function
 */
public interface FunctionalInterfaces {

	/*---- f : A -> R ----*/

	@FunctionalInterface
	public static interface Function<A,R> {
		R apply(A a);

		default <T> Function<A,T> o(Function<? super R, T> f) {
			return (A a) -> f.apply(apply(a));
		}
		default <B> Function<B,R> p(Function<B, ? extends A> f) {
			return f.o(this);
		}
		default <T, E extends Exception> Function_<A,T,E> o_(Function_<? super R, T, E> f) {
			return (A a) -> f.apply(apply(a));
		}

		default <T> Predicate<A> o(Predicate<? super R> f) {
			return (A a) -> f.test(apply(a));
		}
		default <T, E extends Exception> Predicate_<A,E> o_(Predicate_<? super R, E> f) {
			return (A a) -> f.test(apply(a));
		}

		default <T> Consumer<A> o(Consumer<? super R> f) {
			return (A a) -> f.accept(apply(a));
		}
		default <T, E extends Exception> Consumer_<A,E> o_(Consumer_<? super R, E> f) {
			return (A a) -> f.accept(apply(a));
		}

		default <E extends Exception> Function_<A,R,E> toChecked() {
			return (A a) -> apply(a);
		}
	}

	@FunctionalInterface
	public static interface Function_<A,R,E extends Exception> {
		R apply(A a) throws E;

		default <T> Function_<A,T, E> o(Function_<? super R, T, E> f) {
			return (A a) -> f.apply(apply(a));
		}
		default <B> Function_<B,R,E> p(Function_<B, ? extends A, ? extends E> f) {
			return (B b) -> apply(f.apply(b));
		}

		default <T> Predicate_<A,E> o(Predicate_<? super R, E> f) {
			return (A a) -> f.test(apply(a));
		}

		default <T> Consumer_<A,E> o(Consumer_<? super R, E> f) {
			return (A a) -> f.accept(apply(a));
		}
	}


	/*---- f : A,B -> R ----*/

	@FunctionalInterface
	public static interface BiFunction<A,B,R> {
		R apply(A a, B b);

		default <T> BiFunction<A,B,T> o(Function<? super R, T> f) {
			return (A a, B b) -> f.apply(apply(a,b));
		}
		default <T, E extends Exception> BiFunction_<A,B,T,E> o_(Function_<? super R, T, E> f) {
			return (A a, B b) -> f.apply(apply(a,b));
		}

		default <E extends Exception> BiFunction_<A,B,R,E> toChecked() {
			return (A a, B b) -> apply(a,b);
		}

		default <C extends A, D extends B> Function<Pair<C,D>,R> toFunction() {
			return (Pair<C,D> p) -> apply(p.first, p.second);
		}
	}

	@FunctionalInterface
	public static interface BiFunction_<A,B,R,E extends Exception> {
		R apply(A a, B b) throws E;

		default <T> BiFunction_<A,B,T, E> o(Function_<? super R, T, E> f) {
			return (A a, B b) -> f.apply(apply(a,b));
		}

		default <C extends A, D extends B> Function_<Pair<C,D>,R,E> toFunction() {
			return (Pair<C,D> p) -> apply(p.first, p.second);
		}
	}


	/*---- f : A -> boolean ----*/

	@FunctionalInterface
	public static interface Predicate<A> {
		boolean test(A a);

		default <E extends Exception> Predicate_<A,E> toChecked() {
			return (A a) -> test(a);
		}
	}

	@FunctionalInterface
	public static interface Predicate_<A,E extends Exception> {
		boolean test(A a) throws E;
	}

	/*---- f : A,B -> boolean ----*/

	@FunctionalInterface
	public static interface BiPredicate<A,B> {
		boolean test(A a, B b);

		default <E extends Exception> BiPredicate_<A,B,E> toChecked() {
			return (A a, B b) -> test(a,b);
		}

		default <C extends A, D extends B> Predicate<Pair<C,D>> toPredicate() {
			return (Pair<C,D> p) -> test(p.first, p.second);
		}
	}

	@FunctionalInterface
	public static interface BiPredicate_<A,B,E extends Exception> {
		boolean test(A a, B b) throws E;

		default <C extends A, D extends B> Predicate_<Pair<C,D>,E> toPredicate() {
			return (Pair<C,D> p) -> test(p.first, p.second);
		}
	}


	/*---- f : () -> R ----*/

	@FunctionalInterface
	public static interface Supplier<R> {
		R get();

		default <T> Supplier<T> o(Function<? super R, T> f) {
			return () -> f.apply(get());
		}
		default <T, E extends Exception> Supplier_<T,E> o_(Function_<? super R, T, E> f) {
			return () -> f.apply(get());
		}

		default Tester o(Predicate<? super R> f) {
			return () -> f.test(get());
		}
		default <E extends Exception> Tester_<E> o_(Predicate_<? super R, E> f) {
			return () -> f.test(get());
		}

		default Process o(Consumer<? super R> f) {
			return () -> f.accept(get());
		}
		default <E extends Exception> Process_<E> o_(Consumer_<? super R, E> f) {
			return () -> f.accept(get());
		}

		default <E extends Exception> Supplier_<R,E> toChecked() {
			return () -> get();
		}
	}

	@FunctionalInterface
	public static interface Supplier_<R,E extends Exception> {
		R get() throws E;

		default <T> Supplier_<T, E> o(Function_<? super R, T, E> f) {
			return () -> f.apply(get());
		}

		default  Tester_<E> o_(Predicate_<? super R, E> f) {
			return () -> f.test(get());
		}

		default Process_<E> o_(Consumer_<? super R, E> f) {
			return () -> f.accept(get());
		}
	}


	/*---- f : A -> () ----*/

	@FunctionalInterface
	public static interface Consumer<A> {
		void accept(A a);

		default <E extends Exception> Consumer_<A,E> toChecked() {
			return (A a) -> accept(a);
		}
	}

	@FunctionalInterface
	public static interface Consumer_<A,E extends Exception> {
		void accept(A a) throws E;
	}


	/*---- f : A,B -> () ----*/

	@FunctionalInterface
	public static interface BiConsumer<A,B> {
		void accept(A a, B b);

		default <E extends Exception> BiConsumer_<A,B,E> toChecked() {
			return (A a, B b) -> accept(a,b);
		}

		default <C extends A, D extends B> Consumer<Pair<C,D>> toConsumer() {
			return (Pair<C,D> p) -> accept(p.first, p.second);
		}
	}

	@FunctionalInterface
	public static interface BiConsumer_<A,B,E extends Exception> {
		void accept(A a, B b) throws E;

		default <C extends A, D extends B> Consumer_<Pair<C,D>,E> toConsumer() {
			return (Pair<C,D> p) -> accept(p.first, p.second);
		}
	}


	/*---- f : () -> boolean ----*/

	@FunctionalInterface
	public static interface Tester {
		void test();

		default <E extends Exception> Tester_<E> toChecked() {
			return () -> test();
		}
	}

	@FunctionalInterface
	public static interface Tester_<E extends Exception> {
		void test() throws E;
	}


	/*---- f : () -> () ----*/

	@FunctionalInterface
	public static interface Process {
		void exec();

		default <E extends Exception> Process_<E> toChecked() {
			return () -> exec();
		}
	}

	@FunctionalInterface
	public static interface Process_<E extends Exception> {
		void exec() throws E;
	}













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
	
	
	
	
	
	
	public static class Identity<T> implements Function<T, T> {
		@Override
		public T apply(T t) {
			return t;
		}
	}
}
