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

public class Generators {

	private Generators() {}










	public static class EndOfGenerationException extends Exception {
		private static final long serialVersionUID = -4492584892378046784L;

	}

	/*------ Interfaces ------*/


	private static interface AbstractGenerator<T> {
		boolean isEmpty();
		void stopGeneration(); // TODO Salut ?
	}

	public static interface Generator<T> extends AbstractGenerator<T> {
		/*------ Specific ------*/
		T next() throws EndOfGenerationException;
		List<T> toList();
		PairGenerator<Integer,T> enumerate();
		<U> PairGenerator<T,U> cartesianProduct(Generator<U> generator);
		<U> PairGenerator<T,U> gather(Generator<U> generator);
		Generator<T> filter(Predicate<? super T> test);
		Generator<T> append(Generator<? extends T> other);
		T find(Predicate<? super T> test);
		T find(T t);
		Generator<T> async();

		/*------ Without exceptions ------*/
		<U> Generator<U> map(Function<? super T, ? extends U> function);
		<U,V> PairGenerator<U,V> biMap(
				Function<? super T, ? extends U> firstMap,
				Function<? super T, ? extends V> secondMap);
		<U> U fold(BiFunction<? super U, ? super T, ? extends U> function, U init);
		void forEach(Consumer<? super T> function);
		boolean forAll(Predicate<? super T> test);

		/*------ With exceptions ------*/
		default <U, E extends Exception> Generator_<U,E> map_(Function_<? super T, ? extends U, E> function) {
			return this.<E>toChecked().map(function);
		}
		default <U,V, E extends Exception> PairGenerator_<U,V,E> biMap_(
				Function_<? super T, ? extends U,E> firstMap,
				Function_<? super T, ? extends V,E> secondMap) {
			return this.<E>toChecked().biMap(firstMap, secondMap);
		}
		default <U, E extends Exception> U fold_(BiFunction_<? super U, ? super T, ? extends U, E> function, U init) throws E {
			return this.<E>toChecked().fold(function, init);
		}
		default <E extends Exception> void forEach_(Consumer_<? super T,E> function) throws E {
			this.<E>toChecked().forEach(function);
		}
		default <E extends Exception> boolean forAll_(Predicate_<? super T,E> test) throws E {
			return this.<E>toChecked().forAll(test);
		}

		/*------ Conversion ------*/
		<E extends Exception> Generator_<T,E> toChecked();
	}

	public static interface Generator_<T, E extends Exception> extends AbstractGenerator<T> {
		/*------ Specific ------*/
		T next() throws EndOfGenerationException, E;
		List<T> toList() throws E;
		PairGenerator_<Integer,T,E> enumerate();
		<U> PairGenerator_<T,U,E> cartesianProduct(Generator_<U,E> generator) throws E;
		<U> PairGenerator_<T,U,E> gather(Generator_<U,E> generator);
		Generator_<T,E> filter(Predicate_<? super T,E> test);
		Generator_<T,E> append(Generator_<? extends T,E> other);
		T find(Predicate_<? super T, E> test) throws E;
		T find(T t) throws E;
		Generator_<T,E> async();

		/*------ With exceptions ------*/
		<U> Generator_<U,E> map(Function_<? super T, ? extends U, E> function);
		<U,V> PairGenerator_<U,V,E> biMap(
				Function_<? super T, ? extends U,E> firstMap,
				Function_<? super T, ? extends V,E> secondMap);
		<U> U fold(BiFunction_<? super U, ? super T, ? extends U, E> function, U init) throws E;
		void forEach(Consumer_<? super T,E> function) throws E;
		boolean forAll(Predicate_<? super T,E> test) throws E;

		/*------ Conversion ------*/
		Generator<T> check() throws E;
	}




