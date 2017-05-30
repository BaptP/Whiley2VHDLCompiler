package wyvc.utils;

import java.util.function.Function;

import wyvc.utils.FunctionalInterfaces.CheckedFunction;
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

	public <U> Pair<U,T> transformFirst(Function<? super S, ? extends U> function) {
		return new Pair<>(function.apply(first), second);
	}

	public <U> Pair<S,U> transformSecond(Function<? super T, ? extends U> function) {
		return new Pair<>(first, function.apply(second));
	}

	public <U, E extends Exception> Pair<U,T> transformFirst_(CheckedFunction<? super S, ? extends U, E> function) throws E {
		return new Pair<>(function.apply(first), second);
	}

	public <U, E extends Exception> Pair<S,U> transformSecond_(CheckedFunction<? super T, ? extends U, E> function) throws E{
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
