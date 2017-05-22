package wyvc.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import wyvc.utils.CheckedFunctionalInterface.CheckedBiConsumer;
import wyvc.utils.CheckedFunctionalInterface.CheckedBiFunction;
import wyvc.utils.CheckedFunctionalInterface.CheckedBiPredicate;
import wyvc.utils.CheckedFunctionalInterface.CheckedConsumer;
import wyvc.utils.CheckedFunctionalInterface.CheckedFunction;
import wyvc.utils.CheckedFunctionalInterface.CheckedPredicate;
import wyvc.utils.CheckedFunctionalInterface.CheckedSupplier;
import wyvc.utils.Generator.EndOfGenerationException;

/**
 * <p>Python-like generators.</p>
 *
 * <p>
 * The {@link StandardGenerator} class does not handle exceptions, they are supported
 * by the {@link CheckedGenerator} one.
 * </p>
 *
 * <p>
 * They enable easy computation on collections of objects without storing any container.
 * </p>
 *
 * <p>
 * The {@link Generator} class can not be used to handle {@link StandardGenerator} or
 * {@link CheckedGenerator} because handling exception modify methods' signatures.
 * </p>
 *
 * <p>
 * Because the generation runs in an separate thread, they can improve parallelism
 * (values are computed before being needed).
 * </p>
 *
 * <p>
 * In order to support efficiently checked exceptions, methods that use standard
 * functional interfaces in the {@link StandardGenerator} class are provided with a
 * {@link CheckedFunctionalInterface} version starting with a capital letter (to avoid
 * name resolution issues). Calling one of these methods will return a {@link CheckedGenerator}.
 * </p>
 *
 * @author Baptiste Pauget
 *
 * @param <T> The type of generated values
 * @see StandardGenerator
 * @see CheckedGenerator
 */
public abstract class Generator<T> extends Thread {
	private final Generator<?> parent;
	private volatile T value;
	private volatile boolean ready = false;
	private volatile boolean done = false;


	/**
	 * <p>Constructs a generator with the <code>parent</code> other as parent generator.</p>
	 *
	 * <p>
	 * If the generation is interrupted by an exception, the parent generator will
	 * be stopped.
	 * </p>
	 *
	 * @param parent The parent generator
	 */
	public Generator(Generator<?> parent) {
		this.parent = parent;
		start();
	}


	/**
	 * <p>Constructs a generator with no parent.</p>
	 */
	public Generator() {
		this(null);
	}

	public boolean isEmpty() {
		return done;
	}

	/**
	 * <p>Internal usage, redefined by {@link StandardGenerator} and {@link CheckedGenerator}.</p>
	 *
	 * @throws InterruptedException		Provided to use easily the yield method
	 * @throws EndOfGenerationException Provided to use easily an other generator
	 */
	protected abstract void generateValues() throws InterruptedException, EndOfGenerationException;

	/**
	 * <p>Registers a new value to generate.</p>
	 *
	 * <p>
	 * This method will wait until the value has been read before returning.
	 * </p>
	 *
	 * <p>
	 * It is intended to be used in the definition of the <code>generate</code> method
	 * when explicitly instantiating a generator with an anonymous class.
	 * </p>
	 *
	 * @param t The value to yield
	 * @throws InterruptedException Thrown when the generator's thread is interrupted.
	 */
	protected void yield(T t) throws InterruptedException {
		value = t;
		ready = true;
		while (ready)
			Thread.sleep(3);
	}

	/**
	 * <p>Main method of the generation thread.</p>
	 *
	 * <p>
	 * It will be call at construction time.
	 * </p>
	 */
	public final void run() {
		try {
			generateValues();
		}
		catch (InterruptedException e) {}
		catch (EndOfGenerationException e) {}
		catch (RuntimeException e) {}
		ready = true;
		done = true;
		if (parent != null)
			parent.stopGeneration();
	}



	/**
	 * <p>Stop the generation thread, regardless its state.</p>
	 *
	 * <p>
	 * This method is provided to stop the parents of an interrupted generator.
	 * </p>
	 */
	private void stopGeneration() {
		interrupt();
		if (parent != null)
			parent.stopGeneration();
	}

	public <E extends Exception> E stopGeneration(E e) {
		stopGeneration();
		return e;
	}

	/**
	 * <p>Internal usage, used in the <code>generateValues</code> method.</p>
	 *
	 * <p>
	 * Wait until a new value is available or an interruption occurred in the generation thread.
	 * </p>
	 *
	 * <p>
	 * The method <code>getValue</code> gives an access to the calculated value.
	 * </p>
	 *
	 * @throws EndOfGenerationException Thrown when no more values will be generated
	 * @throws InterruptedException		Thrown when the generation thread is interrupted
	 */
	protected void waitValue() throws EndOfGenerationException, InterruptedException {
		if (done)
			throw new EndOfGenerationException();
		while (!ready)
			Thread.sleep(3);
		if (done)
			throw new EndOfGenerationException();
	}

	/**
	 * <p>Internal usage, used in the <code>generateValues</code> method.</p>
	 *
	 * <p>
	 * Return the last value calculated.
	 * </p>
	 *
	 * <p>
	 * This method does not check that a (new) value is actually ready, which should
	 * be done with the <code>waitValue</code> method.
	 * </p>
	 *
	 * @return The last value calculated
	 */
	protected T getValue() {
		T t = value;
		ready = false;
		return t;
	}



	/**
	 * Notifies the end of the generation.
	 *
	 * @author Baptiste Pauget
	 *
	 */
	public static class EndOfGenerationException extends Exception {
		private static final long serialVersionUID = -4492584892378046784L;

	}



	/**
	 * <p>Base class for every generator that does not support exceptions.</p>
	 *
	 * <p>
	 * The only method to implement to instantiate a {@link StandardGenerator} is <code>generate</code>
	 * </p>
	 *
	 * <p>
	 * To avoid an annoying used of the <code>toCheckedGenerator</code> method, the ones that return a
	 * generator that could be used with exception are provided with a capital initial letter.
	 * </p>
	 *
	 * @author Baptiste Pauget
	 *
	 * @param <T>				The type of the generated values
	 */
	public static abstract class StandardGenerator<T> extends Generator<T> {

		/**
		 * <p>Constructs a generator with no parent.</p>
		 */
		public StandardGenerator() {}

		/**
		 * <p>Constructs a generator with the <code>parent</code> other as parent generator.</p>
		 *
		 * <p>
		 * If the generation is interrupted by an exception, the parent generator will
		 * be stopped.
		 * </p>
		 *
		 * @param parent 		The parent generator
		 */
		public StandardGenerator(Generator<?> parent) {
			super(parent);
		}

