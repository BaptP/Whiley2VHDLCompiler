package wyvc.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import wyvc.utils.FunctionalInterfaces.BiConsumer;
import wyvc.utils.FunctionalInterfaces.BiConsumer_;
import wyvc.utils.FunctionalInterfaces.BiFunction;
import wyvc.utils.FunctionalInterfaces.BiFunction_;
import wyvc.utils.FunctionalInterfaces.BiPredicate;
import wyvc.utils.FunctionalInterfaces.BiPredicate_;
import wyvc.utils.FunctionalInterfaces.Consumer;
import wyvc.utils.FunctionalInterfaces.Consumer_;
import wyvc.utils.FunctionalInterfaces.Function;
import wyvc.utils.FunctionalInterfaces.Function_;
import wyvc.utils.FunctionalInterfaces.Identity;
import wyvc.utils.FunctionalInterfaces.Predicate;
import wyvc.utils.FunctionalInterfaces.Predicate_;
import wyvc.utils.FunctionalInterfaces.Supplier;
import wyvc.utils.FunctionalInterfaces.Supplier_;
import wyvc.utils.FunctionalInterfaces.TriFunction;
import wyvc.utils.FunctionalInterfaces.TriFunction_;
import wyvc.utils.Generators.Generator;

public class Generators {
	private Generators() {}

	public static interface GCollection<T> extends Collection<T> {
		public default Generator<T> generate() {
			return Generators.fromCollection(this);
		}
	}
	public static interface GPairCollection<S,T> extends GCollection<Pair<S,T>> {
		public default PairGenerator<S,T> generate() {
			return Generators.fromPairCollection(this);
		}
	}
	public static interface GTripleCollection<S,T,U> extends GCollection<Triple<S,T,U>> {
		public default TripleGenerator<S,T,U> generate() {
			return Generators.fromTripleCollection(this);
		}
	}








	public static class EndOfGenerationException extends Exception {
		private static final long serialVersionUID = -4492584892378046784L;

	}



	/*---------------------------------------------------------------*/
	/*------                    Interfaces                     ------*/
	/*---------------------------------------------------------------*/


	/*---- General ----*/

	private static interface AbstractGenerator<T> {
		boolean isEmpty();
		void stopGeneration(); // TODO Salut ?
	}


	public static interface Generator<T> extends AbstractGenerator<T> {

		/*------ Specific ------*/
		T next() throws EndOfGenerationException;
		GList<T> toList();
		PairGenerator<Integer,T> enumerate();
		<U> PairGenerator<T,U> cartesianProduct(Generator<U> generator);
		<U> PairGenerator<T,U> gather(Generator<U> generator);
		<U,V> TripleGenerator<T,U,V> gatherPair(PairGenerator<U,V> generator);
		<U, E extends Exception> PairGenerator_<T,U,E> gather(Generator_<U, E> generator);
		<U,V, E extends Exception> TripleGenerator_<T,U,V,E> gatherPair(PairGenerator_<U,V, E> generator);
		Generator<T> filter(Predicate<? super T> test);
		Generator<T> append(Generator<? extends T> other);
		T find(Predicate<? super T> test);
		T find(T t);
		Generator<T> async();


		/*------ Without exceptions ------*/
		<U> Generator<U> map(Function<? super T, ? extends U> function);
		<U> PairGenerator<T,U> compute(Function<? super T, ? extends U> function);
		<U,V> PairGenerator<U,V> biMap(
				Function<? super T, ? extends U> firstMap,
				Function<? super T, ? extends V> secondMap);
		<U,V> TripleGenerator<T,U,V> biCompute(
				Function<? super T, ? extends U> firstMap,
				Function<? super T, ? extends V> secondMap);
		<U> U fold(BiFunction<? super U, ? super T, ? extends U> function, U init);
		void forEach(Consumer<? super T> function);
		boolean forAll(Predicate<? super T> test);
		boolean forOne(Predicate<? super T> test);


		/*------ With exceptions ------*/
		<U, E extends Exception> Generator_<U,E> map_(Function_<? super T, ? extends U, E> function);
		<U, E extends Exception> PairGenerator_<T,U,E> compute_(Function_<? super T, ? extends U, E> function);
		<U,V, E extends Exception> PairGenerator_<U,V,E> biMap_(
				Function_<? super T, ? extends U,E> firstMap,
				Function_<? super T, ? extends V,E> secondMap);
		<U,V, E extends Exception> TripleGenerator_<T,U,V,E> biCompute_(
				Function_<? super T, ? extends U,E> firstMap,
				Function_<? super T, ? extends V,E> secondMap);
		<U, E extends Exception> U fold_(BiFunction_<? super U, ? super T, ? extends U, E> function, U init) throws E;
		<E extends Exception> void forEach_(Consumer_<? super T,E> function) throws E;
		<E extends Exception> boolean forAll_(Predicate_<? super T,E> test) throws E;
		<E extends Exception> boolean forOne_(Predicate_<? super T,E> test) throws E;


		/*------ Conversion ------*/
		<E extends Exception> Generator_<T,E> toChecked();
	}


	public static interface Generator_<T, E extends Exception> extends AbstractGenerator<T> {

		/*------ Specific ------*/
		T next() throws EndOfGenerationException, E;
		GList<T> toList() throws E;
		PairGenerator_<Integer,T,E> enumerate();
		<U> PairGenerator_<T,U,E> cartesianProduct(Generator_<U,E> generator) throws E;
		<U> PairGenerator_<T,U,E> gather(Generator<U> generator);
		<U,V> TripleGenerator_<T,U,V,E> gatherPair(PairGenerator<U,V> generator);
		<U> PairGenerator_<T,U,E> gather(Generator_<U, E> generator);
		<U,V> TripleGenerator_<T,U,V,E> gatherPair(PairGenerator_<U,V, E> generator);
		Generator_<T,E> filter(Predicate_<? super T,E> test);
		Generator_<T,E> append(Generator_<? extends T,E> other);
		T find(Predicate_<? super T, E> test) throws E;
		T find(T t) throws E;
		Generator_<T,E> async();


		/*------ With exceptions ------*/
		<U> Generator_<U,E> map(Function_<? super T, ? extends U, E> function);
		<U> PairGenerator_<T,U,E> compute(Function_<? super T, ? extends U, E> function);
		<U,V> PairGenerator_<U,V,E> biMap(
				Function_<? super T, ? extends U,E> firstMap,
				Function_<? super T, ? extends V,E> secondMap);
		<U,V> TripleGenerator_<T,U,V,E> biCompute(
				Function_<? super T, ? extends U,E> firstMap,
				Function_<? super T, ? extends V,E> secondMap);
		<U> U fold(BiFunction_<? super U, ? super T, ? extends U, E> function, U init) throws E;
		void forEach(Consumer_<? super T,E> function) throws E;
		boolean forAll(Predicate_<? super T,E> test) throws E;
		boolean forOne(Predicate_<? super T,E> test) throws E;


		/*------ Conversion ------*/
		Generator<T> check() throws E;
	}



	/*---- Pair ----*/


	public static interface PairGenerator<S,T> extends Generator<Pair<S,T>> {

		/*------ Specific ------*/
		GPairList<S,T> toList();
		Generator<S> takeFirst();
		Generator<T> takeSecond();
		PairGenerator<T,S> swap();
		PairGenerator<S,T> filter(BiPredicate<? super S, ? super T> test);
		PairGenerator<S,T> filter(Predicate<? super S> firstTest, Predicate<? super T> secondTest);
		TripleGenerator<S,S,T> duplicateFirst();
		TripleGenerator<S,T,T> duplicateSecond();
		PairGenerator<S, T> appendPair(PairGenerator<? extends S, ? extends T> other);
		<U> TripleGenerator<S,T,U> addComponent(Generator<U> generator);
		<U, E extends Exception> TripleGenerator_<S,T,U,E> addComponent(Generator_<U, E> generator);
		PairGenerator<S,T> async();

		/*------ Without exceptions ------*/
		<U> PairGenerator<U,T> mapFirst(Function<? super S, ? extends U> function);
		<U> PairGenerator<S,U> mapSecond(Function<? super T, ? extends U> function);
		<U> PairGenerator<U,T> computeFirst(BiFunction<? super S, ? super T, ? extends U> function);
		<U> PairGenerator<S,U> computeSecond(BiFunction<? super S, ? super T, ? extends U> function);
		<U,V> PairGenerator<U,V> map(
				Function<? super S, ? extends U> firstMap,
				Function<? super T, ? extends V> secondMap);
		<U> Generator<U> map(BiFunction<? super S, ? super T, ? extends U> function);
		<U,V> PairGenerator<U,V> biMap(
				BiFunction<? super S, ? super T, ? extends U> firstMap,
				BiFunction<? super S, ? super T, ? extends V> secondMap);
		/*      __     ___
		 * S___/  arg00   \
		 *     \__arg10__  map0--U
		 *               \/
		 *      __     __/\
		 *  ___/  arg00    map1--V
		 * T   \__arg10___/
		 */
		<A,B,C,D,U,V> PairGenerator<U,V> crossMap(
				Function<? super S, A> arg00, Function<? super T, C> arg01, BiFunction<? super A, ? super C, ? extends U> map0,
				Function<? super S, B> arg10, Function<? super T, D> arg11, BiFunction<? super B, ? super D, ? extends V> map1);
		<U> TripleGenerator<S,U,T> compute(BiFunction<? super S, ? super T, ? extends U> function);
		/*      __________
		 * S___/          \
		 *     \__arg10__  \_________S
		 *               \
		 *                function---U
		 *      __     __/  _________
		 *  ___/  arg00    /         T
		 * T   \__________/
		 */
		<A,B,U> TripleGenerator<S,U,T> compute(
				Function<? super S, A> arg0, Function<? super T, B> arg1, BiFunction<? super A, ? super B, ? extends U> function);
		void forEach(BiConsumer<? super S, ? super T> function);
		boolean forAll(BiPredicate<? super S, ? super T> test);
		boolean forAll(Predicate<? super S> firstTest, Predicate<? super T> secondTest);
//		Generator<Pair<S,T>> replace(BiPredicate<? super S, ? super T> test, Supplier<Pair<S,T>> with);


		/*------ With exceptions ------*/
		<U, E extends Exception> PairGenerator_<U,T,E> mapFirst_(Function_<? super S, ? extends U,E> function);
		<U, E extends Exception> PairGenerator_<S,U,E> mapSecond_(Function_<? super T, ? extends U,E> function);
		<U, E extends Exception> PairGenerator_<U,T,E> computeFirst_(BiFunction_<? super S, ? super T, ? extends U,E> function);
		<U, E extends Exception> PairGenerator_<S,U,E> computeSecond_(BiFunction_<? super S, ? super T, ? extends U,E> function);
		<U,V, E extends Exception> PairGenerator_<U,V,E> map_(
				Function_<? super S, ? extends U,E> firstMap,
				Function_<? super T, ? extends V,E> secondMap);
		<U, E extends Exception> Generator_<U,E> map_(BiFunction_<? super S, ? super T, ? extends U, E> function);
		<U,V, E extends Exception> PairGenerator_<U,V,E> biMap_(
				BiFunction_<? super S, ? super T, ? extends U, E> firstMap,
				BiFunction_<? super S, ? super T, ? extends V, E> secondMap);
		<A,B,C,D,U,V, E extends Exception> PairGenerator_<U,V,E> crossMap_(
				Function_<? super S, A, E> arg00, Function_<? super T, C, E> arg01, BiFunction_<? super A, ? super C, ? extends U ,E> map0,
				Function_<? super S, B, E> arg10, Function_<? super T, D, E> arg11, BiFunction_<? super B, ? super D, ? extends V, E> map1);
		<U, E extends Exception> TripleGenerator_<S,U,T,E> compute_(BiFunction_<? super S, ? super T, ? extends U,E> function);
		<A,B,U, E extends Exception> TripleGenerator_<S,U,T,E> compute_(
				Function_<? super S, A, E> arg0, Function_<? super T, B, E> arg1, BiFunction_<? super A, ? super B, ? extends U,E> function);
		<E extends Exception> void forEach_(BiConsumer_<? super S, ? super T,E> function) throws E;
		<E extends Exception> boolean forAll_(BiPredicate_<? super S, ? super T,E> test) throws E;
		<E extends Exception> boolean forAll_(Predicate_<? super S, E> firstTest, Predicate_<? super T, E> secondTest) throws E;


		/*------ Conversion ------*/
		<E extends Exception> PairGenerator_<S,T,E> toChecked();
	}

