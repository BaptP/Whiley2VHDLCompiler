package wyvc.utils;

import wyil.lang.Bytecode.Assign;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import wyil.lang.SyntaxTree;
import wyil.lang.SyntaxTree.Location;

public class Utils {
	public static void printLocation(Location<?> a, String n) {
		System.out.println(n+a.toString());
		for(Location<?> l : a.getOperands())
			printLocation(l, n+" |  ");
		if (a.getBytecode() instanceof Assign) {
			for(Location<?> l : a.getOperandGroup(SyntaxTree.LEFTHANDSIDE))
				printLocation(l, n+" |<-");
			for(Location<?> l : a.getOperandGroup(SyntaxTree.RIGHTHANDSIDE))
				printLocation(l, n+" |->");
		}
	}

	public static <S,T> T[] toArray(Collection<S> l, Function<S,T> f, T[] t) {
		ArrayList<T> m = new ArrayList<>();
		for (S e : l)
			m.add(f.apply(e));
		return m.toArray(t);
	}

	public static <S,T> List<Pair<S,T>> join(List<S> l1, List<T> l2) {
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

	public static <S,T> List<T> convert(List<S> l, Function<S,T> f) {
		ArrayList<T> m = new ArrayList<>(l.size());
		for (S s : l)
			m.add(f.apply(s));
		return m;
	}

	@SuppressWarnings("unchecked")
	public static <S,T> List<T> convert(List<S> l) {
		return convert(l, (S s) -> (T) s);
	}

	public static <S,T> List<T> convertInd(List<S> l, Function<Pair<S,Integer>,T> f) {
		ArrayList<T> m = new ArrayList<>(l.size());
		int k = 0;
		for (S s : l)
			m.add(f.apply(new Pair<S,Integer>(s,k++)));
		return m;
	}
}