		/**
		 * <p>Method that will generate the values using <code>yield</code>.</p>
		 *
		 * @throws InterruptedException		Provided to use easily the yield method
		 * @throws EndOfGenerationException Provided to use easily an other generator
		 */
		protected abstract void generate() throws InterruptedException, EndOfGenerationException;

		/**
		 * <p>Implementation of {@link Generator}'s <code>generateValues</code> method</p>.
		 */
		protected final void generateValues() throws InterruptedException, EndOfGenerationException {
			generate();
		}

		/**
		 * <p>Provides the next value of the generator.</p>
		 *
		 * @return 							The next value
		 * @throws EndOfGenerationException	Thrown if no more values can be generated
		 * @throws InterruptedException		Thrown if the generation thread is interrupted
		 */
		public T next() throws EndOfGenerationException, InterruptedException {
			waitValue();
			return getValue();
		}

		/**
		 * <p>Builds a list of the generator's remaining values.
		 *
		 * @return 				A list of the values generated
		 */
		public List<T> toList() {
			List<T> l = new ArrayList<>();
			try {
				while (true)
					l.add(next());
			} catch (EndOfGenerationException e) {
				return l;
			} catch (InterruptedException e) {
				return null;
			}
		}

		/**
		 * <p>Provides a {@link CheckedGenerator} that will generate the same values.</p>
		 *
		 * <p>
		 * This method is provided to help complying to some methods' signature.
		 * </p>
		 *
		 * @param <E>			The type of exceptions that could be thrown by the {@link CheckedGenerator}
		 *
		 * @return 				A {@link CheckedGenerator} equivalent to this {@link StandardGenerator}
		 */
		public <E extends Exception> CheckedGenerator<T,E> toCheckedGenerator() {
			return new CheckedGenerator<T,E>(this) {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException, E {
					while (true)
						yield(StandardGenerator.this.next());

				}};
		}

		/**
		 * <p>Extends a collection with the remaining values of the generator.</p>
		 *
		 * @param collection 	The collection to  extend
		 */
		public void addToCollection(Collection<T> collection) {
			try {
				while (true)
					collection.add(next());
			}
			catch (EndOfGenerationException e) {}
			catch (InterruptedException e) {}
		}

		/**
		 * <p>Applies a function to each value of the generator</p>.
		 *
		 * @param consumer 		The function to apply
		 */
		public void forEach(Consumer<T> consumer) {
			try {
				while (true)
					consumer.accept(next());
			}
			catch (EndOfGenerationException e) {}
			catch (InterruptedException e) {}
		}

		/**
		 * <p>Applies a function to each value of the generator</p>.
		 *
		 * @param <E>			The type of exceptions thrown by the consumer
		 *
		 * @param consumer 		The function to apply
		 * @throws E			Thrown if a call to <code>consumer</code> thrown an E exception
		 *
		 * @see CheckedConsumer
		 */
		@SuppressWarnings("unchecked")
		public <E extends Exception> void ForEach(CheckedConsumer<T, E> consumer) throws E {
			try {
				while (true)
					consumer.accept(next());
			}
			catch (EndOfGenerationException e) {}
			catch (InterruptedException e) {}
			catch (Exception e) {throw stopGeneration((E) e);}
		}

		/**
		 * <p>Applies a function to each value of the generator, using the result
		 * of the previous call to compute the next result.</p>
		 *
		 * <p>
		 * This is equivalent to
		 * <code>f(f(f(...f(init, next()),...),next()),next())</code>
		 * </p>
		 *
		 * @param <A>			The type of the folding function results
		 *
		 * @param function 		The function to apply
		 * @param init			The initial value of the argument
		 * @return				The result of the call on each value
		 */
		public <A> A fold(BiFunction<A,T,A> function, A init) {
			try {
				while (true)
					init = function.apply(init, next());
			}
			catch (EndOfGenerationException e) {}
			catch (InterruptedException e) {}
			return init;
		}

		/**
		 * <p>Applies a function to each value of the generator, using the result
		 * of the previous call to compute the next result.</p>
		 *
		 * <p>
		 * This is equivalent to
		 * <code>f(f(f(...f(init, next()),...),next()),next())</code>
		 * </p>
		 *
		 * @param <A>			The type of the folding function result
		 * @param <E>			The type of exceptions to thrown by the folding function
		 *
		 *
		 * @param function 		The function to apply
		 * @param init			The initial value of the argument
		 * @return				The result of the call on each value
		 * @throws E 			Thrown if a call to <code>function</code> thrown an E exception
		 *
		 * @see CheckedBiFunction
		 */
		@SuppressWarnings("unchecked")
		public <A, E extends Exception> A Fold(CheckedBiFunction<A,T,A, E> function, A init) throws E {
			try {
				while (true)
					init = function.apply(init, next());
			}
			catch (EndOfGenerationException e) {}
			catch (InterruptedException e) {}
			catch (Exception e) {throw stopGeneration((E) e);}
			return init;
		}

		@SuppressWarnings("unchecked")
		public <U> StandardGenerator<U> convert() {
			return map((T t) -> (U) t);
		}

		/**
		 * <p>Transforms each value of the generator.</p>
		 *
		 * @param <U>			The type of the transformation function results
		 *
		 * @param function 		The transformation function
		 * @return				A {@link StandardGenerator} of the transformed values
		 */
		public <U> StandardGenerator<U> map(Function<? super T, ? extends U> function) {
			StandardGenerator<T> This = this;
			return new StandardGenerator<U>(this) {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException {
					while (true)
						yield(function.apply(This.next()));
				}};
		}

		public <U,V> StandardPairGenerator<U,V> biMap(Function<? super T, ? extends U> function1, Function<? super T, ? extends V> function2) {
			StandardGenerator<T> This = this;
			return new StandardPairGenerator<U,V>(this) {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException {
					while (true){
						final T t = This.next();
						yield(new Pair<>(function1.apply(t), function2.apply(t)));
					}
				}};
		}

		/**
		 * <p>Transforms each value of the generator.</p>
		 *
		 * @param <U>			The type of the transformation function result
		 * @param <E>			The type of exceptions to thrown by the transformation function
		 *
		 *
		 * @param function 		The transformation function
		 * @return				A {@link CheckedGenerator} of the transformed values
		 *
		 * @see CheckedFunction
		 * @see CheckedGenerator
		 */
		public <U, E extends Exception> CheckedGenerator<U, E> Map(CheckedFunction<? super T, ? extends U, E> function) {
			return this.<E>toCheckedGenerator().map(function);
		}

		/**
		 * <p>Transforms each value of the generator using its index.</p>
		 *
		 * @param <U>			The type of the transformation function results
		 *
		 * @param biFunction 	The transformation function
		 * @return				A {@link StandardGenerator} of the transformed values
		 */
		public <U> StandardGenerator<U> enumMap(BiFunction<? super Integer, ? super T, ? extends U> biFunction) {
			StandardGenerator<T> This = this;
			return new StandardGenerator<U>(this) {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException {
					int k = 0;
					while (true)
						yield(biFunction.apply(k++, This.next()));
				}};
		}