	public static interface PairGenerator_<S,T, E extends Exception> extends Generator_<Pair<S,T>,E> {
		/*------ Specific ------*/
		GPairList<S,T> toList() throws E;
		Generator_<S,E> takeFirst();
		Generator_<T,E> takeSecond();
		PairGenerator_<T,S,E> swap();
		PairGenerator_<S,T,E> filter(BiPredicate_<? super S, ? super T,E> test);
		PairGenerator_<S,T,E> filter(Predicate_<? super S, E> firstTest, Predicate_<? super T, E> secondTest);
		TripleGenerator_<S,S,T,E> duplicateFirst();
		TripleGenerator_<S,T,T,E> duplicateSecond();
		<U> TripleGenerator_<S,T,U,E> addComponent(Generator<U> generator);
		<U> TripleGenerator_<S,T,U,E> addComponent(Generator_<U, E> generator);
		PairGenerator_<S,T,E> async();

		/*------ With exceptions ------*/
		<U> PairGenerator_<U,T,E> mapFirst(Function_<? super S, ? extends U,E> function);
		<U> PairGenerator_<S,U,E> mapSecond(Function_<? super T, ? extends U,E> function);
		<U> PairGenerator_<U,T,E> computeFirst(BiFunction_<? super S, ? super T, ? extends U,E> function);
		<U> PairGenerator_<S,U,E> computeSecond(BiFunction_<? super S, ? super T, ? extends U,E> function);
		<U,V> PairGenerator_<U,V,E> map(
				Function_<? super S, ? extends U,E> firstMap,
				Function_<? super T, ? extends V,E> secondMap);
		<U> Generator_<U,E> map(BiFunction_<? super S, ? super T, ? extends U, E> function);
		<U,V> PairGenerator_<U,V,E> biMap(
				BiFunction_<? super S, ? super T, ? extends U, E> firstMap,
				BiFunction_<? super S, ? super T, ? extends V, E> secondMap);
		<A,B,C,D,U,V> PairGenerator_<U,V,E> crossMap(
				Function_<? super S, A, E> arg00, Function_<? super T, C, E> arg01, BiFunction_<? super A, ? super C, ? extends U ,E> map0,
				Function_<? super S, B, E> arg10, Function_<? super T, D, E> arg11, BiFunction_<? super B, ? super D, ? extends V, E> map1);
		<U> TripleGenerator_<S,U,T,E> compute(BiFunction_<? super S, ? super T, ? extends U,E> function);
		<A,B,U> TripleGenerator_<S,U,T,E> compute(
				Function_<? super S, A, E> arg0, Function_<? super T, B, E> arg1, BiFunction_<? super A, ? super B, ? extends U,E> function);
		void forEach(BiConsumer_<? super S,? super T,E> function) throws E;
		boolean forAll(BiPredicate_<? super S,? super T,E> test) throws E;
		boolean forAll(Predicate_<? super S, E> firstTest, Predicate_<? super T, E> secondTest) throws E;


		/*------ Conversion ------*/
		PairGenerator<S,T> check() throws E;
	}



	/*---- Triple ----*/


	public static interface TripleGenerator<S,T,U> extends Generator<Triple<S,T,U>> {
		/*------ Specific ------*/
		GTripleList<S,T,U> toList();

		Generator<S> takeFirst();
		Generator<T> takeSecond();
		Generator<U> takeThird();

		PairGenerator<T,U> dropFirst();
		PairGenerator<S,U> dropSecond();
		PairGenerator<S,T> dropThird();

		PairGenerator<Pair<S,T>,U> gatherFirst();
		PairGenerator<S,Pair<T,U>> gatherLast();

		TripleGenerator<T,S,U> swap12();
		TripleGenerator<S,U,T> swap23();
		TripleGenerator<U,T,S> swap13();
		TripleGenerator<T,U,S> rotate();

		TripleGenerator<S,T,U> async();

		/*------ Without exceptions ------*/
		<V> TripleGenerator<V,T,U> mapFirst(Function<? super S, ? extends V> function);
		<V> TripleGenerator<S,V,U> mapSecond(Function<? super T, ? extends V> function);
		<V> TripleGenerator<S,T,V> mapThird(Function<? super U, ? extends V> function);
		<V,W,X> TripleGenerator<V,W,X> map(
				Function<? super S, ? extends V> firstMap,
				Function<? super T, ? extends W> secondMap,
				Function<? super U, ? extends X> thirdMap);
		<V> Generator<V> map(TriFunction<? super S, ? super T, ? super U, ? extends V> function);

		<V> PairGenerator<V,U> map12(BiFunction<? super S, ? super T, ? extends V> function);
		<V> PairGenerator<V,U> map21(BiFunction<? super T, ? super S, ? extends V> function);
		<V> PairGenerator<S,V> map23(BiFunction<? super T, ? super U, ? extends V> function);
		<V> PairGenerator<S,V> map32(BiFunction<? super U, ? super T, ? extends V> function);


		/*------ With exceptions ------*/
		<V, E extends Exception> TripleGenerator_<V,T,U,E> mapFirst_(Function_<? super S, ? extends V, E> function);
		<V, E extends Exception> TripleGenerator_<S,V,U,E> mapSecond_(Function_<? super T, ? extends V, E> function);
		<V, E extends Exception> TripleGenerator_<S,T,V,E> mapThird_(Function_<? super U, ? extends V, E> function);
		<V,W,X, E extends Exception> TripleGenerator_<V,W,X,E> map_(
				Function_<? super S, ? extends V,E> firstMap,
				Function_<? super T, ? extends W,E> secondMap,
				Function_<? super U, ? extends X,E> thirdMap);
		<V, E extends Exception> Generator_<V,E> map_(TriFunction_<? super S, ? super T, ? super U, ? extends V, E> function);

		<V, E extends Exception> PairGenerator_<V,U,E> map12_(BiFunction_<? super S, ? super T, ? extends V, E> function);
		<V, E extends Exception> PairGenerator_<V,U,E> map21_(BiFunction_<? super T, ? super S, ? extends V, E> function);
		<V, E extends Exception> PairGenerator_<S,V,E> map23_(BiFunction_<? super T, ? super U, ? extends V, E> function);
		<V, E extends Exception> PairGenerator_<S,V,E> map32_(BiFunction_<? super U, ? super T, ? extends V, E> function);


		/*------ Conversion ------*/
		<E extends Exception> TripleGenerator_<S,T,U,E> toChecked();
	}



	public static interface TripleGenerator_<S,T,U, E extends Exception> extends Generator_<Triple<S,T,U>,E> {
		/*------ Specific ------*/
		GTripleList<S,T,U> toList() throws E;

		Generator_<S,E> takeFirst();
		Generator_<T,E> takeSecond();
		Generator_<U,E> takeThird();

		PairGenerator_<T,U,E> dropFirst();
		PairGenerator_<S,U,E> dropSecond();
		PairGenerator_<S,T,E> dropThird();

		PairGenerator_<Pair<S,T>,U,E> gatherFirst();
		PairGenerator_<S,Pair<T,U>,E> gatherLast();

		TripleGenerator_<T,S,U,E> swap12();
		TripleGenerator_<S,U,T,E> swap23();
		TripleGenerator_<U,T,S,E> swap13();
		TripleGenerator_<T,U,S,E> rotate();

		TripleGenerator_<S,T,U,E> async();

		/*------ With exceptions ------*/

		<V> TripleGenerator_<V,T,U,E> mapFirst(Function_<? super S, ? extends V, E> function);
		<V> TripleGenerator_<S,V,U,E> mapSecond(Function_<? super T, ? extends V, E> function);
		<V> TripleGenerator_<S,T,V,E> mapThird(Function_<? super U, ? extends V, E> function);
		<V,W,X> TripleGenerator_<V,W,X,E> map(
				Function_<? super S, ? extends V,E> firstMap,
				Function_<? super T, ? extends W,E> secondMap,
				Function_<? super U, ? extends X,E> thirdMap);
		<V> Generator_<V,E> map(TriFunction_<? super S, ? super T, ? super U, ? extends V, E> function);

		<V> PairGenerator_<V,U,E> map12(BiFunction_<? super S, ? super T, ? extends V, E> function);
		<V> PairGenerator_<V,U,E> map21(BiFunction_<? super T, ? super S, ? extends V, E> function);
		<V> PairGenerator_<S,V,E> map23(BiFunction_<? super T, ? super U, ? extends V, E> function);
		<V> PairGenerator_<S,V,E> map32(BiFunction_<? super U, ? super T, ? extends V, E> function);

		/*------ Conversion ------*/
		TripleGenerator<S,T,U> check() throws E;
	}












	/*------ Default Implementation ------*/

	public static interface DefaultGenerator<T> extends Generator<T> {

		/*------ Specific ------*/
		@Override default GList<T> toList() {
			GList<T> l = new GList.GArrayList<>();
			try {
				while (true)
					l.add(next());
			}
			catch (EndOfGenerationException e) {}
			return l;
		}
		@Override default PairGenerator<Integer,T> enumerate() {
			final Generator<T> This = this;
			return new SimplePairGenerator<Integer,T>(this) {
				private int k = 0;

				@Override
				public Pair<Integer, T> nextValue() throws EndOfGenerationException {
					return new Pair<>(k++, This.next());
				}
				@Override
				public boolean isEmpty() {
					return This.isEmpty();
				}};
		}
		@Override default <U> PairGenerator<T,U> cartesianProduct(Generator<U> generator) {
			List<T> values = toList();
			return new CustomPairGenerator<T,U>(generator) {
				@Override
				protected void generate() throws EndOfGenerationException {
					while (true) {
						U u = generator.next();
						for (T t : values)
							yield(t,u);
					}

				}};
		}
		@Override default <U> PairGenerator<T,U> gather(Generator<U> other) {
			Generator<T> This = this;
			return new SimplePairGenerator<T,U>(Arrays.asList(this, other)) {
				@Override
				public Pair<T, U> nextValue() throws EndOfGenerationException {
					return new Pair<>(This.next(), other.next());
				}
				@Override
				public boolean isEmpty() {
					return This.isEmpty() || other.isEmpty();
				}};
		}
		@Override default <U,E extends Exception> PairGenerator_<T,U,E> gather(Generator_<U,E> other) {
			return this.<E>toChecked().gather(other);
		}
		@Override default <U,V> TripleGenerator<T,U,V> gatherPair(PairGenerator<U,V> generator) {
			return expandSecond(gather((Generator<Pair<U,V>>) generator));
		}
		@Override default <U,V, E extends Exception> TripleGenerator_<T,U,V,E> gatherPair(PairGenerator_<U,V, E> generator) {
			return this.<E>toChecked().gatherPair(generator);
		}
		@Override default Generator<T> filter(Predicate<? super T> test) {
			Generator<T> This = this;
			return new CustomGenerator<T>(this) {
				@Override
				protected void generate() throws EndOfGenerationException {
					while (true) {
						T t = This.next();
						if (test.test(t))
							yield(t);
					}
				}};
		}
		@Override default Generator<T> append(Generator<? extends T> other) { // TODO test other != this
			Generator<T> This = this;
			return new SimpleSourceGenerator<T>(Arrays.asList(this, other)) {
				Generator<T> current = This;

				@Override
				public T nextValue() throws EndOfGenerationException {
					if (current != null) {
						try {return current.next();}
						catch (EndOfGenerationException e) {current = null;}
					}
					return other.next();
				}

				@Override
				public boolean isEmpty() {
					return current == null && other.isEmpty();
				}
			};
		}
		@Override default T find(Predicate<? super T> test) {
			return fold((T t, T n) -> test.test(n) ? n : t, null);
		}
		@Override default T find(T t) {
			return find(t::equals);
		}
		@Override default Generator<T> async() {
			Generator<T> This = this;
			return new CustomGenerator<T>(this) {
				@Override protected void generate() throws EndOfGenerationException {
					while (true) yield(This.next());
				}};
		}


		/*------ Without exceptions ------*/
		@Override default <U> U fold(BiFunction<? super U, ? super T, ? extends U> function, U init) {
			try {
				while (true)
					init = function.apply(init, next());
			}
			catch (EndOfGenerationException e) {return init;}
		}
		@Override default void forEach(Consumer<? super T> function) {
			try {
				while (true)
					function.accept(next());
			}
			catch (EndOfGenerationException e) {}
		}
		@Override default boolean forAll(Predicate<? super T> test) {
			return fold((Boolean b, T t) -> b && test.test(t),true);
		}
		@Override default boolean forOne(Predicate<? super T> test) {
			return !forAll((T t) -> ! test.test(t));
		}