	public static interface PairGenerator<S,T> extends Generator<Pair<S,T>> {
		/*------ Specific ------*/
		Generator<S> takeFirst();
		Generator<T> takeSecond();
		PairGenerator<T,S> swap();
		PairGenerator<S,T> filter(BiPredicate<? super S, ? super T> test);
		PairGenerator<S,T> filter(Predicate<? super S> firstTest, Predicate<? super T> secondTest);

		/*------ Without exceptions ------*/
		<U> PairGenerator<U,T> mapFirst(Function<? super S, ? extends U> function);
		<U> PairGenerator<U,T> mapFirst(BiFunction<? super S, ? super T, ? extends U> function);
		<U> PairGenerator<S,U> mapSecond(BiFunction<? super S, ? super T, ? extends U> function);
		<U> PairGenerator<S,U> mapSecond(Function<? super T, ? extends U> function);
		<U,V> PairGenerator<U,V> map(
				Function<? super S, ? extends U> firstMap,
				Function<? super T, ? extends V> secondMap);
		<U> Generator<U> map(BiFunction<? super S, ? super T, ? extends U> function);
		void forEach(BiConsumer<? super S, ? super T> function);
		boolean forAll(BiPredicate<? super S, ? super T> test);
		boolean forAll(Predicate<? super S> firstTest, Predicate<? super T> secondTest);
//		Generator<Pair<S,T>> replace(BiPredicate<? super S, ? super T> test, Supplier<Pair<S,T>> with);
		PairGenerator<S,T> async();

		/*------ With exceptions ------*/
		default <U, E extends Exception> PairGenerator_<U,T,E> mapFirst_(Function_<? super S, ? extends U,E> function) {
			return this.<E>toChecked().mapFirst(function);
		}
		default <U, E extends Exception> PairGenerator_<U,T,E> mapFirst_(BiFunction_<? super S, ? super T, ? extends U,E> function) {
			return this.<E>toChecked().mapFirst(function);
		}
		default <U, E extends Exception> PairGenerator_<S,U,E> mapSecond_(Function_<? super T, ? extends U,E> function) {
			return this.<E>toChecked().mapSecond(function);
		}
		default <U, E extends Exception> PairGenerator_<S,U,E> mapSecond_(BiFunction_<? super S, ? super T, ? extends U,E> function) {
			return this.<E>toChecked().mapSecond(function);
		}
		default <U,V, E extends Exception> PairGenerator_<U,V,E> map_(
				Function_<? super S, ? extends U,E> firstMap,
				Function_<? super T, ? extends V,E> secondMap) {
			return this.<E>toChecked().map(firstMap, secondMap);
		}
		default <U, E extends Exception> Generator_<U,E> map_(BiFunction_<? super S, ? super T, ? extends U, E> function) {
			return this.<E>toChecked().map(function);
		}
		default <E extends Exception> void forEach_(BiConsumer_<? super S, ? super T,E> function) throws E {
			this.<E>toChecked().forEach(function);
		}
		default <E extends Exception> boolean forAll_(BiPredicate_<? super S, ? super T,E> test) throws E {
			return this.<E>toChecked().forAll(test);
		}
		default <E extends Exception> boolean forAll_(Predicate_<? super S, E> firstTest, Predicate_<? super T, E> secondTest) throws E {
			return this.<E>toChecked().forAll(firstTest, secondTest);
		}

		/*------ Conversion ------*/
		<E extends Exception> PairGenerator_<S,T,E> toChecked();
	}

	public static interface PairGenerator_<S,T, E extends Exception> extends Generator_<Pair<S,T>,E> {
		/*------ Specific ------*/
		Generator_<S,E> takeFirst();
		Generator_<T,E> takeSecond();
		PairGenerator_<T,S,E> swap();
		PairGenerator_<S,T,E> filter(BiPredicate_<? super S, ? super T,E> test);
		PairGenerator_<S,T,E> filter(Predicate_<? super S, E> firstTest, Predicate_<? super T, E> secondTest);

