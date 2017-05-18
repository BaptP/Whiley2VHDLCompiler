package wyvc.utils;

import java.util.function.Function;

import wyvc.utils.CheckedFunctionalInterface.CheckedFunction;

public class  Pair<S,T> {
	public S first;
	public T second;

	public Pair(S first, T second) {
		this.first = first;
		this.second = second;
	}

	public <U> Pair<U,T> transformFirst(Function<? super S, ? extends U> function) {
		return new Pair<>(function.apply(first), second);
	}

	public <U> Pair<S,U> transformSecond(Function<? super T, ? extends U> function) {
		return new Pair<>(first, function.apply(second));
	}

	public <U, E extends Exception> Pair<U,T> transformFirstChecked(CheckedFunction<? super S, ? extends U, E> function) throws E {
		return new Pair<>(function.apply(first), second);
	}

	public <U, E extends Exception> Pair<S,U> transformSecondChecked(CheckedFunction<? super T, ? extends U, E> function) throws E{
		return new Pair<>(first, function.apply(second));
	}

	public boolean equals(Pair<S,T> other) {
		return first.equals(other.first) && second.equals(other.second);
	}
}
