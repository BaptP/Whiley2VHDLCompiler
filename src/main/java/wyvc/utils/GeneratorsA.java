package wyvc.utils;
//
//import java.lang.reflect.Array;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.List;
//
//import wyvc.utils.FunctionalInterfaces.BiConsumer;
//import wyvc.utils.FunctionalInterfaces.BiConsumer_;
//import wyvc.utils.FunctionalInterfaces.BiFunction;
//import wyvc.utils.FunctionalInterfaces.BiFunction_;
//import wyvc.utils.FunctionalInterfaces.BiPredicate;
//import wyvc.utils.FunctionalInterfaces.BiPredicate_;
//import wyvc.utils.FunctionalInterfaces.Consumer;
//import wyvc.utils.FunctionalInterfaces.Consumer_;
//import wyvc.utils.FunctionalInterfaces.Function;
//import wyvc.utils.FunctionalInterfaces.Function_;
//import wyvc.utils.FunctionalInterfaces.Predicate;
//import wyvc.utils.FunctionalInterfaces.Predicate_;
//import wyvc.utils.FunctionalInterfaces.Supplier;
//
///**
// * <p>Python-like generators.</p>
// *
// * <p>
// * The {@link Generator} class does not handle exceptions, they are supported
// * by the {@link Generator_} one.
// * </p>
// *
// * <p>
// * They enable easy computation on collections of objects without storing any container.
// * </p>
// *
// * <p>
// * The {@link GeneratorsA} class can not be used to handle {@link Generator} or
// * {@link Generator_} because handling exception modify methods' signatures.
// * </p>
// *
// * <p>
// * Because the generation runs in an separate thread, they can improve parallelism
// * (values are computed before being needed).
// * </p>
// *
// * <p>
// * In order to support efficiently checked exceptions, methods that use standard
// * functional interfaces in the {@link Generator} class are provided with a
// * {@link FunctionalInterfaces} version starting with a capital letter (to avoid
// * name resolution issues). Calling one of these methods will return a {@link Generator_}.
// * </p>
// *
// * @author Baptiste Pauget
// *
// * @param <T> The type of generated values
// * @see Generator
// * @see Generator_
// */
public abstract class GeneratorsA {}
//
//	/**
//	 * Notifies the end of the generation.
//	 *
//	 * @author Baptiste Pauget
//	 *
//	 */
//	public static class EndOfGenerationException extends Exception {
//		private static final long serialVersionUID = -4492584892378046784L;
//
//	}
//
//	private static interface AbstractGenerator<T> {
//		boolean isEmpty();
//		void stopGeneration();
//		T readLastValue();
//		void setValue(T value);
//		void setReady(boolean ready);
//		void setDone(boolean done);
//		void generateValues() throws InterruptedException, EndOfGenerationException;
//		boolean isReady();
//		boolean isDone();
//		AbstractGenerator<?> getParent();
//		void stopGenerator();
//
//
//		/**
//		 * <p>Internal usage, used in the <code>generateValues</code> method.</p>
//		 *
//		 * <p>
//		 * Wait until a new value is available or an interruption occurred in the generation thread.
//		 * </p>
//		 *
//		 * <p>
//		 * The method <code>getValue</code> gives an access to the calculated value.
//		 * </p>
//		 *
//		 * @throws EndOfGenerationException Thrown when no more values will be generated
//		 * @throws InterruptedException		Thrown when the generation thread is interrupted
//		 */
//		void waitValue() throws EndOfGenerationException, InterruptedException;
//		void yield(T t) throws InterruptedException;
//
//		/**
//		 * <p>Internal usage, used in the <code>generateValues</code> method.</p>
//		 *
//		 * <p>
//		 * Return the last value calculated.
//		 * </p>
//		 *
//		 * <p>
//		 * This method does not check that a (new) value is actually ready, which should
//		 * be done with the <code>waitValue</code> method.
//		 * </p>
//		 *
//		 * @return The last value calculated
//		 */
//		T getValue();
//
//		void beginingOfGeneration();
//		void endOfGeneration();
//		void initialization();
//	}
//
//
//
//	public static interface Generator<T> extends AbstractGenerator<T> {
//		T next() throws EndOfGenerationException, InterruptedException;
//		List<T> toList();
//
//		PairGenerator<Integer,T> enumerate();
//		<U> Generator<U> map(Function<? super T, ? extends U> function);
//		<U> U fold(BiFunction<? super U, ? super T, ? extends U> function, U init);
//		<U,V> PairGenerator<U,V> biMap(
//				Function<? super T, ? extends U> firstMap,
//				Function<? super T, ? extends V> secondMap);
//		void forEach(Consumer<? super T> function);
//		boolean forAll(Predicate<? super T> test);
//
//		<U> PairGenerator<T,U> cartesianProduct(Generator<U> generator);
//		<U> PairGenerator<T,U> gather(Generator<U> generator);
//		Generator<T> filter(Predicate<? super T> test);
//		Generator<T> append(Generator<? extends T> other);
//
//		T find(Predicate<? super T> test);
//		T find(T t);
//
//
//		<E extends Exception> Generator_<T,E> toChecked();
//
//		<U, E extends Exception> Generator_<U,E> map_(Function_<? super T, ? extends U,E> function);
//		<U, E extends Exception> U fold_(BiFunction_<? super U, ? super T, ? extends U, E> function, U init) throws E;
//		<E extends Exception> void forEach_(Consumer_<? super T,E> function) throws E;
//		<E extends Exception> void forAll_(Predicate_<? super T,E> test) throws E;
//		<U,V, E extends Exception> PairGenerator_<U,V,E> biMap_(
//				Function_<? super T, ? extends U, E> firstMap,
//				Function_<? super T, ? extends V, E> secondMap);
//	}
//
//	public static interface PairGenerator<S,T> extends Generator<Pair<S,T>> {
//		Generator<S> takeFirst();
//		Generator<T> takeSecond();
//		PairGenerator<T,S> swap();
//
//		<U,V> PairGenerator<U,V> map(
//				Function<? super S, ? extends U> firstMap,
//				Function<? super T, ? extends V> secondMap);
//		<U> Generator<U> map (BiFunction<? super S, ? super T, ? extends U> function);
//		<U> PairGenerator<U,T> mapFirst(Function<? super S, ? extends U> function);
//		<U> PairGenerator<S,U> mapSecond(Function<? super T, ? extends U> function);
//
//		void forEach(BiConsumer<? super S, ? super T> function);
//		boolean forAll(BiPredicate<? super S, ? super T> test);
//
//		PairGenerator<S,T> filter(BiPredicate<? super S, ? super T> test);
//
//
//		<E extends Exception> PairGenerator_<S,T,E> toChecked();
//
//		<U, E extends Exception> Generator_<U,E> map_(BiFunction_<? super S, ? super T, ? extends U, E> function);
//		<U, E extends Exception> PairGenerator_<U,T,E> mapFirst_(Function_<? super S, ? extends U, E> function);
//		<U, E extends Exception> PairGenerator_<S,U,E> mapSecond_(Function_<? super T, ? extends U, E> function);
//
//		<U,V, E extends Exception> PairGenerator_<U,V,E> map_(
//				Function_<? super S, ? extends U, E> firstMap,
//				Function_<? super T, ? extends V, E> secondMap);
//		<E extends Exception> void forEach_(BiConsumer_<? super S, ? super T,E> function) throws E;
//		<E extends Exception> void forAll_(BiPredicate_<? super S, ? super T,E> test) throws E;
//	}
//
//	public static interface Generator_<T, E extends Exception> extends AbstractGenerator<T> {
//		T next() throws EndOfGenerationException, InterruptedException, E;
//		List<T> toList() throws E;
//
//		PairGenerator_<Integer,T,E> enumerate();
//		<U> Generator_<U,E> map(Function_<? super T, ? extends U, E> function);
//		<U> U fold(BiFunction_<? super U, ? super T, ? extends U, E> function, U init) throws E;
//		<U,V> PairGenerator_<U,V,E> biMap(
//				Function_<? super T, ? extends U, E> firstMap,
//				Function_<? super T, ? extends V, E> secondMap);
//
//
//		void forEach(Consumer_<? super T,E> function) throws E;
//
//		boolean forAll(Predicate_<? super T,E> test) throws E;
//
//		<U> Generator_<U,E> map_(Function<? super T, ? extends U> function);
//		<U> U fold_(BiFunction<? super U, ? super T, ? extends U> function, U init) throws E;
//
//		Generator<T> check() throws E;
//	}
//
//
//	public static interface PairGenerator_<S,T, E extends Exception> extends Generator_<Pair<S,T>,E> {
//		Generator_<S,E> takeFirst();
//		Generator_<T,E> takeSecond();
//		PairGenerator_<T,S,E> swap();
//
//		<U,V> PairGenerator_<U,V,E> map(
//				Function_<? super S, ? extends U, E> firstMap,
//				Function_<? super T, ? extends V, E> secondMap);
//		<U> Generator_<U,E> map(BiFunction_<? super S, ? super T, ? extends U, E> function);
//		<U> PairGenerator_<U,T,E> mapFirst(Function_<? super S, ? extends U, E> function);
//		<U> PairGenerator_<S,U,E> mapSecond(Function_<? super T, ? extends U, E> function);
//
//		<U> Generator_<U,E> map_(BiFunction<? super S, ? super T, ? extends U> function);
//		<U> PairGenerator_<U,T,E> mapFirst_(Function<? super S, ? extends U> function);
//		<U> PairGenerator_<S,U,E> mapSecond_(Function<? super T, ? extends U> function);
//
//		PairGenerator<S,T> check() throws E;
//
//		void forEach(BiConsumer_<? super S,? super T,E> function) throws E;
//		boolean forAll(BiPredicate_<? super S,? super T,E> test) throws E;
//
//		<U,V> PairGenerator_<U,V,E> map_(
//				Function<? super S, ? extends U> firstMap,
//				Function<? super T, ? extends V> secondMap);
//	}
//
//
//
//
//	private static interface DefaultGenerator<T> extends Generator<T> {
//		@Override
//		default PairGenerator<Integer,T> enumerate() {
//			final Generator<T> This = this;
//			return new CustomPairGenerator<Integer,T>(this) {
//				@Override
//				protected void generate() throws InterruptedException, EndOfGenerationException {
//					int k = 0;
//					while (true)
//						yield(k++,This.next());
//
//				}};
//		}
//		@Override
//		default <U> U fold(BiFunction<? super U, ? super T, ? extends U> function, U init) {
//			try {
//				while (true)
//					init = function.apply(init, next());
//			}
//			catch (EndOfGenerationException e) {return init;}
//			catch (InterruptedException e) {return null;} // TODO ok ?
//		}
//		@Override
//		default void forEach(Consumer<? super T> function) {
//			try {
//				while (true)
//					function.accept(next());
//			}
//			catch (EndOfGenerationException e) {}
//			catch (InterruptedException e) {} // TODO ok ?
//			return;
//		}
//		@Override
//		default boolean forAll(Predicate<? super T> test) {
//			return fold((Boolean b, T t) -> b && test.test(t),true);
//		}
//
//		@Override
//		default Generator<T> append(Generator<? extends T> other) {
//			Generator<T> This = this;
//			return new CustomGenerator<T>() {
//				@Override
//				protected void generate() throws InterruptedException, EndOfGenerationException {
//					try {
//						while (true)
//							yield(This.next());
//					}
//					catch (EndOfGenerationException e) {}
//					catch (InterruptedException e) {}
//					while (true)
//						yield(other.next());
//				}
//			};
//		}
//
//		@Override
//		default T find(Predicate<? super T> test) {
//			return fold((T t, T n) -> test.test(n) ? n : t, null);
//		}
//		@Override
//		default T find(T t) {
//			return find(t::equals);
//		}
//
//
//		@Override
//		default <U, E extends Exception> Generator_<U,E> map_(Function_<? super T, ? extends U,E> function) {
//			return this.<E>toChecked().map(function);
//		}
//		@Override
//		default <U, E extends Exception> U fold_(BiFunction_<? super U, ? super T, ? extends U, E> function, U init) throws E {
//			return this.<E>toChecked().fold(function, init);
//		}
//		@Override
//		default <E extends Exception> void forEach_(Consumer_<? super T,E> function) throws E {
//			this.<E>toChecked().forEach(function);
//		}
//		@Override
//		default <E extends Exception> void forAll_(Predicate_<? super T,E> test) throws E {
//			this.<E>toChecked().forAll(test);
//		}
//		@Override
//		default <U,V, E extends Exception> PairGenerator_<U,V,E> biMap_(
//				Function_<? super T, ? extends U, E> firstMap,
//				Function_<? super T, ? extends V, E> secondMap) {
//			return this.<E>toChecked().biMap(firstMap, secondMap);
//		}
//
//
//
//	}
//
//	private static interface DefaultPairGenerator<S,T> extends DefaultGenerator<Pair<S,T>>,PairGenerator<S,T> {
//		@Override
//		default Generator<S> takeFirst() {
//			return map(Pair::getFirst);
//		}
//		@Override
//		default Generator<T> takeSecond() {
//			return map(Pair::getSecond);
//		}
//		@Override
//		default <U,V> PairGenerator<U,V> map(
//				Function<? super S, ? extends U> firstMap,
//				Function<? super T, ? extends V> secondMap) {
//			return this.<U>mapFirst(firstMap).mapSecond(secondMap);
//		}
//
//		@Override
//		default void forEach(BiConsumer<? super S, ? super T> function) {
//			forEach(function.toConsumer());
//		}
//		@Override
//		default boolean forAll(BiPredicate<? super S, ? super T> test) {
//			return forAll(test.toPredicate());
//		}
//
//
//		@Override
//		default <U, E extends Exception> Generator_<U,E> map_(BiFunction_<? super S, ? super T, ? extends U, E> function) {
//			return this.<E>toChecked().map(function);
//		}
//		@Override
//		default <U, E extends Exception> PairGenerator_<U,T,E> mapFirst_(Function_<? super S, ? extends U, E> function) {
//			return this.<E>toChecked().mapFirst(function);
//		}
//		@Override
//		default <U, E extends Exception> PairGenerator_<S,U,E> mapSecond_(Function_<? super T, ? extends U, E> function) {
//			return this.<E>toChecked().mapSecond(function);
//		}
//
//
//		@Override
//		default <U,V, E extends Exception> PairGenerator_<U,V,E> map_(
//				Function_<? super S, ? extends U, E> firstMap,
//				Function_<? super T, ? extends V, E> secondMap) {
//			return this.<E>toChecked().<U>mapFirst(firstMap).mapSecond(secondMap);
//		}
//		@Override
//		default <E extends Exception> void forEach_(BiConsumer_<? super S, ? super T, E> function) throws E {
//			this.<E>toChecked().forEach(function);
//		}
//		@Override
//		default <E extends Exception> void forAll_(BiPredicate_<? super S, ? super T, E> test) throws E {
//			this.<E>toChecked().forAll(test);
//		}
//
//	}
//
//	private static interface DefaultGenerator_<T, E extends Exception> extends Generator_<T,E> {
//		@Override
//		default PairGenerator_<Integer,T,E> enumerate() {
//			final Generator_<T,E> This = this;
//			return new CustomPairGenerator_<Integer,T,E>(this) {
//				@Override
//				protected void generate() throws InterruptedException, EndOfGenerationException, E {
//					int k = 0;
//					while (true)
//						yield(k++,This.next());
//
//				}};
//		}
//		@Override
//		default <U> U fold(BiFunction_<? super U, ? super T, ? extends U, E> function, U init) throws E {
//			try {
//				while (true)
//					init = function.apply(init, next());
//			}
//			catch (EndOfGenerationException e) {return init;}
//			catch (InterruptedException e) {return null;} // TODO ok ?
//		}
//
//		@Override
//		default void forEach(Consumer_<? super T,E> function) throws E {
//			try {
//				while (true)
//					function.accept(next());
//			}
//			catch (EndOfGenerationException e) {}
//			catch (InterruptedException e) {} // TODO ok ?
//			return;
//		}
//
//		@Override
//		default boolean forAll(Predicate_<? super T,E> test) throws E {
//			return fold((Boolean b, T t) -> b && test.test(t),true);
//		}
//
//		@Override
//		default <U> Generator_<U,E> map_(Function<? super T, ? extends U> function) {
//			return map(function.toChecked());
//		}
//		@Override
//		default <U> U fold_(BiFunction<? super U, ? super T, ? extends U> function, U init) throws E {
//			return fold(function.toChecked(), init);
//		}
//	}
//
//	private static interface DefaultPairGenerator_<S,T, E extends Exception> extends DefaultGenerator_<Pair<S,T>,E>,PairGenerator_<S,T,E> {
//		@Override
//		default Generator_<S,E> takeFirst() {
//			return map(Pair::getFirst);
//		}
//		@Override
//		default Generator_<T,E> takeSecond() {
//			return map(Pair::getSecond);
//		}
//
//		@Override
//		default <U,V> PairGenerator_<U,V,E> map(
//				Function_<? super S, ? extends U, E> firstMap,
//				Function_<? super T, ? extends V, E> secondMap) {
//			return this.<U>mapFirst(firstMap).mapSecond(secondMap);
//		}
//		@Override
//		default <U> Generator_<U,E> map_(BiFunction<? super S, ? super T, ? extends U> function) {
//			return map(function.toChecked());
//		}
//		@Override
//		default <U> PairGenerator_<U,T,E> mapFirst_(Function<? super S, ? extends U> function) {
//			return mapFirst(function.toChecked());
//		}
//		@Override
//		default <U> PairGenerator_<S,U,E> mapSecond_(Function<? super T, ? extends U> function) {
//			return mapSecond(function.toChecked());
//		}
//		@Override
//		default void forEach(BiConsumer_<? super S,? super T,E> function) throws E {
//			forEach(function.toConsumer());
//		}
//		@Override
//		default boolean forAll(BiPredicate_<? super S,? super T,E> test) throws E {
//			return forAll(test.toPredicate());
//		}
//
//		@Override
//		default <U,V> PairGenerator_<U,V,E> map_(
//				Function<? super S, ? extends U> firstMap,
//				Function<? super T, ? extends V> secondMap) {
//			return map(firstMap.toChecked(), secondMap.toChecked());
//		}
//	}
//
//
//	public static abstract class CustomGenerator<T> extends AbstractThreadGenerator<T> implements DefaultGenerator<T>{
//
//		/**
//		 * <p>Constructs a generator with no parent.</p>
//		 */
//		public CustomGenerator() {}
//
//		/**
//		 * <p>Constructs a generator with the <code>parent</code> other as parent generator.</p>
//		 *
//		 * <p>
//		 * If the generation is interrupted by an exception, the parent generator will
//		 * be stopped.
//		 * </p>
//		 *
//		 * @param parent 		The parent generator
//		 */
//		public CustomGenerator(AbstractGenerator<?> parent) {
//			super(parent);
//		}
//
//		/**
//		 * <p>Method that will generate the values using <code>yield</code>.</p>
//		 *
//		 * @throws InterruptedException		Provided to use easily the yield method
//		 * @throws EndOfGenerationException Provided to use easily an other generator
//		 */
//		protected abstract void generate() throws InterruptedException, EndOfGenerationException;
//
//		/**
//		 * <p>Implementation of {@link GeneratorsA}'s <code>generateValues</code> method</p>.
//		 */
//		public final void generateValues() throws InterruptedException, EndOfGenerationException {
//			generate();
//		}
//
//		/**
//		 * <p>Provides the next value of the generator.</p>
//		 *
//		 * @return 							The next value
//		 * @throws EndOfGenerationException	Thrown if no more values can be generated
//		 * @throws InterruptedException		Thrown if the generation thread is interrupted
//		 */
//		public T next() throws EndOfGenerationException, InterruptedException {
//			waitValue();
//			return getValue();
//		}
//
//		/**
//		 * <p>Builds a list of the generator's remaining values.
//		 *
//		 * @return 				A list of the values generated
//		 */
//		public List<T> toList() {
//			List<T> l = new ArrayList<>();
//			try {
//				while (true)
//					l.add(next());
//			} catch (EndOfGenerationException e) {
//				return l;
//			} catch (InterruptedException e) {
//				return null;
//			}
//		}
//
//		@Override
//		public <U> Generator<U> map(Function<? super T, ? extends U> function) {
//			return new Mapper<>(this, function);
//		}
//
//		@Override
//		public <U,V> PairGenerator<U,V> biMap(
//				Function<? super T, ? extends U> firstMap,
//				Function<? super T, ? extends V> secondMap) {
//			return new PairMapper<>(this, firstMap, secondMap);
//		}
//
//		@Override
//		public <E extends Exception> Generator_<T, E> toChecked() {
//			return new CustomGenerator_<T,E>(this){
//				@Override
//				protected void generate() throws InterruptedException, EndOfGenerationException, E {
//					while (true)
//						yield(CustomGenerator.this.next());
//
//				}};
//		}
//
//		@Override
//		public <U> PairGenerator<T,U> cartesianProduct(Generator<U> generator) {
//			List<T> values = toList();
//			return new CustomPairGenerator<T,U>(generator) {
//				@Override
//				protected void generate() throws InterruptedException, EndOfGenerationException {
//					while (true) {
//						U u = generator.next();
//						for (T t : values)
//							yield(t,u);
//					}
//
//				}};
//		}
//
//		@Override
//		public <U> PairGenerator<T,U> gather(Generator<U> generator) {
//			Generator<T> This = this;
//			return new CustomPairGenerator<T,U>(generator) {
//				@Override
//				protected void generate() throws InterruptedException, EndOfGenerationException {
//					while (true)
//						yield(This.next(), generator.next());
//				}};
//		}
//
//		@Override
//		public Generator<T> filter(Predicate<? super T> test) {
//			Generator<T> This = this;
//			return new CustomGenerator<T>(this) {
//				@Override
//				protected void generate() throws InterruptedException, EndOfGenerationException {
//					while (true) {
//						T t = This.next();
//						if (test.test(t))
//							yield(t);
//					}
//				}};
//		}
//	}
//
//
//	public static abstract class CustomPairGenerator<S,T> extends CustomGenerator<Pair<S,T>> implements DefaultPairGenerator<S,T> {
//		public CustomPairGenerator() {
//			super();
//		}
//
//		public CustomPairGenerator(AbstractGenerator<?> parent) {
//			super(parent);
//		}
//
//		protected void yield(S s, T t) throws InterruptedException {
//			yield(new Pair<>(s,t));
//		}
//
//		@Override
//		public PairGenerator<T, S> swap() {
//			return new PairMapper<>(this, Pair::getSecond, Pair::getFirst);
//		}
//
////		private PairMapper<Pair<S,T>,S,T> toPairMapper() {
////			return new PairMapper<>(this, Pair::getFirst, Pair::getSecond);
////		}
//
//		@Override
//		public <U> Generator<U> map(BiFunction<? super S, ? super T, ? extends U> function) {
//			return toPairMapper().map(function);
//		}
//
//		@Override
//		public <U> PairGenerator<U, T> mapFirst(Function<? super S, ? extends U> function) {
//			return toPairMapper().mapFirst(function);
//		}
//
//		@Override
//		public <U> PairGenerator<S, U> mapSecond(Function<? super T, ? extends U> function) {
//			return toPairMapper().mapSecond(function);
//		}
//
//		@Override
//		public <E extends Exception> PairGenerator_<S,T, E> toChecked() {
//			return new CustomPairGenerator_<S,T,E>(this){
//				@Override
//				protected void generate() throws InterruptedException, EndOfGenerationException, E {
//					while (true)
//						yield(CustomPairGenerator.this.next());
//
//				}};
//		}
//
//		@Override
//		public PairGenerator<S,T> filter(BiPredicate<? super S, ? super T> test) {
//			PairGenerator<S,T> This = this;
//			return new CustomPairGenerator<S,T>(this) {
//				@Override
//				protected void generate() throws InterruptedException, EndOfGenerationException {
//					while (true) {
//						Pair<S,T> p = This.next();
//						if (test.test(p.first, p.second))
//							yield(p);
//					}
//				}};
//		}
//	}
///*
//	private static class Mapper<S,T> extends CustomGenerator<T> {
//		private final Generator<S> source;
//		private final Function<? super S, ? extends T> map;
//
//		public Mapper(Generator<S> source, Function<? super S, ? extends T> map) {
//			super(source);
//			this.source = source;
//			this.map = map;
//		}
//
//		@Override
//		protected void generate() throws InterruptedException, EndOfGenerationException {
//			while (true)
//				yield(map.apply(source.next()));
//
//		}
//
//		@Override
//		public <U> Generator<U> map(Function<? super T, ? extends U> function) {
//			return new Mapper<S,U>(source, map.o(function));
//		}
//
//
//		@Override
//		public <U, V> PairGenerator<U, V> biMap(
//				Function<? super T, ? extends U> firstMap,
//				Function<? super T, ? extends V> secondMap) {
//			return new PairMapper<S,U,V>(source, map.o(firstMap), map.o(secondMap));
//		}
//
//		@Override
//		public <E extends Exception> Generator_<T, E> toChecked() {
//			return new Mapper_<S,T,E>(source.toChecked(), map.<E>toChecked());
//		}
//
//	}
//
//	private static class PairMapper<S,T,U> extends CustomPairGenerator<T,U> {
//		private final Generator<S> source;
//		private final Function<? super S, ? extends T> firstMap;
//		private final Function<? super S, ? extends U> secondMap;
//
//		public PairMapper(Generator<S> source,
//				Function<? super S, ? extends T> firstMap,
//				Function<? super S, ? extends U> secondMap) {
//			super(source);
//			this.source = source;
//			this.firstMap = firstMap;
//			this.secondMap = secondMap;
//		}
//
//		@Override
//		public PairGenerator<U, T> swap() {
//			return new PairMapper<>(source, secondMap, firstMap);
//		}
//
//		@Override
//		public <V> Generator<V> map(BiFunction<? super T, ? super U, ? extends V> function) {
//			return new Mapper<>(source, (S s) -> function.apply(firstMap.apply(s),secondMap.apply(s)));
//		}
//
//		@Override
//		public <V> PairGenerator<V, U> mapFirst(Function<? super T, ? extends V> function) {
//			return new PairMapper<>(source, firstMap.o(function), secondMap);
//		}
//
//		@Override
//		public <V> PairGenerator<T, V> mapSecond(Function<? super U, ? extends V> function) {
//			return new PairMapper<>(source, firstMap, secondMap.o(function));
//		}
//
//		@Override
//		protected void generate() throws InterruptedException, EndOfGenerationException {
//			while (true) {
//				S s = source.next();
//				yield(firstMap.apply(s), secondMap.apply(s));
//			}
//		}
//
//		@Override
//		public <V> Generator<V> map(Function<? super Pair<T,U>, ? extends V> function) {
//			return map((T t, U u) -> function.apply(new Pair<>(t,u)));
//		}
//
//	}
//*/
//
//
//
//	public static abstract class CustomGenerator_<T, E extends Exception> extends AbstractThreadGenerator<T> implements DefaultGenerator_<T,E>{
//		private E exception = null;
//
//		/**
//		 * <p>Constructs a generator with no parent.</p>
//		 */
//		public CustomGenerator_() {}
//
//		/**
//		 * <p>Constructs a generator with the <code>parent</code> other as parent generator.</p>
//		 *
//		 * <p>
//		 * If the generation is interrupted by an exception, the parent generator will
//		 * be stopped.
//		 * </p>
//		 *
//		 * @param parent 		The parent generator
//		 */
//		public CustomGenerator_(AbstractGenerator<?> parent) {
//			super(parent);
//		}
//
//		/**
//		 * <p>Method that will generate the values using <code>yield</code>.</p>
//		 *
//		 * @throws InterruptedException		Provided to use easily the yield method
//		 * @throws EndOfGenerationException Provided to use easily an other generator
//		 */
//		protected abstract void generate() throws InterruptedException, EndOfGenerationException, E;
//
//		/**
//		 * <p>Implementation of {@link GeneratorsA}'s <code>generateValues</code> method</p>.
//		 */
//		@SuppressWarnings("unchecked")
//		protected final void generateValues() throws InterruptedException, EndOfGenerationException {
//			try {
//				generate();
//			}
//			catch (InterruptedException e) {throw e;}
//			catch (EndOfGenerationException e) {throw e;}
//			catch (Exception e) {exception = (E) e;}
//		}
//
//		/**
//		 * <p>Provides the next value of the generator.</p>
//		 *
//		 * @return 							The next value
//		 * @throws EndOfGenerationException	Thrown if no more values can be generated
//		 * @throws InterruptedException		Thrown if the generation thread is interrupted
//		 */
//		public T next() throws EndOfGenerationException, InterruptedException, E {
//			try {
//				waitValue();
//			}
//			catch (EndOfGenerationException e) {
//				if (exception != null)
//					throw exception;
//				throw e;
//			}
//			return getValue();
//		}
//
//		/**
//		 * <p>Builds a list of the generator's remaining values.
//		 *
//		 * @return 				A list of the values generated
//		 */
//		public List<T> toList() throws E{
//			List<T> l = new ArrayList<>();
//			try {
//				while (true)
//					l.add(next());
//			} catch (EndOfGenerationException e) {
//				return l;
//			} catch (InterruptedException e) {
//				return null;
//			}
//		}
//
//		@Override
//		public <U> Generator_<U,E> map(Function_<? super T, ? extends U, E> function) {
//			return new Mapper_<>(this, function);
//		}
//
//		@Override
//		public <U,V> PairGenerator_<U,V,E> biMap(
//				Function_<? super T, ? extends U, E> firstMap,
//				Function_<? super T, ? extends V, E> secondMap) {
//			return new PairMapper_<>(this, firstMap, secondMap);
//		}
//
//		@Override
//		public Generator<T> check() throws E {
//			return fromCollection(toList());
//		}
//	}
//
//
//	public static abstract class CustomPairGenerator_<S,T, E extends Exception> extends CustomGenerator_<Pair<S,T>,E> implements DefaultPairGenerator_<S,T,E> {
//		public CustomPairGenerator_() {
//			super();
//		}
//
//		public CustomPairGenerator_(AbstractGenerator<?> parent) {
//			super(parent);
//		}
//
//		protected void yield(S s, T t) throws InterruptedException {
//			yield(new Pair<>(s,t));
//		}
//
//		@Override
//		public PairGenerator_<T,S,E> swap() {
//			return new PairMapper_<>(this, Pair::getSecond, Pair::getFirst);
//		}
//
//		private PairMapper_<Pair<S,T>,S,T,E> toPairMapper() {
//			return new PairMapper_<>(this, Pair::getFirst, Pair::getSecond);
//		}
//
//		@Override
//		public <U> Generator_<U, E> map(BiFunction_<? super S, ? super T, ? extends U, E> function) {
//			return toPairMapper().map(function);
//		}
//
//		@Override
//		public <U> PairGenerator_<U, T, E> mapFirst(Function_<? super S, ? extends U, E> function) {
//			return toPairMapper().mapFirst(function);
//		}
//
//		@Override
//		public <U> PairGenerator_<S, U, E> mapSecond(Function_<? super T, ? extends U, E> function) {
//			return toPairMapper().mapSecond(function);
//		}
//
//		@Override
//		public PairGenerator<S,T> check() throws E {
//			return fromPairCollection(toList());
//		}
//	}
///*
//	private static class Mapper_<S,T, E extends Exception> extends CustomGenerator_<T,E> {
//		private final Generator_<S, E> source;
//		private final Function_<? super S, ? extends T, E> map;
//
//		public Mapper_(Generator_<S,E> source, Function_<? super S, ? extends T, E> map) {
//			super(source);
//			this.source = source;
//			this.map = map;
//		}
//
//		@Override
//		protected void generate() throws InterruptedException, EndOfGenerationException, E {
//			while (true)
//				yield(map.apply(source.next()));
//
//		}
//
//		@Override
//		public <U> Generator_<U, E> map(Function_<? super T, ? extends U, E> function) {
//			return new Mapper_<S,U, E>(source, map.o(function));
//		}
//
//
//		@Override
//		public <U, V> PairGenerator_<U, V, E> biMap(
//				Function_<? super T, ? extends U, E> firstMap,
//				Function_<? super T, ? extends V, E> secondMap) {
//			return new PairMapper_<S,U,V, E>(source, map.o(firstMap), map.o(secondMap));
//		}
//
//	}
//
//	private static class PairMapper_<S,T,U, E extends Exception> extends CustomPairGenerator_<T,U,E> {
//		private final Generator_<S,E> source;
//		private final Function_<? super S, ? extends T, E> firstMap;
//		private final Function_<? super S, ? extends U, E> secondMap;
//
//		public PairMapper_(Generator_<S,E> source,
//				Function_<? super S, ? extends T, E> firstMap,
//				Function_<? super S, ? extends U, E> secondMap) {
//			super(source);
//			this.source = source;
//			this.firstMap = firstMap;
//			this.secondMap = secondMap;
//		}
//
//		@Override
//		public PairGenerator_<U, T, E> swap() {
//			return new PairMapper_<>(source, secondMap, firstMap);
//		}
//
//		@Override
//		public <V> Generator_<V, E> map(BiFunction_<? super T, ? super U, ? extends V, E> function) {
//			return new Mapper_<>(source, (S s) -> function.apply(firstMap.apply(s),secondMap.apply(s)));
//		}
//
//		@Override
//		public <V> PairGenerator_<V, U, E> mapFirst(Function_<? super T, ? extends V, E> function) {
//			return new PairMapper_<>(source, firstMap.o(function), secondMap);
//		}
//
//		@Override
//		public <V> PairGenerator_<T, V, E> mapSecond(Function_<? super U, ? extends V, E> function) {
//			return new PairMapper_<>(source, firstMap, secondMap.o(function));
//		}
//
//		@Override
//		protected void generate() throws InterruptedException, EndOfGenerationException, E {
//			while (true) {
//				S s = source.next();
//				yield(firstMap.apply(s), secondMap.apply(s));
//			}
//		}
//
//		@Override
//		public <V> Generator_<V, E> map(Function_<? super Pair<T,U>, ? extends V, E> function) {
//			return map((T t, U u) -> function.apply(new Pair<>(t,u)));
//		}
//
//
//	}
//
//
//
//
//
//*/
//
//
//
//
//
//
////
////
////
////
////
////
////
////
////
////
////
////
////
////
////
////
////
////
////
////
////
////
////	/**
////	 * <p>Base class for every generator that does not support exceptions.</p>
////	 *
////	 * <p>
////	 * The only method to implement to instantiate a {@link Generator} is <code>generate</code>
////	 * </p>
////	 *
////	 * <p>
////	 * To avoid an annoying used of the <code>toCheckedGenerator</code> method, the ones that return a
////	 * generator that could be used with exception are provided with a capital initial letter.
////	 * </p>
////	 *
////	 * @author Baptiste Pauget
////	 *
////	 * @param <T>				The type of the generated values
////	 */
////	public static abstract class Generator<T> extends AbstractGenerator<T> {
////
////		/**
////		 * <p>Constructs a generator with no parent.</p>
////		 */
////		public Generator() {}
////
////		/**
////		 * <p>Constructs a generator with the <code>parent</code> other as parent generator.</p>
////		 *
////		 * <p>
////		 * If the generation is interrupted by an exception, the parent generator will
////		 * be stopped.
////		 * </p>
////		 *
////		 * @param parent 		The parent generator
////		 */
////		public Generator(AbstractGenerator<?> parent) {
////			super(parent);
////		}
////
////		/**
////		 * <p>Method that will generate the values using <code>yield</code>.</p>
////		 *
////		 * @throws InterruptedException		Provided to use easily the yield method
////		 * @throws EndOfGenerationException Provided to use easily an other generator
////		 */
////		protected abstract void generate() throws InterruptedException, EndOfGenerationException;
////
////		/**
////		 * <p>Implementation of {@link Generators}'s <code>generateValues</code> method</p>.
////		 */
////		protected final void generateValues() throws InterruptedException, EndOfGenerationException {
////			generate();
////		}
////
////		/**
////		 * <p>Provides the next value of the generator.</p>
////		 *
////		 * @return 							The next value
////		 * @throws EndOfGenerationException	Thrown if no more values can be generated
////		 * @throws InterruptedException		Thrown if the generation thread is interrupted
////		 */
////		public T next() throws EndOfGenerationException, InterruptedException {
////			waitValue();
////			return getValue();
////		}
////
////		/**
////		 * <p>Builds a list of the generator's remaining values.
////		 *
////		 * @return 				A list of the values generated
////		 */
////		public List<T> toList() {
////			List<T> l = new ArrayList<>();
////			try {
////				while (true)
////					l.add(next());
////			} catch (EndOfGenerationException e) {
////				return l;
////			} catch (InterruptedException e) {
////				return null;
////			}
////		}
////
////		/**
////		 * <p>Provides a {@link Generator_} that will generate the same values.</p>
////		 *
////		 * <p>
////		 * This method is provided to help complying to some methods' signature.
////		 * </p>
////		 *
////		 * @param <E>			The type of exceptions that could be thrown by the {@link Generator_}
////		 *
////		 * @return 				A {@link Generator_} equivalent to this {@link Generator}
////		 */
////		public <E extends Exception> Generator_<T,E> toCheckedGenerator() {
////			return new Generator_<T,E>(this) {
////				@Override
////				protected void generate() throws InterruptedException, EndOfGenerationException, E {
////					while (true)
////						yield(Generator.this.next());
////
////				}};
////		}
////
////		/**
////		 * <p>Extends a collection with the remaining values of the generator.</p>
////		 *
////		 * @param collection 	The collection to  extend
////		 */
////		public void addToCollection(Collection<T> collection) {
////			try {
////				while (true)
////					collection.add(next());
////			}
////			catch (EndOfGenerationException e) {}
////			catch (InterruptedException e) {}
////		}
////
////		/**
////		 * <p>Applies a function to each value of the generator</p>.
////		 *
////		 * @param consumer 		The function to apply
////		 */
////		public void forEach(Consumer<T> consumer) {
////			try {
////				while (true)
////					consumer.accept(next());
////			}
////			catch (EndOfGenerationException e) {}
////			catch (InterruptedException e) {}
////		}
////
////		/**
////		 * <p>Applies a function to each value of the generator</p>.
////		 *
////		 * @param <E>			The type of exceptions thrown by the consumer
////		 *
////		 * @param consumer 		The function to apply
////		 * @throws E			Thrown if a call to <code>consumer</code> thrown an E exception
////		 *
////		 * @see CheckedConsumer
////		 */
////		@SuppressWarnings("unchecked")
////		public <E extends Exception> void ForEach(CheckedConsumer<T, E> consumer) throws E {
////			try {
////				while (true)
////					consumer.accept(next());
////			}
////			catch (EndOfGenerationException e) {}
////			catch (InterruptedException e) {}
////			catch (Exception e) {throw stopGeneration((E) e);}
////		}
////
////		/**
////		 * <p>Applies a function to each value of the generator, using the result
////		 * of the previous call to compute the next result.</p>
////		 *
////		 * <p>
////		 * This is equivalent to
////		 * <code>f(f(f(...f(init, next()),...),next()),next())</code>
////		 * </p>
////		 *
////		 * @param <A>			The type of the folding function results
////		 *
////		 * @param function 		The function to apply
////		 * @param init			The initial value of the argument
////		 * @return				The result of the call on each value
////		 */
////		public <A> A fold(BiFunction<A,T,A> function, A init) {
////			try {
////				while (true)
////					init = function.apply(init, next());
////			}
////			catch (EndOfGenerationException e) {}
////			catch (InterruptedException e) {}
////			return init;
////		}
////
////		/**
////		 * <p>Applies a function to each value of the generator, using the result
////		 * of the previous call to compute the next result.</p>
////		 *
////		 * <p>
////		 * This is equivalent to
////		 * <code>f(f(f(...f(init, next()),...),next()),next())</code>
////		 * </p>
////		 *
////		 * @param <A>			The type of the folding function result
////		 * @param <E>			The type of exceptions to thrown by the folding function
////		 *
////		 *
////		 * @param function 		The function to apply
////		 * @param init			The initial value of the argument
////		 * @return				The result of the call on each value
////		 * @throws E 			Thrown if a call to <code>function</code> thrown an E exception
////		 *
////		 * @see CheckedBiFunction
////		 */
////		@SuppressWarnings("unchecked")
////		public <A, E extends Exception> A Fold(CheckedBiFunction<A,T,A, E> function, A init) throws E {
////			try {
////				while (true)
////					init = function.apply(init, next());
////			}
////			catch (EndOfGenerationException e) {}
////			catch (InterruptedException e) {}
////			catch (Exception e) {throw stopGeneration((E) e);}
////			return init;
////		}
////
////		@SuppressWarnings("unchecked")
////		public <U> Generator<U> convert() {
////			return map((T t) -> (U) t);
////		}
////
////		/**
////		 * <p>Transforms each value of the generator.</p>
////		 *
////		 * @param <U>			The type of the transformation function results
////		 *
////		 * @param function 		The transformation function
////		 * @return				A {@link Generator} of the transformed values
////		 */
////		public <U> Generator<U> map(Function<? super T, ? extends U> function) {
////			Generator<T> This = this;
////			return new Generator<U>(this) {
////				@Override
////				protected void generate() throws InterruptedException, EndOfGenerationException {
////					while (true)
////						yield(function.apply(This.next()));
////				}};
////		}
////
////		public <U,V> StandardPairGenerator<U,V> biMap(Function<? super T, ? extends U> function1, Function<? super T, ? extends V> function2) {
////			Generator<T> This = this;
////			return new StandardPairGenerator<U,V>(this) {
////				@Override
////				protected void generate() throws InterruptedException, EndOfGenerationException {
////					while (true){
////						final T t = This.next();
////						yield(new Pair<>(function1.apply(t), function2.apply(t)));
////					}
////				}};
////		}
////
////		/**
////		 * <p>Transforms each value of the generator.</p>
////		 *
////		 * @param <U>			The type of the transformation function result
////		 * @param <E>			The type of exceptions to thrown by the transformation function
////		 *
////		 *
////		 * @param function 		The transformation function
////		 * @return				A {@link Generator_} of the transformed values
////		 *
////		 * @see CheckedFunction
////		 * @see Generator_
////		 */
////		public <U, E extends Exception> Generator_<U, E> Map(CheckedFunction<? super T, ? extends U, E> function) {
////			return this.<E>toCheckedGenerator().map(function);
////		}
////
////		/**
////		 * <p>Transforms each value of the generator using its index.</p>
////		 *
////		 * @param <U>			The type of the transformation function results
////		 *
////		 * @param biFunction 	The transformation function
////		 * @return				A {@link Generator} of the transformed values
////		 */
////		public <U> Generator<U> enumMap(BiFunction<? super Integer, ? super T, ? extends U> biFunction) {
////			Generator<T> This = this;
////			return new Generator<U>(this) {
////				@Override
////				protected void generate() throws InterruptedException, EndOfGenerationException {
////					int k = 0;
////					while (true)
////						yield(biFunction.apply(k++, This.next()));
////				}};
////		}
////
////		/**
////		 * <p>Transforms each value of the generator using its index.</p>
////		 *
////		 * @param <U>			The type of the transformation function result
////		 * @param <E>			The type of exceptions to thrown by the transformation function
////		 *
////		 * @param function 	The transformation function
////		 * @return				A {@link Generator_} of the transformed values
////		 *
////		 * @see CheckedBiFunction
////		 * @see Generator_
////		 */
////		public <U, E extends Exception> Generator_<U, E> EnumMap(CheckedBiFunction<? super Integer, ? super T, ? extends U, E> function) {
////			return this.<E>toCheckedGenerator().enumMap(function);
////		}
////
////		/**
////		 * <p>Constructs a unique generator from <code>this</code> and an other one.</p>
////		 *
////		 * <p>
////		 * The generation will stop as soon as one of the component fails to provide a value.
////		 * </p>
////		 *
////		 * @param <U>			The type of the <code>generator</code>'s values.
////		 *
////		 * @param generator 	The generator of the second component
////		 * @return 				A {@link StandardPairGenerator} delivering pair of values of <code>this</code> and <code>generator</code>
////		 */
////		public <U> StandardPairGenerator<T,U> gather(Generator<U> generator) {
////			Generator<T> This = this;
////			return new StandardPairGenerator<T,U>(this) {
////				@Override
////				protected void generate() throws InterruptedException, EndOfGenerationException {
////					while (true)
////						yield(new Pair<>(This.next(), generator.next()));
////				}};
////		}
////
////		/**
////		 * <p>Constructs a unique generator from <code>this</code> and an other one.</p>
////		 *
////		 * <p>
////		 * The generation will stop as soon as one of the component fails to provide a value,
////		 * or an exception occurs.
////		 * </p>
////		 *
////		 * @param <U>			The type of the <code>generator</code>'s values.
////		 * @param <E>			The type of exceptions to thrown by the <code>generator</code>
////		 *
////		 * @param generator 	The generator of the second component
////		 * @return 				A {@link CheckedPairGenerator} delivering pair of values of <code>this</code> and <code>generator</code>
////		 *
////		 * @see Generator_
////		 */
////		public <U, E extends Exception> CheckedPairGenerator<T,U, E> Gather(Generator_<U, E> generator) {
////			return this.<E>toCheckedGenerator().gather(generator);
////		}
////
////		/**
////		 * <p>Removes the values that does not pass the test.</p>
////		 *
////		 * @param test 			The test used to select values
////		 * @return 				A {@link Generator} of the fitting values
////		 */
////		public Generator<T> filter(Predicate<? super T> test) {
////			Generator<T> This = this;
////			return new Generator<T>(this) {
////				@Override
////				protected void generate() throws InterruptedException, EndOfGenerationException {
////					while (true) {
////						T t = This.next();
////						if (test.test(t))
////							yield(t);
////					}
////				}};
////		}
////
////		/**
////		 * <p>Removes all occurrences of <code>value</code></p>
////		 *
////		 * <p>
////		 * Object equality is used, so this method is mainly useful to
////		 * clear <code>null</code> values from the generated ones.
////		 * </p>
////		 *
////		 * @param value 		The value to remove
////		 * @return 				A {@link Generator} of the other values
////		 */
////		public Generator<T> remove(T value) {
////			return filter((T t) -> t != value);
////		}
////
////		/**
////		 * <p>Looks for the first value matching the test Test.</p>
////		 *
////		 * @param <E>			The type of exceptions to throw if not found.
////		 *
////		 * @param test 			The test to use
////		 * @param exception 	A exception provider to call if no fitting value is found
////		 * @return 				The first value passing the test
////		 * @throws E 			Thrown when no fitting value is found
////		 */
////		public <E extends Exception> T findOrThrow(Predicate<? super T> test, Supplier<E> exception) throws E {
////			try {
////				while (true) {
////					T t = next();
////					if (test.test(t))
////						return t;
////				}
////			}
////			catch (EndOfGenerationException e) {}
////			catch (InterruptedException e) {}
////			throw exception.get();
////		}
////
////		/**
////		 * <p>Looks for the first value equals to <code>t</code>.</p>
////		 *
////		 * @param <E>			The type of exceptions to throw if not found.
////		 *
////		 * <p>
////		 * The Object method <code>equals</code> is used to compare values.
////		 * </p>
////		 *
////		 * @param t				The value to look for
////		 * @param exception 	A exception provider to call if no fitting value is found
////		 * @return				The first equal value
////		 * @throws E			Thrown when no fitting value is found
////		 */
////		public <E extends Exception> T findOrThrow(T t, Supplier<E> exception) throws E {
////			return findOrThrow((T n) -> n.equals(t), exception);
////		}
////
////		/**
////		 * <p>Looks for the first value matching the test Test.</p>
////		 *
////		 * @param test 			The test to use
////		 * @return 				The first value passing the test or null when no such value is found
////		 */
////		public T find(Predicate<? super T> test) {
////			return fold((T t, T n) -> test.test(n) ? n : t, null);
////		}
////
////		/**
////		 * <p>Looks for the first value equals to <code>t</code>.</p>
////		 *
////		 * <p>
////		 * The Object method <code>equals</code> is used to compare values.
////		 * </p>
////		 *
////		 * @param t				The value to look for
////		 * @return				The first equal value or null when no such value is found
////		 */
////		public T find(T t) {
////			return find((T n) -> n.equals(t));
////		}
////
////		/**
////		 * <p>Replace values that match the <code>test</code> with the one provided by the supplier</p>
////		 *
////		 * @param test			The test to use
////		 * @param nv			The replacing value supplier
////		 * @return				A {@link Generator} of the values after replacement
////		 */
////		public Generator<T> replace(Predicate<? super T> test, Supplier<? extends T> nv) {
////			return map((T t) -> test.test(t) ? nv.get() : t);
////		}
////
////		/**
////		 * <p>Replace occurrences of <code>t</code> with the one provided by the supplier</p>
////		 *
////		 * <p>
////		 * Object equality is used, so this method is mainly useful to
////		 * replace <code>null</code> values from the generated ones.
////		 * </p>
////		 *
////		 * @param value				The value to replace
////		 * @param nv			The replacing value supplier
////		 * @return				A {@link Generator} of the values after replacement
////		 */
////		public Generator<T> replace(T value, Supplier<T> nv) {
////			return replace((T t) -> t == value, nv);
////		}
////
////		/**
////		 * <p>Replace values that match the <code>test</code> with the one provided by the supplier</p>
////		 *
////		 * @param <E>			The type of exceptions thrown by the test or the supplier.
////		 *
////		 * @param test			The test to use
////		 * @param nv			The replacing value supplier
////		 * @return				A {@link Generator_} of the values after replacement
////		 *
////		 * @see CheckedPredicate
////		 * @see CheckedSupplier
////		 */
////		public <E extends Exception> Generator_<T,E> Replace(CheckedPredicate<? super T, E> test, CheckedSupplier<? extends T, E> nv) {
////			return this.<E>toCheckedGenerator().replace(test, nv);
////		}
////
////		/**
////		 * <p>Replace occurrences of <code>t</code> with the one provided by the supplier.</p>
////		 *
////		 * <p>
////		 * Object equality is used, so this method is mainly useful to
////		 * replace <code>null</code> values from the generated ones.
////		 * </p>
////		 *
////		 * @param <E>			The type of exceptions thrown by the supplier.
////		 *
////		 * @param value				The value to replace
////		 * @param nv			The replacing value supplier
////		 * @return				A {@link Generator_} of the values after replacement
////		 *
////		 * @see CheckedSupplier
////		 */
////		public <E extends Exception> Generator_<T,E> Replace(T value, CheckedSupplier<T, E> nv) {
////			return this.<E>toCheckedGenerator().replace(value, nv);
////		}
////
////		/**
////		 * <p>Appends the values of <code>other</code> at the end of the one of <code>this</code>.</p>
////		 *
////		 * @param other			The generator to concatenate
////		 * @return				A {@link Generator} of the value of <code>this</code> followed by the ones of <code>other</code>
////		 */
////		public Generator<T> append(Generator<T> other) {
////			Generator<T> This = this;
////			return new Generator<T>(this) {
////				@Override
////				protected void generate() throws InterruptedException, EndOfGenerationException {
////					try {
////						while (true)
////							yield(This.next());
////					}
////					catch (EndOfGenerationException e) {}
////					catch (InterruptedException e) {}
////					while (true)
////						yield(other.next());
////				}
////			};
////		}
////
////		/**
////		 * <p>Enumerates the values with an {@link Integer} index.</p>
////		 *
////		 * @return				A {@link StandardPairGenerator} of the values with their index.
////		 */
////		public StandardPairGenerator<Integer, T> enumerate() {
////			Generator<T> This = this;
////			return new StandardPairGenerator<Integer, T>(this) {
////				@Override
////				protected void generate() throws InterruptedException, EndOfGenerationException {
////					int k = 0;
////					while (true)
////						yield(new Pair<>(k++, This.next()));
////				}};
////		}
////
////		/**
////		 * <p>Checks a property on each value of the generator.</p>
////		 *
////		 * @param test			The test to use
////		 * @return				<code>true</code> if every value pass the test, else <code>false</code>
////		 */
////		public boolean forAll(Predicate<? super T> test) {
////			return fold((Boolean b, T t) -> b && test.test(t), true);
////		}
////
////
////		/**
////		 * <p>Checks a property on each value of the generator.</p>
////		 *
////		 * @param <E>			The type of exceptions thrown by the test.
////		 *
////		 * @param test			The test to use
////		 * @return				<code>true</code> if every value pass the test, else <code>false</code>
////		 * @throws E			Thrown if a call to the test throws it
////		 *
////		 * @see CheckedPredicate
////		 */
////		public <E extends Exception> boolean ForAll(CheckedPredicate<? super T, E> test) throws E {
////			return Fold((Boolean b, T t) -> b && test.test(t), true);
////		}
////
////		public Generator<T> insert(T value) {
////			Generator<T> This = this;
////			return new Generator<T>(this) {
////				@Override
////				protected void generate() throws InterruptedException, EndOfGenerationException {
////					yield(value);
////					while (true)
////						yield(This.next());
////
////				}
////			};
////		}
////	}
////
////
////	/**
////	 * <p>Base class for every generator that supports exceptions.</p>
////	 *
////	 * <p>
////	 * The only method to implement to instantiate a {@link Generator} is <code>generate</code>
////	 * </p>
////	 *
////	 * @author Baptiste Pauget
////	 *
////	 * @param <T>				The type of the generated values
////	 * @param <E>				The type of exceptions that may be thrown during generation
////	 */
////	public static abstract class Generator_<T, E extends Exception> extends AbstractGenerator<T> {
////		private E exception = null;
////
////		/**
////		 * <p>Constructs a generator with no parent.</p>
////		 */
////		public Generator_() {}
////
////		/**
////		 * <p>Constructs a generator with the <code>parent</code> other as parent generator.</p>
////		 *
////		 * <p>
////		 * If the generation is interrupted by an exception, the parent generator will
////		 * be stopped.
////		 * </p>
////		 *
////		 * @param parent 		The parent generator
////		 */
////		public Generator_(AbstractGenerator<?> parent) {
////			super(parent);
////		}
////
////		/**
////		 * <p>Method that will generate the values using <code>yield</code>.</p>
////		 *
////		 * @throws InterruptedException		Provided to use easily the yield method
////		 * @throws EndOfGenerationException Provided to use easily an other generator
////		 * @throws E						Provided to use the generator with custom exceptions
////		 */
////		protected abstract void generate() throws InterruptedException, EndOfGenerationException, E;
////
////		/**
////		 * <p>Implementation of {@link Generators}'s <code>generateValues</code> method</p>.
////		 */
////		@SuppressWarnings("unchecked")
////		public final void generateValues() throws InterruptedException, EndOfGenerationException {
////			try {
////				generate();
////			}
////			catch (InterruptedException e) {throw e;}
////			catch (EndOfGenerationException e) {throw e;}
////			catch (Exception e) {exception = (E) e;}
////		}
////
////		/**
////		 * <p>Provides the next value of the generator.</p>
////		 *
////		 * @return 							The next value
////		 * @throws EndOfGenerationException	Thrown if no more values can be generated
////		 * @throws InterruptedException		Thrown if the generation thread is interrupted
////		 * @throws E						Thrown if the generation has thrown it in the generation thread
////		 */
////		public T next() throws EndOfGenerationException, InterruptedException, E {
////			try {
////				waitValue();
////			}
////			catch (EndOfGenerationException e) {
////				if (exception != null)
////					throw exception;
////				throw e;
////			}
////			return getValue();
////		}
////
////		/**
////		 * <p>Builds a list of the generator's remaining values.
////		 *
////		 * @return 				A list of the values generated
////		 * @throws E			Thrown if the generation has thrown it
////		 */
////		public List<T> toList() throws E {
////			List<T> l = new ArrayList<>();
////			try {
////				while (true)
////					l.add(next());
////			}
////			catch (EndOfGenerationException e) {return l;}
////			catch (InterruptedException e) {return null;}
////		}
////
////		/**
////		 * <p>Applies a function to each value of the generator</p>.
////		 *
////		 * @param consumer 		The function to apply
////		 * @throws E			Thrown if the generation has thrown it
////		 */
////		@SuppressWarnings("unchecked")
////		public void forEach(CheckedConsumer<T, E> consumer) throws E {
////			try {
////				while (true)
////					consumer.accept(next());
////			}
////			catch (EndOfGenerationException e) {}
////			catch (InterruptedException e) {}
////			catch (Exception e) {throw stopGeneration((E) e);}
////		}
////
////		/**
////		 * <p>Applies a function to each value of the generator, using the result
////		 * of the previous call to compute the next result.</p>
////		 *
////		 * <p>
////		 * This is equivalent to
////		 * <code>f(f(f(...f(init, next()),...),next()),next())</code>
////		 * </p>
////		 *
////		 * @param <A> 			The type of the result
////		 *
////		 * @param function 		The function to apply
////		 * @param init			The initial value of the argument
////		 * @return				The result of the call on each value
////		 * @throws E			Thrown if the generation has thrown it
////		 */
////		@SuppressWarnings("unchecked")
////		public <A> A fold(CheckedBiFunction<A,T,A, E> function, A init) throws E {
////			try {
////				while (true)
////					init = function.apply(init, next());
////			}
////			catch (EndOfGenerationException e) {}
////			catch (InterruptedException e) {}
////			catch (Exception e) {throw stopGeneration((E) e);}
////			return init;
////		}
////
////
////		/**
////		 * <p>Transforms each value of the generator.</p>
////		 *
////		 * @param <U> 			The type of the transformation function result
////		 *
////		 * @param function 		The transformation function
////		 * @return				A {@link Generator} of the transformed values
////		 */
////		public <U> Generator_<U, E> map(CheckedFunction<? super T, ? extends U, E> function) {
////			Generator_<T, E> This = this;
////			return new Generator_<U, E>(this) {
////				@Override
////				protected void generate() throws InterruptedException, EndOfGenerationException, E {
////					while (true)
////						yield(function.apply(This.next()));
////				}};
////		}
////
////		/**
////		 * <p>Transforms each value of the generator using its index.</p>
////		 *
////		 * @param <U> 			The type of the transformation function result
////		 *
////		 * @param function 	The transformation function
////		 * @return				A {@link Generator_} of the transformed values
////		 *
////		 * @see CheckedBiFunction
////		 */
////		public <U> Generator_<U, E> enumMap(CheckedBiFunction<? super Integer, ? super T, ? extends U, E> function) {
////			Generator_<T, E> This = this;
////			return new Generator_<U, E>(this) {
////				@Override
////				protected void generate() throws InterruptedException, EndOfGenerationException, E {
////					int k = 0;
////					while (true)
////						yield(function.apply(k++, This.next()));
////				}};
////		}
////
////		/**
////		 * <p>Constructs a unique generator from <code>this</code> and an other one.</p>
////		 *
////		 * <p>
////		 * The generation will stop as soon as one of the component fails to provide a value,
////		 * or an exception occurs.
////		 * </p>
////		 *
////		 * @param <U> 			The type of the transformation function result
////		 *
////		 * @param generator 	The generator of the second component
////		 * @return 				A {@link CheckedPairGenerator} delivering pair of values of <code>this</code> and <code>generator</code>
////		 */
////		public <U> CheckedPairGenerator<T,U, E> gather(Generator_<U, E> generator) {
////			Generator_<T, E> This = this;
////			return new CheckedPairGenerator<T,U, E>(this) {
////				@Override
////				protected void generate() throws InterruptedException, EndOfGenerationException, E {
////					while (true)
////						yield(This.next(), generator.next());
////				}};
////		}
////
////		/**
////		 * <p>Removes the values that does not pass the test.</p>
////		 *
////		 * @param test 			The test used to select values
////		 * @return 				A {@link Generator} of the fitting values
////		 */
////		public Generator_<T,E> filter(Predicate<? super T> test) {
////			Generator_<T,E> This = this;
////			return new Generator_<T,E>(this) {
////				@Override
////				protected void generate() throws InterruptedException, EndOfGenerationException, E {
////					while (true) {
////						T t = This.next();
////						if (test.test(t))
////							yield(t);
////					}
////				}};
////		}
////
////		/**
////		 * <p>Removes all occurrences of <code>value</code></p>
////		 *
////		 * <p>
////		 * Object equality is used, so this method is mainly useful to
////		 * clear <code>null</code> values from the generated ones.
////		 * </p>
////		 *
////		 * @param value 		The value to remove
////		 * @return 				A {@link Generator} of the other values
////		 */
////		public Generator_<T,E> remove(T value) {
////			return filter((T t) -> t != value);
////		}
////
////		/**
////		 * <p>Looks for the first value matching the test Test.</p>
////		 *
////		 * @param <F>			The type of exceptions to thrown if not found
////		 *
////		 * @param test 			The test to use
////		 * @param exception 	A exception provider to call if no fitting value is found
////		 * @return 				The first value passing the test
////		 * @throws E			Thrown if the generation has thrown it
////		 * @throws F 			Thrown when no fitting value is found
////		 */
////		public <F extends Exception> T findOrThrow(Predicate<? super T> test, Supplier<F> exception) throws E, F {
////			try {
////				while (true) {
////					T t = next();
////					if (test.test(t))
////						return t;
////				}
////			}
////			catch (EndOfGenerationException e) {}
////			catch (InterruptedException e) {}
////			throw exception.get();
////		}
////
////		/**
////		 * <p>Looks for the first value equals to <code>t</code>.</p>
////		 *
////		 * <p>
////		 * The Object method <code>equals</code> is used to compare values.
////		 * </p>
////		 *
////		 * @param <F>			The type of exceptions to thrown if not found
////		 *
////		 * @param t				The value to look for
////		 * @param exception 	A exception provider to call if no fitting value is found
////		 * @return				The first equal value
////		 * @throws E			Thrown if the generation has thrown it
////		 * @throws F			Thrown when no fitting value is found
////		 */
////		public <F extends Exception> T findOrThrow(T t, Supplier<F> exception) throws E, F {
////			return findOrThrow((T n) -> n.equals(t), exception);
////		}
////
////		/**
////		 * <p>Looks for the first value matching the test Test.</p>
////		 *
////		 * @param test 			The test to use
////		 * @return 				The first value passing the test or null when no such value is found
////		 * @throws E			Thrown if the generation has thrown it
////		 */
////		public T find(Predicate<? super T> test) throws E {
////			return fold((T t, T n) -> test.test(n) ? n : t, null);
////		}
////
////		/**
////		 * <p>Looks for the first value equals to <code>t</code>.</p>
////		 *
////		 * <p>
////		 * The Object method <code>equals</code> is used to compare values.
////		 * </p>
////		 *
////		 * @param t				The value to look for
////		 * @return				The first equal value or null when no such value is found
////		 * @throws E			Thrown if the generation has thrown it
////		 */
////		public T find(T t) throws E {
////			return find((T n) -> n.equals(t));
////		}
////
////		/**
////		 * <p>Replace values that match the <code>test</code> with the one provided by the supplier</p>
////		 *
////		 * @param test			The test to use
////		 * @param nv			The replacing value supplier
////		 * @return				A {@link Generator_} of the values after replacement
////		 *
////		 * @see CheckedPredicate
////		 * @see CheckedSupplier
////		 */
////		public Generator_<T,E> replace(CheckedPredicate<? super T, E> test, CheckedSupplier<? extends T, E> nv) {
////			return map((T t) -> test.test(t) ? nv.get() : t);
////		}
////
////		/**
////		 * <p>Replace occurrences of <code>t</code> with the one provided by the supplier.</p>
////		 *
////		 * <p>
////		 * Object equality is used, so this method is mainly useful to
////		 * replace <code>null</code> values from the generated ones.
////		 * </p>
////		 *
////		 * @param value				The value to replace
////		 * @param nv			The replacing value supplier
////		 * @return				A {@link Generator_} of the values after replacement
////		 *
////		 * @see CheckedSupplier
////		 */
////		public Generator_<T,E> replace(T value, CheckedSupplier<T, E> nv) {
////			return replace((T t) -> t == value, nv);
////		}
////
////		/**
////		 * <p>Enumerates the values with an {@link Integer} index.</p>
////		 *
////		 * @return				A {@link StandardPairGenerator} of the values with their index.
////		 */
////		public CheckedPairGenerator<Integer, T, E> enumerate() {
////			Generator_<T, E> This = this;
////			return new CheckedPairGenerator<Integer, T, E>(this) {
////				@Override
////				protected void generate() throws InterruptedException, EndOfGenerationException, E {
////					int k = 0;
////					while (true)
////						yield(k++, This.next());
////				}};
////		}
////
////		/**
////		 * <p>Checks a property on each value of the generator.</p>
////		 *
////		 * @param test			The test to use
////		 * @return				<code>true</code> if every value pass the test, else <code>false</code>
////		 * @throws E			Thrown if the generation has thrown it
////		 */
////		public boolean forAll(CheckedPredicate<? super T, E> test) throws E {
////			return fold((Boolean b, T t) -> b && test.test(t), true);
////		}
////
////		public Generator<T> check() throws E {
////			return Generators.fromCollection(toList());
////		}
////	}
////
////
////	/**
////	 * <p>Extends the {@link Generator} class in the case of {@link Pair} values with useful methods.</p>
////	 *
////	 * @author Baptiste Pauget
////	 *
////	 * @param <S>				The type of the first value's component
////	 * @param <T>				The type of the second value's component
////	 */
////	public static abstract class StandardPairGenerator<S,T> extends Generator<Pair<S,T>> {
////
////		/**
////		 * <p>Constructs a generator with no parent.</p>
////		 */
////		public StandardPairGenerator() {}
////
////		/**
////		 * <p>Constructs a generator with the <code>parent</code> other as parent generator.</p>
////		 *
////		 * <p>
////		 * If the generation is interrupted by an exception, the parent generator will
////		 * be stopped.
////		 * </p>
////		 *
////		 * @param parent 		The parent generator
////		 */
////		public StandardPairGenerator(AbstractGenerator<?> parent) {
////			super(parent);
////		}
////
////		public void yield(S s, T t) throws InterruptedException {
////			yield(new Pair<>(s,t));
////		}
////
////		/**
////		 * Provides the first component of each value
////		 *
////		 * @return				A {@link Generator} of the first component of the generated values
////		 */
////		public Generator<S> takeFirst() {
////			return map((Pair<S,T> p) -> p.first);
////		}
////
////		/**
////		 * Provides the second component of each value
////		 *
////		 * @return				A {@link Generator} of the second component of the generated values
////		 */
////		public Generator<T> takeSecond() {
////			return map((Pair<S,T> p) -> p.second);
////		}
////
////		/**
////		 * <p>Transforms each value of the generator.</p>
////		 *
////		 * @param <U>			The type of transformation function result.
////		 *
////		 * @param function 		The transformation function
////		 * @return				A {@link Generator} of the transformed values
////		 */
////		public <U> Generator<U> map(BiFunction<? super S, ? super T, ? extends U> function) {
////			return map((Pair<S,T> p) -> function.apply(p.first, p.second));
////		}
////
////		public <U,V> StandardPairGenerator<U,V> map(
////				Function<? super S, ? extends U> firstMap,
////				Function<? super T, ? extends V> secondMap) {
////			return this.<U>mapFirst(firstMap).mapSecond(secondMap);
////}
////
////		/**
////		 * <p>Transforms each value of the generator.</p>
////		 *
////		 * @param <U>			The type of transformation function result.
////		 * @param <E>			The type of exceptions thrown by the consumer.
////		 *
////		 * @param function 		The transformation function
////		 * @return				A {@link Generator_} of the transformed values
////		 *
////		 * @see CheckedBiFunction
////		 */
////		public <U, E extends Exception> Generator_<U, E> Map(
////				CheckedBiFunction<? super S, ? super T, ? extends U, E> function) {
////			return Map((Pair<S,T> p) -> function.apply(p.first, p.second));
////		}
////		public <U,V, E extends Exception> CheckedPairGenerator<U,V,E> Map(
////				CheckedFunction<? super S, ? extends U, E> firstMap,
////				CheckedFunction<? super T, ? extends V, E> secondMap) {
////			return this.<U,E>MapFirst(firstMap).mapSecond(secondMap);
////		}
////
////		/**
////		 * <p>Transforms the first component of each value of the generator.</p>
////		 *
////		 * @param <U>			The type of transformation function result
////		 *
////		 * @param function 		The transformation function
////		 * @return				A {@link StandardPairGenerator} of the transformed values
////		 */
////		public <U> StandardPairGenerator<U,T> mapFirst(Function<? super S, ? extends U> function) {
////			StandardPairGenerator<S,T> This = this;
////			return new StandardPairGenerator<U, T>(this) {
////				@Override
////				protected void generate() throws InterruptedException,EndOfGenerationException {
////					while (true)
////						yield(This.next().transformFirst(function));
////				}};
////		}
////
////		/**
////		 * <p>Transforms the first component of each value of the generator.</p>
////		 *
////		 * @param <U>			The type of transformation function result
////		 * @param <E>			The type of exceptions thrown by the transformation function
////		 *
////		 * @param function 		The transformation function
////		 * @return				A {@link StandardPairGenerator} of the transformed values
////		 */
////		public <U, E extends Exception> CheckedPairGenerator<U,T, E> MapFirst(CheckedFunction<? super S, ? extends U, E> function) {
////			StandardPairGenerator<S,T> This = this;
////			return new CheckedPairGenerator<U, T, E>(this) {
////				@Override
////				protected void generate() throws InterruptedException, EndOfGenerationException, E {
////					while (true)
////						yield(This.next().transformFirstChecked(function));
////				}};
////		}
////
////		/**
////		 * <p>Transforms the second component of each value of the generator.</p>
////		 *
////		 * @param <U>			The type of transformation function result
////		 *
////		 * @param function 		The transformation function
////		 * @return				A {@link StandardPairGenerator} of the transformed values
////		 */
////		public <U> StandardPairGenerator<S,U> mapSecond(Function<? super T, ? extends U> function) {
////			StandardPairGenerator<S,T> This = this;
////			return new StandardPairGenerator<S, U>(this) {
////				@Override
////				protected void generate() throws InterruptedException, EndOfGenerationException {
////					while (true)
////						yield(This.next().transformSecond(function));
////				}};
////		}
////
////		/**
////		 * <p>Transforms the second component of each value of the generator.</p>
////		 *
////		 * @param <U>			The type of transformation function result
////		 * @param <E>			The type of exceptions thrown by the transformation function
////		 *
////		 * @param function 		The transformation function
////		 * @return				A {@link StandardPairGenerator} of the transformed values
////		 */
////		public <U, E extends Exception> CheckedPairGenerator<S,U, E> MapSecond(CheckedFunction<? super T, ? extends U, E> function) {
////			StandardPairGenerator<S,T> This = this;
////			return new CheckedPairGenerator<S, U, E>(this) {
////				@Override
////				protected void generate() throws InterruptedException, EndOfGenerationException, E {
////					while (true)
////						yield(This.next().transformSecondChecked(function));
////				}};
////		}
////
////		/**
////		 * <p>Applies a function to each value of the generator</p>.
////		 *
////		 * @param consumer 		The function to apply
////		 */
////		public void forEach(BiConsumer<S, T> consumer) {
////			forEach((Pair<S,T> p) -> consumer.accept(p.first, p.second));
////		}
////
////		/**
////		 * <p>Applies a function to each value of the generator</p>.
////		 *
////		 * @param <E>			The type of exceptions thrown by the consumer.
////		 *
////		 * @param consumer 		The function to apply
////		 * @throws E			Thrown if a call to <code>consumer</code> thrown an E exception
////		 *
////		 * @see CheckedBiConsumer
////		 */
////		public <E extends Exception> void ForEach(CheckedBiConsumer<S, T, E> consumer) throws E {
////			ForEach((Pair<S,T> p) -> consumer.accept(p.first, p.second));
////		}
////
////		/**
////		 * <p>Checks a property on each value of the generator.</p>
////		 *
////		 * @param test			The test to use
////		 * @return				<code>true</code> if every value pass the test, else <code>false</code>
////		 */
////		public boolean forAll(BiPredicate<? super S, ? super T> test) {
////			return fold((Boolean b, Pair<S,T> p) -> b && test.test(p.first, p.second), true);
////		}
////
////		/**
////		 * <p>Checks a property on each value of the generator.</p>
////		 *
////		 * @param <E>			The type of exceptions thrown by the test.
////		 *
////		 * @param test			The test to use
////		 * @return				<code>true</code> if every value pass the test, else <code>false</code>
////		 * @throws E			Thrown if a call to the test throws it
////		 *
////		 * @see CheckedBiPredicate
////		 */
////		public <E extends Exception> boolean ForAll(CheckedBiPredicate<? super S, ? super T, E> test) throws E {
////			return Fold((Boolean b, Pair<S,T> p) -> b && test.test(p.first, p.second), true);
////		}
////	}
////
////
////	/**
////	 * <p>Extends the {@link Generator_} class in the case of {@link Pair} values with useful methods.</p>
////	 *
////	 * @author Baptiste Pauget
////	 *
////	 * @param <S>				The type of the first value's component
////	 * @param <T>				The type of the second value's component
////	 * @param <E>				The type of exceptions that may be thrown during generation
////	 */
////	public static abstract class CheckedPairGenerator<S,T, E extends Exception> extends Generator_<Pair<S,T>, E> {
////
////		/**
////		 * <p>Constructs a generator with no parent.</p>
////		 */
////		public CheckedPairGenerator() {}
////
////		/**
////		 * <p>Constructs a generator with the <code>parent</code> other as parent generator.</p>
////		 *
////		 * <p>
////		 * If the generation is interrupted by an exception, the parent generator will
////		 * be stopped.
////		 * </p>
////		 *
////		 * @param parent 		The parent generator
////		 */
////		public CheckedPairGenerator(AbstractGenerator<?> parent) {
////			super(parent);
////		}
////
////		public void yield(S s, T t) throws InterruptedException {
////			yield(new Pair<>(s,t));
////		}
////
////		/**
////		 * Provides the first component of each value
////		 *
////		 * @return				A {@link Generator} of the first component of the generated values
////		 */
////		public Generator_<S, E> takeFirst() {
////			return map((Pair<S,T> p) -> p.first);
////		}
////
////		/**
////		 * Provides the second component of each value
////		 *
////		 * @return				A {@link Generator} of the second component of the generated values
////		 */
////		public Generator_<T, E> takeSecond() {
////			return map((Pair<S,T> p) -> p.second);
////		}
////
////		/**
////		 * <p>Transforms the first component of each value of the generator.</p>
////		 *
////		 * @param <U>			The type of transformation function result
////		 *
////		 * @param function 		The transformation function
////		 * @return				A {@link StandardPairGenerator} of the transformed values
////		 */
////		public <U> CheckedPairGenerator<U,T, E> mapFirst(CheckedFunction<? super S, ? extends U, E> function) {
////			CheckedPairGenerator<S,T,E> This = this;
////			return new CheckedPairGenerator<U, T, E>(this) {
////				@Override
////				protected void generate() throws InterruptedException, EndOfGenerationException, E {
////					while (true)
////						yield(This.next().transformFirstChecked(function));
////				}};
////		}
////
////		/**
////		 * <p>Transforms the second component of each value of the generator.</p>
////		 *
////		 * @param <U>			The type of transformation function result
////		 *
////		 * @param function 		The transformation function
////		 * @return				A {@link StandardPairGenerator} of the transformed values
////		 */
////		public <U> CheckedPairGenerator<S,U, E> mapSecond(CheckedFunction<? super T, ? extends U, E> function) {
////			CheckedPairGenerator<S,T,E> This = this;
////			return new CheckedPairGenerator<S, U, E>(this) {
////				@Override
////				protected void generate() throws InterruptedException, EndOfGenerationException, E {
////					while (true)
////						yield(This.next().transformSecondChecked(function));
////				}};
////		}
////
////		/**
////		 * <p>Transforms each value of the generator.</p>
////		 *
////		 * @param <U> 			The type of the transformation function result
////		 *
////		 * @param function 		The transformation function
////		 * @return				A {@link Generator_} of the transformed values
////		 *
////		 * @see CheckedBiFunction
////		 */
////		public <U> Generator_<U, E> map(CheckedBiFunction<? super S, ? super T, ? extends U, E> function) {
////			return map((Pair<S,T> p) -> function.apply(p.first, p.second));
////		}
////
////		public <U,V> CheckedPairGenerator<U,V,E> map(
////				CheckedFunction<? super S, ? extends U, E> firstMap,
////				CheckedFunction<? super T, ? extends V, E> secondMap) {
////			return Generators.toPairGenerator(map((Pair<S,T> p) -> new Pair<>(firstMap.apply(p.first), secondMap.apply(p.second))));
////		}
////
////		/**
////		 * <p>Applies a function to each value of the generator</p>.
////		 *
////		 * @param consumer 		The function to apply
////		 * @throws E			Thrown if a call to the test or the generation throws it
////		 */
////		public void forEach(CheckedBiConsumer<? super S, ? super T, ? extends E> consumer) throws E {
////			forEach((Pair<S,T> p) -> consumer.accept(p.first, p.second));
////		}
////
////		/**
////		 * <p>Checks a property on each value of the generator.</p>
////		 *
////		 * @param test			The test to use
////		 * @return				<code>true</code> if every value pass the test, else <code>false</code>
////		 * @throws E			Thrown if a call to the test or the generation throws it
////		 *
////		 * @see CheckedBiPredicate
////		 */
////		public boolean forAll(CheckedBiPredicate<? super S, ? super T, E> test) throws E {
////			return fold((Boolean b, Pair<S,T> p) -> b && test.test(p.first, p.second), true);
////		}
////
////		public StandardPairGenerator<S,T> check() throws E {
////			return Generators.fromPairCollection(toList());
////		}
////	}
////
////
////
////
////
////
////
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//	public static <S> Generator<S> fromCollection(Collection<S> collection) {
//		return new CustomGenerator<S>() {
//			@Override
//			protected void generate() throws InterruptedException, EndOfGenerationException {
//				for (S s : collection)
//					yield(s);
//
//			}};
//	}
//
//	public static <S,T> PairGenerator<S, T> fromPairCollection(Collection<Pair<S,T>> collection) {
//		return new CustomPairGenerator<S, T>() {
//			@Override
//			protected void generate() throws InterruptedException, EndOfGenerationException {
//				for (Pair<S,T> p : collection)
//					yield(p);
//
//			}};
//	}
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//	/**
//	 * <p>Converts a {@link Generator} of {@link Pair} to a {@link StandardPairGenerator}.</p>
//	 *
//	 * @param <S> 				The first component type
//	 * @param <T> 				The second component type
//	 *
//	 * @param generator			The {@link Generator} to convert
//	 * @return					A {@link StandardPairGenerator} generating the same values
//	 */
//	@SuppressWarnings("unchecked")
//	public static <S,T> PairGenerator<S,T> toPairGenerator(Generator<Pair<S,T>> generator) {
//		if (generator instanceof PairGenerator)
//			return (PairGenerator<S,T>) generator;
//		return new CustomPairGenerator<S, T>(generator) {
//			@Override
//			protected void generate() throws InterruptedException, EndOfGenerationException {
//				while (true)
//					yield(generator.next());
//			}};
//	}
//
//	/**
//	 * <p>Converts a {@link Generator_} of {@link Pair} to a {@link CheckedPairGenerator}.</p>
//	 *
//	 * @param <S> 				The first component type
//	 * @param <T> 				The second component type
//	 * @param <E>				The type of exceptions that can be thrown by the generator
//	 *
//	 * @param generator			The {@link Generator_} to convert
//	 * @return					A {@link CheckedPairGenerator} generating the same values
//	 */
//	@SuppressWarnings("unchecked")
//	public static <S,T, E extends Exception> PairGenerator_<S,T,E> toPairGenerator(Generator_<Pair<S,T>,E> generator) {
//		if (generator instanceof PairGenerator_)
//			return (PairGenerator_<S,T,E>) generator;
//		return new CustomPairGenerator_<S, T, E>(generator) {
//			@Override
//			protected void generate() throws InterruptedException, EndOfGenerationException, E {
//				while (true)
//					yield(generator.next());
//			}};
//	}
//
//	/**
//	 * Builds a {@link StandardGenerator} that produce the values of a {@link Collection}.
//	 *
//	 * @param <S> 				The collection values' type
//	 *
//	 * @param c					The collection to convert
//	 * @return					A {@link StandardGenerator} of the values of the collection
//	 */
////	public static <S> StandardGenerator<S> fromCollection(Collection<S> c) {
////		return new StandardGenerator<S>(){
////			@Override
////			protected void generate() throws InterruptedException {
////				for (S s : c)
////					yield(s);
////			}};
////	}
//
//	/**
//	 * Builds a {@link Generator} that produce the values of a {@link Array}.
//	 *
//	 * @param <S> 				The array values' type
//	 *
//	 * @param a					The array to convert
//	 * @return					A {@link Generator} of the values of the array
//	 */
//	public static <S> Generator<S> fromCollection(S[] a) {
//		return fromCollection(Arrays.asList(a));
//	}
//
//	/**
//	 * Builds a {@link StandardPairGenerator} that produce the values of a {@link Collection} of pairs.
//	 *
//	 * @param <S> 				The first component type
//	 * @param <T> 				The second component type
//	 *
//	 * @param c					The collection to convert
//	 * @return					A {@link StandardPairGenerator} of the values of the collection
//	 */
//	public static <S,T> StandardPairGenerator<S,T> fromPairCollection(Collection<Pair<S,T>> c) {
//		return new StandardPairGenerator<S,T>(){
//			@Override
//			protected void generate() throws InterruptedException {
//				for (Pair<S,T> p : c)
//					yield(p);
//			}};
//	}
//
//	/**
//	 * <p>Generates a {@link Generator} that will produce one value.</p>
//	 *
//	 * @param <S> 				The generators values' type
//	 *
//	 * @param s					The value to wrap in a generator
//	 * @return					A generator producing only one <code>s</code> value
//	 */
//	public static <S> Generator<S> fromSingleton(S s) {
//		return new CustomGenerator<S>() {
//			@Override
//			protected void generate() throws InterruptedException, EndOfGenerationException {
//				yield(s);
//			}};
//	}
//
//	public static <S, T> PairGenerator<S, T> fromPairSingleton(S s, T t) {
//		return new CustomPairGenerator<S, T>() {
//			@Override
//			protected void generate() throws InterruptedException, EndOfGenerationException {
//				yield(s,t);
//			}};
//	}
//
//	/**
//	 * <p>Generates a empty {@link StandardPairGenerator}.</p>
//	 *
//	 * @param <S> 				The generators values' type
//	 *
//	 * @return A {@link Generator} that produce no value
//	 */
//	public static <S> Generator<S> emptyGenerator() {
//		return new CustomGenerator<S>() {
//			@Override
//			protected void generate() throws InterruptedException, EndOfGenerationException {
//			}};
//	}
//
//	/**
//	 * <p>Generates a empty {@link StandardPairGenerator}.</p>
//	 *
//	 * @param <S> 				The first component type
//	 * @param <T> 				The second component type
//	 *
//	 * @return A {@link StandardPairGenerator} that produce no value
//	 */
//	public static <S,T> PairGenerator<S,T> emptyPairGenerator() {
//		return new CustomPairGenerator<S,T>() {
//			@Override
//			protected void generate() throws InterruptedException, EndOfGenerationException {
//			}};
//	}
//
//	/**
//	 * <p>Concatenates generators of the same type.</p>
//	 *
//	 * @param <S> 				The generators values' type
//	 * @param generators		The generators to concatenate
//	 * @return 					A {@link Generator} producing the value of each generator.
//	 */
//	public static <S> Generator<S> concat(Generator<Generator<S>> generators) {
//		return generators.fold((Generator<S> g, Generator<S> t) -> g.append(t), emptyGenerator());
//	}
////
////	public static <S> Generator<S> constant(Supplier<S> f) {
////		return new Generator<S>() {
////			@Override
////			protected void generate() throws InterruptedException, EndOfGenerationException {
////				while (true)
////					yield(f.get());
////			}
////		};
////	}
//
//	public static <S> Generator<Generator<S>> cartesianProduct(Generator<Generator<S>> generators) {
//		final List<List<S>> values = generators.map(Generator::toList).toList();
//		final int noc = values.size();
//		return noc == 0 ? emptyGenerator() : new CustomGenerator<Generator<S>>() {
//			@Override
//			protected void generate() throws InterruptedException, EndOfGenerationException {
//				int[] index = new int[noc];
//				Arrays.fill(index, 0);
//				int[] lenghts = new int[noc];
//				GeneratorsA.fromCollection(values).enumerate().forEach((Integer k, List<S> l) -> lenghts[k] = l.size());
//				for (int j : lenghts)
//					if (j == 0)
//						return;
//				while(index[0] != lenghts[0]) {
//					yield(new CustomGenerator<S>() {
//						@Override
//						protected void generate() throws InterruptedException, EndOfGenerationException {
//							int k = 0;
//							for (int i : index)
//								yield(values.get(k++).get(i));
//							}});
//					int k = noc -1;
//					++index[k];
//					while (k>0 && index[k]==lenghts[k]) {
//						index[k] = 0;
//						++index[--k];
//					}
//				}
//			}};
//	}
//
//
//}