		/*------ With exceptions ------*/
		@Override default <U, E extends Exception> Generator_<U,E> map_(Function_<? super T, ? extends U, E> function) {
			return this.<E>toChecked().map(function);
		}
		@Override default <U, E extends Exception> PairGenerator_<T,U,E> compute_(Function_<? super T, ? extends U, E> function) {
			return this.<E>toChecked().compute(function);
		}
		@Override default <U,V, E extends Exception> PairGenerator_<U,V,E> biMap_(
				Function_<? super T, ? extends U,E> firstMap,
				Function_<? super T, ? extends V,E> secondMap) {
			return this.<E>toChecked().biMap(firstMap, secondMap);
		}
		@Override default <U,V, E extends Exception> TripleGenerator_<T,U,V,E> biCompute_(
				Function_<? super T, ? extends U,E> firstMap,
				Function_<? super T, ? extends V,E> secondMap) {
			return this.<E>toChecked().biCompute(firstMap, secondMap);
		}
		@Override default <U, E extends Exception> U fold_(BiFunction_<? super U, ? super T, ? extends U, E> function, U init) throws E {
			return this.<E>toChecked().fold(function, init);
		}
		@Override default <E extends Exception> void forEach_(Consumer_<? super T,E> function) throws E {
			this.<E>toChecked().forEach(function);
		}
		@Override default <E extends Exception> boolean forAll_(Predicate_<? super T,E> test) throws E {
			return this.<E>toChecked().forAll(test);
		}
		@Override default <E extends Exception> boolean forOne_(Predicate_<? super T,E> test) throws E {
			return this.<E>toChecked().forOne(test);
		}
	}


	public static interface DefaultGenerator_<T, E extends Exception> extends Generator_<T,E> {

		/*------ Specific ------*/
		@Override default GList<T> toList() throws E {
			GList<T> l = new GList.GArrayList<>();
			try {
				while (true)
					l.add(next());
			}
			catch (EndOfGenerationException e) {}
			return l;
		}
		@Override default PairGenerator_<Integer,T,E> enumerate() {
			final Generator_<T,E> This = this;
			return new SimplePairGenerator_<Integer,T,E>(this) {
				int k = 0;
				@Override
				public Pair<Integer, T> nextValue() throws EndOfGenerationException, E {
					return new Pair<>(k++, This.next());
				}
				@Override
				public boolean isEmpty() {
					return This.isEmpty();
				}};
		}
		@Override default <U> PairGenerator_<T,U,E> cartesianProduct(Generator_<U,E> generator) throws E {
			List<T> values = toList();
			return new CustomPairGenerator_<T,U,E>(generator) {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException, E {
					while (true) {
						U u = generator.next();
						for (T t : values)
							yield(t,u);
					}

				}};
		}
		@Override default <U> PairGenerator_<T,U,E> gather(Generator_<U,E> other) {
			Generator_<T,E> This = this;
			return new SimplePairGenerator_<T,U, E>(Arrays.asList(this, other)) {
				@Override
				public Pair<T, U> nextValue() throws EndOfGenerationException, E {
					return new Pair<>(This.next(), other.next());
				}
				@Override
				public boolean isEmpty() {
					return This.isEmpty() || other.isEmpty();
				}};
		}
		@Override default <U> PairGenerator_<T,U,E> gather(Generator<U> other) {
			return gather(other.toChecked());
		}
		@Override default <U,V> TripleGenerator_<T,U,V,E> gatherPair(PairGenerator<U,V> generator) {
			return gatherPair(generator.toChecked());
		}
		@Override default <U,V> TripleGenerator_<T,U,V,E> gatherPair(PairGenerator_<U,V, E> generator) {
			return expandSecond(gather((Generator_<Pair<U,V>,E>) generator));
		}
		@Override default Generator_<T,E> filter(Predicate_<? super T, E> test) {
			Generator_<T,E> This = this;
			return new CustomGenerator_<T,E>(this) {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException, E {
					while (true) {
						T t = This.next();
						if (test.test(t))
							yield(t);
					}
				}};
		}
		@Override default Generator_<T,E> append(Generator_<? extends T,E> other) {
			Generator_<T,E> This = this;
			return new SimpleSourceGenerator_<T,E>(Arrays.asList(this, other)) {
				Generator_<T,E> current = This;

				@Override
				public T nextValue() throws EndOfGenerationException, E {
					if (current != null) {
						try {return current.next();}
						catch (EndOfGenerationException e) {current = null;}
					}
					return other.next();
				}

				@Override
				public boolean isEmpty() {
					return current == null && other.isEmpty();
				}
			};
		}
		@Override default T find(Predicate_<? super T,E> test) throws E {
			return fold((T t, T n) -> test.test(n) ? n : t, null);
		}
		@Override default T find(T t) throws E {
			return find(t::equals);
		}
		@Override default Generator_<T,E> async() {
			Generator_<T,E> This = this;
			return new CustomGenerator_<T,E>(this) {
				@Override protected void generate() throws EndOfGenerationException,E {
					while (true) yield(This.next());
				}};
		}


		/*------ With exceptions ------*/
		@Override default <U> U fold(BiFunction_<? super U, ? super T, ? extends U, E> function, U init) throws E {
			try {
				while (true)
					init = function.apply(init, next());
			}
			catch (EndOfGenerationException e) {return init;}
		}
		@Override default void forEach(Consumer_<? super T,E> function) throws E {
			try {
				while (true)
					function.accept(next());
			}
			catch (EndOfGenerationException e) {}
			return;
		}
		@Override default boolean forAll(Predicate_<? super T,E> test) throws E {
			return fold((Boolean b, T t) -> b && test.test(t),true);
		}
		@Override default boolean forOne(Predicate_<? super T,E> test) throws E {
			return !forAll((T t) -> ! test.test(t));
		}


		/*------ Conversion ------*/
		@Override default Generator<T> check() throws E {
			return Generators.fromCollection(toList());
		}
	}



	private static interface DefaultPairGenerator<S,T> extends PairGenerator<S,T>, DefaultGenerator<Pair<S,T>> {
		@Override default GPairList<S,T> toList() {
			GPairList<S,T> l = new GPairList.GPairArrayList<>();
			try {
				while (true)
					l.add(next());
			}
			catch (EndOfGenerationException e) {}
			return l;
		}

		@Override default <U> TripleGenerator<S,T,U> addComponent(Generator<U> generator) {
			return expandFirst(gather(generator));
		}
		@Override default <U, E extends Exception> TripleGenerator_<S,T,U,E> addComponent(Generator_<U, E> generator) {
			return expandFirst(gather(generator));
		}

		/*------ Without exceptions ------*/
		@Override default <U, V> PairGenerator<U, V> map(
				Function<? super S, ? extends U> firstMap,
				Function<? super T, ? extends V> secondMap) {
			return this.<U>mapFirst(firstMap).mapSecond(secondMap);
		}
		@Override default <U> Generator<U> map(BiFunction<? super S, ? super T, ? extends U> function) {
			return map(function.toFunction());
		}
		@Override default <U,V> PairGenerator<U,V> biMap(
				BiFunction<? super S, ? super T, ? extends U> firstMap,
				BiFunction<? super S, ? super T, ? extends V> secondMap) {
			return biMap(firstMap.toFunction(), secondMap.toFunction());
		}
		@Override default <A,B,C,D,U,V> PairGenerator<U,V> crossMap(
				Function<? super S, A> arg00, Function<? super T, C> arg01, BiFunction<? super A, ? super C, ? extends U> map0,
				Function<? super S, B> arg10, Function<? super T, D> arg11, BiFunction<? super B, ? super D, ? extends V> map1) {
			return biMap(map0.p(arg00, arg01).toFunction(), map1.p(arg10, arg11).toFunction());
		}
		@Override default <A,B,U> TripleGenerator<S,U,T> compute(
				Function<? super S, A> arg0, Function<? super T, B> arg1, BiFunction<? super A, ? super B, ? extends U> function) {
			return compute(function.p(arg0, arg1));
		}
		@Override default void forEach(BiConsumer<? super S, ? super T> function) {
			forEach(function.toConsumer());
		}
		@Override default boolean forAll(BiPredicate<? super S, ? super T> test) {
			return forAll(test.toPredicate());
		}
		@Override default boolean forAll(Predicate<? super S> firstTest, Predicate<? super T> secondTest) {
			return forAll((S s,T t) -> firstTest.test(s) && secondTest.test(t));
		}
		@Override default PairGenerator<S,T> filter(BiPredicate<? super S, ? super T> test) {
			return Generators.toPairGenerator(filter((Pair<S,T> p) -> test.test(p.first, p.second)));
		}
		@Override default PairGenerator<S,T> filter(Predicate<? super S> firstTest, Predicate<? super T> secondTest) {
			return Generators.toPairGenerator(filter((Pair<S,T> p) -> firstTest.test(p.first) && secondTest.test(p.second)));
		}
		@Override default PairGenerator<S,T> async() {
			PairGenerator<S,T> This = this;
			return new CustomPairGenerator<S,T>(this) {
				@Override
				protected void generate() throws EndOfGenerationException {
					while (true) yield(This.next());
				}};
		}
		@Override default PairGenerator<S, T> appendPair(PairGenerator<? extends S, ? extends T> other) { // TODO test other != this
			return toPairGenerator(append(other.map((S s) -> s, (T t) -> t)));
		}
		/*------ With exceptions ------*/
		@Override default <U, E extends Exception> PairGenerator_<U,T,E> mapFirst_(Function_<? super S, ? extends U,E> function) {
			return this.<E>toChecked().mapFirst(function);
		}
		@Override default <U, E extends Exception> PairGenerator_<U,T,E> computeFirst_(BiFunction_<? super S, ? super T, ? extends U,E> function) {
			return this.<E>toChecked().computeFirst(function);
		}
		@Override default <U, E extends Exception> PairGenerator_<S,U,E> mapSecond_(Function_<? super T, ? extends U,E> function) {
			return this.<E>toChecked().mapSecond(function);
		}
		@Override default <U, E extends Exception> PairGenerator_<S,U,E> computeSecond_(BiFunction_<? super S, ? super T, ? extends U,E> function) {
			return this.<E>toChecked().computeSecond(function);
		}
		@Override default <U,V, E extends Exception> PairGenerator_<U,V,E> map_(
				Function_<? super S, ? extends U,E> firstMap,
				Function_<? super T, ? extends V,E> secondMap) {
			return this.<E>toChecked().map(firstMap, secondMap);
		}
		@Override default <U, E extends Exception> Generator_<U,E> map_(BiFunction_<? super S, ? super T, ? extends U, E> function) {
			return this.<E>toChecked().map(function);
		}
		@Override default <U,V, E extends Exception> PairGenerator_<U,V,E> biMap_(
				BiFunction_<? super S, ? super T, ? extends U, E> firstMap,
				BiFunction_<? super S, ? super T, ? extends V, E> secondMap) {
			return this.<E>toChecked().biMap(firstMap, secondMap);
		}
		@Override default <A,B,C,D,U,V, E extends Exception> PairGenerator_<U,V,E> crossMap_(
				Function_<? super S, A, E> arg00, Function_<? super T, C, E> arg01, BiFunction_<? super A, ? super C, ? extends U ,E> map0,
				Function_<? super S, B, E> arg10, Function_<? super T, D, E> arg11, BiFunction_<? super B, ? super D, ? extends V, E> map1) {
			return this.<E>toChecked().crossMap(arg00, arg01, map0, arg10, arg11, map1);
		}
		@Override default <U, E extends Exception> TripleGenerator_<S,U,T,E> compute_(BiFunction_<? super S, ? super T, ? extends U,E> function) {
			return this.<E>toChecked().compute(function);
		}
		@Override default <A,B,U, E extends Exception> TripleGenerator_<S,U,T,E> compute_(
				Function_<? super S, A, E> arg0, Function_<? super T, B, E> arg1, BiFunction_<? super A, ? super B, ? extends U,E> function) {
			return this.<E>toChecked().compute(arg0, arg1, function);
		}
		@Override default <E extends Exception> void forEach_(BiConsumer_<? super S, ? super T,E> function) throws E {
			this.<E>toChecked().forEach(function);
		}
		@Override default <E extends Exception> boolean forAll_(BiPredicate_<? super S, ? super T,E> test) throws E {
			return this.<E>toChecked().forAll(test);
		}
		@Override default <E extends Exception> boolean forAll_(Predicate_<? super S, E> firstTest, Predicate_<? super T, E> secondTest) throws E {
			return this.<E>toChecked().forAll(firstTest, secondTest);
		}
	}

	public static interface DefaultPairGenerator_<S,T, E extends Exception> extends PairGenerator_<S,T,E>, DefaultGenerator_<Pair<S,T>,E> {
		@Override default GPairList<S,T> toList() throws E {
			GPairList<S,T> l = new GPairList.GPairArrayList<>();
			try {
				while (true)
					l.add(next());
			}
			catch (EndOfGenerationException e) {}
			return l;
		}