		/*------ With exceptions ------*/
		<U> PairGenerator_<U,T,E> mapFirst(Function_<? super S, ? extends U,E> function);
		<U> PairGenerator_<U,T,E> mapFirst(BiFunction_<? super S, ? super T, ? extends U,E> function);
		<U> PairGenerator_<S,U,E> mapSecond(Function_<? super T, ? extends U,E> function);
		<U> PairGenerator_<S,U,E> mapSecond(BiFunction_<? super S, ? super T, ? extends U,E> function);
		<U,V> PairGenerator_<U,V,E> map(
				Function_<? super S, ? extends U,E> firstMap,
				Function_<? super T, ? extends V,E> secondMap);
		<U> Generator_<U,E> map(BiFunction_<? super S, ? super T, ? extends U, E> function);
		void forEach(BiConsumer_<? super S,? super T,E> function) throws E;
		boolean forAll(BiPredicate_<? super S,? super T,E> test) throws E;
		boolean forAll(Predicate_<? super S, E> firstTest, Predicate_<? super T, E> secondTest) throws E;
		PairGenerator_<S,T,E> async();


		/*------ Conversion ------*/
		PairGenerator<S,T> check() throws E;
	}








	/*------ Default Implementation ------*/

	public static interface DefaultGenerator<T> extends Generator<T> {
		@Override
		default List<T> toList() {
			List<T> l = new ArrayList<>();
			try {
				while (true)
					l.add(next());
			}
			catch (EndOfGenerationException e) {}
			return l;
		}


		@Override
		default <U> Generator<U> map(Function<? super T, ? extends U> function) {
			return new Mapper<>(this, function);
		}

		@Override
		default <U,V> PairGenerator<U,V> biMap(Function<? super T, ? extends U> firstMap, Function<? super T, ? extends V> secondMap) {
			return new PairMapper<>(this, firstMap, secondMap);
		}

