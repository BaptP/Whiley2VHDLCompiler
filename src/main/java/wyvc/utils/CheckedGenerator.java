package wyvc.utils;

import java.util.ArrayList;
import java.util.List;

public abstract class CheckedGenerator<T, E extends Exception> extends Thread {
	public static class EndOfGeneratorException extends Exception {
		private static final long serialVersionUID = -4492584892378046784L;

	}


	private T value;
	private E exception = null;
	private boolean ready = false;

	public CheckedGenerator() {
		start();
	}


	protected void yield(T t) throws InterruptedException {
		value = t;
		ready = true;
		while (ready)
			Thread.sleep(3);
	}

	protected abstract void generate() throws InterruptedException,E ;

	@SuppressWarnings("unchecked")
	public final void run() {
		try {
			generate();
		} catch (InterruptedException e) {
		} catch (Exception e) {
			exception = (E) e;
		}
		ready = true;
	}

	public T next() throws EndOfGeneratorException, InterruptedException, E {
		while (!ready)
			Thread.sleep(3);
		if (!isAlive())
			throw new EndOfGeneratorException();
		if (exception != null)
			throw exception;
		T t = value;
		ready = false;
		return t;
	}

	public List<T> toList() throws E {
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
}