		@Override default <U> TripleGenerator_<S,T,U,E> addComponent(Generator<U> generator) {
			return expandFirst(gather(generator));
		}
		@Override default <U> TripleGenerator_<S,T,U,E> addComponent(Generator_<U, E> generator) {
			return expandFirst(gather(generator));
		}

		/*------ With exceptions ------*/
		@Override default <U, V> PairGenerator_<U, V,E> map(
				Function_<? super S, ? extends U, E> firstMap,
				Function_<? super T, ? extends V, E> secondMap) {
			return this.<U>mapFirst(firstMap).mapSecond(secondMap);
		}
		@Override default <U> Generator_<U,E> map(BiFunction_<? super S, ? super T, ? extends U, E> function) {
			return map(function.toFunction());
		}
		@Override default <U,V> PairGenerator_<U,V,E> biMap(
				BiFunction_<? super S, ? super T, ? extends U, E> firstMap,
				BiFunction_<? super S, ? super T, ? extends V, E> secondMap) {
			return biMap(firstMap.toFunction(), secondMap.toFunction());
		}
		@Override default <A,B,C,D,U,V> PairGenerator_<U,V,E> crossMap(
				Function_<? super S, A, E> arg00, Function_<? super T, C, E> arg01, BiFunction_<? super A, ? super C, ? extends U ,E> map0,
				Function_<? super S, B, E> arg10, Function_<? super T, D, E> arg11, BiFunction_<? super B, ? super D, ? extends V, E> map1) {
			return biMap(map0.p(arg00, arg01).toFunction(), map1.p(arg10, arg11).toFunction());
		}
		@Override default <A,B,U> TripleGenerator_<S,U,T,E> compute(
				Function_<? super S, A, E> arg0, Function_<? super T, B, E> arg1, BiFunction_<? super A, ? super B, ? extends U,E> function) {
			return compute(function.p(arg0, arg1));
		}
		@Override default void forEach(BiConsumer_<? super S,? super T,E> function) throws E {
			forEach(function.toConsumer());
		}
		@Override default boolean forAll(BiPredicate_<? super S,? super T,E> test) throws E {
			return forAll(test.toPredicate());
		}
		@Override default boolean forAll(Predicate_<? super S,E> firstTest, Predicate_<? super T,E> secondTest) throws E {
			return forAll((S s,T t) -> firstTest.test(s) && secondTest.test(t));
		}
		@Override default PairGenerator_<S,T,E> filter(BiPredicate_<? super S, ? super T,E> test) {
			return Generators.toPairGenerator(filter((Pair<S,T> p) -> test.test(p.first, p.second)));
		}
		@Override default PairGenerator_<S,T,E> filter(Predicate_<? super S,E> firstTest, Predicate_<? super T,E> secondTest) {
			return Generators.toPairGenerator(filter((Pair<S,T> p) -> firstTest.test(p.first) && secondTest.test(p.second)));
		}
		@Override default PairGenerator<S,T> check() throws E {
			return Generators.fromPairCollection(toList());
		}
		@Override default PairGenerator_<S,T,E> async() {
			PairGenerator_<S,T,E> This = this;
			return new CustomPairGenerator_<S,T,E>(this) {
				@Override
				protected void generate() throws EndOfGenerationException,E {
					while (true) yield(This.next());
				}};
		}
	}



	public static interface DefaultTripleGenerator<S,T,U> extends TripleGenerator<S,T,U>, DefaultGenerator<Triple<S,T,U>> {
		@Override default GTripleList<S,T,U> toList() {
			GTripleList<S,T,U> l = new GTripleList.GTripleArrayList<>();
			try {
				while (true)
					l.add(next());
			}
			catch (EndOfGenerationException e) {}
			return l;
		}

		/*------ Without exceptions ------*/

		@Override default <V,W,X> TripleGenerator<V,W,X> map(
				Function<? super S, ? extends V> firstMap,
				Function<? super T, ? extends W> secondMap,
				Function<? super U, ? extends X> thirdMap) {
			return this.<V>mapFirst(firstMap).<W>mapSecond(secondMap).mapThird(thirdMap);
		}
		@Override default <V> Generator<V> map(TriFunction<? super S, ? super T, ? super U, ? extends V> function) {
			return map(function.toFunction());
		}
		@Override default TripleGenerator<S,T,U> async() {
			TripleGenerator<S,T,U> This = this;
			return new CustomTripleGenerator<S,T,U>(this) {
				@Override
				protected void generate() throws EndOfGenerationException {
					while (true) yield(This.next());
				}};
		}


		/*------ With exceptions ------*/

		@Override default <V, E extends Exception> TripleGenerator_<V,T,U,E> mapFirst_(Function_<? super S, ? extends V, E> function) {
			return this.<E>toChecked().mapFirst(function);
		}
		@Override default <V, E extends Exception> TripleGenerator_<S,V,U,E> mapSecond_(Function_<? super T, ? extends V, E> function) {
			return this.<E>toChecked().mapSecond(function);
		}
		@Override default <V, E extends Exception> TripleGenerator_<S,T,V,E> mapThird_(Function_<? super U, ? extends V, E> function) {
			return this.<E>toChecked().mapThird(function);
		}

		@Override default <V, E extends Exception> PairGenerator_<V,U,E> map12_(BiFunction_<? super S, ? super T, ? extends V, E> function) {
			return this.<E>toChecked().map12(function);
		}
		@Override default <V, E extends Exception> PairGenerator_<V,U,E> map21_(BiFunction_<? super T, ? super S, ? extends V, E> function) {
			return this.<E>toChecked().map21(function);
		}
		@Override default <V, E extends Exception> PairGenerator_<S,V,E> map23_(BiFunction_<? super T, ? super U, ? extends V, E> function) {
			return this.<E>toChecked().map23(function);
		}
		@Override default <V, E extends Exception> PairGenerator_<S,V,E> map32_(BiFunction_<? super U, ? super T, ? extends V, E> function) {
			return this.<E>toChecked().map32(function);
		}

		@Override default <V,W,X, E extends Exception> TripleGenerator_<V,W,X,E> map_(
				Function_<? super S, ? extends V,E> firstMap,
				Function_<? super T, ? extends W,E> secondMap,
				Function_<? super U, ? extends X,E> thirdMap) {
			return this.<E>toChecked().map(firstMap, secondMap, thirdMap);
		}
		@Override default <V, E extends Exception> Generator_<V,E> map_(TriFunction_<? super S, ? super T, ? super U, ? extends V, E> function) {
			return this.<E>toChecked().map(function);
		}

	}



	public static interface DefaultTripleGenerator_<S,T,U, E extends Exception> extends TripleGenerator_<S,T,U,E>, DefaultGenerator_<Triple<S,T,U>,E> {
		@Override default GTripleList<S,T,U> toList() throws E {
			GTripleList<S,T,U> l = new GTripleList.GTripleArrayList<>();
			try {
				while (true)
					l.add(next());
			}
			catch (EndOfGenerationException e) {}
			return l;
		}

		@Override default TripleGenerator_<S,T,U,E> async() {
			TripleGenerator_<S,T,U,E> This = this;
			return new CustomTripleGenerator_<S,T,U,E>(this) {
				@Override
				protected void generate() throws EndOfGenerationException,E {
					while (true) yield(This.next());
				}};
		}

		/*------ With exceptions ------*/
		@Override default <V,W,X> TripleGenerator_<V,W,X,E> map(
				Function_<? super S, ? extends V,E> firstMap,
				Function_<? super T, ? extends W,E> secondMap,
				Function_<? super U, ? extends X,E> thirdMap) {
			return this.<V>mapFirst(firstMap).<W>mapSecond(secondMap).mapThird(thirdMap);
		}
		@Override default <V> Generator_<V,E> map(TriFunction_<? super S, ? super T, ? super U, ? extends V, E> function) {
			return map(function.toFunction());
		}

		/*------ Conversion ------*/
		@Override default TripleGenerator<S,T,U> check() throws E {
			return Generators.fromTripleCollection(toList());

		}
	}



	private static interface SourceGenerator<T> extends DefaultGenerator<T> {
		@Override default <U> Generator<U> map(Function<? super T, ? extends U> function) {
			return new Mapper<>(this, function);
		}
		@Override default <U,V> PairGenerator<U,V> biMap(Function<? super T, ? extends U> firstMap, Function<? super T, ? extends V> secondMap) {
			return new PairMapper<>(this, firstMap, secondMap);
		}
		@Override default <E extends Exception> Generator_<T, E> toChecked() {
			return new Mapper_<>(this, (T t) -> t);
		}
		@Override default <U> PairGenerator<T,U> compute(Function<? super T, ? extends U> function) {
			return biMap(t->t, function);
		}
		@Override default <U,V> TripleGenerator<T, U,V> biCompute(Function<? super T, ? extends U> firstMap, Function<? super T, ? extends V> secondMap) {
			return new TripleMapper<>(this, t -> t, firstMap, secondMap);
		}
	}

	private static interface SourceGenerator_<T, E extends Exception> extends DefaultGenerator_<T, E> {
		@Override default <U> Generator_<U,E> map(Function_<? super T, ? extends U, E> function) {
			return new Mapper_<>(this, function);
		}
		@Override default <U,V> PairGenerator_<U,V,E> biMap(Function_<? super T, ? extends U,E> firstMap, Function_<? super T, ? extends V,E> secondMap) {
			return new PairMapper_<>(this, firstMap, secondMap);
		}
		@Override default <U> PairGenerator_<T,U,E> compute(Function_<? super T, ? extends U, E> function) {
			return biMap(t->t, function);
		}
		@Override default <U,V> TripleGenerator_<T, U,V,E> biCompute(Function_<? super T, ? extends U,E> firstMap, Function_<? super T, ? extends V,E> secondMap) {
			return new TripleMapper_<>(this, t -> t, firstMap, secondMap);
		}
	}


	private static interface PairSourceGenerator<S,T> extends DefaultPairGenerator<S, T>, SourceGenerator<Pair<S,T>> {

		default PairMapper<Pair<S,T>, S,T> toMapper() {
			return new PairMapper<>(this, Pair::getFirst, Pair::getSecond);
		}


		/*------ Specific ------*/
		@Override default TripleGenerator<S,S,T> duplicateFirst() {
			return toMapper().duplicateFirst();
		}
		@Override default TripleGenerator<S,T,T> duplicateSecond() {
			return toMapper().duplicateSecond();
		}

		/*------ Without exceptions ------*/
		@Override default <U,V> PairGenerator<U,V> map(
				Function<? super S, ? extends U> firstMap,
				Function<? super T, ? extends V> secondMap) {
			return toMapper().map(firstMap, secondMap);
		}
		@Override default Generator<S> takeFirst() {
			return toMapper().takeFirst();
		}
		@Override default Generator<T> takeSecond() {
			return toMapper().takeSecond();
		}
		@Override default PairGenerator<T,S> swap() {
			return toMapper().swap();
		}
		@Override default <U> PairGenerator<U, T> mapFirst(Function<? super S, ? extends U> function) {
			return toMapper().mapFirst(function);
		}
		@Override default <U> PairGenerator<U, T> computeFirst(BiFunction<? super S, ? super T, ? extends U> function) {
			return toMapper().computeFirst(function);
		}
		@Override default <U> PairGenerator<S, U> mapSecond(Function<? super T, ? extends U> function) {
			return toMapper().mapSecond(function);
		}
		@Override default <U> PairGenerator<S, U> computeSecond(BiFunction<? super S, ? super T, ? extends U> function) {
			return toMapper().computeSecond(function);
		}
		@Override default <U> Generator<U> map(BiFunction<? super S, ? super T, ? extends U> function) {
			return toMapper().map(function);
		}
		@Override default <U> TripleGenerator<S,U,T> compute(BiFunction<? super S, ? super T, ? extends U> function) {
			return toMapper().compute(function);
		}


		/*------ Conversion ------*/
		@Override default <E extends Exception> PairGenerator_<S,T,E> toChecked() {
			return toMapper().toChecked();
		}
	}


	private static interface PairSourceGenerator_<S,T,E extends Exception> extends DefaultPairGenerator_<S, T, E>, SourceGenerator_<Pair<S,T>,E> {

		default PairMapper_<Pair<S,T>, S,T,E> toMapper() {
			return new PairMapper_<>(this, Pair::getFirst, Pair::getSecond);
		}

		/*------ Specific ------*/
		@Override default TripleGenerator_<S,S,T,E> duplicateFirst() {
			return toMapper().duplicateFirst();
		}
		@Override default TripleGenerator_<S,T,T,E> duplicateSecond() {
			return toMapper().duplicateSecond();
		}