		/**
		 * <p>Transforms each value of the generator using its index.</p>
		 *
		 * @param <U>			The type of the transformation function result
		 * @param <E>			The type of exceptions to thrown by the transformation function
		 *
		 * @param function 	The transformation function
		 * @return				A {@link CheckedGenerator} of the transformed values
		 *
		 * @see CheckedBiFunction
		 * @see CheckedGenerator
		 */
		public <U, E extends Exception> CheckedGenerator<U, E> EnumMap(CheckedBiFunction<? super Integer, ? super T, ? extends U, E> function) {
			return this.<E>toCheckedGenerator().enumMap(function);
		}

		/**
		 * <p>Constructs a unique generator from <code>this</code> and an other one.</p>
		 *
		 * <p>
		 * The generation will stop as soon as one of the component fails to provide a value.
		 * </p>
		 *
		 * @param <U>			The type of the <code>generator</code>'s values.
		 *
		 * @param generator 	The generator of the second component
		 * @return 				A {@link StandardPairGenerator} delivering pair of values of <code>this</code> and <code>generator</code>
		 */
		public <U> StandardPairGenerator<T,U> gather(StandardGenerator<U> generator) {
			StandardGenerator<T> This = this;
			return new StandardPairGenerator<T,U>(this) {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException {
					while (true)
						yield(new Pair<>(This.next(), generator.next()));
				}};
		}

		/**
		 * <p>Constructs a unique generator from <code>this</code> and an other one.</p>
		 *
		 * <p>
		 * The generation will stop as soon as one of the component fails to provide a value,
		 * or an exception occurs.
		 * </p>
		 *
		 * @param <U>			The type of the <code>generator</code>'s values.
		 * @param <E>			The type of exceptions to thrown by the <code>generator</code>
		 *
		 * @param generator 	The generator of the second component
		 * @return 				A {@link CheckedPairGenerator} delivering pair of values of <code>this</code> and <code>generator</code>
		 *
		 * @see CheckedGenerator
		 */
		public <U, E extends Exception> CheckedPairGenerator<T,U, E> Gather(CheckedGenerator<U, E> generator) {
			return this.<E>toCheckedGenerator().gather(generator);
		}

		/**
		 * <p>Removes the values that does not pass the test.</p>
		 *
		 * @param test 			The test used to select values
		 * @return 				A {@link StandardGenerator} of the fitting values
		 */
		public StandardGenerator<T> filter(Predicate<? super T> test) {
			StandardGenerator<T> This = this;
			return new StandardGenerator<T>(this) {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException {
					while (true) {
						T t = This.next();
						if (test.test(t))
							yield(t);
					}
				}};
		}

		/**
		 * <p>Removes all occurrences of <code>value</code></p>
		 *
		 * <p>
		 * Object equality is used, so this method is mainly useful to
		 * clear <code>null</code> values from the generated ones.
		 * </p>
		 *
		 * @param value 		The value to remove
		 * @return 				A {@link StandardGenerator} of the other values
		 */
		public StandardGenerator<T> remove(T value) {
			return filter((T t) -> t != value);
		}

		/**
		 * <p>Looks for the first value matching the test Test.</p>
		 *
		 * @param <E>			The type of exceptions to throw if not found.
		 *
		 * @param test 			The test to use
		 * @param exception 	A exception provider to call if no fitting value is found
		 * @return 				The first value passing the test
		 * @throws E 			Thrown when no fitting value is found
		 */
		public <E extends Exception> T findOrThrow(Predicate<? super T> test, Supplier<E> exception) throws E {
			try {
				while (true) {
					T t = next();
					if (test.test(t))
						return t;
				}
			}
			catch (EndOfGenerationException e) {}
			catch (InterruptedException e) {}
			throw exception.get();
		}

		/**
		 * <p>Looks for the first value equals to <code>t</code>.</p>
		 *
		 * @param <E>			The type of exceptions to throw if not found.
		 *
		 * <p>
		 * The Object method <code>equals</code> is used to compare values.
		 * </p>
		 *
		 * @param t				The value to look for
		 * @param exception 	A exception provider to call if no fitting value is found
		 * @return				The first equal value
		 * @throws E			Thrown when no fitting value is found
		 */
		public <E extends Exception> T findOrThrow(T t, Supplier<E> exception) throws E {
			return findOrThrow((T n) -> n.equals(t), exception);
		}

		/**
		 * <p>Looks for the first value matching the test Test.</p>
		 *
		 * @param test 			The test to use
		 * @return 				The first value passing the test or null when no such value is found
		 */
		public T find(Predicate<? super T> test) {
			return fold((T t, T n) -> test.test(n) ? n : t, null);
		}

		/**
		 * <p>Looks for the first value equals to <code>t</code>.</p>
		 *
		 * <p>
		 * The Object method <code>equals</code> is used to compare values.
		 * </p>
		 *
		 * @param t				The value to look for
		 * @return				The first equal value or null when no such value is found
		 */
		public T find(T t) {
			return find((T n) -> n.equals(t));
		}

		/**
		 * <p>Replace values that match the <code>test</code> with the one provided by the supplier</p>
		 *
		 * @param test			The test to use
		 * @param nv			The replacing value supplier
		 * @return				A {@link StandardGenerator} of the values after replacement
		 */
		public StandardGenerator<T> replace(Predicate<? super T> test, Supplier<? extends T> nv) {
			return map((T t) -> test.test(t) ? nv.get() : t);
		}

		/**
		 * <p>Replace occurrences of <code>t</code> with the one provided by the supplier</p>
		 *
		 * <p>
		 * Object equality is used, so this method is mainly useful to
		 * replace <code>null</code> values from the generated ones.
		 * </p>
		 *
		 * @param value				The value to replace
		 * @param nv			The replacing value supplier
		 * @return				A {@link StandardGenerator} of the values after replacement
		 */
		public StandardGenerator<T> replace(T value, Supplier<T> nv) {
			return replace((T t) -> t == value, nv);
		}

		/**
		 * <p>Replace values that match the <code>test</code> with the one provided by the supplier</p>
		 *
		 * @param <E>			The type of exceptions thrown by the test or the supplier.
		 *
		 * @param test			The test to use
		 * @param nv			The replacing value supplier
		 * @return				A {@link CheckedGenerator} of the values after replacement
		 *
		 * @see CheckedPredicate
		 * @see CheckedSupplier
		 */
		public <E extends Exception> CheckedGenerator<T,E> Replace(CheckedPredicate<? super T, E> test, CheckedSupplier<? extends T, E> nv) {
			return this.<E>toCheckedGenerator().replace(test, nv);
		}

