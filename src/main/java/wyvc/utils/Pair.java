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

	public Pair<S,T> transformFirst(Function<S,S> f) {
		first = f.apply(first);
		return this;
	}

	public Pair<S,T> transformSecond(Function<T,T> f) {
		second = f.apply(second);
		return this;
	}


	public <E extends Exception> Pair<S,T> transformFirstChecked(CheckedFunction<S,S,E> f) throws E {
		first = f.apply(first);
		return this;
	}

	public <E extends Exception> Pair<S,T> transformSecondChecked(CheckedFunction<T,T,E> f) throws E {
		second = f.apply(second);
		return this;
	}
}