		/*------ With exceptions ------*/
		@Override default Generator_<S, E> takeFirst() {
			return toMapper().takeFirst();
		}
		@Override default Generator_<T, E> takeSecond() {
			return toMapper().takeSecond();
		}
		@Override default PairGenerator_<T, S, E> swap() {
			return toMapper().swap();
		}
		@Override default <U> PairGenerator_<U,T,E> mapFirst(Function_<? super S, ? extends U,E> function) {
			return toMapper().mapFirst(function);
		}
		@Override default <U> PairGenerator_<U,T,E> computeFirst(BiFunction_<? super S, ? super T, ? extends U,E> function) {
			return toMapper().computeFirst(function);
		}
		@Override default <U> PairGenerator_<S,U,E> mapSecond(Function_<? super T, ? extends U,E> function) {
			return toMapper().mapSecond(function);
		}
		@Override default <U> PairGenerator_<S,U,E> computeSecond(BiFunction_<? super S, ? super T, ? extends U,E> function) {
			return toMapper().computeSecond(function);
		}
		@Override default <U,V> PairGenerator_<U,V,E> map(
				Function_<? super S, ? extends U,E> firstMap,
				Function_<? super T, ? extends V,E> secondMap) {
			return toMapper().map(firstMap, secondMap);
		}
		@Override default <U> Generator_<U,E> map(BiFunction_<? super S, ? super T, ? extends U,E> function) {
			return toMapper().map(function);
		}
		@Override default <U> TripleGenerator_<S,U,T,E> compute(BiFunction_<? super S, ? super T, ? extends U,E> function) {
			return toMapper().compute(function);
		}

	}



	private static interface TripleSourceGenerator<S,T,U> extends DefaultTripleGenerator<S,T,U>, SourceGenerator<Triple<S,T,U>> {

		default TripleMapper<Triple<S,T,U>, S, T, U> toMapper() {
			return new TripleMapper<>(this,Triple::getFirst, Triple::getSecond, Triple::getThird);
		}

		/*------ Specific ------*/
		@Override default Generator<S> takeFirst() {
			return toMapper().takeFirst();
		}
		@Override default Generator<T> takeSecond() {
			return toMapper().takeSecond();
		}
		@Override default Generator<U> takeThird() {
			return toMapper().takeThird();
		}

		@Override default PairGenerator<T,U> dropFirst() {
			return toMapper().dropFirst();
		}
		@Override default PairGenerator<S,U> dropSecond() {
			return toMapper().dropSecond();
		}
		@Override default PairGenerator<S,T> dropThird() {
			return toMapper().dropThird();
		}

		@Override default PairGenerator<Pair<S,T>,U> gatherFirst() {
			return toMapper().gatherFirst();
		}
		@Override default PairGenerator<S,Pair<T,U>> gatherLast() {
			return toMapper().gatherLast();
		}

		@Override default TripleGenerator<T,S,U> swap12() {
			return toMapper().swap12();
		}
		@Override default TripleGenerator<S,U,T> swap23() {
			return toMapper().swap23();
		}
		@Override default TripleGenerator<U,T,S> swap13() {
			return toMapper().swap13();
		}
		@Override default TripleGenerator<T,U,S> rotate() {
			return toMapper().rotate();
		}


		/*------ Without exceptions ------*/
		@Override default <V> TripleGenerator<V,T,U> mapFirst(Function<? super S, ? extends V> function) {
			return toMapper().mapFirst(function);
		}
		@Override default <V> TripleGenerator<S,V,U> mapSecond(Function<? super T, ? extends V> function) {
			return toMapper().mapSecond(function);
		}
		@Override default <V> TripleGenerator<S,T,V> mapThird(Function<? super U, ? extends V> function) {
			return toMapper().mapThird(function);
		}

		@Override default <V> PairGenerator<V,U> map12(BiFunction<? super S, ? super T, ? extends V> function) {
			return toMapper().map12(function);
		}
		@Override default <V> PairGenerator<V,U> map21(BiFunction<? super T, ? super S, ? extends V> function) {
			return toMapper().map21(function);
		}
		@Override default <V> PairGenerator<S,V> map23(BiFunction<? super T, ? super U, ? extends V> function) {
			return toMapper().map23(function);
		}
		@Override default <V> PairGenerator<S,V> map32(BiFunction<? super U, ? super T, ? extends V> function) {
			return toMapper().map32(function);
		}



		/*------ Conversion ------*/
		@Override
		default <E extends Exception> TripleGenerator_<S,T,U,E> toChecked() {
			return new TripleMapper_<>(this,Triple::getFirst, Triple::getSecond, Triple::getThird);
		}

	}





	private static interface TripleSourceGenerator_<S,T,U,E extends Exception> extends DefaultTripleGenerator_<S,T,U,E>, SourceGenerator_<Triple<S,T,U>,E> {

		default TripleMapper_<Triple<S,T,U>, S, T, U, E> toMapper() {
			return new TripleMapper_<>(this, Triple::getFirst, Triple::getSecond, Triple::getThird);
		}


		/*------ Specific ------*/

		@Override default Generator_<S,E> takeFirst() {
			return toMapper().takeFirst();
		}
		@Override default Generator_<T,E> takeSecond() {
			return toMapper().takeSecond();
		}
		@Override default Generator_<U,E> takeThird() {
			return toMapper().takeThird();
		}

		@Override default PairGenerator_<T,U,E> dropFirst() {
			return toMapper().dropFirst();
		}
		@Override default PairGenerator_<S,U,E> dropSecond() {
			return toMapper().dropSecond();
		}
		@Override default PairGenerator_<S,T,E> dropThird() {
			return toMapper().dropThird();
		}

		@Override default PairGenerator_<Pair<S,T>,U,E> gatherFirst() {
			return toMapper().gatherFirst();
		}
		@Override default PairGenerator_<S,Pair<T,U>,E> gatherLast() {
			return toMapper().gatherLast();
		}

		@Override default TripleGenerator_<T,S,U,E> swap12() {
			return toMapper().swap12();
		}
		@Override default TripleGenerator_<S,U,T,E> swap23() {
			return toMapper().swap23();
		}
		@Override default TripleGenerator_<U,T,S,E> swap13() {
			return toMapper().swap13();
		}
		@Override default TripleGenerator_<T,U,S,E> rotate() {
			return toMapper().rotate();
		}


		/*------ With exceptions ------*/

		@Override default <V> TripleGenerator_<V,T,U,E> mapFirst(Function_<? super S, ? extends V,E> function) {
			return toMapper().mapFirst(function);
		}
		@Override default <V> TripleGenerator_<S,V,U,E> mapSecond(Function_<? super T, ? extends V,E> function) {
			return toMapper().mapSecond(function);
		}
		@Override default <V> TripleGenerator_<S,T,V,E> mapThird(Function_<? super U, ? extends V,E> function) {
			return toMapper().mapThird(function);
		}

		@Override default <V> PairGenerator_<V,U,E> map12(BiFunction_<? super S, ? super T, ? extends V, E> function) {
			return toMapper().map12(function);
		}
		@Override default <V> PairGenerator_<V,U,E> map21(BiFunction_<? super T, ? super S, ? extends V, E> function) {
			return toMapper().map21(function);
		}
		@Override default <V> PairGenerator_<S,V,E> map23(BiFunction_<? super T, ? super U, ? extends V, E> function) {
			return toMapper().map23(function);
		}
		@Override default <V> PairGenerator_<S,V,E> map32(BiFunction_<? super U, ? super T, ? extends V, E> function) {
			return toMapper().map32(function);
		}

	}












	/*------ Implementation ------*/


	private static abstract class SimpleAbstractGenerator<T> implements AbstractGenerator<T> {
		private final List<AbstractGenerator<?>> parents;
		private volatile boolean done = false;

		public SimpleAbstractGenerator() {
			this.parents = Collections.emptyList();
//			System.out.println("Deb "+this);
		}

		public SimpleAbstractGenerator(AbstractGenerator<?> parent) {
			this.parents = Collections.singletonList(parent);
//			System.out.println("Deb "+this);
		}
		public SimpleAbstractGenerator(List<AbstractGenerator<?>> parents) {
			this.parents = parents;
//			System.out.println("Deb "+this);
		}

		protected final EndOfGenerationException generationFinished() {
			done = true;
			stopGeneration();
			return new EndOfGenerationException();
		}

		public final boolean isDone() {
			return done;
		}

		protected void interrupt(){}

		@Override
		public void stopGeneration() { // TODO : pas au dessus ?
			interrupt();
			for (AbstractGenerator<?> g : parents) g.stopGeneration();
//			Generators.fromCollection(parents).forEach(AbstractGenerator<?>::stopGeneration);
		}
	}

	private static abstract class SimpleGenerator<T> extends SimpleAbstractGenerator<T> implements DefaultGenerator<T> {
		public SimpleGenerator() {}
		public SimpleGenerator(AbstractGenerator<?> parent) {super(parent);}
		public SimpleGenerator(List<AbstractGenerator<?>> parents) {super(parents);}

		@Override
		public final T next() throws EndOfGenerationException {
			if (isDone())
				throw new EndOfGenerationException();
			try{return nextValue();}
			catch (EndOfGenerationException e) {throw generationFinished();}
		}

		protected abstract T nextValue() throws EndOfGenerationException;
	}
	private static abstract class SimpleSourceGenerator<T> extends SimpleGenerator<T> implements SourceGenerator<T> {
		public SimpleSourceGenerator() {}
		public SimpleSourceGenerator(AbstractGenerator<?> parent) {super(parent);}
		public SimpleSourceGenerator(List<AbstractGenerator<?>> parents) {super(parents);}
	}

	private static abstract class SimpleGenerator_<T, E extends Exception> extends SimpleAbstractGenerator<T> implements DefaultGenerator_<T,E> {
		public SimpleGenerator_() {}
		public SimpleGenerator_(AbstractGenerator<?> parent) {super(parent);}
		public SimpleGenerator_(List<AbstractGenerator<?>> parents) {super(parents);}

		@SuppressWarnings("unchecked")
		@Override
		public final T next() throws EndOfGenerationException, E {
			if (isDone())
				throw new EndOfGenerationException();
			try{return nextValue();}
			catch (EndOfGenerationException e) {throw generationFinished();}
			catch (Exception e) {generationFinished(); throw (E) e;}
		}

		protected abstract T nextValue() throws EndOfGenerationException, E;
	}

	private static abstract class SimpleSourceGenerator_<T, E extends Exception> extends SimpleGenerator_<T,E> implements SourceGenerator_<T,E> {
		public SimpleSourceGenerator_() {}
		public SimpleSourceGenerator_(AbstractGenerator<?> parent) {super(parent);}
		public SimpleSourceGenerator_(List<AbstractGenerator<?>> parents) {super(parents);}
}






	private static class Mapper<S,T> extends SimpleGenerator<T> {
		protected final Generator<S> source;
		private final Function<? super S,? extends T> map;

		public Mapper(Generator<S> source, Function<? super S,? extends T> map) {
			super(source);
			this.source = source;
			this.map = map;
		}
		@Override
		public final boolean isEmpty() {
			return source.isEmpty();
		}

		@Override
		public T nextValue() throws EndOfGenerationException {
			return map.apply(source.next());
		}

		@Override
		public final <U> Generator<U> map(Function<? super T, ? extends U> function) {
			return new Mapper<>(source, map.o(function));
		}

		@Override
		public final <U> PairGenerator<T,U> compute(Function<? super T, ? extends U> function) {
			return new PairMapper<>(source, map, map.o(function));
		}

		@Override
		public <E extends Exception> Generator_<T, E> toChecked() {
			return new Mapper_<>(source.toChecked(), map.toChecked());
		}

		@Override
		public final <U,V> PairGenerator<U,V> biMap(Function<? super T, ? extends U> firstMap, Function<? super T, ? extends V> secondMap) {
			return new PairMapper<>(source, firstMap.p(map), secondMap.p(map));
		}

		@Override
		public final <U,V> TripleGenerator<T,U,V> biCompute(Function<? super T, ? extends U> firstMap, Function<? super T, ? extends V> secondMap) {
			return new TripleMapper<>(source, map, firstMap.p(map), secondMap.p(map));
		}
	}

	private static class Mapper_<S,T,E extends Exception> extends SimpleGenerator_<T,E> {
		protected final AbstractGenerator<S> generator;
		protected final GeneratorSupplier_<S,E> source;
		private final Function_<? super S,? extends T,E> map;

