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

		default <T> Consumer<A> c(Consumer<? super R> f) {
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

		default <C> Function<C,R> q(Function<? super C,? extends A> f, Function<? super C,? extends B> g) {
			return (C c) -> apply(f.apply(c),g.apply(c));
		}
		default <C,D> BiFunction<C,D,R> p(Function<? super C,? extends A> f, Function<? super D,? extends B> g) {
			return (C c, D d) -> apply(f.apply(c),g.apply(d));
		}

		default <C, E extends Exception> Function_<C,R,E> q_(Function_<? super C,? extends A,E> f, Function_<? super C,? extends B,E> g) {
			return (C c) -> apply(f.apply(c),g.apply(c));
		}
		default <C, D, E extends Exception> BiFunction_<C,D,R,E> p_(Function_<? super C,? extends A,E> f, Function_<? super D,? extends B,E> g) {
			return (C c, D d) -> apply(f.apply(c),g.apply(d));
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

		default <C, D> BiFunction_<C,D,R,E> p(Function_<? super C,? extends A,E> f, Function_<? super D,? extends B,E> g) {
			return (C c, D d) -> apply(f.apply(c),g.apply(d));
		}
		default <C> Function_<C,R,E> q(Function_<? super C,? extends A,E> f, Function_<? super C,? extends B,E> g) {
			return (C c) -> apply(f.apply(c),g.apply(c));
		}

		default <C extends A, D extends B> Function_<Pair<C,D>,R,E> toFunction() {
			return (Pair<C,D> p) -> apply(p.first, p.second);
		}
	}

	/*---- f : A,B,C -> R ----*/

	@FunctionalInterface
	public static interface TriFunction<A,B,C,R> {
		R apply(A a, B b, C c);

		default <T> TriFunction<A,B,C,T> o(Function<? super R, T> f) {
			return (A a, B b, C c) -> f.apply(apply(a,b,c));
		}
		default <T, E extends Exception> TriFunction_<A,B,C,T,E> o_(Function_<? super R, T, E> f) {
			return (A a, B b, C c) -> f.apply(apply(a,b,c));
		}

		default <D> Function<D,R> q(Function<? super D,? extends A> f, Function<? super D,? extends B> g, Function<? super D,? extends C> h) {
			return (D d) -> apply(f.apply(d),g.apply(d),h.apply(d));
		}
		default <D,E,F> TriFunction<D,E,F,R> p(Function<? super D,? extends A> d, Function<? super E,? extends B> e, Function<? super F,? extends C> f) {
			return (D d_, E e_, F f_) -> apply(d.apply(d_),e.apply(e_),f.apply(f_));
		}

		default <D, E extends Exception> Function_<D,R,E> q_(Function_<? super D,? extends A,E> f, Function_<? super D,? extends B,E> g, Function_<? super D,? extends C,E> h) {
			return (D d) -> apply(f.apply(d),g.apply(d),h.apply(d));
		}
		default <F, G, H, E extends Exception> TriFunction_<F,G,H,R,E> p_(Function_<? super F,? extends A,E> f, Function_<? super G,? extends B,E> g, Function_<? super H,? extends C,E> h) {
			return (F f_, G g_, H h_) -> apply(f.apply(f_),g.apply(g_),h.apply(h_));
		}

		default <E extends Exception> TriFunction_<A,B,C,R,E> toChecked() {
			return (A a, B b, C c) -> apply(a,b,c);
		}

		default <D extends A, E extends B, F extends C> Function<Triple<D,E,F>,R> toFunction() {
			return (Triple<D,E,F> t) -> apply(t.first, t.second,t.third);
		}
	}

	@FunctionalInterface
	public static interface TriFunction_<A,B,C,R,E extends Exception> {
		R apply(A a, B b, C c) throws E;

		default <T> TriFunction_<A,B,C,T,E> o(Function_<? super R, T, E> f) {
			return (A a, B b, C c) -> f.apply(apply(a,b,c));
		}

		default <D> Function_<D,R,E> q(Function_<? super D,? extends A,E> f, Function_<? super D,? extends B,E> g, Function_<? super D,? extends C,E> h) {
			return (D d) -> apply(f.apply(d),g.apply(d),h.apply(d));
		}
		default <F, G, H> TriFunction_<F,G,H,R,E> p(Function_<? super F,? extends A,E> f, Function_<? super G,? extends B,E> g, Function_<? super H,? extends C,E> h) {
			return (F f_, G g_, H h_) -> apply(f.apply(f_),g.apply(g_),h.apply(h_));
		}

		default <F extends A, G extends B, H extends C> Function_<Triple<F,G,H>,R,E> toFunction() {
			return (Triple<F,G,H> t) -> apply(t.first, t.second,t.third);
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

	/*---- f : A,B,C -> boolean ----*/

	@FunctionalInterface
	public static interface TriPredicate<A,B,C> {
		boolean test(A a, B b, C c);

		default <E extends Exception> TriPredicate_<A,B,C,E> toChecked() {
			return (A a, B b, C c) -> test(a,b,c);
		}

		default <D extends A, E extends B, F extends C> Predicate<Triple<D,E,F>> toConsumer() {
			return (Triple<D,E,F> t) -> test(t.first, t.second, t.third);
		}
	}

	@FunctionalInterface
	public static interface TriPredicate_<A,B,C,E extends Exception> {
		boolean test(A a, B b, C c) throws E;


		default <D extends A, F extends B, G extends C> Consumer_<Triple<D,F,G>,E> toConsumer() {
			return (Triple<D,F,G> t) -> test(t.first, t.second, t.third);
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

	/*---- f : A,B,C -> () ----*/

	@FunctionalInterface
	public static interface TriConsumer<A,B,C> {
		void accept(A a, B b, C c);

		default <E extends Exception> TriConsumer_<A,B,C,E> toChecked() {
			return (A a, B b, C c) -> accept(a,b, c);
		}

		default <D extends A, E extends B, F extends C> Consumer<Triple<D,E,F>> toConsumer() {
			return (Triple<D,E,F> t) -> accept(t.first, t.second, t.third);
		}
	}

	@FunctionalInterface
	public static interface TriConsumer_<A,B,C,E extends Exception> {
		void accept(A a, B b, C c) throws E;

		default <D extends A, F extends B, G extends C> Consumer_<Triple<D,F,G>,E> toConsumer() {
			return (Triple<D,F,G> t) -> accept(t.first, t.second, t.third);
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







	public static class Identity<T> implements Function<T, T> {
		@Override
		public T apply(T t) {
			return t;
		}
	}
}
