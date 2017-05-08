package wyvc.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import wyvc.utils.CheckedFunctionalInterface.CheckedBiFunction;
import wyvc.utils.CheckedFunctionalInterface.CheckedConsumer;
import wyvc.utils.CheckedFunctionalInterface.CheckedFunction;
import wyvc.utils.CheckedFunctionalInterface.CheckedPredicate;
import wyvc.utils.CheckedFunctionalInterface.CheckedSupplier;

public interface Generator {
	public static class EndOfGenerationException extends Exception {
		private static final long serialVersionUID = -4492584892378046784L;

	}

	public static abstract class StandardGenerator<T> extends Thread {
		private T value;
		private volatile boolean ready = false;
		private volatile boolean done = false;

		public StandardGenerator() {
			start();
		}


		protected void yield(T t) throws InterruptedException {
//			System.out.println(this.getClass() + " " +this+ " yield debut");
			value = t;
			ready = true;
			while (ready)
				Thread.sleep(3);
//			System.out.println(this.getClass() + " " +this+ " yield fin");
		}

		protected abstract void generate() throws InterruptedException, EndOfGenerationException;

		public final void run() {
			try {
				generate();
			}
			catch (InterruptedException e) {}
			catch (EndOfGenerationException e) {}
			catch (RuntimeException e) {}
//			System.out.println(this.getClass() + " " +this+ " generate fin");
			ready = true;
			done = true;
		}

		public T next() throws EndOfGenerationException, InterruptedException {
			if (done)
				throw new EndOfGenerationException();
//			System.out.println(this.getClass() + " " +this+ " next début");
			while (!ready)
				Thread.sleep(3);
			if (done)
				throw new EndOfGenerationException();
			T t = value;
//			System.out.println(this.getClass() + " " +this+ " next fin");
			ready = false;
			return t;
		}

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

		public void addToCollection(Collection<T> c) {
			try {
				while (true)
					c.add(next());
			}
			catch (EndOfGenerationException e) {}
			catch (InterruptedException e) {}
		}

		public void forEach(Consumer<T> f) {
			try {
				while (true)
					f.accept(next());
			}
			catch (EndOfGenerationException e) {}
			catch (InterruptedException e) {}
		}

		public <E extends Exception> void ForEach(CheckedConsumer<T, E> f) throws E {
			try {
				while (true)
					f.accept(next());
			}
			catch (EndOfGenerationException e) {}
			catch (InterruptedException e) {}
		}

		public <A> A fold(BiFunction<A,T,A> f, A init) {
			try {
				while (true)
					init = f.apply(init, next());
			}
			catch (EndOfGenerationException e) {}
			catch (InterruptedException e) {}
			return init;
		}

		public <A, E extends Exception> A Fold(CheckedBiFunction<A,T,A, E> f, A init) throws E {
			try {
				while (true)
					init = f.apply(init, next());
			}
			catch (EndOfGenerationException e) {}
			catch (InterruptedException e) {}
			return init;
		}

		public <U> StandardGenerator<U> map(Function<? super T, ? extends U> function) {
			StandardGenerator<T> This = this;
			return new StandardGenerator<U>() {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException {
					while (true)
						yield(function.apply(This.next()));
				}};
		}
		public <U> StandardGenerator<U> map(BiFunction<? super Integer, ? super T, ? extends U> function) {
			StandardGenerator<T> This = this;
			return new StandardGenerator<U>() {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException {
					int k = 0;
					while (true)
						yield(function.apply(k++, This.next()));
				}};
		}

		public <U, E extends Exception> CheckedGenerator<U, E> Map(CheckedFunction<? super T, ? extends U, ? extends E> function) {
			StandardGenerator<T> This = this;
			return new CheckedGenerator<U, E>() {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException, E {
					while (true)
						yield(function.apply(This.next()));
				}};
		}

		public <U, E extends Exception> CheckedGenerator<U, E> Map(CheckedBiFunction<? super Integer, ? super T, ? extends U, ? extends E> function) {
			StandardGenerator<T> This = this;
			return new CheckedGenerator<U, E>() {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException, E {
					int k = 0;
					while (true)
						yield(function.apply(k++, This.next()));
				}};
		}

		public <U> StandardPairGenerator<T,U> gather(StandardGenerator<U> generator) {
			StandardGenerator<T> This = this;
			return new StandardPairGenerator<T,U>() {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException {
					while (true)
						yield(new Pair<>(This.next(), generator.next()));
				}};
		}