		public Mapper_(AbstractGenerator<S> generator, GeneratorSupplier_<S,E> source,
				Function_<? super S,? extends T,E> map) {
			super(generator);
			this.source = source;
			this.generator = generator;
			this.map = map;
		}
		public Mapper_(Generator_<S,E> source, Function_<? super S,? extends T,E> map) {
			this(source, source::next, map);
		}
		public Mapper_(Generator<S> source, Function_<? super S,? extends T,E> map) {
			this(source, source::next, map);
		}

		@Override
		public final boolean isEmpty() {
			return generator.isEmpty();
		}

		@Override
		public T nextValue() throws EndOfGenerationException, E {
			return map.apply(source.get());
		}

		@Override
		public final <U> Generator_<U,E> map(Function_<? super T, ? extends U, E> function) {
			return new Mapper_<>(generator, source, map.o(function));
		}
		@Override
		public final <U> PairGenerator_<T, U, E> compute(Function_<? super T, ? extends U, E> function) {
			return new PairMapper_<>(generator, source, map, function.p(map));
		}
		@Override
		public final <U, V> PairGenerator_<U, V, E> biMap(Function_<? super T, ? extends U, E> firstMap,
				Function_<? super T, ? extends V, E> secondMap) {
			return new PairMapper_<>(generator,  source, firstMap.p(map), secondMap.p(map));
		}
		@Override
		public final <U, V> TripleGenerator_<T, U, V, E> biCompute(Function_<? super T, ? extends U, E> firstMap,
				Function_<? super T, ? extends V, E> secondMap) {
			return new TripleMapper_<>(generator,  source, map, firstMap.p(map), secondMap.p(map));
		}
	}








	private static abstract class DefaultAbstractGenerator<T> extends SimpleAbstractGenerator<T> implements Runnable {
		private final Thread thread = new Thread(this);
		private volatile T value;
		private volatile boolean initialized = false;
		private volatile boolean ready = false;
		private volatile RuntimeException exception = null;

		public DefaultAbstractGenerator() {
			super();
			thread.start();
		}
		public DefaultAbstractGenerator(AbstractGenerator<?> parent) {
			super(parent);
			thread.start();
		}
		public DefaultAbstractGenerator(List<AbstractGenerator<?>> parents) {
			super(parents);
			thread.start();
		}

		protected abstract void generateValues() throws EndOfGenerationException;

		protected final void yield(T value) throws EndOfGenerationException {
//			System.out.println(">> Yield début "+this);
			this.value = value;
			ready = true;
			while (ready) {
				try {Thread.sleep(0,300);}
				catch (InterruptedException e) {throw generationFinished();}
			}
//			System.out.println(">> Yield fin   "+this);

		}

		protected void initializationFinished() {
			initialized = true;
		}

		@Override
		public final boolean isEmpty() {
			waitValue();
			return isDone();
		}

		@Override
		public final void run() {
			while (!initialized){}
			try {generateValues();}
			catch (RuntimeException e) {exception = e;}
			catch (EndOfGenerationException e) {}
//			System.out.println("Sortie ");
//			System.out.println("Je meurs "+this);
			ready = true;
			generationFinished();
		}

		protected final void waitValue() {
//			System.out.println(">> Wait début "+this);
			if (isDone())
				return;
			while (!ready)
				try {Thread.sleep(0,100);}
				catch (InterruptedException e) {}
//			System.out.println(">> Wait fin   "+this);
		}

		protected final T readValue() throws EndOfGenerationException {
//			System.out.println("Read "+this+" "+exception);
			if (exception != null)
				throw exception;
			if (isEmpty())
				throw new EndOfGenerationException();
			return value;
		}

		protected final T getValue() throws EndOfGenerationException {
//			System.out.println("Read value "+this);
			T t = readValue();
			ready = false;
			return t;
		}

		@Override
		protected final void interrupt() {
			thread.interrupt();
		}
	}


	private static abstract class DefaultThreadGenerator<T> extends DefaultAbstractGenerator<T> implements SourceGenerator<T> {

		protected  DefaultThreadGenerator(List<AbstractGenerator<?>> parent, boolean init) {
			super(parent);
			if (init) initializationFinished();
		}
		public DefaultThreadGenerator(AbstractGenerator<?> parent, boolean init) {
			super(parent);
			if (init) initializationFinished();
		}
		public DefaultThreadGenerator(boolean init) {
			if (init) initializationFinished();
		}

		protected abstract void generate() throws EndOfGenerationException;

		@Override
		protected final void generateValues() throws EndOfGenerationException {
			generate();
		}
		@Override
		public final T next() throws EndOfGenerationException {
//			System.out.println("Next début "+this);
			waitValue();
//			System.out.println("Next fin   "+this);
			return getValue();
		}


		@Override
		public Generator<T> async() {
			return this;
		}
	}



	public static abstract class CustomGenerator<T> extends DefaultThreadGenerator<T> {

		public CustomGenerator(List<? extends AbstractGenerator<?>> parents) {
			super(Generators.fromCollection(parents).map(new Identity<AbstractGenerator<?>>()).toList(), true);
		}
		public CustomGenerator(Generator<?> parent) {
			super(parent, true);
		}
		public CustomGenerator(Generator_<?,?> parent) {
			super(parent, true);
		}
		public CustomGenerator() {
			super(true);
		}
	}



	private static abstract class DefaultThreadGenerator_<T,E extends Exception> extends DefaultAbstractGenerator<T> implements DefaultGenerator_<T,E> {
		private volatile E exception = null;

		protected  DefaultThreadGenerator_(List<AbstractGenerator<?>> parent, boolean init) {
			super(parent);
			if (init) initializationFinished();
		}
		public DefaultThreadGenerator_(AbstractGenerator<?> parent, boolean init) {
			super(parent);
			if (init) initializationFinished();
		}
		public DefaultThreadGenerator_(boolean init) {
			if (init) initializationFinished();
		}

		protected abstract void generate() throws InterruptedException, EndOfGenerationException, E;
		@SuppressWarnings("unchecked")
		@Override
		protected final void generateValues() throws EndOfGenerationException {
			try {generate();}
			catch (RuntimeException e) {throw e;}
			catch (EndOfGenerationException e) {throw e;}
			catch (Exception e) {exception = (E)e;}
		}

		@Override
		public final T next() throws EndOfGenerationException, E {
			waitValue();
			if (exception != null)
				throw exception;
			return getValue();
		}


		@Override
		public Generator_<T,E> async() {
			return this;
		}
	}

	public static abstract class CustomGenerator_<T,E extends Exception> extends DefaultThreadGenerator_<T,E> implements SourceGenerator_<T,E> {

		public CustomGenerator_(List<? extends AbstractGenerator<?>> parents) {
			super(Generators.fromCollection(parents).map(new Identity<AbstractGenerator<?>>()).toList(), true);
		}
		public CustomGenerator_(Generator<?> parent) {
			super(parent,true);
		}
		public CustomGenerator_(Generator_<?,?> parent) {
			super(parent,true);
		}
		public CustomGenerator_() {
			super(true);
		}
	}








	private abstract static class SimplePairGenerator<S,T> extends SimpleGenerator<Pair<S,T>> implements PairSourceGenerator<S, T> {
		public SimplePairGenerator() {}
		public SimplePairGenerator(AbstractGenerator<?> parent) {super(parent);}
		public SimplePairGenerator(List<AbstractGenerator<?>> parents) {super(parents);}

	}

	private static abstract class SimplePairGenerator_<S,T, E extends Exception> extends SimpleGenerator_<Pair<S,T>,E> implements PairSourceGenerator_<S, T, E> {
		public SimplePairGenerator_() {}
		public SimplePairGenerator_(AbstractGenerator<?> parent) {super(parent);}
		public SimplePairGenerator_(List<AbstractGenerator<?>> parents) {super(parents);}
	}










	private static class PairMapper<A,S,T> extends Mapper<A,Pair<S,T>> implements DefaultPairGenerator<S,T> {
		private final Function<? super A,? extends S> firstMap;
		private final Function<? super A,? extends T> secondMap;

		public PairMapper(Generator<A> source, Function<? super A,? extends S> firstMap, Function<? super A,? extends T> secondMap) {
			super(source, (A a) -> new Pair<>(firstMap.apply(a), secondMap.apply(a)));
			this.firstMap = firstMap;
			this.secondMap = secondMap;
		}

		@Override
		public final Pair<S,T> nextValue() throws EndOfGenerationException {
			A a = source.next();
			return new Pair<>(firstMap.apply(a), secondMap.apply(a));
		}

		@Override
		public Generator<S> takeFirst() {
			return new Mapper<>(source, firstMap);
		}
		@Override
		public Generator<T> takeSecond() {
			return new Mapper<>(source, secondMap);
		}
		@Override
		public PairGenerator<T,S> swap() {
			return new PairMapper<>(source, secondMap, firstMap);
		}
		@Override
		public <U> PairGenerator<U,T> mapFirst(Function<? super S, ? extends U> function) {
			return new PairMapper<>(source, function.p(firstMap), secondMap);
		}

		@Override
		public <U> PairGenerator<U,T> computeFirst(BiFunction<? super S, ? super T, ? extends U> function) {
			return new PairMapper<>(source, function.q(firstMap, secondMap), secondMap);
		}

		@Override
		public <U> PairGenerator<S,U> mapSecond(Function<? super T, ? extends U> function) {
			return new PairMapper<>(source, firstMap, function.p(secondMap));
		}


		@Override
		public <U> PairGenerator<S,U> computeSecond(BiFunction<? super S, ? super T, ? extends U> function) {
			return new PairMapper<>(source, firstMap, function.q(firstMap, secondMap));
		}

		@Override
		public <E extends Exception> PairGenerator_<S,T, E> toChecked() {
			return new PairMapper_<>(source, firstMap.toChecked(), secondMap.toChecked());
		}

		@Override public TripleGenerator<S,S,T> duplicateFirst() {
			return new TripleMapper<>(source, firstMap, firstMap, secondMap);
		}
		@Override public TripleGenerator<S,T,T> duplicateSecond() {
			return new TripleMapper<>(source, firstMap, secondMap, secondMap);
		}

		@Override public <U> TripleGenerator<S,U,T> compute(BiFunction<? super S, ? super T, ? extends U> function) {
			return new TripleMapper<>(source, firstMap, function.q(firstMap, secondMap), secondMap);
		}

	}

	private static class PairMapper_<A,S,T,E extends Exception> extends Mapper_<A,Pair<S,T>,E> implements DefaultPairGenerator_<S,T,E> {
		private final Function_<? super A,? extends S,E> firstMap;
		private final Function_<? super A,? extends T,E> secondMap;

		public PairMapper_(AbstractGenerator<A> generator, GeneratorSupplier_<A,E> source,
				Function_<? super A,? extends S,E> firstMap,
				Function_<? super A,? extends T,E> secondMap) {
			super(generator, source, (A a) -> new Pair<>(firstMap.apply(a), secondMap.apply(a)));
			this.firstMap = firstMap;
			this.secondMap = secondMap;
		}
		public PairMapper_(Generator_<A,E> generator,
				Function_<? super A,? extends S,E> firstMap,
				Function_<? super A,? extends T,E> secondMap) {
			this(generator, generator::next, firstMap, secondMap);
		}
		public PairMapper_(Generator<A> generator,
				Function_<? super A,? extends S,E> firstMap,
				Function_<? super A,? extends T,E> secondMap) {
			this(generator, generator::next, firstMap, secondMap);
		}

		@Override public Pair<S,T> nextValue() throws EndOfGenerationException, E {
			A a = source.get();
			return new Pair<>(firstMap.apply(a), secondMap.apply(a));
		}

		@Override public Generator_<S,E> takeFirst() {
			return new Mapper_<>(generator, source, firstMap);
		}
		@Override public Generator_<T,E> takeSecond() {
			return new Mapper_<>(generator, source, secondMap);
		}
		@Override public PairGenerator_<T, S, E> swap() {
			return new PairMapper_<>(generator, source, secondMap, firstMap);
		}
		@Override public <U> PairGenerator_<U,T,E> mapFirst(Function_<? super S, ? extends U,E> function) {
			return new PairMapper_<>(generator, source, function.p(firstMap), secondMap);
		}

		@Override public <U> PairGenerator_<S,U,E> mapSecond(Function_<? super T, ? extends U,E> function) {
			return new PairMapper_<>(generator, source, firstMap, function.p(secondMap));
		}
		@Override public <U> PairGenerator_<U, T, E> computeFirst(BiFunction_<? super S, ? super T, ? extends U, E> function) {
			return new PairMapper_<>(generator, source, function.q(firstMap, secondMap), secondMap);
		}
		@Override public <U> PairGenerator_<S, U, E> computeSecond(BiFunction_<? super S, ? super T, ? extends U, E> function) {
			return new PairMapper_<>(generator, source, firstMap, function.q(firstMap, secondMap));
		}

