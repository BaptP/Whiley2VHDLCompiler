package wyvc.utils;

import java.util.function.BiFunction;
import java.util.function.Function;

import wyvc.utils.FunctionalInterfaces.BiFunction_;
import wyvc.utils.FunctionalInterfaces.Function_;
import wyvc.utils.FunctionalInterfaces.Predicate;

public class  Pair<S,T> {
	public final S first;
	public final T second;

	public Pair(S first, T second) {
		this.first = first;
		this.second = second;
	}

	public S getFirst() {
		return first;
	}

	public T getSecond() {
		return second;
	}

	public <U,V> Pair<U,V> transform(Function<? super S, ? extends U> fisrtFunction, Function<? super T, ? extends V> secondFunction) {
		return new Pair<>(fisrtFunction.apply(first), secondFunction.apply(second));
	}

	public <U> U transform(BiFunction<? super S, ? super T, ? extends U> function) {
		return function.apply(first,second);
	}

	public <U> Pair<U,T> transformFirst(Function<? super S, ? extends U> function) {
		return new Pair<>(function.apply(first), second);
	}

	public <U> Pair<S,U> transformSecond(Function<? super T, ? extends U> function) {
		return new Pair<>(first, function.apply(second));
	}

	public <U, E extends Exception> U transform_(BiFunction_<? super S, ? super T, ? extends U, E> function) throws E {
		return function.apply(first,second);
	}

	public <U,V, E extends Exception> Pair<U,V> transform_(Function_<? super S, ? extends U, E> fisrtFunction, Function_<? super T, ? extends V, E> secondFunction) throws E {
		return new Pair<>(fisrtFunction.apply(first), secondFunction.apply(second));
	}

	public <U, E extends Exception> Pair<U,T> transformFirst_(Function_<? super S, ? extends U, E> function) throws E {
		return new Pair<>(function.apply(first), second);
	}

	public <U, E extends Exception> Pair<S,U> transformSecond_(Function_<? super T, ? extends U, E> function) throws E{
		return new Pair<>(first, function.apply(second));
	}

	public Pair<T,S> swap() {
		return new Pair<>(second,first);
	}

	public boolean equals(Pair<S,T> other) {
		return first.equals(other.first) && second.equals(other.second);
	}

	public boolean test(Predicate<S> firstTest, Predicate<T> secondTest) {
		return firstTest.test(first) && secondTest.test(second);
	}
}