		public <U, E extends Exception> CheckedPairGenerator<T,U, E> Gather(CheckedGenerator<U, E> generator) {
			StandardGenerator<T> This = this;
			return new CheckedPairGenerator<T,U, E>() {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException, E {
					while (true)
						yield(new Pair<>(This.next(), generator.next()));
				}};
		}

		public StandardGenerator<T> filter(Predicate<? super T> test) {
			StandardGenerator<T> This = this;
			return new StandardGenerator<T>() {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException {
					while (true) {
						T t = This.next();
						if (test.test(t))
							yield(t);
					}
				}};
		}
		public StandardGenerator<T> remove(T value) {
			return filter((T t) -> t != value);
		}

		public T find(Predicate<? super T> test) {
			return fold((T t, T n) -> test.test(n) ? n : t, null);
		}

		public T find(T t) {
			return find((T n) -> n.equals(t));
		}

		public StandardGenerator<T> replace(Predicate<? super T> test, Supplier<? extends T> nv) {
			return map((T t) -> test.test(t) ? nv.get() : t);
		}
		public StandardGenerator<T> replace(T value, Supplier<T> nv) {
			return replace((T t) -> t == value, nv);
		}

		public <E extends Exception> CheckedGenerator<T,E> Replace(CheckedPredicate<? super T, E> test, CheckedSupplier<? extends T, E> nv) {
			return Map((T t) -> test.test(t) ? nv.get() : t);
		}
		public <E extends Exception> CheckedGenerator<T,E> Replace(T value, CheckedSupplier<T, E> nv) {
			return Replace((T t) -> t == value, nv);
		}
	}


	public static abstract class CheckedGenerator<T, E extends Exception> extends Thread {
		private T value;
		private E exception = null;
		private volatile boolean ready = false;
		private volatile boolean done = false;

		public CheckedGenerator() {
			start();
		}


		protected void yield(T t) throws InterruptedException {
//			System.out.println(this.getClass() + " " +this+ " yield debut");
			value = t;
			ready = true;
			while (ready)
				Thread.sleep(3);
//			System.out.println(this.getClass() + " " +this+ " yield fin");
		}

		protected abstract void generate() throws InterruptedException, EndOfGenerationException, E;

		@SuppressWarnings("unchecked")
		public final void run() {
			try {
				generate();
			}
			catch (InterruptedException e) {}
			catch (RuntimeException e) {}
			catch (Exception e) {
				exception = (E) e;
			}
//			System.out.println(this.getClass() + " " +this+ " generate fin !");
			ready = true;
			done = true;
		}

		public T next() throws EndOfGenerationException, InterruptedException, E {
			if (done)
				throw new EndOfGenerationException();
//			System.out.println(this.getClass() + " " +this+ " next debut");
			while (!ready)
				Thread.sleep(3);
			if (done)
				throw new EndOfGenerationException();
			if (exception != null)
				throw exception;
			T t = value;
//			System.out.println(this.getClass() + " " +this+ " next fin");
			ready = false;
			return t;
		}

