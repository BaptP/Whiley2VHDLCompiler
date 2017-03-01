package wyvc.builder;

import wyil.lang.Bytecode.Assign;

import java.util.ArrayList;
import java.util.Collection;
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
}