		/**
		 * <p>Replace occurrences of <code>t</code> with the one provided by the supplier.</p>
		 *
		 * <p>
		 * Object equality is used, so this method is mainly useful to
		 * replace <code>null</code> values from the generated ones.
		 * </p>
		 *
		 * @param <E>			The type of exceptions thrown by the supplier.
		 *
		 * @param value				The value to replace
		 * @param nv			The replacing value supplier
		 * @return				A {@link CheckedGenerator} of the values after replacement
		 *
		 * @see CheckedSupplier
		 */
		public <E extends Exception> CheckedGenerator<T,E> Replace(T value, CheckedSupplier<T, E> nv) {
			return this.<E>toCheckedGenerator().replace(value, nv);
		}

		/**
		 * <p>Appends the values of <code>other</code> at the end of the one of <code>this</code>.</p>
		 *
		 * @param other			The generator to concatenate
		 * @return				A {@link StandardGenerator} of the value of <code>this</code> followed by the ones of <code>other</code>
		 */
		public StandardGenerator<T> append(StandardGenerator<T> other) {
			StandardGenerator<T> This = this;
			return new StandardGenerator<T>(this) {
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

		/**
		 * <p>Enumerates the values with an {@link Integer} index.</p>
		 *
		 * @return				A {@link StandardPairGenerator} of the values with their index.
		 */
		public StandardPairGenerator<Integer, T> enumerate() {
			StandardGenerator<T> This = this;
			return new StandardPairGenerator<Integer, T>(this) {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException {
					int k = 0;
					while (true)
						yield(new Pair<>(k++, This.next()));
				}};
		}

		/**
		 * <p>Checks a property on each value of the generator.</p>
		 *
		 * @param test			The test to use
		 * @return				<code>true</code> if every value pass the test, else <code>false</code>
		 */
		public boolean forAll(Predicate<? super T> test) {
			return fold((Boolean b, T t) -> b && test.test(t), true);
		}


		/**
		 * <p>Checks a property on each value of the generator.</p>
		 *
		 * @param <E>			The type of exceptions thrown by the test.
		 *
		 * @param test			The test to use
		 * @return				<code>true</code> if every value pass the test, else <code>false</code>
		 * @throws E			Thrown if a call to the test throws it
		 *
		 * @see CheckedPredicate
		 */
		public <E extends Exception> boolean ForAll(CheckedPredicate<? super T, E> test) throws E {
			return Fold((Boolean b, T t) -> b && test.test(t), true);
		}

		public StandardGenerator<T> insert(T value) {
			StandardGenerator<T> This = this;
			return new StandardGenerator<T>(this) {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException {
					yield(value);
					while (true)
						yield(This.next());

				}
			};
		}
	}


	/**
	 * <p>Base class for every generator that supports exceptions.</p>
	 *
	 * <p>
	 * The only method to implement to instantiate a {@link StandardGenerator} is <code>generate</code>
	 * </p>
	 *
	 * @author Baptiste Pauget
	 *
	 * @param <T>				The type of the generated values
	 * @param <E>				The type of exceptions that may be thrown during generation
	 */
	public static abstract class CheckedGenerator<T, E extends Exception> extends Generator<T> {
		private E exception = null;

		/**
		 * <p>Constructs a generator with no parent.</p>
		 */
		public CheckedGenerator() {}

		/**
		 * <p>Constructs a generator with the <code>parent</code> other as parent generator.</p>
		 *
		 * <p>
		 * If the generation is interrupted by an exception, the parent generator will
		 * be stopped.
		 * </p>
		 *
		 * @param parent 		The parent generator
		 */
		public CheckedGenerator(Generator<?> parent) {
			super(parent);
		}

		/**
		 * <p>Method that will generate the values using <code>yield</code>.</p>
		 *
		 * @throws InterruptedException		Provided to use easily the yield method
		 * @throws EndOfGenerationException Provided to use easily an other generator
		 * @throws E						Provided to use the generator with custom exceptions
		 */
		protected abstract void generate() throws InterruptedException, EndOfGenerationException, E;

		/**
		 * <p>Implementation of {@link Generator}'s <code>generateValues</code> method</p>.
		 */
		@SuppressWarnings("unchecked")
		public final void generateValues() throws InterruptedException, EndOfGenerationException {
			try {
				generate();
			}
			catch (InterruptedException e) {throw e;}
			catch (EndOfGenerationException e) {throw e;}
			catch (Exception e) {exception = (E) e;}
		}

		/**
		 * <p>Provides the next value of the generator.</p>
		 *
		 * @return 							The next value
		 * @throws EndOfGenerationException	Thrown if no more values can be generated
		 * @throws InterruptedException		Thrown if the generation thread is interrupted
		 * @throws E						Thrown if the generation has thrown it in the generation thread
		 */
		public T next() throws EndOfGenerationException, InterruptedException, E {
			try {
				waitValue();
			}
			catch (EndOfGenerationException e) {
				if (exception != null)
					throw exception;
				throw e;
			}
			return getValue();
		}

		/**
		 * <p>Builds a list of the generator's remaining values.
		 *
		 * @return 				A list of the values generated
		 * @throws E			Thrown if the generation has thrown it
		 */
		public List<T> toList() throws E {
			List<T> l = new ArrayList<>();
			try {
				while (true)
					l.add(next());
			}
			catch (EndOfGenerationException e) {return l;}
			catch (InterruptedException e) {return null;}
		}

		/**
		 * <p>Applies a function to each value of the generator</p>.
		 *
		 * @param consumer 		The function to apply
		 * @throws E			Thrown if the generation has thrown it
		 */
		@SuppressWarnings("unchecked")
		public void forEach(CheckedConsumer<T, E> consumer) throws E {
			try {
				while (true)
					consumer.accept(next());
			}
			catch (EndOfGenerationException e) {}
			catch (InterruptedException e) {}
			catch (Exception e) {throw stopGeneration((E) e);}
		}

		/**
		 * <p>Applies a function to each value of the generator, using the result
		 * of the previous call to compute the next result.</p>
		 *
		 * <p>
		 * This is equivalent to
		 * <code>f(f(f(...f(init, next()),...),next()),next())</code>
		 * </p>
		 *
		 * @param <A> 			The type of the result
		 *
		 * @param function 		The function to apply
		 * @param init			The initial value of the argument
		 * @return				The result of the call on each value
		 * @throws E			Thrown if the generation has thrown it
		 */
		@SuppressWarnings("unchecked")
		public <A> A fold(CheckedBiFunction<A,T,A, E> function, A init) throws E {
			try {
				while (true)
					init = function.apply(init, next());
			}
			catch (EndOfGenerationException e) {}
			catch (InterruptedException e) {}
			catch (Exception e) {throw stopGeneration((E) e);}
			return init;
		}


		/**
		 * <p>Transforms each value of the generator.</p>
		 *
		 * @param <U> 			The type of the transformation function result
		 *
		 * @param function 		The transformation function
		 * @return				A {@link StandardGenerator} of the transformed values
		 */
		public <U> CheckedGenerator<U, E> map(CheckedFunction<? super T, ? extends U, E> function) {
			CheckedGenerator<T, E> This = this;
			return new CheckedGenerator<U, E>(this) {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException, E {
					while (true)
						yield(function.apply(This.next()));
				}};
		}

		/**
		 * <p>Transforms each value of the generator using its index.</p>
		 *
		 * @param <U> 			The type of the transformation function result
		 *
		 * @param function 	The transformation function
		 * @return				A {@link CheckedGenerator} of the transformed values
		 *
		 * @see CheckedBiFunction
		 */
		public <U> CheckedGenerator<U, E> enumMap(CheckedBiFunction<? super Integer, ? super T, ? extends U, E> function) {
			CheckedGenerator<T, E> This = this;
			return new CheckedGenerator<U, E>(this) {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException, E {
					int k = 0;
					while (true)
						yield(function.apply(k++, This.next()));
				}};
		}

		/**
		 * <p>Constructs a unique generator from <code>this</code> and an other one.</p>
		 *
		 * <p>
		 * The generation will stop as soon as one of the component fails to provide a value,
		 * or an exception occurs.
		 * </p>
		 *
		 * @param <U> 			The type of the transformation function result
		 *
		 * @param generator 	The generator of the second component
		 * @return 				A {@link CheckedPairGenerator} delivering pair of values of <code>this</code> and <code>generator</code>
		 */
		public <U> CheckedPairGenerator<T,U, E> gather(CheckedGenerator<U, E> generator) {
			CheckedGenerator<T, E> This = this;
			return new CheckedPairGenerator<T,U, E>(this) {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException, E {
					while (true)
						yield(This.next(), generator.next());
				}};
		}

		/**
		 * <p>Removes the values that does not pass the test.</p>
		 *
		 * @param test 			The test used to select values
		 * @return 				A {@link StandardGenerator} of the fitting values
		 */
		public CheckedGenerator<T,E> filter(Predicate<? super T> test) {
			CheckedGenerator<T,E> This = this;
			return new CheckedGenerator<T,E>(this) {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException, E {
					while (true) {
						T t = This.next();
						if (test.test(t))
							yield(t);
					}
				}};
		}

		/**
		 * <p>Removes all occurrences of <code>value</code></p>
		 *
		 * <p>
		 * Object equality is used, so this method is mainly useful to
		 * clear <code>null</code> values from the generated ones.
		 * </p>
		 *
		 * @param value 		The value to remove
		 * @return 				A {@link StandardGenerator} of the other values
		 */
		public CheckedGenerator<T,E> remove(T value) {
			return filter((T t) -> t != value);
		}

		/**
		 * <p>Looks for the first value matching the test Test.</p>
		 *
		 * @param <F>			The type of exceptions to thrown if not found
		 *
		 * @param test 			The test to use
		 * @param exception 	A exception provider to call if no fitting value is found
		 * @return 				The first value passing the test
		 * @throws E			Thrown if the generation has thrown it
		 * @throws F 			Thrown when no fitting value is found
		 */
		public <F extends Exception> T findOrThrow(Predicate<? super T> test, Supplier<F> exception) throws E, F {
			try {
				while (true) {
					T t = next();
					if (test.test(t))
						return t;
				}
			}
			catch (EndOfGenerationException e) {}
			catch (InterruptedException e) {}
			throw exception.get();
		}

		/**
		 * <p>Looks for the first value equals to <code>t</code>.</p>
		 *
		 * <p>
		 * The Object method <code>equals</code> is used to compare values.
		 * </p>
		 *
		 * @param <F>			The type of exceptions to thrown if not found
		 *
		 * @param t				The value to look for
		 * @param exception 	A exception provider to call if no fitting value is found
		 * @return				The first equal value
		 * @throws E			Thrown if the generation has thrown it
		 * @throws F			Thrown when no fitting value is found
		 */
		public <F extends Exception> T findOrThrow(T t, Supplier<F> exception) throws E, F {
			return findOrThrow((T n) -> n.equals(t), exception);
		}

		/**
		 * <p>Looks for the first value matching the test Test.</p>
		 *
		 * @param test 			The test to use
		 * @return 				The first value passing the test or null when no such value is found
		 * @throws E			Thrown if the generation has thrown it
		 */
		public T find(Predicate<? super T> test) throws E {
			return fold((T t, T n) -> test.test(n) ? n : t, null);
		}

		/**
		 * <p>Looks for the first value equals to <code>t</code>.</p>
		 *
		 * <p>
		 * The Object method <code>equals</code> is used to compare values.
		 * </p>
		 *
		 * @param t				The value to look for
		 * @return				The first equal value or null when no such value is found
		 * @throws E			Thrown if the generation has thrown it
		 */
		public T find(T t) throws E {
			return find((T n) -> n.equals(t));
		}

		/**
		 * <p>Replace values that match the <code>test</code> with the one provided by the supplier</p>
		 *
		 * @param test			The test to use
		 * @param nv			The replacing value supplier
		 * @return				A {@link CheckedGenerator} of the values after replacement
		 *
		 * @see CheckedPredicate
		 * @see CheckedSupplier
		 */
		public CheckedGenerator<T,E> replace(CheckedPredicate<? super T, E> test, CheckedSupplier<? extends T, E> nv) {
			return map((T t) -> test.test(t) ? nv.get() : t);
		}

		/**
		 * <p>Replace occurrences of <code>t</code> with the one provided by the supplier.</p>
		 *
		 * <p>
		 * Object equality is used, so this method is mainly useful to
		 * replace <code>null</code> values from the generated ones.
		 * </p>
		 *
		 * @param value				The value to replace
		 * @param nv			The replacing value supplier
		 * @return				A {@link CheckedGenerator} of the values after replacement
		 *
		 * @see CheckedSupplier
		 */
		public CheckedGenerator<T,E> replace(T value, CheckedSupplier<T, E> nv) {
			return replace((T t) -> t == value, nv);
		}

		/**
		 * <p>Enumerates the values with an {@link Integer} index.</p>
		 *
		 * @return				A {@link StandardPairGenerator} of the values with their index.
		 */
		public CheckedPairGenerator<Integer, T, E> enumerate() {
			CheckedGenerator<T, E> This = this;
			return new CheckedPairGenerator<Integer, T, E>(this) {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException, E {
					int k = 0;
					while (true)
						yield(k++, This.next());
				}};
		}

		/**
		 * <p>Checks a property on each value of the generator.</p>
		 *
		 * @param test			The test to use
		 * @return				<code>true</code> if every value pass the test, else <code>false</code>
		 * @throws E			Thrown if the generation has thrown it
		 */
		public boolean forAll(CheckedPredicate<? super T, E> test) throws E {
			return fold((Boolean b, T t) -> b && test.test(t), true);
		}

		public StandardGenerator<T> check() throws E {
			return Generator.fromCollection(toList());
		}
	}


	/**
	 * <p>Extends the {@link StandardGenerator} class in the case of {@link Pair} values with useful methods.</p>
	 *
	 * @author Baptiste Pauget
	 *
	 * @param <S>				The type of the first value's component
	 * @param <T>				The type of the second value's component
	 */
	public static abstract class StandardPairGenerator<S,T> extends StandardGenerator<Pair<S,T>> {

		/**
		 * <p>Constructs a generator with no parent.</p>
		 */
		public StandardPairGenerator() {}

		/**
		 * <p>Constructs a generator with the <code>parent</code> other as parent generator.</p>
		 *
		 * <p>
		 * If the generation is interrupted by an exception, the parent generator will
		 * be stopped.
		 * </p>
		 *
		 * @param parent 		The parent generator
		 */
		public StandardPairGenerator(Generator<?> parent) {
			super(parent);
		}

		public void yield(S s, T t) throws InterruptedException {
			yield(new Pair<>(s,t));
		}

		/**
		 * Provides the first component of each value
		 *
		 * @return				A {@link StandardGenerator} of the first component of the generated values
		 */
		public StandardGenerator<S> takeFirst() {
			return map((Pair<S,T> p) -> p.first);
		}

		/**
		 * Provides the second component of each value
		 *
		 * @return				A {@link StandardGenerator} of the second component of the generated values
		 */
		public StandardGenerator<T> takeSecond() {
			return map((Pair<S,T> p) -> p.second);
		}

		/**
		 * <p>Transforms each value of the generator.</p>
		 *
		 * @param <U>			The type of transformation function result.
		 *
		 * @param function 		The transformation function
		 * @return				A {@link StandardGenerator} of the transformed values
		 */
		public <U> StandardGenerator<U> map(BiFunction<? super S, ? super T, ? extends U> function) {
			return map((Pair<S,T> p) -> function.apply(p.first, p.second));
		}

		public <U,V> StandardPairGenerator<U,V> map(
				Function<? super S, ? extends U> firstMap, Function<? super T, ? extends V> secondMap) {
			return Generator.toPairGenerator(map((Pair<S,T> p) -> new Pair<>(firstMap.apply(p.first), secondMap.apply(p.second))));
		}

		/**
		 * <p>Transforms each value of the generator.</p>
		 *
		 * @param <U>			The type of transformation function result.
		 * @param <E>			The type of exceptions thrown by the consumer.
		 *
		 * @param function 		The transformation function
		 * @return				A {@link CheckedGenerator} of the transformed values
		 *
		 * @see CheckedBiFunction
		 */
		public <U, E extends Exception> CheckedGenerator<U, E> Map(
				CheckedBiFunction<? super S, ? super T, ? extends U, E> function) {
			return Map((Pair<S,T> p) -> function.apply(p.first, p.second));
		}
		public <U,V, E extends Exception> CheckedPairGenerator<U,V,E> Map(
				CheckedFunction<? super S, ? extends U, E> firstMap,
				CheckedFunction<? super T, ? extends V, E> secondMap) {
			return Generator.toPairGenerator(Map((Pair<S,T> p) -> new Pair<>(firstMap.apply(p.first), secondMap.apply(p.second))));
		}

		/**
		 * <p>Transforms the first component of each value of the generator.</p>
		 *
		 * @param <U>			The type of transformation function result
		 *
		 * @param function 		The transformation function
		 * @return				A {@link StandardPairGenerator} of the transformed values
		 */
		public <U> StandardPairGenerator<U,T> mapFirst(Function<? super S, ? extends U> function) {
			StandardPairGenerator<S,T> This = this;
			return new StandardPairGenerator<U, T>(this) {
				@Override
				protected void generate() throws InterruptedException, wyvc.utils.Generator.EndOfGenerationException {
					while (true)
						yield(This.next().transformFirst(function));
				}};
		}

		/**
		 * <p>Transforms the first component of each value of the generator.</p>
		 *
		 * @param <U>			The type of transformation function result
		 * @param <E>			The type of exceptions thrown by the transformation function
		 *
		 * @param function 		The transformation function
		 * @return				A {@link StandardPairGenerator} of the transformed values
		 */
		public <U, E extends Exception> CheckedPairGenerator<U,T, E> MapFirst(CheckedFunction<? super S, ? extends U, E> function) {
			StandardPairGenerator<S,T> This = this;
			return new CheckedPairGenerator<U, T, E>(this) {
				@Override
				protected void generate() throws InterruptedException, wyvc.utils.Generator.EndOfGenerationException, E {
					while (true)
						yield(This.next().transformFirstChecked(function));
				}};
		}

		/**
		 * <p>Transforms the second component of each value of the generator.</p>
		 *
		 * @param <U>			The type of transformation function result
		 *
		 * @param function 		The transformation function
		 * @return				A {@link StandardPairGenerator} of the transformed values
		 */
		public <U> StandardPairGenerator<S,U> mapSecond(Function<? super T, ? extends U> function) {
			StandardPairGenerator<S,T> This = this;
			return new StandardPairGenerator<S, U>(this) {
				@Override
				protected void generate() throws InterruptedException, wyvc.utils.Generator.EndOfGenerationException {
					while (true)
						yield(This.next().transformSecond(function));
				}};
		}

		/**
		 * <p>Transforms the second component of each value of the generator.</p>
		 *
		 * @param <U>			The type of transformation function result
		 * @param <E>			The type of exceptions thrown by the transformation function
		 *
		 * @param function 		The transformation function
		 * @return				A {@link StandardPairGenerator} of the transformed values
		 */
		public <U, E extends Exception> CheckedPairGenerator<S,U, E> MapSecond(CheckedFunction<? super T, ? extends U, E> function) {
			StandardPairGenerator<S,T> This = this;
			return new CheckedPairGenerator<S, U, E>(this) {
				@Override
				protected void generate() throws InterruptedException, wyvc.utils.Generator.EndOfGenerationException, E {
					while (true)
						yield(This.next().transformSecondChecked(function));
				}};
		}

		/**
		 * <p>Applies a function to each value of the generator</p>.
		 *
		 * @param consumer 		The function to apply
		 */
		public void forEach(BiConsumer<S, T> consumer) {
			forEach((Pair<S,T> p) -> consumer.accept(p.first, p.second));
		}

		/**
		 * <p>Applies a function to each value of the generator</p>.
		 *
		 * @param <E>			The type of exceptions thrown by the consumer.
		 *
		 * @param consumer 		The function to apply
		 * @throws E			Thrown if a call to <code>consumer</code> thrown an E exception
		 *
		 * @see CheckedBiConsumer
		 */
		public <E extends Exception> void ForEach(CheckedBiConsumer<S, T, E> consumer) throws E {
			ForEach((Pair<S,T> p) -> consumer.accept(p.first, p.second));
		}

		/**
		 * <p>Checks a property on each value of the generator.</p>
		 *
		 * @param test			The test to use
		 * @return				<code>true</code> if every value pass the test, else <code>false</code>
		 */
		public boolean forAll(BiPredicate<? super S, ? super T> test) {
			return fold((Boolean b, Pair<S,T> p) -> b && test.test(p.first, p.second), true);
		}

		/**
		 * <p>Checks a property on each value of the generator.</p>
		 *
		 * @param <E>			The type of exceptions thrown by the test.
		 *
		 * @param test			The test to use
		 * @return				<code>true</code> if every value pass the test, else <code>false</code>
		 * @throws E			Thrown if a call to the test throws it
		 *
		 * @see CheckedBiPredicate
		 */
		public <E extends Exception> boolean ForAll(CheckedBiPredicate<? super S, ? super T, E> test) throws E {
			return Fold((Boolean b, Pair<S,T> p) -> b && test.test(p.first, p.second), true);
		}
	}


	/**
	 * <p>Extends the {@link CheckedGenerator} class in the case of {@link Pair} values with useful methods.</p>
	 *
	 * @author Baptiste Pauget
	 *
	 * @param <S>				The type of the first value's component
	 * @param <T>				The type of the second value's component
	 * @param <E>				The type of exceptions that may be thrown during generation
	 */
	public static abstract class CheckedPairGenerator<S,T, E extends Exception> extends CheckedGenerator<Pair<S,T>, E> {

		/**
		 * <p>Constructs a generator with no parent.</p>
		 */
		public CheckedPairGenerator() {}

		/**
		 * <p>Constructs a generator with the <code>parent</code> other as parent generator.</p>
		 *
		 * <p>
		 * If the generation is interrupted by an exception, the parent generator will
		 * be stopped.
		 * </p>
		 *
		 * @param parent 		The parent generator
		 */
		public CheckedPairGenerator(Generator<?> parent) {
			super(parent);
		}

		public void yield(S s, T t) throws InterruptedException {
			yield(new Pair<>(s,t));
		}

		/**
		 * Provides the first component of each value
		 *
		 * @return				A {@link StandardGenerator} of the first component of the generated values
		 */
		public CheckedGenerator<S, E> takeFirst() {
			return map((Pair<S,T> p) -> p.first);
		}

		/**
		 * Provides the second component of each value
		 *
		 * @return				A {@link StandardGenerator} of the second component of the generated values
		 */
		public CheckedGenerator<T, E> takeSecond() {
			return map((Pair<S,T> p) -> p.second);
		}

		/**
		 * <p>Transforms the first component of each value of the generator.</p>
		 *
		 * @param <U>			The type of transformation function result
		 *
		 * @param function 		The transformation function
		 * @return				A {@link StandardPairGenerator} of the transformed values
		 */
		public <U> CheckedPairGenerator<U,T, E> mapFirst(CheckedFunction<? super S, ? extends U, E> function) {
			CheckedPairGenerator<S,T,E> This = this;
			return new CheckedPairGenerator<U, T, E>(this) {
				@Override
				protected void generate() throws InterruptedException, wyvc.utils.Generator.EndOfGenerationException, E {
					while (true)
						yield(This.next().transformFirstChecked(function));
				}};
		}

		/**
		 * <p>Transforms the second component of each value of the generator.</p>
		 *
		 * @param <U>			The type of transformation function result
		 *
		 * @param function 		The transformation function
		 * @return				A {@link StandardPairGenerator} of the transformed values
		 */
		public <U> CheckedPairGenerator<S,U, E> mapSecond(CheckedFunction<? super T, ? extends U, E> function) {
			CheckedPairGenerator<S,T,E> This = this;
			return new CheckedPairGenerator<S, U, E>(this) {
				@Override
				protected void generate() throws InterruptedException, wyvc.utils.Generator.EndOfGenerationException, E {
					while (true)
						yield(This.next().transformSecondChecked(function));
				}};
		}

		/**
		 * <p>Transforms each value of the generator.</p>
		 *
		 * @param <U> 			The type of the transformation function result
		 *
		 * @param function 		The transformation function
		 * @return				A {@link CheckedGenerator} of the transformed values
		 *
		 * @see CheckedBiFunction
		 */
		public <U> CheckedGenerator<U, E> map(CheckedBiFunction<? super S, ? super T, ? extends U, E> function) {
			return map((Pair<S,T> p) -> function.apply(p.first, p.second));
		}

		public <U,V> CheckedPairGenerator<U,V,E> map(
				CheckedFunction<? super S, ? extends U, E> firstMap,
				CheckedFunction<? super T, ? extends V, E> secondMap) {
			return Generator.toPairGenerator(map((Pair<S,T> p) -> new Pair<>(firstMap.apply(p.first), secondMap.apply(p.second))));
		}

		/**
		 * <p>Applies a function to each value of the generator</p>.
		 *
		 * @param consumer 		The function to apply
		 * @throws E			Thrown if a call to the test or the generation throws it
		 */
		public void forEach(CheckedBiConsumer<? super S, ? super T, ? extends E> consumer) throws E {
			forEach((Pair<S,T> p) -> consumer.accept(p.first, p.second));
		}

		/**
		 * <p>Checks a property on each value of the generator.</p>
		 *
		 * @param test			The test to use
		 * @return				<code>true</code> if every value pass the test, else <code>false</code>
		 * @throws E			Thrown if a call to the test or the generation throws it
		 *
		 * @see CheckedBiPredicate
		 */
		public boolean forAll(CheckedBiPredicate<? super S, ? super T, E> test) throws E {
			return fold((Boolean b, Pair<S,T> p) -> b && test.test(p.first, p.second), true);
		}

		public StandardPairGenerator<S,T> check() throws E {
			return Generator.fromPairCollection(toList());
		}
	}



	/**
	 * <p>Converts a {@link StandardGenerator} of {@link Pair} to a {@link StandardPairGenerator}.</p>
	 *
	 * @param <S> 				The first component type
	 * @param <T> 				The second component type
	 *
	 * @param generator			The {@link StandardGenerator} to convert
	 * @return					A {@link StandardPairGenerator} generating the same values
	 */
	@SuppressWarnings("unchecked")
	public static <S,T> StandardPairGenerator<S,T> toPairGenerator(StandardGenerator<Pair<S,T>> generator) {
		if (generator instanceof StandardPairGenerator)
			return (StandardPairGenerator<S,T>) generator;
		return new StandardPairGenerator<S, T>(generator) {
			@Override
			protected void generate() throws InterruptedException, EndOfGenerationException {
				while (true)
					yield(generator.next());
			}};
	}

	/**
	 * <p>Converts a {@link CheckedGenerator} of {@link Pair} to a {@link CheckedPairGenerator}.</p>
	 *
	 * @param <S> 				The first component type
	 * @param <T> 				The second component type
	 * @param <E>				The type of exceptions that can be thrown by the generator
	 *
	 * @param generator			The {@link CheckedGenerator} to convert
	 * @return					A {@link CheckedPairGenerator} generating the same values
	 */
	@SuppressWarnings("unchecked")
	public static <S,T, E extends Exception> CheckedPairGenerator<S,T,E> toPairGenerator(CheckedGenerator<Pair<S,T>,E> generator) {
		if (generator instanceof CheckedPairGenerator)
			return (CheckedPairGenerator<S,T,E>) generator;
		return new CheckedPairGenerator<S, T, E>(generator) {
			@Override
			protected void generate() throws InterruptedException, EndOfGenerationException, E {
				while (true)
					yield(generator.next());
			}};
	}

	/**
	 * Builds a {@link StandardPairGenerator} that enumerates a {@link Collection}.
	 *
	 * @param <S> 				The collection values' type
	 *
	 * @param c					The collection to convert
	 * @return					A {@link StandardPairGenerator} of the indexes and values of the collection
	 */
	public static <S> StandardPairGenerator<Integer, S> enumerateFromCollection(Collection<S> c) {
		return new StandardPairGenerator<Integer, S>(){
			@Override
			protected void generate() throws InterruptedException {
				int k = 0;
				for (S s : c)
					yield(k++,s);
			}};
	}

	/**
	 * Builds a {@link StandardPairGenerator} that enumerates an {@link Array}.
	 *
	 * @param <S> 				The array values' type
	 *
	 * @param a					The array to convert
	 * @return					A {@link StandardPairGenerator} of the indexes and values of the array
	 */
	public static <S> StandardPairGenerator<Integer, S> enumerateFromCollection(S[] a) {
		return enumerateFromCollection(Arrays.asList(a));
	}

	/**
	 * Builds a {@link StandardGenerator} that produce the values of a {@link Collection}.
	 *
	 * @param <S> 				The collection values' type
	 *
	 * @param c					The collection to convert
	 * @return					A {@link StandardGenerator} of the values of the collection
	 */
	public static <S> StandardGenerator<S> fromCollection(Collection<S> c) {
		return new StandardGenerator<S>(){
			@Override
			protected void generate() throws InterruptedException {
				for (S s : c)
					yield(s);
			}};
	}

	/**
	 * Builds a {@link StandardGenerator} that produce the values of a {@link Array}.
	 *
	 * @param <S> 				The array values' type
	 *
	 * @param a					The array to convert
	 * @return					A {@link StandardGenerator} of the values of the array
	 */
	public static <S> StandardGenerator<S> fromCollection(S[] a) {
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
	public static <S,T> StandardPairGenerator<S,T> fromPairCollection(Collection<Pair<S,T>> c) {
		return new StandardPairGenerator<S,T>(){
			@Override
			protected void generate() throws InterruptedException {
				for (Pair<S,T> p : c)
					yield(p);
			}};
	}

	/**
	 * <p>Generates a {@link StandardGenerator} that will produce one value.</p>
	 *
	 * @param <S> 				The generators values' type
	 *
	 * @param s					The value to wrap in a generator
	 * @return					A generator producing only one <code>s</code> value
	 */
	public static <S> StandardGenerator<S> fromSingleton(S s) {
		return new StandardGenerator<S>() {
			@Override
			protected void generate() throws InterruptedException, EndOfGenerationException {
				yield(s);
			}};
	}

	public static <S, T> StandardPairGenerator<S, T> fromPairSingleton(S s, T t) {
		return new StandardPairGenerator<S, T>() {
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
	 * @return A {@link StandardGenerator} that produce no value
	 */
	public static <S> StandardGenerator<S> emptyGenerator() {
		return new StandardGenerator<S>() {
			@Override
			protected void generate() throws InterruptedException, EndOfGenerationException {
			}};
	}

	/**
	 * <p>Generates a empty {@link StandardPairGenerator}.</p>
	 *
	 * @param <S> 				The first component type
	 * @param <T> 				The second component type
	 *
	 * @return A {@link StandardPairGenerator} that produce no value
	 */
	public static <S,T> StandardPairGenerator<S,T> emptyPairGenerator() {
		return new StandardPairGenerator<S,T>() {
			@Override
			protected void generate() throws InterruptedException, EndOfGenerationException {
			}};
	}

	/**
	 * <p>Concatenates generators of the same type.</p>
	 *
	 * @param <S> 				The generators values' type
	 * @param generators		The generators to concatenate
	 * @return 					A {@link StandardGenerator} producing the value of each generator.
	 */
	public static <S> StandardGenerator<S> concat(StandardGenerator<StandardGenerator<S>> generators) {
		return generators.fold((StandardGenerator<S> g, StandardGenerator<S> t) -> g.append(t), emptyGenerator());
	}

	public static <S> StandardGenerator<S> constant(Supplier<S> f) {
		return new StandardGenerator<S>() {
			@Override
			protected void generate() throws InterruptedException, EndOfGenerationException {
				while (true)
					yield(f.get());
			}
		};
	}

	public static <S> StandardGenerator<StandardGenerator<S>> cartesianProduct(StandardGenerator<StandardGenerator<S>> generators) {
		final List<List<S>> values = generators.map(StandardGenerator::toList).toList();
		final int noc = values.size();
		return noc == 0 ? emptyGenerator() : new StandardGenerator<StandardGenerator<S>>() {
			@Override
			protected void generate() throws InterruptedException, EndOfGenerationException {
				int[] index = new int[noc];
				Arrays.fill(index, 0);
				int[] lenghts = new int[noc];
				Generator.enumerateFromCollection(values).forEach((Integer k, List<S> l) -> lenghts[k] = l.size());
				for (int j : lenghts)
					if (j == 0)
						return;
				while(index[0] != lenghts[0]) {
					yield(new StandardGenerator<S>() {
						@Override
						protected void generate() throws InterruptedException, EndOfGenerationException {
							int k = 0;
							for (int i : index)
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
