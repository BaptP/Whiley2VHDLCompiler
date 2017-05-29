package wyvc.utils;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import wyil.lang.Bytecode.Assign;
import wyil.lang.Bytecode.If;
import wyil.lang.SyntaxTree;
import wyil.lang.SyntaxTree.Location;
import wyvc.builder.CompilerLogger;
import wyvc.utils.FunctionalInterfaces.CheckedBiConsumer;
import wyvc.utils.FunctionalInterfaces.CheckedBiFunction;
import wyvc.utils.FunctionalInterfaces.CheckedConsumer;
import wyvc.utils.FunctionalInterfaces.CheckedFunction;
import wyvc.utils.FunctionalInterfaces.CheckedSupplier;

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
		if (a.getBytecode() instanceof If) {
			printLocation(logger, a.getBlock(0), n+" |T ");
			if (((If)a.getBytecode()).hasFalseBranch())
				printLocation(logger, a.getBlock(1), n+" |F ");
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

	public static <S, T, E extends Exception> void checkedForEach(Map<S,T> m, CheckedBiConsumer<S,T,E> f) throws E {
		for (S k : m.keySet())
			f.accept(k, m.get(k));
	}

	public static <S> void forEach(Collection<S> c, BiConsumer<Integer, S> f) {
		int k = 0;
		for (S s : c)
			f.accept(k++, s);
	}

	public static <S, E extends Exception> void checkedForEach(Collection<S> c, CheckedConsumer<S, E> f) throws E {
		for (S s : c)
			f.accept(s);
	}

	public static <S, E extends Exception> void checkedForEach(Collection<S> c, CheckedBiConsumer<Integer, S, E> f) throws E {
		int k = 0;
		for (S s : c)
			f.accept(k++, s);
	}

	public static <S> void ignore(S s) {}

	public static <S,T> T addIfAbsent(Map<S,T> m, S k, Supplier<T> v) {
		if (!m.containsKey(k))
			m.put(k, v.get());
		return m.get(k);
	}

	public static <S,T, E extends Exception> T checkedAddIfAbsent(Map<S,T> m, S k, CheckedSupplier<T, E> v) throws E {
		if (!m.containsKey(k))
			m.put(k, v.get());
		return m.get(k);
	}

	public static <S> boolean isAll(Collection<S> c, Predicate<S> p) {
		for (S s : c)
			if (!p.test(s))
				return false;
		return true;
	}

}