		@Override public TripleGenerator_<S,S,T,E> duplicateFirst() {
			return new TripleMapper_<>(generator, source, firstMap, firstMap, secondMap);
		}
		@Override public TripleGenerator_<S,T,T,E> duplicateSecond() {
			return new TripleMapper_<>(generator, source, firstMap, secondMap, secondMap);
		}
		@Override public <U> TripleGenerator_<S,U,T,E> compute(BiFunction_<? super S, ? super T, ? extends U,E> function) {
			return new TripleMapper_<>(generator, source, firstMap, function.q(firstMap, secondMap), secondMap);
		}
	}




	public static abstract class CustomPairGenerator<S,T> extends CustomGenerator<Pair<S,T>> implements PairSourceGenerator<S, T> {
		public CustomPairGenerator(List<? extends AbstractGenerator<?>> parents) {
			super(parents);
		}
		public CustomPairGenerator(Generator<?> parent) {
			super(parent);
		}
		public CustomPairGenerator(Generator_<?,?> parent) {
			super(parent);
		}
		public CustomPairGenerator() {}

		protected final void yield(S s, T t) throws EndOfGenerationException {
			yield(new Pair<>(s,t));
		}

		@Override
		public PairGenerator<S,T> async() {
			return this;
		}
	}



	public static abstract class CustomPairGenerator_<S,T,E extends Exception> extends CustomGenerator_<Pair<S,T>,E> implements PairSourceGenerator_<S, T,E> {
		public CustomPairGenerator_(List<? extends AbstractGenerator<?>> parents) {
			super(parents);
		}
		public CustomPairGenerator_(Generator<?> parent) {
			super(parent);
		}
		public CustomPairGenerator_(Generator_<?,?> parent) {
			super(parent);
		}
		public CustomPairGenerator_() {}
		protected final void yield(S s, T t) throws EndOfGenerationException {
			yield(new Pair<>(s,t));
		}

		@Override
		public PairGenerator_<S,T,E> async() {
			return this;
		}
	}
















	private static abstract class SimpleTripleGenerator<S,T,U> extends SimpleGenerator<Triple<S,T,U>> implements TripleSourceGenerator<S, T,U> {
		public SimpleTripleGenerator() {}
		public SimpleTripleGenerator(AbstractGenerator<?> parent) {super(parent);}
		public SimpleTripleGenerator(List<AbstractGenerator<?>> parents) {super(parents);}

	}
	private static abstract class SimpleTripleGenerator_<S,T,U, E extends Exception> extends SimpleGenerator_<Triple<S,T,U>,E> implements TripleSourceGenerator_<S, T, U, E> {
		/*------ Constructors ------*/

		public SimpleTripleGenerator_() {}
		public SimpleTripleGenerator_(AbstractGenerator<?> parent) {super(parent);}
		public SimpleTripleGenerator_(List<AbstractGenerator<?>> parents) {super(parents);}


	}




	public static abstract class CustomTripleGenerator<S,T,U> extends CustomGenerator<Triple<S,T,U>> implements TripleSourceGenerator<S, T, U> {
		public CustomTripleGenerator(List<? extends AbstractGenerator<?>> parents) {
			super(parents);
		}
		public CustomTripleGenerator(Generator<?> parent) {
			super(parent);
		}
		public CustomTripleGenerator(Generator_<?,?> parent) {
			super(parent);
		}
		public CustomTripleGenerator() {}

		protected final void yield(S s, T t, U u) throws EndOfGenerationException {
			yield(new Triple<>(s,t,u));
		}

		@Override
		public TripleGenerator<S,T,U> async() {
			return this;
		}
	}

	public static abstract class CustomTripleGenerator_<S,T,U,E extends Exception> extends CustomGenerator_<Triple<S,T,U>,E> implements TripleSourceGenerator_<S, T, U,E> {
		public CustomTripleGenerator_(List<? extends AbstractGenerator<?>> parents) {
			super(parents);
		}
		public CustomTripleGenerator_(Generator<?> parent) {
			super(parent);
		}
		public CustomTripleGenerator_(Generator_<?,?> parent) {
			super(parent);
		}
		public CustomTripleGenerator_() {}

		protected final void yield(S s, T t, U u) throws EndOfGenerationException {
			yield(new Triple<>(s,t, u));
		}

		@Override
		public TripleGenerator_<S,T,U,E> async() {
			return this;
		}
	}






	private static class TripleMapper<A,S,T,U> extends Mapper<A,Triple<S,T,U>> implements DefaultTripleGenerator<S,T,U> {
		private final Function<? super A,? extends S> firstMap;
		private final Function<? super A,? extends T> secondMap;
		private final Function<? super A,? extends U> thirdMap;

		public TripleMapper(Generator<A> source,
				Function<? super A,? extends S> firstMap,
				Function<? super A,? extends T> secondMap,
				Function<? super A,? extends U> thirdMap) {
			super(source, (A a) -> new Triple<>(firstMap.apply(a), secondMap.apply(a), thirdMap.apply(a)));
			this.firstMap = firstMap;
			this.secondMap = secondMap;
			this.thirdMap = thirdMap;
		}

		@Override
		public final Triple<S,T,U> nextValue() throws EndOfGenerationException {
			A a = source.next();
			return new Triple<>(firstMap.apply(a), secondMap.apply(a), thirdMap.apply(a));
		}

		@Override
		public final Generator<S> takeFirst() {
			return new Mapper<>(source, firstMap);
		}
		@Override
		public final Generator<T> takeSecond() {
			return new Mapper<>(source, secondMap);
		}
		@Override
		public final Generator<U> takeThird() {
			return new Mapper<>(source, thirdMap);
		}

		@Override
		public final PairGenerator<T,U> dropFirst() {
			return new PairMapper<>(source, secondMap, thirdMap);
		}
		@Override
		public final PairGenerator<S,U> dropSecond() {
			return new PairMapper<>(source, firstMap, thirdMap);
		}
		@Override
		public final PairGenerator<S,T> dropThird() {
			return new PairMapper<>(source, firstMap, secondMap);
		}

		@Override
		public final PairGenerator<Pair<S,T>,U> gatherFirst() {
			return new PairMapper<>(source, (A a) -> new Pair<>(firstMap.apply(a), secondMap.apply(a)), thirdMap);
		};
		@Override
		public final PairGenerator<S,Pair<T,U>> gatherLast() {
			return new PairMapper<>(source, firstMap, (A a) -> new Pair<>(secondMap.apply(a), thirdMap.apply(a)));
		};

		@Override
		public final TripleGenerator<T,S,U> swap12() {
			return new TripleMapper<>(source, secondMap, firstMap, thirdMap);
		}
		@Override
		public final TripleGenerator<S,U,T> swap23() {
			return new TripleMapper<>(source, firstMap, thirdMap, secondMap);
		}
		@Override
		public final TripleGenerator<U,T,S> swap13() {
			return new TripleMapper<>(source, thirdMap, secondMap, firstMap);
		}
		@Override
		public TripleGenerator<T,U,S> rotate() {
			return new TripleMapper<>(source, secondMap, thirdMap, firstMap);
		}


		@Override
		public final <V> TripleGenerator<V,T,U> mapFirst(Function<? super S, ? extends V> function) {
			return new TripleMapper<>(source, function.p(firstMap), secondMap, thirdMap);
		}
		@Override
		public final <V> TripleGenerator<S,V,U> mapSecond(Function<? super T, ? extends V> function) {
			return new TripleMapper<>(source, firstMap, function.p(secondMap), thirdMap);
		}
		@Override
		public final <V> TripleGenerator<S,T,V> mapThird(Function<? super U, ? extends V> function) {
			return new TripleMapper<>(source, firstMap, secondMap, function.p(thirdMap));
		}

		@Override
		public final <V> PairGenerator<V,U> map12(BiFunction<? super S, ? super T, ? extends V> function) {
			return new PairMapper<A,V,U>(source, function.q(firstMap, secondMap), thirdMap);
		}
		@Override
		public final <V> PairGenerator<V,U> map21(BiFunction<? super T, ? super S, ? extends V> function) {
			return new PairMapper<>(source, function.q(secondMap, firstMap), thirdMap);
		}
		@Override
		public final <V> PairGenerator<S,V> map23(BiFunction<? super T, ? super U, ? extends V> function) {
			return new PairMapper<>(source, firstMap, function.q(secondMap, thirdMap));
		}
		@Override
		public final <V> PairGenerator<S,V> map32(BiFunction<? super U, ? super T, ? extends V> function) {
			return new PairMapper<>(source, firstMap, function.q(thirdMap, secondMap));
		}

		@Override
		public final <E extends Exception> TripleGenerator_<S,T,U, E> toChecked() {
			return new TripleMapper_<>(source.toChecked(), firstMap.toChecked(), secondMap.toChecked(), thirdMap.toChecked());
		}
	}


	@FunctionalInterface
	private interface GeneratorSupplier_<T,E extends Exception> {
		T get() throws EndOfGenerationException, E;
	}


	private static class TripleMapper_<A,S,T,U,E extends Exception> extends Mapper_<A,Triple<S,T,U>,E> implements DefaultTripleGenerator_<S,T,U,E> {
		private final Function_<? super A,? extends S,E> firstMap;
		private final Function_<? super A,? extends T,E> secondMap;
		private final Function_<? super A,? extends U,E> thirdMap;

		public TripleMapper_(AbstractGenerator<A> generator, GeneratorSupplier_<A,E> source,
				Function_<? super A,? extends S,E> firstMap,
				Function_<? super A,? extends T,E> secondMap,
				Function_<? super A,? extends U,E> thirdMap) {
			super(generator, source, (A a) -> new Triple<>(firstMap.apply(a), secondMap.apply(a), thirdMap.apply(a)));
			this.firstMap = firstMap;
			this.secondMap = secondMap;
			this.thirdMap = thirdMap;
		}
		public TripleMapper_(Generator_<A,E> source,
				Function_<? super A,? extends S,E> firstMap,
				Function_<? super A,? extends T,E> secondMap,
				Function_<? super A,? extends U,E> thirdMap) {
			this(source,source::next,firstMap,secondMap,thirdMap);
		}
		public TripleMapper_(Generator<A> source,
				Function_<? super A,? extends S,E> firstMap,
				Function_<? super A,? extends T,E> secondMap,
				Function_<? super A,? extends U,E> thirdMap) {
			this(source,source::next,firstMap,secondMap,thirdMap);
		}

		@Override
		public Triple<S,T,U> nextValue() throws EndOfGenerationException, E {
			A a = source.get();
			return new Triple<>(firstMap.apply(a), secondMap.apply(a), thirdMap.apply(a));
		}

		@Override
		public Generator_<S,E> takeFirst() {
			return new Mapper_<>(generator, source, firstMap);
		}
		@Override
		public Generator_<T,E> takeSecond() {
			return new Mapper_<>(generator, source, secondMap);
		}
		@Override
		public Generator_<U,E> takeThird() {
			return new Mapper_<>(generator, source, thirdMap);
		}

		@Override
		public final PairGenerator_<T,U,E> dropFirst() {
			return new PairMapper_<>(generator,source, secondMap, thirdMap);
		}
		@Override
		public final PairGenerator_<S,U,E> dropSecond() {
			return new PairMapper_<>(generator,source, firstMap, thirdMap);
		}
		@Override
		public final PairGenerator_<S,T,E> dropThird() {
			return new PairMapper_<>(generator,source, firstMap, secondMap);
		}

		@Override
		public final PairGenerator_<Pair<S,T>,U,E> gatherFirst() {
			return new PairMapper_<>(generator,source, (A a) -> new Pair<>(firstMap.apply(a), secondMap.apply(a)), thirdMap);
		};
		@Override
		public final PairGenerator_<S,Pair<T,U>,E> gatherLast() {
			return new PairMapper_<>(generator,source, firstMap, (A a) -> new Pair<>(secondMap.apply(a), thirdMap.apply(a)));
		};


		@Override
		public TripleGenerator_<T,S,U,E> swap12() {
			return new TripleMapper_<>(generator, source, secondMap, firstMap, thirdMap);
		}
		@Override
		public TripleGenerator_<S,U,T,E> swap23() {
			return new TripleMapper_<>(generator, source, firstMap, thirdMap, secondMap);
		}
		@Override
		public TripleGenerator_<U,T,S,E> swap13() {
			return new TripleMapper_<>(generator, source, thirdMap, secondMap, firstMap);
		}
		@Override
		public TripleGenerator_<T,U,S,E> rotate() {
			return new TripleMapper_<>(generator, source, secondMap, thirdMap, firstMap);
		}

