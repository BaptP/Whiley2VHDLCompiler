package wyvc.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public abstract class Generator<T> extends Thread {
	public static class EndOfGeneratorException extends Exception {
		private static final long serialVersionUID = -4492584892378046784L;

	}

	private T value;
	private boolean ready = false;

	public Generator() {
		start();
	}


	protected void yield(T t) throws InterruptedException {
		value = t;
		ready = true;
		while (ready)
			Thread.sleep(3);
	}

	protected abstract void generate() throws InterruptedException, EndOfGeneratorException;

	public final void run() {
		try {
			generate();
		}
		catch (InterruptedException e) {}
		catch (EndOfGeneratorException e) {}
		ready = true;
	}

	public T next() throws EndOfGeneratorException, InterruptedException {
		while (!ready)
			Thread.sleep(3);
		if (!isAlive())
			throw new EndOfGeneratorException();
		T t = value;
		ready = false;
		return t;
	}

	public List<T> toList() {
		List<T> l = new ArrayList<>();
		try {
			while (true)
				l.add(next());
		} catch (EndOfGeneratorException e) {
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
		catch (EndOfGeneratorException e) {}
		catch (InterruptedException e) {}
	}

	public <U> Generator<U> map(Function<? super T, ? extends U> f) {
		Generator<T> This = this;
		return new Generator<U>(){
			@Override
			protected void generate() throws InterruptedException, EndOfGeneratorException {
				while (true)
					yield(f.apply(This.next()));
			}};
	}


	public static <S> Generator<S> fromCollection(Collection<S> c) {
		return new Generator<S>(){
			@Override
			protected void generate() throws InterruptedException {
				for (S s : c)
					yield(s);
			}};
	}
}
