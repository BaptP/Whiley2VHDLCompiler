package wyvc.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
		void stopGeneration();
	}

	public static interface Generator<T> extends AbstractGenerator<T> {
		/*------ Specific ------*/
		T next() throws EndOfGenerationException, InterruptedException;
		List<T> toList();
		PairGenerator<Integer,T> enumerate();
		<U> PairGenerator<T,U> cartesianProduct(Generator<U> generator);
		<U> PairGenerator<T,U> gather(Generator<U> generator);
		Generator<T> filter(Predicate<? super T> test);
		Generator<T> append(Generator<? extends T> other);
		T find(Predicate<? super T> test);
		T find(T t);

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
		T next() throws EndOfGenerationException, InterruptedException, E;
		List<T> toList() throws E;
		PairGenerator_<Integer,T,E> enumerate();
		<U> PairGenerator_<T,U,E> cartesianProduct(Generator_<U,E> generator);
		<U> PairGenerator_<T,U,E> gather(Generator_<U,E> generator);
		Generator_<T,E> filter(Predicate_<? super T,E> test);
		Generator_<T,E> append(Generator_<? extends T,E> other);
		T find(Predicate_<? super T, E> test) throws E;
		T find(T t) throws E;

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

	private static interface AbstractMapper<S,P,T> extends AbstractGenerator<T> {
		//void interruptGeneration();
	}

	private static interface Mapper<S,P,T> extends AbstractMapper<S,P,T>, Generator<T> {
		/*------ Specific ------*/
		Function<? super S,? extends T> getMap();
		T readLastValue() throws InterruptedException, EndOfGenerationException;
		Generator<S> getSource();

		/*------ Without exceptions ------*/
		void moveGenerationTo(FollowingMapper<S,T,?> next);

		/*------ With exceptions ------*/
		void moveGenerationTo(FollowingMapper_<S,T,?,?> next);

	}

	private static interface Mapper_<S,P,T, E extends Exception> extends AbstractMapper<S,P,T>, Generator_<T,E> {
		/*------ Specific ------*/
		Function_<? super S,? extends T, E> getMap();
		T readLastValue() throws InterruptedException, EndOfGenerationException, E;
		Generator_<S,? extends E> getSource();

		/*------ With exceptions ------*/
		void moveGenerationTo(FollowingMapper_<S,T,?,? super E> next);
	}

	public static interface PairGenerator<S,T> extends Generator<Pair<S,T>> {
		/*------ Specific ------*/
		Generator<S> takeFirst();
		Generator<T> takeSecond();
		PairGenerator<T,S> swap();
		PairGenerator<S,T> filter(BiPredicate<? super S, ? super T> test);

		/*------ Without exceptions ------*/
		<U> PairGenerator<U,T> mapFirst(Function<? super S, ? extends U> function);
		<U> PairGenerator<S,U> mapSecond(Function<? super T, ? extends U> function);
		<U,V> PairGenerator<U,V> map(
				Function<? super S, ? extends U> firstMap,
				Function<? super T, ? extends V> secondMap);
		<U> Generator<U> map(BiFunction<? super S, ? super T, ? extends U> function);
		void forEach(BiConsumer<? super S, ? super T> function);
		boolean forAll(BiPredicate<? super S, ? super T> test);
//		boolean forAll(Predicate<? super S> firstTest, Predicate<? super T> secondTest);
//		Generator<Pair<S,T>> replace(BiPredicate<? super S, ? super T> test, Supplier<Pair<S,T>> with);

		/*------ With exceptions ------*/
		default <U, E extends Exception> PairGenerator_<U,T,E> mapFirst_(Function_<? super S, ? extends U,E> function) {
			return this.<E>toChecked().mapFirst(function);
		}
		default <U, E extends Exception> PairGenerator_<S,U,E> mapSecond_(Function_<? super T, ? extends U,E> function) {
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
//		default <E extends Exception> boolean forAll_(Predicate_<? super S, E> firstTest, Predicate_<? super T, E> secondTest) throws E {
//			return this.<E>toChecked().forAll(firstTest, secondTest);
//		}

		/*------ Conversion ------*/
		<E extends Exception> PairGenerator_<S,T,E> toChecked();






	}

	public static interface PairGenerator_<S,T, E extends Exception> extends Generator_<Pair<S,T>,E> {
		/*------ Specific ------*/
		Generator_<S,E> takeFirst();
		Generator_<T,E> takeSecond();
		PairGenerator_<T,S,E> swap();
		PairGenerator_<S,T,E> filter(BiPredicate_<? super S, ? super T,E> test);

		/*------ With exceptions ------*/
		<U> PairGenerator_<U,T,E> mapFirst(Function_<? super S, ? extends U,E> function);
		<U> PairGenerator_<S,U,E> mapSecond(Function_<? super T, ? extends U,E> function);
		<U,V> PairGenerator_<U,V,E> map(
				Function_<? super S, ? extends U,E> firstMap,
				Function_<? super T, ? extends V,E> secondMap);


		<U> Generator_<U,E> map(BiFunction_<? super S, ? super T, ? extends U, E> function);
		void forEach(BiConsumer_<? super S,? super T,E> function) throws E;
		boolean forAll(BiPredicate_<? super S,? super T,E> test) throws E;
//		boolean forAll(Predicate_<? super S, E> firstTest, Predicate_<? super T, E> secondTest) throws E;


		/*------ Conversion ------*/
		PairGenerator<S,T> check() throws E;
	}

	private static interface PairMapper<S,P,T,U> extends Mapper<S,P,Pair<T,U>>, PairGenerator<T,U> {
		/*------ Specific ------*/
		Function<? super S, ? extends T> getFirstMap();
		Function<? super S, ? extends U> getSecondMap();


	}

	private static interface PairMapper_<S,P,T,U, E extends Exception> extends Mapper_<S,P,Pair<T,U>,E>, PairGenerator_<T,U,E> {
		/*------ Specific ------*/
		Function_<? super S, ? extends T, E> getFirstMap();
		Function_<? super S, ? extends U, E> getSecondMap();

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
			catch (InterruptedException e) {}
			return l;
		}
		default <E extends Exception> Generator_<T,E> toChecked() {
			return new WrappedGenerator_<>(this);
		}



		@Override
		default <U> Generator<U> map(Function<? super T, ? extends U> function) {
			return new FirstMapper<>(this, function);
		}

		@Override
		default <U,V> PairGenerator<U,V> biMap(Function<? super T, ? extends U> firstMap, Function<? super T, ? extends V> secondMap) {
			return new FirstPairMapper<>(this, firstMap, secondMap);
		}

		@Override
		default PairGenerator<Integer,T> enumerate() {
			final Generator<T> This = this;
			return new CustomPairGenerator<Integer,T>(this) {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException {
					int k = 0;
					while (true)
						yield(k++,This.next());

				}};
		}
		@Override
		default <U> U fold(BiFunction<? super U, ? super T, ? extends U> function, U init) {
			try {
				while (true)
					init = function.apply(init, next());
			}
			catch (EndOfGenerationException e) {return init;}
			catch (InterruptedException e) {return null;} // TODO ok ?
		}
		@Override
		default void forEach(Consumer<? super T> function) {
			try {
				while (true)
					function.accept(next());
			}
			catch (EndOfGenerationException e) {}
			catch (InterruptedException e) {} // TODO ok ?
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
		default Generator<T> append(Generator<? extends T> other) {
			Generator<T> This = this;
			return new CustomGenerator<T>() { // TODO ?? qui le parent
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException {
					try {
						while (true)
							yield(This.next());
					}
					catch (EndOfGenerationException e) {}
					catch (InterruptedException e) {}
					while (true)
						yield(other.next());
				}
			};
		}


		@Override
		default <U> PairGenerator<T,U> cartesianProduct(Generator<U> generator) {
			List<T> values = toList();
			return new CustomPairGenerator<T,U>(generator) {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException {
					while (true) {
						U u = generator.next();
						for (T t : values)
							yield(t,u);
					}

				}};
		}

		@Override
		default <U> PairGenerator<T,U> gather(Generator<U> generator) {
			Generator<T> This = this;
			return new CustomPairGenerator<T,U>(generator) {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException {
					while (true)
						yield(This.next(), generator.next());
				}};
		}

		@Override
		default Generator<T> filter(Predicate<? super T> test) {
			Generator<T> This = this;
			return new CustomGenerator<T>(this) {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException {
					while (true) {
						T t = This.next();
						if (test.test(t))
							yield(t);
					}
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
			catch (InterruptedException e) {}
			return l;
		}


		@Override
		default <U> Generator_<U,E> map(Function_<? super T, ? extends U, E> function) {
			return new FirstMapper_<>(this, function);
		}

		@Override
		default <U,V> PairGenerator_<U,V,E> biMap(Function_<? super T, ? extends U,E> firstMap, Function_<? super T, ? extends V,E> secondMap) {
			return new FirstPairMapper_<>(this, firstMap, secondMap);
		}

		@Override
		default PairGenerator_<Integer,T,E> enumerate() {
			final Generator_<T,E> This = this;
			return new CustomPairGenerator_<Integer,T,E>(this) {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException, E {
					int k = 0;
					while (true)
						yield(k++,This.next());

				}};
		}
		@Override
		default <U> U fold(BiFunction_<? super U, ? super T, ? extends U, E> function, U init) throws E {
			try {
				while (true)
					init = function.apply(init, next());
			}
			catch (EndOfGenerationException e) {return init;}
			catch (InterruptedException e) {return null;} // TODO ok ?
		}

		@Override
		default void forEach(Consumer_<? super T,E> function) throws E {
			try {
				while (true)
					function.accept(next());
			}
			catch (EndOfGenerationException e) {}
			catch (InterruptedException e) {} // TODO ok ?
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
			return new CustomGenerator_<T,E>() { // TODO ?? qui le parent
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException, E {
					try {
						while (true)
							yield(This.next());
					}
					catch (EndOfGenerationException e) {}
					catch (InterruptedException e) {}
					while (true)
						yield(other.next());
				}
			};
		}


		@Override
		default <U> PairGenerator_<T,U,E> cartesianProduct(Generator_<U,E> generator) {
			List<T> values;
			try {values = toList();}
			catch (Exception e) {return Generators.<T,U>emptyPairGenerator().toChecked();}
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
		default <U> PairGenerator_<T,U,E> gather(Generator_<U,E> generator) {
			Generator_<T,E> This = this;
			return new CustomPairGenerator_<T,U,E>(generator) {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException, E {
					while (true)
						yield(This.next(), generator.next());
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
	}

	public static interface DefaultMapper<S,P,T> extends Mapper<S,P,T>, DefaultGenerator<T> {

		@Override
		default <U> Generator<U> map(Function<? super T, ? extends U> function) {
			return new FollowingMapper<>(this, function);
		}

		@Override
		default <U, E extends Exception> Generator_<U, E> map_(Function_<? super T, ? extends U, E> function) {
			return new FollowingMapper_<>(this, function);
		}
	}

	private static interface DefaultMapper_<S,P,T, E extends Exception> extends Mapper_<S,P,T,E>, DefaultGenerator_<T,E> {


		@Override
		default <U> Generator_<U,E> map(Function_<? super T, ? extends U, E> function) {
			return new FollowingMapper_<>(this, function);
		}

		@Override
		default <U,V> PairGenerator_<U,V,E> biMap(Function_<? super T, ? extends U,E> firstMap, Function_<? super T, ? extends V,E> secondMap) {
			return new FollowingPairMapper_<>(this, firstMap, secondMap);
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
			return new FirstMapper<>(this, Pair::getFirst);
		}


		@Override
		default Generator<T> takeSecond() {
			return new FirstMapper<>(this, Pair::getSecond);
		}


		@Override
		default PairGenerator<T,S> swap() {
			return new FirstPairMapper<>(this, Pair::getSecond, Pair::getFirst);
		}

		@Override
		default <U> PairGenerator<U, T> mapFirst(Function<? super S, ? extends U> function) {
			return new FirstPairPairMapper<>(this, function, (T t) -> t);
		}

		@Override
		default <U> PairGenerator<S, U> mapSecond(Function<? super T, ? extends U> function) {
			return new FirstPairPairMapper<>(this, (S s) -> s, function);
		}

		@Override
		default <E extends Exception> PairGenerator_<S,T,E> toChecked() {
			return new WrappedPairGenerator_<>(this);
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
		default PairGenerator<S,T> filter(BiPredicate<? super S, ? super T> test) {
			return Generators.toPairGenerator(filter((Pair<S,T> p) -> test.test(p.first, p.second)));
		}

	}



	public static interface DefaultPairGenerator_<S,T, E extends Exception> extends PairGenerator_<S,T,E>, DefaultGenerator_<Pair<S,T>,E> {

		@Override
		default Generator_<S, E> takeFirst() {
			return new FirstMapper_<>(this, Pair::getFirst);
		}
		@Override
		default Generator_<T, E> takeSecond() {
			return new FirstMapper_<>(this, Pair::getSecond);
		}
		@Override
		default PairGenerator_<T, S, E> swap() {
			return new FirstPairMapper_<>(this, Pair::getSecond, Pair::getFirst);
		}

		@Override
		default
		<U> PairGenerator_<U,T,E> mapFirst(Function_<? super S, ? extends U,E> function) {
			return new FirstPairPairMapper_<>(this, function, (T t) -> t);
		}

		@Override
		default
		<U> PairGenerator_<S,U,E> mapSecond(Function_<? super T, ? extends U,E> function) {
			return new FirstPairPairMapper_<>(this, (S s) -> s, function);
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
		default PairGenerator_<S,T,E> filter(BiPredicate_<? super S, ? super T,E> test) {
			return Generators.toPairGenerator(filter((Pair<S,T> p) -> test.test(p.first, p.second)));
		}

		@Override
		default PairGenerator<S,T> check() throws E {
			return Generators.fromPairCollection(toList());
		}
	}

	private static interface DefaultPairMapper<S,P,T,U> extends PairMapper<S,P,T,U>, DefaultMapper<S,P,Pair<T,U>>, DefaultPairGenerator<T,U> {


		@Override
		default Generator<T> takeFirst() {
			return new FollowingMapper<>(this, getFirstMap(), Pair::getFirst);
		}


		@Override
		default Generator<U> takeSecond() {
			return new FollowingMapper<>(this, getSecondMap(), Pair::getSecond);
		}


		@Override
		default PairGenerator<U, T> swap() {
			return new FollowingPairMapper<>(this, getSecondMap(), Pair::getSecond, getFirstMap(), Pair::getFirst);
		}

		@Override
		default <V> PairGenerator<V, U> mapFirst(Function<? super T, ? extends V> function) {
			return new FollowingPairPairMapper<>(this, function, (U u) -> u);
		}

		@Override
		default <V> PairGenerator<T, V> mapSecond(Function<? super U, ? extends V> function) {
			return new FollowingPairPairMapper<>(this, (T t) -> t, function);
		}
	}

	private static interface DefaultPairMapper_<S,P,T,U, E extends Exception> extends PairMapper_<S,P,T,U,E>, DefaultMapper_<S,P,Pair<T,U>,E>, DefaultPairGenerator_<T,U,E> {

		@Override
		default Generator_<T, E> takeFirst() {
			return new FollowingMapper_<>(this, getFirstMap(), Pair::getFirst);
		}
		@Override
		default Generator_<U, E> takeSecond() {
			return new FollowingMapper_<>(this, getSecondMap(), Pair::getSecond);
		}
		@Override
		default PairGenerator_<U, T, E> swap() {
			return new FollowingPairMapper_<>(this, getSecondMap(), Pair::getSecond, getFirstMap(), Pair::getFirst);
		}

		@Override
		default
		<V> PairGenerator_<V,U,E> mapFirst(Function_<? super T, ? extends V,E> function) {
			return new FollowingPairPairMapper_<>(this, function, (U u) -> u);
		}

		@Override
		default
		<V> PairGenerator_<T,V,E> mapSecond(Function_<? super U, ? extends V,E> function) {
			return new FollowingPairPairMapper_<>(this, (T t) -> t, function);
		}

		@Override
		default
		<V,W> PairGenerator_<V,W,E> map(
				Function_<? super T, ? extends V,E> firstMap,
				Function_<? super U, ? extends W,E> secondMap) {
			return this.<V>mapFirst(firstMap).mapSecond(secondMap);
		}
	}


	/*------ Implementation ------*/

	private static class StoppingException extends RuntimeException {
		private static final long serialVersionUID = 6160251749314271292L;
	}

	private static abstract class DefaultAbstractGenerator<T> implements AbstractGenerator<T>, Runnable {
		private volatile T value;
		private volatile boolean initialized = false;
		private volatile boolean ready = false;
		private volatile boolean done = false;
		private volatile boolean stopGenerationRequest = false;
		private volatile RuntimeException exception = null;
		private volatile InterruptedException interruption = null;
		private final AbstractGenerator<?> parent;

		public DefaultAbstractGenerator(AbstractGenerator<?> parent) {
			this.parent = parent;
		}

		protected abstract void generateValues() throws EndOfGenerationException, InterruptedException;

		protected final void yield(T value) throws InterruptedException {
//			System.out.println(">> Yield début "+this);
			this.value = value;
			ready = true;
			while (ready) {
				Thread.sleep(0,100);
				if (stopGenerationRequest)
					throw new StoppingException();
			}
//			System.out.println(">> Yield fin   "+this);

		}

		protected final void startGeneration() {
			while (!initialized){}
			done = false;
		}

		protected final void endGeneration() {
			done = true;
			ready = true;
		}

		protected void initializationFinished() {
			initialized = true;
		}

		@Override
		public final boolean isEmpty() {
			try {
				waitValue();
			} catch (InterruptedException e) {}
			return done;
		}

		@Override
		public final void run() {
			startGeneration();
			try {generateValues();}
			catch (StoppingException e) {}
			catch (RuntimeException e) {exception = e;}
			catch (EndOfGenerationException e) {}
			catch (InterruptedException e) {interruption = e;}
//			System.out.println("Sortie ");
//			System.out.println("Je meurs "+this);
			endGeneration();
			if (parent != null)
				parent.stopGeneration();
		}

		public final void stopGeneration() {
			stopGenerationRequest = true;
			while (!done);
		}

		protected final void waitValue() throws InterruptedException {
//			System.out.println(">> Wait début "+this);
			if (done)
				return;
			while (!ready)
				Thread.sleep(0,100);
//			System.out.println(">> Wait fin   "+this);
		}

		protected final T readValue() throws EndOfGenerationException, InterruptedException {
//			System.out.println("Read "+this+" "+exception);
			if (exception != null)
				throw exception;
			if (interruption != null)
				throw interruption;
			if (done)
				throw new EndOfGenerationException();
			return value;
		}

		protected final T getValue() throws EndOfGenerationException, InterruptedException {
//			System.out.println("Read value "+this);
			T t = readValue();
			ready = false;
			return t;
		}
	}

	private static abstract class AbstractThreadGenerator<T> extends DefaultAbstractGenerator<T> {
		private final Thread thread = new Thread(this);
//		private final static ExecutorService executor = Executors.newCachedThreadPool();

		public AbstractThreadGenerator(AbstractGenerator<?> parent) {
			super(parent);
//			System.out.println("New Thread "+thread);
			thread.start();
//			executor.execute(this);
		}
		public AbstractThreadGenerator() {
			this(null);
		}

		protected final void interrupt() {
			thread.interrupt();
		}
	}



	private static abstract class DefaultThreadGenerator<T> extends AbstractThreadGenerator<T> implements DefaultGenerator<T> {
		protected abstract void generate() throws InterruptedException, EndOfGenerationException;

		public DefaultThreadGenerator(AbstractGenerator<?> parent) {
			super(parent);
			initializationFinished();
		}
		public DefaultThreadGenerator() {
			initializationFinished();
		}

		protected  DefaultThreadGenerator(AbstractGenerator<?> parent, boolean init) {
			super(parent);
			if (init)
				initializationFinished();
		}

		@Override
		protected final void generateValues() throws EndOfGenerationException, InterruptedException {
			generate();
		}
		@Override
		public final T next() throws EndOfGenerationException, InterruptedException {
//			System.out.println("Next début "+this);
			waitValue();
//			System.out.println("Next fin   "+this);
			return getValue();
		}
	}


	public static abstract class CustomGenerator<T> extends DefaultThreadGenerator<T> {

		public CustomGenerator(Generator<?> parent) {
			super(parent);
		}
		public CustomGenerator(Generator_<?,?> parent) {
			super(parent);
		}
		public CustomGenerator() {}
	}




	private static class WrappedGenerator_<T,E extends Exception> implements DefaultGenerator_<T,E> {
		private final Generator<T> generator;

		public WrappedGenerator_(Generator<T> generator) {
			this.generator = generator;
		}

		@Override
		public final void stopGeneration() {
			generator.stopGeneration();

		}

		@Override
		public final T next() throws EndOfGenerationException, InterruptedException, E {
			return generator.next();
		}

		@Override
		public final boolean isEmpty() {
			return generator.isEmpty();
		}
	}

	private static abstract class DefaultThreadGenerator_<T,E extends Exception> extends AbstractThreadGenerator<T> implements DefaultGenerator_<T,E> {
		private volatile E exception = null;

		protected abstract void generate() throws InterruptedException, EndOfGenerationException, E;

		public DefaultThreadGenerator_(AbstractGenerator<?> parent) {
			super(parent);
			initializationFinished();
		}
		public DefaultThreadGenerator_() {
			initializationFinished();
		}

		protected  DefaultThreadGenerator_(AbstractGenerator<?> parent, boolean init) {
			super(parent);
			if (init)
				initializationFinished();
		}

		@SuppressWarnings("unchecked")
		@Override
		protected final void generateValues() throws EndOfGenerationException, InterruptedException {
			try {generate();}
			catch (RuntimeException e) {throw e;}
			catch (EndOfGenerationException e) {throw e;}
			catch (InterruptedException e) {throw e;}
			catch (Exception e) {exception = (E)e;}
		}

		@Override
		public final T next() throws EndOfGenerationException, InterruptedException, E {
			waitValue();
			if (exception != null)
				throw exception;
			return getValue();
		}
	}

	public static abstract class CustomGenerator_<T,E extends Exception> extends DefaultThreadGenerator_<T,E> implements DefaultGenerator_<T,E> {

		public CustomGenerator_(Generator<?> parent) {
			super(parent);
			initializationFinished();
		}
		public CustomGenerator_(Generator_<?,?> parent) {
			super(parent);
			initializationFinished();
		}
		public CustomGenerator_() {
			initializationFinished();
		}
	}

	private static class FirstMapper<S,T> extends DefaultThreadGenerator<T> implements DefaultMapper<S,S,T> {
		private final Generator<S> source;
		private final Function<? super S,? extends T> map;
		private volatile FollowingMapper<S, T, ?> next;
		private volatile FollowingMapper_<S, T, ?,?> next_;

		public FirstMapper(Generator<S> source, Function<? super S, ? extends T> map) {
			super(source, false);
			this.source = source;
			this.map = map;
			initializationFinished();
		}

		@Override
		public final <U> Generator<U> map(Function<? super T, ? extends U> function) {
			return new FollowingMapper<>(this, function);
		}

		@Override
		public final <U, E extends Exception> Generator_<U, E> map_(Function_<? super T, ? extends U, E> function) {
			return new FollowingMapper_<>(this, function);
		}

		@Override
		public final void moveGenerationTo(FollowingMapper<S, T, ?> next) {
//			System.out.println("Demande de saut " + this + " ->" + next);
			try {waitValue();}
			catch (InterruptedException e){}
			this.next = next;
			if (isEmpty())
				next.run();
			else
				stopGeneration();
		}

		@Override
		public final void moveGenerationTo(FollowingMapper_<S, T, ?, ?> next) {
			try {waitValue();}
			catch (InterruptedException e){}
			this.next_ = next;
			if (isEmpty())
				next.run();
			else
				stopGeneration();
		}

		@Override
		protected void generate() throws InterruptedException, EndOfGenerationException {
			try {
				while (true)
					yield(map.apply(source.next()));
			}
			catch (StoppingException e) {
				if (next != null) next.run();
				if (next_ != null) next_.run();
				else throw e;
			}

		}

		@Override
		public final Function<? super S,? extends T> getMap() {
			return map;
		}

		@Override
		public final Generator<S> getSource() {
			return source;
		}

		@Override
		public final T readLastValue() throws InterruptedException, EndOfGenerationException {
//			System.out.println("Read last "+this);
			T t = readValue();
			endGeneration();
			return t;
		}

		@Override
		public final <U, V> PairGenerator<U, V> biMap(Function<? super T, ? extends U> firstMap,
				Function<? super T, ? extends V> secondMap) {
			return new FollowingPairMapper<>(this, firstMap, secondMap);
		}

		@Override
		public final <U,V, E extends Exception> PairGenerator_<U,V,E> biMap_(Function_<? super T, ? extends U,E> firstMap, Function_<? super T, ? extends V,E> secondMap) {
			return new FirstPairMapper_<>(this, firstMap, secondMap);
		}

	}

	private static class WrappedMapper_<S,P,T,E extends Exception> implements DefaultMapper_<S,P,T,E> {
		private final Mapper<S,P,T> mapper;

		public WrappedMapper_(Mapper<S,P,T> mapper) {
			this.mapper = mapper;
		}

		@Override
		public final T next() throws EndOfGenerationException, InterruptedException, E {
			return mapper.next();
		}

		@Override
		public final void stopGeneration() {
			mapper.stopGeneration();

		}

		@Override
		public final void moveGenerationTo(FollowingMapper_<S, T, ?, ? super E> next) {
			mapper.moveGenerationTo(next);
		}

		@Override
		public final Function_<? super S, ? extends T, E> getMap() {
			return mapper.getMap().toChecked();
		}

		@Override
		public final T readLastValue() throws InterruptedException, EndOfGenerationException, E {
			return mapper.readLastValue();
		}

		@Override
		public final Generator_<S, ? extends E> getSource() {
			return new WrappedGenerator_<>(mapper.getSource());
		}

		@Override
		public final boolean isEmpty() {
			return mapper.isEmpty();
		}

	}

	private static class FirstMapper_<S,T,E extends Exception> extends DefaultThreadGenerator_<T,E> implements DefaultMapper_<S,S,T,E> {
		private final Generator_<S, ? extends E> source;
		private final Function_<? super S,? extends T, E> map;
		private volatile FollowingMapper_<S, T, ?, ? super E> next;
		private volatile E exception = null;

		public FirstMapper_(Generator_<S, ? extends E> source, Function_<? super S, ? extends T, E> map) {
			super(source, false);
			this.source = source;
			this.map = map;
			initializationFinished();
		}

		@Override
		public final void moveGenerationTo(FollowingMapper_<S, T, ?, ? super E> next) {
			try {waitValue();}
			catch (InterruptedException e){}
			this.next = next;
			if (isEmpty())
				next.run();
			else
				stopGeneration();

		}

		@Override
		public final Function_<? super S, ? extends T, E> getMap() {
			return map;
		}

		@Override
		public final T readLastValue() throws InterruptedException, EndOfGenerationException, E {
			if (exception != null)
				throw exception;
			T t = readValue();
			endGeneration();
			if (exception != null)
				throw exception;
			return t;
		}

		@Override
		public final Generator_<S, ? extends E> getSource() {
			return source;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void generate() throws InterruptedException, EndOfGenerationException, E {
			try {
				while (true)
					yield(map.apply(source.next()));
			}
			catch (StoppingException e) {
				if (next != null) next.run();
				else throw e;
			}
			catch (RuntimeException e) {throw e;}
			catch (EndOfGenerationException e) {throw e;}
			catch (InterruptedException e) {throw e;}
			catch (Exception e) {exception = (E) e;}
		}

	}


	private static abstract class AbstractFollowingMapper<S,P,T>  extends DefaultAbstractGenerator<T> {
		private volatile AbstractFollowingMapper<S, T, ?> next = null;

		public AbstractFollowingMapper(AbstractGenerator<?> parent) {
			super(parent);
		}

		protected abstract void generate() throws InterruptedException, EndOfGenerationException;

		protected final void moveTo(AbstractFollowingMapper<S, T, ?> next) {
			try {waitValue();}
			catch (InterruptedException e){}
			this.next = next;
			if (isEmpty())
				next.run();
			else
				stopGeneration();
		}

		public final void moveGenerationTo(FollowingMapper<S, T, ?> next) {
			moveTo(next);
		}

		@Override
		protected final void generateValues() throws EndOfGenerationException, InterruptedException {
			try {
				generate();
			}
			catch (StoppingException e) {
				if (next != null) next.run();
				else throw e;
			}
		}
	}

	private static class FollowingMapper<S,P,T> extends AbstractFollowingMapper<S,P,T> implements DefaultMapper<S,P,T> {
		private final Mapper<S,?,P> previous;
		private final Function<? super S, ? extends T> map;
		private final Function<? super P, ? extends T> step;
		private final Generator<S> source;

		public FollowingMapper(Mapper<S,?,P> previous, Function<? super S, ? extends T> map, Function<? super P, ? extends T> step) {
			super(previous);
			this.previous = previous;
			this.map = map;
			this.step = step;
			this.source = previous.getSource();
			initializationFinished();
			previous.moveGenerationTo(this);
		}
		public FollowingMapper(Mapper<S,?,P> previous, Function<? super P, ? extends T> step) {
			this(previous, previous.getMap().o(step), step);
		}

		@Override
		public final T next() throws EndOfGenerationException, InterruptedException {
//			System.out.println("Next demande "+this);
			waitValue();
//			System.out.println("Next "+this);
			return getValue();
		}

		@Override
		protected void generate() throws EndOfGenerationException, InterruptedException {
//			System.out.println("Generate de "+this + " sur " +previous);
			yield(step.apply(previous.readLastValue()));
//			System.out.println("Generate de +"+this);
			while (true)
				yield(map.apply(source.next()));
		}

		@Override
		public final Function<? super S,? extends T> getMap() {
			return map;
		}

		@Override
		public Generator<S> getSource() {
			return source;
		}

		@Override
		public final T readLastValue() throws InterruptedException, EndOfGenerationException {
//			System.out.println("Read last "+this);
			T t = readValue();
//			System.out.println("Reading done "+this);
			endGeneration();
			return t;
		}

		@Override
		public final void moveGenerationTo(FollowingMapper_<S, T, ?, ?> next) {
			moveTo(next);
		}

	}

	private static class FollowingMapper_<S,P,T,E extends Exception> extends AbstractFollowingMapper<S,P,T> implements DefaultMapper_<S,P,T,E> {
		private final Mapper_<S,?,P, ? extends E> previous;
		private final Function_<? super S, ? extends T, E> map;
		private final Function_<? super P, ? extends T, E> step;
		private final Generator_<S, ? extends E> source;
		private volatile E exception = null;

		public FollowingMapper_(Mapper_<S,?,P, ? extends E> previous, Function_<? super S, ? extends T, E> map, Function_<? super P, ? extends T, E> step) {
			super(previous);
			this.previous = previous;
			this.map = map;
			this.step = step;
			this.source = previous.getSource();
			initializationFinished();
			previous.moveGenerationTo(this);
		}
		public FollowingMapper_(Mapper_<S,?,P, ? extends E> previous, Function_<? super P, ? extends T, E> step) {
			this(previous, step.p(previous.getMap()),step);
		}
		public FollowingMapper_(Mapper<S,?,P> previous, Function_<? super P, ? extends T, E> step) {
			this(new WrappedMapper_<>(previous), step);
		}



		@Override
		public final T next() throws EndOfGenerationException, InterruptedException, E {
			waitValue();
			if (exception != null)
				throw exception;
			return getValue();
		}

		@Override
		public final <U> Generator_<U, E> map(Function_<? super T, ? extends U, E> function) {
			return new FollowingMapper_<>(this, function);
		}

		@Override
		public final void moveGenerationTo(FollowingMapper_<S, T, ?, ? super E> next) {
			moveTo(next);
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void generate() throws EndOfGenerationException, InterruptedException {
			try {
//				System.out.println("Generate de "+this + " sur " +previous);
				yield(step.apply(previous.readLastValue()));
//				System.out.println("Generate de +"+this);
				while (true)
					yield(map.apply(source.next()));
			}
			catch (RuntimeException e) {throw e;}
			catch (EndOfGenerationException e) {throw e;}
			catch (InterruptedException e) {throw e;}
			catch (Exception e) {exception = (E) e;}

		}

		@Override
		public final Function_<? super S, ? extends T, E> getMap() {
			return map;
		}

		@Override
		public final T readLastValue() throws InterruptedException, EndOfGenerationException, E {
			if (exception != null)
				throw exception;
			T t = readValue();
			endGeneration();
			if (exception != null)
				throw exception;
			return t;
		}

		@Override
		public final Generator_<S, ? extends E> getSource() {
			return source;
		}
	}



	public static abstract class CustomPairGenerator<S,T> extends CustomGenerator<Pair<S,T>> implements DefaultPairGenerator<S, T> {
		public CustomPairGenerator(Generator<?> parent) {
			super(parent);
		}
		public CustomPairGenerator(Generator_<?,?> parent) {
			super(parent);
		}
		public CustomPairGenerator() {}

		protected final void yield(S s, T t) throws InterruptedException {
			yield(new Pair<>(s,t));
		}
	}
	private static class WrappedPairGenerator_<S,T,E extends Exception> implements DefaultPairGenerator_<S,T,E> {
		private final PairGenerator<S,T> generator;

		public WrappedPairGenerator_(PairGenerator<S,T> generator) {
			this.generator = generator;
		}

		@Override
		public Pair<S, T> next() throws EndOfGenerationException, InterruptedException, E {
			return generator.next();
		}

		@Override
		public boolean isEmpty() {
			return generator.isEmpty();
		}

		@Override
		public void stopGeneration() {
			generator.stopGeneration();

		}
	}

	public static abstract class CustomPairGenerator_<S,T,E extends Exception> extends CustomGenerator_<Pair<S,T>,E> implements DefaultPairGenerator_<S, T,E> {
		public CustomPairGenerator_(Generator<?> parent) {
			super(parent);
		}
		public CustomPairGenerator_(Generator_<?,?> parent) {
			super(parent);
		}
		public CustomPairGenerator_() {}
		protected final void yield(S s, T t) throws InterruptedException {
			yield(new Pair<>(s,t));
		}
	}

	private static class FirstPairMapper<S,T,U> extends FirstMapper<S,Pair<T,U>> implements DefaultPairMapper<S,S,T,U> {
		private final Function<? super S, ? extends T> firstMap;
		private final Function<? super S, ? extends U> secondMap;

		public FirstPairMapper(Generator<S> source, Function<? super S, ? extends T> firstMap, Function<? super S, ? extends U> secondMap) {
			super(source, (S s) -> new Pair<>(firstMap.apply(s), secondMap.apply(s)));
			this.firstMap = firstMap;
			this.secondMap = secondMap;
		}

		@Override
		public final Function<? super S, ? extends T> getFirstMap() {
			return firstMap;
		}
		@Override
		public final Function<? super S, ? extends U> getSecondMap() {
			return secondMap;
		}

	}
	private static class FirstPairPairMapper<P,Q,T,U> extends FirstPairMapper<Pair<P,Q>,T,U> {
		public FirstPairPairMapper(PairGenerator<P,Q> source, Function<? super P, ? extends T> firstStep, Function<? super Q, ? extends U> secondStep) {
			super(source, firstStep.p(Pair::getFirst), secondStep.p(Pair::getSecond));
		}
	}

	private static class FirstPairMapper_<S,T,U,E extends Exception> extends FirstMapper_<S,Pair<T,U>,E> implements DefaultPairMapper_<S,S,T,U,E> {
		private final Function_<? super S, ? extends T, E> firstMap;
		private final Function_<? super S, ? extends U, E> secondMap;


		public FirstPairMapper_(Generator_<S,? extends E> source, Function_<? super S, ? extends T,E> firstMap, Function_<? super S, ? extends U,E> secondMap) {
			super(source, (S s) -> new Pair<>(firstMap.apply(s), secondMap.apply(s)));
			this.firstMap = firstMap;
			this.secondMap = secondMap;
		}

		public FirstPairMapper_(Generator<S> source, Function_<? super S, ? extends T,E> firstMap, Function_<? super S, ? extends U,E> secondMap) {
			this(new WrappedGenerator_<>(source), firstMap, secondMap);
		}


		@Override
		public final Function_<? super S, ? extends T, E> getFirstMap() {
			return firstMap;
		}
		@Override
		public final Function_<? super S, ? extends U, E> getSecondMap() {
			return secondMap;
		}
	}

	private static class FirstPairPairMapper_<P,Q,T,U, E extends Exception> extends FirstPairMapper_<Pair<P,Q>,T,U,E> {
		public FirstPairPairMapper_(
				PairGenerator_<P,Q, ? extends E> source,
				Function_<? super P, ? extends T, E> firstStep,
				Function_<? super Q, ? extends U, E> secondStep) {
			super(source, firstStep.p(Pair::getFirst), secondStep.p(Pair::getSecond));
		}
	}

	private static class FollowingPairMapper<S,P,T,U> extends FollowingMapper<S,P,Pair<T,U>> implements DefaultPairMapper<S,P,T,U> {
		private final Function<? super S, ? extends T> firstMap;
		private final Function<? super S, ? extends U> secondMap;

		public FollowingPairMapper(Mapper<S,?,P> previous,
				Function<? super S, ? extends T> firstMap, Function<? super P, ? extends T> firstStep,
				Function<? super S, ? extends U> secondMap, Function<? super P, ? extends U> secondStep) {
			super(previous, (S s) -> new Pair<>(firstMap.apply(s), secondMap.apply(s)), (P p) -> new Pair<>(firstStep.apply(p), secondStep.apply(p)));
			this.firstMap = firstMap;
			this.secondMap = secondMap;
		}
		public FollowingPairMapper(Mapper<S,?,P> previous, Function<? super P, ? extends T> firstStep, Function<? super P, ? extends U> secondStep) {
			this(previous, previous.getMap().o(firstStep), firstStep, previous.getMap().o(secondStep), secondStep);
		}

		@Override
		public final Function<? super S, ? extends T> getFirstMap() {
			return firstMap;
		}
		@Override
		public final Function<? super S, ? extends U> getSecondMap() {
			return secondMap;
		}
	}
	private static class FollowingPairPairMapper<S,P,Q,T,U> extends FollowingPairMapper<S,Pair<P,Q>,T,U> {
		public FollowingPairPairMapper(PairMapper<S,?,P,Q> previous, Function<? super P, ? extends T> firstStep, Function<? super Q, ? extends U> secondStep) {
			super(previous,
					previous.getFirstMap().o(firstStep), firstStep.p(Pair::getFirst),
					previous.getSecondMap().o(secondStep), secondStep.p(Pair::getSecond));
		}
	}

	private static class FollowingPairMapper_<S,P,T,U,E extends Exception> extends FollowingMapper_<S,P,Pair<T,U>,E> implements DefaultPairMapper_<S,P,T,U,E> {
		private final Function_<? super S, ? extends T, E> firstMap;
		private final Function_<? super S, ? extends U, E> secondMap;
		public FollowingPairMapper_(Mapper_<S,?,P, ? extends E> previous,
				Function_<? super S, ? extends T, E> firstMap, Function_<? super P, ? extends T, E> firstStep,
				Function_<? super S, ? extends U, E> secondMap, Function_<? super P, ? extends U, E> secondStep) {
			super(previous, (S s) -> new Pair<>(firstMap.apply(s), secondMap.apply(s)), (P p) -> new Pair<>(firstStep.apply(p), secondStep.apply(p)));
			this.firstMap = firstMap;
			this.secondMap = secondMap;
		}
		public FollowingPairMapper_(Mapper_<S,?,P, ? extends E> previous,
				Function_<? super P, ? extends T, E> firstStep,
				Function_<? super P, ? extends U, E> secondStep) {
			this(previous, firstStep.p(previous.getMap()), firstStep, secondStep.p(previous.getMap()), secondStep);
		}

		@Override
		public final Function_<? super S, ? extends T, E> getFirstMap() {
			return firstMap;
		}
		@Override
		public final Function_<? super S, ? extends U, E> getSecondMap() {
			return secondMap;
		}

	}
	private static class FollowingPairPairMapper_<S,P,Q,T,U, E extends Exception> extends FollowingPairMapper_<S,Pair<P,Q>,T,U,E> {
		public FollowingPairPairMapper_(
				PairMapper_<S,?,P,Q, ? extends E> previous,
				Function_<? super P, ? extends T, E> firstStep,
				Function_<? super Q, ? extends U, E> secondStep) {
			super(previous,
					firstStep.p(previous.getFirstMap()), firstStep.p(Pair::getFirst),
					secondStep.p(previous.getSecondMap()), secondStep.p(Pair::getSecond));
		}
	}



	public static interface GeneratorGenerator<T> extends Generator<Generator<T>> {
		GeneratorGenerator<T> cartesianProduct();
		Generator<T> concat();
		<U> Generator<Generator<U>> mapE(Function<? super T, ? extends U> function);
	}

	public static interface DefaultGeneratorGenerator<T> extends DefaultGenerator<Generator<T>>, GeneratorGenerator<T> {
		default GeneratorGenerator<T> cartesianProduct() {
			return Generators.cartesianProduct(this);
		}
		default Generator<T> concat() {
			return fold((Generator<T> g, Generator<T> t) -> g.append(t), emptyGenerator());
		}
		default <U> Generator<Generator<U>> mapE(Function<? super T, ? extends U> function) {
			return null;
		}
	}


	public abstract static class CustomGeneratorGenerator<T> extends CustomGenerator<Generator<T>> implements DefaultGeneratorGenerator<T> {
		public CustomGeneratorGenerator(Generator<?> parent) {
			super(parent);
		}
		public CustomGeneratorGenerator(Generator_<?,?> parent) {
			super(parent);
		}
		public CustomGeneratorGenerator() {}
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
		return new CustomPairGenerator<S, T>(generator) {
			@Override
			protected void generate() throws InterruptedException, EndOfGenerationException {
				while (true)
					yield(generator.next());
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
		return new CustomPairGenerator_<S, T, E>(generator) {
			@Override
			protected void generate() throws InterruptedException, EndOfGenerationException, E {
				while (true)
					yield(generator.next());
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
	public static <S> Generator<S> fromCollection(Collection<S> collection) {
		return new CustomGenerator<S>() {
			@Override
			protected void generate() throws InterruptedException, EndOfGenerationException {
				for (S s : collection)
					yield(s);

			}};
	}

	public static <S,T> PairGenerator<S,T> fromMap(Map<S,T> map) {
		return new CustomPairGenerator<S,T>() {
			@Override
			protected void generate() throws InterruptedException, EndOfGenerationException {
				for (Entry<S,T> s : map.entrySet())
					yield(s.getKey(),s.getValue());

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

	/**
	 * Builds a {@link StandardPairGenerator} that produce the values of a {@link Collection} of pairs.
	 *
	 * @param <S> 				The first component type
	 * @param <T> 				The second component type
	 *
	 * @param c					The collection to convert
	 * @return					A {@link StandardPairGenerator} of the values of the collection
	 */
	public static <S,T> CustomPairGenerator<S,T> fromPairCollection(Collection<Pair<S,T>> c) {
		return new CustomPairGenerator<S,T>(){
			@Override
			protected void generate() throws InterruptedException {
				for (Pair<S,T> p : c)
					yield(p);
			}};
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
		return new CustomGenerator<S>() {
			@Override
			protected void generate() throws InterruptedException, EndOfGenerationException {
				yield(s);
			}};
	}

	public static <S, T> PairGenerator<S, T> fromPairSingleton(S s, T t) {
		return new CustomPairGenerator<S, T>() {
			@Override
			protected void generate() throws InterruptedException, EndOfGenerationException {
				yield(s,t);
			}};
	}

	/**
	 * <p>Generates a empty {@link StandardPairGenerator}.</p>
	 *
	 * @param <S> 				The generators values' type
	 *
	 * @return A {@link Generator} that produce no value
	 */
	public static <S> Generator<S> emptyGenerator() {
		return new CustomGenerator<S>() {@Override protected void generate() {}};
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
		return new CustomPairGenerator<S,T>() {@Override protected void generate() {}};
	}

	public static <S> GeneratorGenerator<S> emptyGeneratorGenerator() {
		return new CustomGeneratorGenerator<S>() {@Override protected void generate() {}};
	}


	public static <S> Generator<S> constant(S s) {
		return new CustomGenerator<S>() {
			@Override
			protected void generate() throws InterruptedException, EndOfGenerationException {
				while (true)
					yield(s);
			}};
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


	public static <S> GeneratorGenerator<S> cartesianProduct(Generator<Generator<S>> generators) {
		final List<List<S>> values = generators.map(Generator::toList).toList();
		final int noc = values.size();
		return noc == 0 ? emptyGeneratorGenerator() : new CustomGeneratorGenerator<S>() {
			@Override
			protected void generate() throws InterruptedException, EndOfGenerationException {
				int[] index = new int[noc];
				Arrays.fill(index, 0);
				int[] lenghts = new int[noc];
				Generators.fromCollection(values).enumerate().forEach((Integer k, List<S> l) -> lenghts[k] = l.size());
				for (int j : lenghts)
					if (j == 0)
						return;
				while(index[0] != lenghts[0]) {
					yield(new CustomGenerator<S>() {
						private int[] pIndex = index.clone();
						@Override
						protected void generate() throws InterruptedException, EndOfGenerationException {
							int k = 0;
							for (int i : pIndex)
								yield(values.get(k++).get(i));
							}});
					int k = noc -1;
					++index[k];
					while (k>0 && index[k]==lenghts[k]) {
						index[k] = 0;
						++index[--k];
					}
				}
			}};
	}
}