		public List<T> toList() throws E {
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

		public void forEach(Consumer<T> f) throws E {
			try {
				while (true)
					f.accept(next());
			}
			catch (EndOfGenerationException e) {}
			catch (InterruptedException e) {}
		}

		public <F extends Exception> void ForEach(CheckedConsumer<T, F> f) throws E, F {
			try {
				while (true)
					f.accept(next());
			}
			catch (EndOfGenerationException e) {}
			catch (InterruptedException e) {}
		}

		public <A> A fold(BiFunction<A,T,A> f, A init) throws E {
			try {
				while (true)
					init = f.apply(init, next());
			}
			catch (EndOfGenerationException e) {}
			catch (InterruptedException e) {}
			return init;
		}

		public <A, F extends Exception> A Fold(CheckedBiFunction<A,T,A, F> f, A init) throws E, F {
			try {
				while (true)
					init = f.apply(init, next());
			}
			catch (EndOfGenerationException e) {}
			catch (InterruptedException e) {}
			return init;
		}

		public <U> CheckedGenerator<U, E> map(CheckedFunction<? super T, ? extends U, ? extends E> function) {
			CheckedGenerator<T, E> This = this;
			return new CheckedGenerator<U, E>() {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException, E {
					while (true)
						yield(function.apply(This.next()));
				}};
		}

		public <U> CheckedGenerator<U, E> map(CheckedBiFunction<? super Integer, ? super T, ? extends U, ? extends E> function) {
			CheckedGenerator<T, E> This = this;
			return new CheckedGenerator<U, E>() {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException, E {
					int k = 0;
					while (true)
						yield(function.apply(k++, This.next()));
				}};
		}

		public <U> CheckedPairGenerator<T,U, E> gather(CheckedGenerator<U, E> generator) {
			CheckedGenerator<T, E> This = this;
			return new CheckedPairGenerator<T,U, E>() {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException, E {
					while (true)
						yield(new Pair<>(This.next(), generator.next()));
				}};
		}

		public CheckedGenerator<T,E> filter(Predicate<? super T> test) {
			CheckedGenerator<T,E> This = this;
			return new CheckedGenerator<T,E>() {
				@Override
				protected void generate() throws InterruptedException, EndOfGenerationException, E {
					while (true) {
						T t = This.next();
						if (test.test(t))
							yield(t);
					}
				}};
		}
		public CheckedGenerator<T,E> remove(T value) {
			return filter((T t) -> t != value);
		}

		public T find(Predicate<? super T> test) throws E {
			return fold((T t, T n) -> test.test(n) ? n : t, null);
		}

		public T find(T t) throws E {
			return find((T n) -> n.equals(t));
		}

		public CheckedGenerator<T,E> replace(CheckedPredicate<? super T, E> test, CheckedSupplier<? extends T, E> nv) {
			return map((T t) -> test.test(t) ? nv.get() : t);
		}
		public CheckedGenerator<T,E> replace(T value, CheckedSupplier<T, E> nv) {
			return replace((T t) -> t == value, nv);
		}
	}



	public static abstract class StandardPairGenerator<S,T> extends StandardGenerator<Pair<S,T>> {
		public StandardGenerator<S> takeFirst() {
			return map((Pair<S,T> p) -> p.first);
		}
		public StandardGenerator<T> takeSecond() {
			return map((Pair<S,T> p) -> p.second);
		}
	}

	public static abstract class CheckedPairGenerator<S,T, E extends Exception> extends CheckedGenerator<Pair<S,T>, E> {
		public CheckedGenerator<S, E> takeFirst() {
			return map((Pair<S,T> p) -> p.first);
		}
		public CheckedGenerator<T, E> takeSecond() {
			return map((Pair<S,T> p) -> p.second);
		}
	}


	public static <S,T> StandardPairGenerator<S,T> toPairGenerator(StandardGenerator<Pair<S,T>> g) {
		return new StandardPairGenerator<S, T>() {
			@Override
			protected void generate() throws InterruptedException, EndOfGenerationException {
				while (true)
					yield(g.next());
			}};
	}

	public static <S,T, E extends Exception> CheckedPairGenerator<S,T,E> toPairGenerator(CheckedGenerator<Pair<S,T>,E> g) {
		return new CheckedPairGenerator<S, T, E>() {
			@Override
			protected void generate() throws InterruptedException, EndOfGenerationException, E {
				while (true)
					yield(g.next());
			}};
	}


	public static <S> StandardPairGenerator<Integer, S> enumerateFromCollection(Collection<S> c) {
		return new StandardPairGenerator<Integer, S>(){
			@Override
			protected void generate() throws InterruptedException {
				int k = 0;
				for (S s : c)
					yield(new Pair<>(k,s));
			}};
	}
	public static <S> StandardPairGenerator<Integer, S> enumerateFromCollection(S[] a) {
		return enumerateFromCollection(Arrays.asList(a));
	}
	public static <S> StandardGenerator<S> fromCollection(Collection<S> c) {
		return new StandardGenerator<S>(){
			@Override
			protected void generate() throws InterruptedException {
				for (S s : c)
					yield(s);
			}};
	}
	public static <S> StandardGenerator<S> fromCollection(S[] a) {
		return fromCollection(Arrays.asList(a));
	}
	public static <S,T> StandardPairGenerator<S,T> fromPairCollection(Collection<Pair<S,T>> c) {
		return new StandardPairGenerator<S,T>(){
			@Override
			protected void generate() throws InterruptedException {
				for (Pair<S,T> p : c)
					yield(p);
			}};
	}
}