		@Override
		public <V> TripleGenerator_<V,T,U,E> mapFirst(Function_<? super S, ? extends V,E> function) {
			return new TripleMapper_<>(generator, source, function.p(firstMap), secondMap, thirdMap);
		}
		@Override
		public <V> TripleGenerator_<S,V,U,E> mapSecond(Function_<? super T, ? extends V,E> function) {
			return new TripleMapper_<>(generator, source, firstMap, function.p(secondMap), thirdMap);
		}
		@Override
		public <V> TripleGenerator_<S,T,V,E> mapThird(Function_<? super U, ? extends V,E> function) {
			return new TripleMapper_<>(generator, source, firstMap, secondMap, function.p(thirdMap));
		}

		@Override
		public <V> PairGenerator_<V,U,E> map12(BiFunction_<? super S, ? super T, ? extends V, E> function) {
			return new PairMapper_<>(generator, source, function.q(firstMap, secondMap), thirdMap);
		}
		@Override
		public <V> PairGenerator_<V,U,E> map21(BiFunction_<? super T, ? super S, ? extends V, E> function) {
			return new PairMapper_<>(generator, source, function.q(secondMap, firstMap), thirdMap);
		}
		@Override
		public <V> PairGenerator_<S,V,E> map23(BiFunction_<? super T, ? super U, ? extends V, E> function) {
			return new PairMapper_<>(generator, source, firstMap, function.q(secondMap, thirdMap));
		}
		@Override
		public <V> PairGenerator_<S,V,E> map32(BiFunction_<? super U, ? super T, ? extends V, E> function) {
			return new PairMapper_<>(generator, source, firstMap, function.q(thirdMap, secondMap));
		}
	}









	/**
	 * <p>Converts a {@link Generator} of {@link Pair} to a {@link StandardPairGenerator}.</p>
	 *
	 * @param <S> 				The first component type
	 * @param <T> 				The second component type
	 *
	 * @param generator			The {@link Generator} to convert
	 * @return					A {@link StandardPairGenerator} generating the same values
	 */
	@SuppressWarnings("unchecked")
	public static <S,T> PairGenerator<S,T> toPairGenerator(Generator<Pair<S,T>> generator) {
		if (generator instanceof PairGenerator)
			return (PairGenerator<S,T>) generator;
		return new SimplePairGenerator<S, T>(generator) {
			@Override
			public Pair<S, T> nextValue() throws EndOfGenerationException {
				return generator.next();
			}
			@Override
			public boolean isEmpty() {
				return generator.isEmpty();
			}};
	}


	/**
	 * <p>Converts a {@link Generator_} of {@link Pair} to a {@link CheckedPairGenerator}.</p>
	 *
	 * @param <S> 				The first component type
	 * @param <T> 				The second component type
	 * @param <E>				The type of exceptions that can be thrown by the generator
	 *
	 * @param generator			The {@link Generator_} to convert
	 * @return					A {@link CheckedPairGenerator} generating the same values
	 */
	@SuppressWarnings("unchecked")
	public static <S,T, E extends Exception> PairGenerator_<S,T,E> toPairGenerator(Generator_<Pair<S,T>,E> generator) {
		if (generator instanceof PairGenerator_)
			return (PairGenerator_<S,T,E>) generator;
		return new SimplePairGenerator_<S, T, E>(generator) {
			@Override
			public Pair<S, T> nextValue() throws EndOfGenerationException, E {
				return generator.next();
			}
			@Override
			public boolean isEmpty() {
				return generator.isEmpty();
			}};
	}

	public static <S,T,U> TripleGenerator<S,T,U> expandFirst(PairGenerator<Pair<S,T>,U> generator) {
		return generator.duplicateFirst().mapFirst(Pair::getFirst).mapSecond(Pair::getSecond);
	}
	public static <S,T,U, E extends Exception> TripleGenerator_<S,T,U,E> expandFirst(PairGenerator_<Pair<S,T>,U,E> generator) {
		return generator.duplicateFirst().mapFirst(Pair::getFirst).mapSecond(Pair::getSecond);
	}
	public static <S,T,U> TripleGenerator<S,T,U> expandSecond(PairGenerator<S,Pair<T,U>> generator) {
		return generator.duplicateSecond().mapSecond(Pair::getFirst).mapThird(Pair::getSecond);
	}
	public static <S,T,U, E extends Exception> TripleGenerator_<S,T,U,E> expandSecond(PairGenerator_<S,Pair<T,U>,E> generator) {
		return generator.duplicateSecond().mapSecond(Pair::getFirst).mapThird(Pair::getSecond);
	}


	/**
	 * <p>Converts a {@link Generator} of {@link Pair} to a {@link StandardPairGenerator}.</p>
	 *
	 * @param <S> 				The first component type
	 * @param <T> 				The second component type
	 *
	 * @param generator			The {@link Generator} to convert
	 * @return					A {@link StandardPairGenerator} generating the same values
	 */
	@SuppressWarnings("unchecked")
	public static <S,T,U> TripleGenerator<S,T,U> toTripleGenerator(Generator<Triple<S,T,U>> generator) {
		if (generator instanceof TripleGenerator)
			return (TripleGenerator<S,T,U>) generator;
		return new SimpleTripleGenerator<S, T, U>(generator) {
			@Override
			public Triple<S, T, U> nextValue() throws EndOfGenerationException {
				return generator.next();
			}
			@Override
			public boolean isEmpty() {
				return generator.isEmpty();
			}};
	}


	/**
	 * <p>Converts a {@link Generator_} of {@link Pair} to a {@link CheckedPairGenerator}.</p>
	 *
	 * @param <S> 				The first component type
	 * @param <T> 				The second component type
	 * @param <E>				The type of exceptions that can be thrown by the generator
	 *
	 * @param generator			The {@link Generator_} to convert
	 * @return					A {@link CheckedPairGenerator} generating the same values
	 */
	@SuppressWarnings("unchecked")
	public static <S,T,U, E extends Exception> TripleGenerator_<S,T,U,E> toTripleGenerator(Generator_<Triple<S,T,U>,E> generator) {
		if (generator instanceof TripleGenerator_)
			return (TripleGenerator_<S,T,U,E>) generator;
		return new SimpleTripleGenerator_<S, T, U, E>(generator) {
			@Override
			public Triple<S, T, U> nextValue() throws EndOfGenerationException, E {
				return generator.next();
			}
			@Override
			public boolean isEmpty() {
				return generator.isEmpty();
			}};
	}

	/**
	 * Builds a {@link Generator} that produce the values of a {@link Array}.
	 *
	 * @param <S> 				The array values' type
	 *
	 * @param a					The array to convert
	 * @return					A {@link Generator} of the values of the array
	 */
	public static <T> Generator<T> fromCollection(Collection<? extends T> collection) {
		return new SimpleSourceGenerator<T>() {
			private final Iterator<? extends T> it = collection.iterator();
			@Override
			public T nextValue() throws EndOfGenerationException {
				if (isEmpty()) throw new EndOfGenerationException();
				try {return it.next();}
				catch (NoSuchElementException e) {throw generationFinished();}
			}
			@Override
			public boolean isEmpty() {
				return !it.hasNext();
			}};
	}

	/**
	 * Builds a {@link Generator} that produce the values of a {@link Array}.
	 *
	 * @param <S> 				The array values' type
	 *
	 * @param a					The array to convert
	 * @return					A {@link Generator} of the values of the array
	 */
	public static <S> Generator<S> fromCollection(S[] a) {
		return fromCollection(Arrays.asList(a));
	}
	public static <S> Generator<S> fromValues(S... a) {
		return fromCollection(Arrays.asList(a));
	}


	public static <S,T> PairGenerator<S,T> fromMap(Map<S,T> map) {
		return fromCollection(map.entrySet()).biMap(Entry::getKey, Entry::getValue);
	}


	/**
	 * Builds a {@link StandardPairGenerator} that produce the values of a {@link Collection} of pairs.
	 *
	 * @param <S> 				The first component type
	 * @param <T> 				The second component type
	 *
	 * @param collection		The collection to convert
	 * @return					A {@link StandardPairGenerator} of the values of the collection
	 */
	public static <S,T> PairGenerator<S,T> fromPairCollection(Collection<Pair<S,T>> collection) {
		return toPairGenerator(fromCollection(collection));
	}
	public static <S,T> PairGenerator<S,T> fromPairCollection(Pair<S,T>[] collection) {
		return toPairGenerator(fromCollection(collection));
	}


	public static <S,T,U> TripleGenerator<S,T,U> fromTripleCollection(Collection<Triple<S,T,U>> collection) {
		return toTripleGenerator(fromCollection(collection));
	}
	public static <S,T,U> TripleGenerator<S,T,U> fromTripleCollection(Triple<S,T,U>[] collection) {
		return toTripleGenerator(fromCollection(collection));
	}


	/**
	 * <p>Generates a {@link Generator} that will produce one value.</p>
	 *
	 * @param <S> 				The generators values' type
	 *
	 * @param s					The value to wrap in a generator
	 * @return					A generator producing only one <code>s</code> value
	 */
	public static <S> Generator<S> fromSingleton(S s) {
		return new SimpleSourceGenerator<S>() {
			boolean done = false;

			@Override
			public S nextValue() throws EndOfGenerationException {
				if (done) throw generationFinished();
				done = true;
				return s;
			}

			@Override
			public boolean isEmpty() {
				return done;
			}};
	}

	public static <S, T> PairGenerator<S, T> fromPairSingleton(S s, T t) {
		return new SimplePairGenerator<S, T>() {
			boolean done = false;

			@Override
			public Pair<S,T> nextValue() throws EndOfGenerationException {
				if (done) throw generationFinished();
				done = true;
				return new Pair<>(s,t);
			}

			@Override
			public boolean isEmpty() {
				return done;
			}};
	}



	public static <S> Generator<S> emptyGenerator() {
		return new SimpleSourceGenerator<S>() {
			@Override public S nextValue() throws EndOfGenerationException {throw generationFinished();}
			@Override public boolean isEmpty() {return true;}};
	}

	/**
	 * <p>Generates a empty {@link StandardPairGenerator}.</p>
	 *
	 * @param <S> 				The first component type
	 * @param <T> 				The second component type
	 *
	 * @return A {@link StandardPairGenerator} that produce no value
	 */
	public static <S,T> PairGenerator<S,T> emptyPairGenerator() {
		return new SimplePairGenerator<S,T>() {
			@Override public Pair<S,T> nextValue() throws EndOfGenerationException {throw generationFinished();}
			@Override public boolean isEmpty() {return true;}};
	}



	public static <S> Generator<S> constant(S s) {
		return new SimpleSourceGenerator<S>() {
			@Override public S nextValue() throws EndOfGenerationException {return s;}
			@Override public boolean isEmpty() {return false;}};
	}

	public static <S> Generator<S> constant(Supplier<S> s) {
		return new SimpleSourceGenerator<S>() {
			@Override public S nextValue() throws EndOfGenerationException {return s.get();}
			@Override public boolean isEmpty() {return false;}};
	}


	/**
	 * <p>Concatenates generators of the same type.</p>
	 *
	 * @param <S> 				The generators values' type
	 * @param generators		The generators to concatenate
	 *
	 * @return 					A {@link Generator} producing the value of each generator.
	 */
	public static <S> Generator<S> concat(Generator<Generator<S>> generators) {
		return generators.fold((Generator<S> g, Generator<S> t) -> g.append(t), emptyGenerator());
	}
	public static <S,E extends Exception> Generator<S> concat(Generator_<Generator<S>,E> generators) throws E {
		return generators.fold((Generator<S> g, Generator<S> t) -> g.append(t), emptyGenerator());
	}


	public static <S> Generator<Generator<S>> cartesianProduct(Generator<? extends Generator<S>> generators) {
		final GList<GList<S>> values = generators.map(Generator::toList).toList();
		final int noc = values.size();
		Integer[] index = new Integer[noc];
		Arrays.fill(index, 0);
		int[] lenghts = new int[noc];
		fromCollection(values).enumerate().forEach((Integer k, List<S> l) -> lenghts[k] = l.size());
		return noc == 0 ? emptyGenerator() : new SimpleSourceGenerator<Generator<S>>() {

			@Override
			public Generator<S> nextValue() throws EndOfGenerationException {
				if (isEmpty()) throw new EndOfGenerationException();
				Integer[] current = index.clone();
				int k = noc -1;
				++index[k];
				while (k>0 && index[k]==lenghts[k]) {
					index[k] = 0;
					++index[--k];
				}
				return fromCollection(current).enumerate().mapFirst(values::get).map(List<S>::get);
			}

			@Override
			public boolean isEmpty() {
				return index[0] == lenghts[0];
			}};
	}
}
