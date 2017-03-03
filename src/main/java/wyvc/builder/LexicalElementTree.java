package wyvc.builder;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import wyvc.builder.VHDLCompileTask.VHDLCompilationException;
import wyvc.utils.Pair;

public final class LexicalElementTree {
	static class IdentifierStructureException extends VHDLCompilationException {
		private static final long serialVersionUID = 3351421359541042141L;
		private final Tree<?,?> expected;
		private final Tree<?,?> encountered;

		public IdentifierStructureException(Tree<?,?> expected, Tree<?,?> encountered) {
			this.expected = expected;
			this.encountered = encountered;
		}

		@Override
		public void details() {
			System.err.println("    Elements structure incoherent");
			System.err.println("    Expected :    *");
			expected.printStructure(System.err, "                 ");
			System.err.println("    Encountered : *");
			encountered.printStructure(System.err, "                 ");

		}
	}

	static class IdentifierComponentException extends VHDLCompilationException {
		private static final long serialVersionUID = 1678182755447780658L;
		private final String name;

		public IdentifierComponentException(String name) {
			this.name = name;
		}

		@Override
		public void details() {
			System.err.println("    No such component : "+name);
		}
	}

	public static interface Tree<T extends Tree<T,V>,V> {
		public V getValue();
		public List<Pair<String,T>> getComponents();
		public <S extends Tree<S,U>,U> boolean isStructuredAs(Tree<S,U> other);
		public default int getComponentNumber()	{
			return getComponents().size();
		}
		public default <S extends Tree<S,U>,U> void checkIdenticalStructure(Tree<S,U> other) throws IdentifierStructureException {
			if (!isStructuredAs(other))
				throw new IdentifierStructureException(this, other);
		}
		public default List<V> getValues() {
			if (this instanceof Primitive<?,?>)
				return Collections.<V>singletonList(getValue());
			ArrayList<V> val = new ArrayList<>();
			for (Pair<String, T> t : getComponents())
				val.addAll(t.second.getValues());
			return val;
		}
		public default boolean hasComponent(String c) {
			for (Pair<String,T> p : getComponents())
				if (p.first.equals(c))
					return true;
			return false;
		}
		public default T getComponent(String c) throws IdentifierComponentException {
			for (Pair<String,T> p : getComponents())
				if (p.first.equals(c))
					return p.second;
			throw new IdentifierComponentException(c);
		}
		public default void printStructure(PrintStream s, String start) {
			List<Pair<String, T>> l = getComponents();
			int m = l.size();
			int k = 0;
			for (Pair<String, T> p : l){
				s.println(start + (k+++1 == m ? " └─ " : " ├─ ") + p.first);
				p.second.printStructure(s,  start + " │ ");
			}
		}
	}


	public static class Primitive<T extends Tree<T,V>,V> implements Tree<T,V> {
		protected V value;

		public Primitive(V value) {
			this.value = value;
		}

		@Override
		public V getValue() {
			return value;
		}
		@Override
		public List<Pair<String, T>> getComponents() {
			return Collections.<Pair<String, T>>emptyList();
		}
		@Override
		public <S extends Tree<S,U>,U> boolean isStructuredAs(Tree<S,U> other) {
			return other instanceof Primitive;
		}


	}

	public static class Compound<T extends Tree<T,V>,V> implements Tree<T,V> {
		private final List<Pair<String, T>> components;

		public Compound(List<Pair<String, T>> components) {
			this.components = components;
		}

		@Override
		public V getValue() {
			return null;
		}
		@Override
		public List<Pair<String, T>> getComponents() {
			return components;
		}
		@Override
		public <S extends Tree<S,U>,U> boolean isStructuredAs(Tree<S,U> other) {
			if (getComponentNumber() != other.getComponentNumber())
				return false;
			List<Pair<String, S>> otherComponents = other.getComponents();
			for (int k = 0; k < components.size(); ++k)
				if (!components.get(k).first.equals(otherComponents.get(k).first) ||
					!components.get(k).second.isStructuredAs(otherComponents.get(k).second))
					return false;
			return true;
		}
	}




//
//
//	///////////////////////////////////////////////////////////////////////
//	//                            Signals                                //
//	///////////////////////////////////////////////////////////////////////
//
//	public static interface SignalTree extends Tree<SignalTree,Signal> {
//		public void assign(ExpressionTree expression);
//
//		public static SignalTree create(String ident, TypeTree t) {
//			if (t instanceof PrimitiveType)
//				return new PrimitiveSignal(new Signal(ident, t.getValue()));
//			ArrayList<SignalTree> cmp = new ArrayList<>(t.getComponentNumber());
//			for (Pair<String, TypeTree> p : t.getComponents())
//				cmp.add(create(ident, p.second));
//			return null;
//		}
//	}
//
//	public static class PrimitiveSignal extends Primitive<SignalTree,Signal> implements SignalTree {
//		public PrimitiveSignal(Signal signal) {
//			super(signal);
//		}
//
//		@Override
//		public void assign(ExpressionTree expression) {
//
//		}
//	}
//
//	public static class CompoundSignal extends Compound<SignalTree,Signal> implements SignalTree {
//		public CompoundSignal(List<Pair<String, SignalTree>> components) {
//			super(components);
//		}
//
//		@Override
//		public void assign(ExpressionTree expression) {
//
//		}
//	}


}
