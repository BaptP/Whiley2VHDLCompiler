package wyvc.utils;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import wyil.lang.Bytecode.Assign;
import wyil.lang.SyntaxTree;
import wyil.lang.SyntaxTree.Location;
import wyvc.builder.CompilerLogger;
import wyvc.utils.CheckedFunctionalInterface.CheckedBiFunction;
import wyvc.utils.CheckedFunctionalInterface.CheckedFunction;

public class Utils {
	public static void printLocation(CompilerLogger logger, Location<?> a, String n) {
		logger.debug(n+a.toString());
		for(Location<?> l : a.getOperands())
			printLocation(logger, l, n+" |  ");
		if (a.getBytecode() instanceof Assign) {
			for(Location<?> l : a.getOperandGroup(SyntaxTree.LEFTHANDSIDE))
				printLocation(logger, l, n+" |<-");
			for(Location<?> l : a.getOperandGroup(SyntaxTree.RIGHTHANDSIDE))
				printLocation(logger, l, n+" |->");
		}
	}

	public static <S,T> T[] toArray(Collection<S> l, Function<? super S, ? extends T> f, T[] t) {
		ArrayList<T> m = new ArrayList<>();
		for (S e : l)
			m.add(f.apply(e));
		return m.toArray(t);
	}

	public static <S,T> List<Pair<S,T>> gather(List<S> l1, List<T> l2) {
		int m = Math.min(l1.size(), l2.size());
		ArrayList<Pair<S,T>> l = new ArrayList<>(m);
		for (int k = 0; k < m; ++k)
			l.add(new Pair<S,T>(l1.get(k),l2.get(k)));
		return l;
	}

	public static <S,T> List<S> takeFirst(List<Pair<S,T>> l) {
		ArrayList<S> n = new ArrayList<>(l.size());
		for (Pair<S,T> p : l)
			n.add(p.first);
		return n;
	}

	public static <S,T> List<T> takeSecond(List<Pair<S,T>> l) {
		ArrayList<T> n = new ArrayList<>(l.size());
		for (Pair<S,T> p : l)
			n.add(p.second);
		return n;
	}

	public static <S,T> List<T> convert(List<S> l, Function<? super S, ? extends T> f) {
		ArrayList<T> m = new ArrayList<>(l.size());
		for (S s : l)
			m.add(f.apply(s));
		return m;
	}

	public static <S,T,E extends Exception> List<T> checkedConvert(List<S> l, CheckedFunction<? super S, ? extends T, E> f) throws E {
		ArrayList<T> m = new ArrayList<>(l.size());
		for (S s : l)
			m.add(f.apply(s));
		return m;
	}

	@SuppressWarnings("unchecked")
	public static <S,T> List<T> convert(List<S> l) {
		return convert(l, (S s) -> (T) s);
	}

	public static <S,T> List<T> convert(List<S> l, BiFunction<? super S, ? super Integer, ? extends T> f) {
		ArrayList<T> m = new ArrayList<>(l.size());
		int k = 0;
		for (S s : l)
			m.add(f.apply(s,k++));
		return m;
	}

	public static <S,T,E extends Exception> List<T> checkedConvert(List<S> l, CheckedBiFunction<? super S,? super Integer, ? extends T, E> f) throws E {
		ArrayList<T> m = new ArrayList<>(l.size());
		int k = 0;
		for (S s : l)
			m.add(f.apply(s, k++));
		return m;
	}

	public static <S> List<S> concat(List<S> l1, List<S> l2) {
		List<S> l = new ArrayList<>(l1.size()+l2.size());
		l.addAll(l1);
		l.addAll(l2);
		return l;
	}

	public static <S, T, E extends Exception> CheckedFunction<S, T, E> FICE(CheckedFunction<S, T, E> f) {
		return f;
	}
}