		@Override
		default PairGenerator<Integer,T> enumerate() {
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
		@Override
		default <U> U fold(BiFunction<? super U, ? super T, ? extends U> function, U init) {
			try {
				while (true)
					init = function.apply(init, next());
			}
			catch (EndOfGenerationException e) {return init;}
		}
		@Override
		default void forEach(Consumer<? super T> function) {
			try {
				while (true)
					function.accept(next());
			}
			catch (EndOfGenerationException e) {}
			return;
		}
		@Override
		default boolean forAll(Predicate<? super T> test) {
			return fold((Boolean b, T t) -> b && test.test(t),true);
		}

		@Override
		default T find(Predicate<? super T> test) {
			return fold((T t, T n) -> test.test(n) ? n : t, null);
		}
		@Override
		default T find(T t) {
			return find(t::equals);
		}

		@Override
		default Generator<T> append(Generator<? extends T> other) { // TODO test other != this
			Generator<T> This = this;
			return new SimpleGenerator<T>(Arrays.asList(this, other)) {
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


		@Override
		default <U> PairGenerator<T,U> cartesianProduct(Generator<U> generator) {
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

		@Override
		default <U> PairGenerator<T,U> gather(Generator<U> other) {
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

		@Override
		default Generator<T> filter(Predicate<? super T> test) {
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


		@Override
		default <E extends Exception> Generator_<T, E> toChecked() {
			return new GeneratorWrapper_<T,E>(this);
		}

		@Override
		default Generator<T> async() {
			Generator<T> This = this;
			return new CustomGenerator<T>() {
				@Override
				protected void generate() throws EndOfGenerationException {
					while (true) yield(This.next());
				}};
		}
	}

	public static interface DefaultGenerator_<T, E extends Exception> extends Generator_<T,E> {
		@Override
		default List<T> toList() throws E {
			List<T> l = new ArrayList<>();
			try {
				while (true)
					l.add(next());
			}
			catch (EndOfGenerationException e) {}
			return l;
		}


		@Override
		default <U> Generator_<U,E> map(Function_<? super T, ? extends U, E> function) {
			return new Mapper_<>(this, function);
		}

		@Override
		default <U,V> PairGenerator_<U,V,E> biMap(Function_<? super T, ? extends U,E> firstMap, Function_<? super T, ? extends V,E> secondMap) {
			return new PairMapper_<>(this, firstMap, secondMap);
		}

		@Override
		default PairGenerator_<Integer,T,E> enumerate() {
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
		@Override
		default <U> U fold(BiFunction_<? super U, ? super T, ? extends U, E> function, U init) throws E {
			try {
				while (true)
					init = function.apply(init, next());
			}
			catch (EndOfGenerationException e) {return init;}
		}

		@Override
		default void forEach(Consumer_<? super T,E> function) throws E {
			try {
				while (true)
					function.accept(next());
			}
			catch (EndOfGenerationException e) {}
			return;
		}

		@Override
		default boolean forAll(Predicate_<? super T,E> test) throws E {
			return fold((Boolean b, T t) -> b && test.test(t),true);
		}


		@Override
		default T find(Predicate_<? super T,E> test) throws E {
			return fold((T t, T n) -> test.test(n) ? n : t, null);
		}
		@Override
		default T find(T t) throws E {
			return find(t::equals);
		}

		@Override
		default Generator_<T,E> append(Generator_<? extends T,E> other) {
			Generator_<T,E> This = this;
			return new SimpleGenerator_<T,E>(Arrays.asList(this, other)) {
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


		@Override
		default <U> PairGenerator_<T,U,E> cartesianProduct(Generator_<U,E> generator) throws E {
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

		@Override
		default <U> PairGenerator_<T,U,E> gather(Generator_<U,E> other) {
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

		@Override
		default Generator_<T,E> filter(Predicate_<? super T, E> test) {
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

		@Override
		default Generator<T> check() throws E {
			return Generators.fromCollection(toList());
		}

		@Override
		default Generator_<T,E> async() {
			Generator_<T,E> This = this;
			return new CustomGenerator_<T,E>() {
				@Override
				protected void generate() throws EndOfGenerationException,E {
					while (true) yield(This.next());
				}};
		}
	}



	public static interface DefaultPairGenerator<S,T> extends PairGenerator<S,T>, DefaultGenerator<Pair<S,T>> {
		@Override
		default <U,V> PairGenerator<U,V> map(
				Function<? super S, ? extends U> firstMap,
				Function<? super T, ? extends V> secondMap) {
			return this.<U>mapFirst(firstMap).mapSecond(secondMap);
		}

		@Override
		default Generator<S> takeFirst() {
			return new Mapper<>(this, Pair::getFirst);
		}


		@Override
		default Generator<T> takeSecond() {
			return new Mapper<>(this, Pair::getSecond);
		}


		@Override
		default PairGenerator<T,S> swap() {
			return new PairMapper<>(this, Pair::getSecond, Pair::getFirst);
		}

		@Override
		default <U> PairGenerator<U, T> mapFirst(Function<? super S, ? extends U> function) {
			return new PairMapper<>(this, function.p(Pair::getFirst), Pair::getSecond);
		}

		@Override
		default <U> PairGenerator<U, T> mapFirst(BiFunction<? super S, ? super T, ? extends U> function) {
			return new PairMapper<>(this, function.toFunction(), Pair::getSecond);
		}

		@Override
		default <U> PairGenerator<S, U> mapSecond(Function<? super T, ? extends U> function) {
			return new PairMapper<>(this, Pair::getFirst, function.p(Pair::getSecond));
		}

		@Override
		default <U> PairGenerator<S, U> mapSecond(BiFunction<? super S, ? super T, ? extends U> function) {
			return new PairMapper<>(this, Pair::getFirst, function.toFunction());
		}

		@Override
		default <U> Generator<U> map(BiFunction<? super S, ? super T, ? extends U> function) {
			return map((Pair<S,T> p) -> function.apply(p.first, p.second));
		}

		@Override
		default void forEach(BiConsumer<? super S, ? super T> function) {
			forEach(function.toConsumer());
		}
		@Override
		default boolean forAll(BiPredicate<? super S, ? super T> test) {
			return forAll(test.toPredicate());
		}
		@Override
		default boolean forAll(Predicate<? super S> firstTest, Predicate<? super T> secondTest) {
			return forAll((S s,T t) -> firstTest.test(s) && secondTest.test(t));
		}
		@Override
		default PairGenerator<S,T> filter(BiPredicate<? super S, ? super T> test) {
			return Generators.toPairGenerator(filter((Pair<S,T> p) -> test.test(p.first, p.second)));
		}
		@Override
		default PairGenerator<S,T> filter(Predicate<? super S> firstTest, Predicate<? super T> secondTest) {
			return Generators.toPairGenerator(filter((Pair<S,T> p) -> firstTest.test(p.first) && secondTest.test(p.second)));
		}

		@Override
		default <E extends Exception> PairGenerator_<S,T,E> toChecked() {
			return new PairGeneratorWrapper_<>(this);
		}

		@Override
		default PairGenerator<S,T> async() {
			PairGenerator<S,T> This = this;
			return new CustomPairGenerator<S,T>() {
				@Override
				protected void generate() throws EndOfGenerationException {
					while (true) yield(This.next());
				}};
		}
	}

	public static interface DefaultPairGenerator_<S,T, E extends Exception> extends PairGenerator_<S,T,E>, DefaultGenerator_<Pair<S,T>,E> {

		@Override
		default Generator_<S, E> takeFirst() {
			return new Mapper_<>(this, Pair::getFirst);
		}
		@Override
		default Generator_<T, E> takeSecond() {
			return new Mapper_<>(this, Pair::getSecond);
		}
		@Override
		default PairGenerator_<T, S, E> swap() {
			return new PairMapper_<>(this, Pair::getSecond, Pair::getFirst);
		}
		@Override
		default
		<U> PairGenerator_<U,T,E> mapFirst(Function_<? super S, ? extends U,E> function) {
			return new PairMapper_<>(this, function.p(Pair::getFirst), Pair::getSecond);
		}

		@Override
		default
		<U> PairGenerator_<U,T,E> mapFirst(BiFunction_<? super S, ? super T, ? extends U,E> function) {
			return new PairMapper_<>(this, function.toFunction(), Pair::getSecond);
		}

		default
		<U> PairGenerator_<S,U,E> mapSecond(Function_<? super T, ? extends U,E> function) {
			return new PairMapper_<>(this, Pair::getFirst, function.p(Pair::getSecond));
		}

		default
		<U> PairGenerator_<S,U,E> mapSecond(BiFunction_<? super S, ? super T, ? extends U,E> function) {
			return new PairMapper_<>(this, Pair::getFirst, function.toFunction());
		}

		@Override
		default
		<U,V> PairGenerator_<U,V,E> map(
				Function_<? super S, ? extends U,E> firstMap,
				Function_<? super T, ? extends V,E> secondMap) {
			return this.<U>mapFirst(firstMap).mapSecond(secondMap);
		}

		@Override
		default <U> Generator_<U,E> map(BiFunction_<? super S, ? super T, ? extends U,E> function) {
			return map((Pair<S,T> p) -> function.apply(p.first, p.second));
		}

		@Override
		default void forEach(BiConsumer_<? super S,? super T,E> function) throws E {
			forEach(function.toConsumer());
		}
		@Override
		default boolean forAll(BiPredicate_<? super S,? super T,E> test) throws E {
			return forAll(test.toPredicate());
		}
		@Override
		default boolean forAll(Predicate_<? super S,E> firstTest, Predicate_<? super T,E> secondTest) throws E {
			return forAll((S s,T t) -> firstTest.test(s) && secondTest.test(t));
		}
		@Override
		default PairGenerator_<S,T,E> filter(BiPredicate_<? super S, ? super T,E> test) {
			return Generators.toPairGenerator(filter((Pair<S,T> p) -> test.test(p.first, p.second)));
		}
		@Override
		default PairGenerator_<S,T,E> filter(Predicate_<? super S,E> firstTest, Predicate_<? super T,E> secondTest) {
			return Generators.toPairGenerator(filter((Pair<S,T> p) -> firstTest.test(p.first) && secondTest.test(p.second)));
		}

		@Override
		default PairGenerator<S,T> check() throws E {
			return Generators.fromPairCollection(toList());
		}

		@Override
		default PairGenerator_<S,T,E> async() {
			PairGenerator_<S,T,E> This = this;
			return new CustomPairGenerator_<S,T,E>() {
				@Override
				protected void generate() throws EndOfGenerationException,E {
					while (true) yield(This.next());
				}};
		}
	}














	/*------ Implementation ------*/


	private static abstract class SimpleAbstractGenerator<T> implements AbstractGenerator<T> {
		private final List<AbstractGenerator<?>> parents;
		private volatile boolean done = false;

		public SimpleAbstractGenerator() {
			this.parents = Collections.emptyList();
		}

		public SimpleAbstractGenerator(AbstractGenerator<?> parent) {
			this.parents = Collections.singletonList(parent);
		}
		public SimpleAbstractGenerator(List<AbstractGenerator<?>> parents) {
			this.parents = parents;
		}

		protected final EndOfGenerationException generationFinished() {
			done = true;
			stopGeneration();
			return new EndOfGenerationException();
		}

		public final boolean isDone() {
			return done;
		}

		@Override
		public void stopGeneration() {
			Generators.fromCollection(parents).forEach(AbstractGenerator<?>::stopGeneration);
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
			return nextValue();
		}

		protected abstract T nextValue() throws EndOfGenerationException;
	}

	private static class GeneratorWrapper_<T, E extends Exception> extends SimpleAbstractGenerator<T> implements DefaultGenerator_<T,E> {
		private final Generator<T> source;

		public GeneratorWrapper_(Generator<T> source) {
			super(source);
			this.source = source;
		}
		@Override
		public boolean isEmpty() {
			return source.isEmpty();
		}

		@Override
		public T next() throws EndOfGenerationException, E {
			return source.next();
		}
	}

	private static abstract class SimpleGenerator_<T, E extends Exception> extends SimpleAbstractGenerator<T> implements DefaultGenerator_<T,E> {
		public SimpleGenerator_() {}
		public SimpleGenerator_(AbstractGenerator<?> parent) {super(parent);}
		public SimpleGenerator_(List<AbstractGenerator<?>> parents) {super(parents);}

		@Override
		public final T next() throws EndOfGenerationException, E {
			if (isDone())
				throw new EndOfGenerationException();
			return nextValue();
		}

		protected abstract T nextValue() throws EndOfGenerationException, E;
	}










	private static class Mapper<S,T> extends SimpleAbstractGenerator<T> implements DefaultGenerator<T> {
		private final Generator<S> source;
		private final Function<? super S,? extends T> map;

		public Mapper(Generator<S> source, Function<? super S,? extends T> map) {
			super(source);
			this.source = source;
			this.map = map;
		}
		@Override
		public boolean isEmpty() {
			return source.isEmpty();
		}

		@Override
		public T next() throws EndOfGenerationException {
			return map.apply(source.next());
		}

		@Override
		public <U> Generator<U> map(Function<? super T, ? extends U> function) {
			return new Mapper<>(source, map.o(function));
		}

		@Override
		public <E extends Exception> Generator_<T, E> toChecked() {
			return new Mapper_<>(source.toChecked(), map.toChecked());
		}
	}

	private static class Mapper_<S,T,E extends Exception> extends SimpleAbstractGenerator<T> implements DefaultGenerator_<T,E> {
		private final Generator_<S,E> source;
		private final Function_<? super S,? extends T,E> map;

		public Mapper_(Generator_<S,E> source, Function_<? super S,? extends T,E> map) {
			super(source);
			this.source = source;
			this.map = map;
		}
		@Override
		public boolean isEmpty() {
			return source.isEmpty();
		}

		@Override
		public T next() throws EndOfGenerationException, E {
			return map.apply(source.next());
		}

		@Override
		public <U> Generator_<U,E> map(Function_<? super T, ? extends U, E> function) {
			return new Mapper_<>(source, map.o(function));
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

		protected final void interrupt() {
			thread.interrupt();
		}
	}


	private static abstract class DefaultThreadGenerator<T> extends DefaultAbstractGenerator<T> implements DefaultGenerator<T> {

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

	public static abstract class CustomGenerator_<T,E extends Exception> extends DefaultThreadGenerator_<T,E> implements DefaultGenerator_<T,E> {

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








	private abstract static class SimplePairGenerator<S,T> extends SimpleGenerator<Pair<S,T>> implements DefaultPairGenerator<S, T> {
		public SimplePairGenerator() {}
		public SimplePairGenerator(AbstractGenerator<?> parent) {super(parent);}
		public SimplePairGenerator(List<AbstractGenerator<?>> parents) {super(parents);}

	}

	private static class PairGeneratorWrapper_<S,T, E extends Exception> implements DefaultPairGenerator_<S, T, E> {
		private final PairGenerator<S,T> source;

		public PairGeneratorWrapper_(PairGenerator<S,T> source) {
			this.source = source;
		}

		@Override
		public final Pair<S, T> next() throws EndOfGenerationException {
			return source.next();
		}

		@Override
		public final boolean isEmpty() {
			return source.isEmpty();
		}

		@Override
		public void stopGeneration() {
			source.stopGeneration();
		}
	}

	private static abstract class SimplePairGenerator_<S,T, E extends Exception> extends SimpleGenerator_<Pair<S,T>,E> implements DefaultPairGenerator_<S, T, E> {
		public SimplePairGenerator_() {}
		public SimplePairGenerator_(AbstractGenerator<?> parent) {super(parent);}
		public SimplePairGenerator_(List<AbstractGenerator<?>> parents) {super(parents);}
	}









	private static class PairMapper<A,S,T> extends SimplePairGenerator<S,T> implements DefaultPairGenerator<S,T> {
		private final Generator<A> source;
		private final Function<? super A,? extends S> firstMap;
		private final Function<? super A,? extends T> secondMap;

		public PairMapper(Generator<A> source, Function<? super A,? extends S> firstMap, Function<? super A,? extends T> secondMap) {
			super(source);
			this.source = source;
			this.firstMap = firstMap;
			this.secondMap = secondMap;
		}
		@Override
		public boolean isEmpty() {
			return source.isEmpty();
		}

		@Override
		public Pair<S,T> nextValue() throws EndOfGenerationException {
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
		public <U> PairGenerator<U,T> mapFirst(BiFunction<? super S, ? super T, ? extends U> function) {
			return new PairMapper<>(source, function.p(firstMap, secondMap), secondMap);
		}

		public <U> PairGenerator<S,U> mapSecond(Function<? super T, ? extends U> function) {
			return new PairMapper<>(source, firstMap, function.p(secondMap));
		}


		public <U> PairGenerator<S,U> mapSecond(BiFunction<? super S, ? super T, ? extends U> function) {
			return new PairMapper<>(source, firstMap, function.p(firstMap, secondMap));
		}

		@Override
		public <E extends Exception> PairGenerator_<S,T, E> toChecked() {
			return new PairMapper_<>(source.toChecked(), firstMap.toChecked(), secondMap.toChecked());
		}
	}

	private static class PairMapper_<A,S,T,E extends Exception> extends SimplePairGenerator_<S,T,E> implements DefaultPairGenerator_<S,T,E> {
		private final Generator_<A,E> source;
		private final Function_<? super A,? extends S,E> firstMap;
		private final Function_<? super A,? extends T,E> secondMap;

		public PairMapper_(Generator_<A,E> source, Function_<? super A,? extends S,E> firstMap, Function_<? super A,? extends T,E> secondMap) {
			super(source);
			this.source = source;
			this.firstMap = firstMap;
			this.secondMap = secondMap;
		}
		@Override
		public boolean isEmpty() {
			return source.isEmpty();
		}

		@Override
		public Pair<S,T> nextValue() throws EndOfGenerationException, E {
			A a = source.next();
			return new Pair<>(firstMap.apply(a), secondMap.apply(a));
		}

		@Override
		public Generator_<S,E> takeFirst() {
			return new Mapper_<>(source, firstMap);
		}
		@Override
		public Generator_<T,E> takeSecond() {
			return new Mapper_<>(source, secondMap);
		}
		@Override
		public PairGenerator_<T, S, E> swap() {
			return new PairMapper_<>(source, secondMap, firstMap);
		}
		@Override
		public <U> PairGenerator_<U,T,E> mapFirst(Function_<? super S, ? extends U,E> function) {
			return new PairMapper_<>(source, function.p(firstMap), secondMap);
		}

		@Override
		public <U> PairGenerator_<S,U,E> mapSecond(Function_<? super T, ? extends U,E> function) {
			return new PairMapper_<>(source, firstMap, function.p(secondMap));
		}
	}




	public static abstract class CustomPairGenerator<S,T> extends CustomGenerator<Pair<S,T>> implements DefaultPairGenerator<S, T> {
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



	public static abstract class CustomPairGenerator_<S,T,E extends Exception> extends CustomGenerator_<Pair<S,T>,E> implements DefaultPairGenerator_<S, T,E> {
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


	/**
	 * Builds a {@link Generator} that produce the values of a {@link Array}.
	 *
	 * @param <S> 				The array values' type
	 *
	 * @param a					The array to convert
	 * @return					A {@link Generator} of the values of the array
	 */
	public static <T> Generator<T> fromCollection(Collection<T> collection) {
		return new SimpleGenerator<T>() {
			private final Iterator<T> it = collection.iterator();
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


	/**
	 * <p>Generates a {@link Generator} that will produce one value.</p>
	 *
	 * @param <S> 				The generators values' type
	 *
	 * @param s					The value to wrap in a generator
	 * @return					A generator producing only one <code>s</code> value
	 */
	public static <S> Generator<S> fromSingleton(S s) {
		return new SimpleGenerator<S>() {
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
		return new SimpleGenerator<S>() {
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
		return new SimpleGenerator<S>() {
			@Override public S nextValue() throws EndOfGenerationException {return s;}
			@Override public boolean isEmpty() {return false;}};
	}

	public static <S> Generator<S> constant(Supplier<S> s) {
		return new SimpleGenerator<S>() {
			@Override public S nextValue() throws EndOfGenerationException {return s.get();}
			@Override public boolean isEmpty() {return false;}};
	}


	/**
	 * <p>Concatenates generators of the same type.</p>
	 *
	 * @param <S> 				The generators values' type
	 * @param generators		The generators to concatenate
	 * @return 					A {@link Generator} producing the value of each generator.
	 */
	public static <S> Generator<S> concat(Generator<Generator<S>> generators) {
		return generators.fold((Generator<S> g, Generator<S> t) -> g.append(t), emptyGenerator());
	}


	public static <S> Generator<Generator<S>> cartesianProduct(Generator<Generator<S>> generators) {
		final List<List<S>> values = generators.map(Generator::toList).toList();
		final int noc = values.size();
		Integer[] index = new Integer[noc];
		Arrays.fill(index, 0);
		int[] lenghts = new int[noc];
		Generators.fromCollection(values).enumerate().forEach((Integer k, List<S> l) -> lenghts[k] = l.size());
		return noc == 0 ? emptyGenerator() : new SimpleGenerator<Generator<S>>() {

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
