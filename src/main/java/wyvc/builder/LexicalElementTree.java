package wyvc.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import wyvc.builder.CompilerLogger.CompilerError;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.utils.Pair;

public final class LexicalElementTree {
	static class TreeStructureError extends CompilerError {
		private final Tree<?,?> expected;
		private final Tree<?,?> encountered;

		public TreeStructureError(Tree<?,?> expected, Tree<?,?> encountered) {
			this.expected = expected;
			this.encountered = encountered;
		}

		@Override
		public String info() {
			return "Tree structures incoherent\n"+
					" Expected :    *\n"+expected.toString("             ")+
					" Encountered : *\n"+encountered.toString("             ");
		}
	}

	static class TreeComponentException extends CompilerError {
		private final String name;
		private final Tree<?,?> tree;

		public TreeComponentException(String name, Tree<?,?> tree) {
			this.name = name;
			this.tree = tree;
		}

		@Override
		public String info() {
			return "No such component : "+name+"in\n"+
					"  "+tree+"\n"+tree.toString("  ");
		}
	}

	public static interface Tree<T extends Tree<T,V>,V> {
		public V getValue();
		public List<Pair<String,T>> getComponents();
		public <S extends Tree<S,U>,U> boolean isStructuredAs(Tree<S,U> other);
		public default int getComponentNumber()	{
			return getComponents().size();
		}
		public default <S extends Tree<S,U>,U> void checkIdenticalStructure(Tree<S,U> other) throws CompilerException {
			if (!isStructuredAs(other))
				throw new CompilerException(new TreeStructureError(this, other));
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
		public default T getComponent(String c) throws CompilerException {
			for (Pair<String,T> p : getComponents())
				if (p.first.equals(c))
					return p.second;
			throw new CompilerException(new TreeComponentException(c, this));
		}
		public default void printStructure(CompilerLogger logger, String start) {
			List<Pair<String, T>> l = getComponents();
			int m = l.size();
			int k = 0;
			for (Pair<String, T> p : l){
				logger.debug(start + (k+++1 == m ? " └─ " : " ├─ ") + p.first);
				p.second.printStructure(logger,  start + " │ ");
			}
		}
		public default String toString(String prefix) {
			String a = "";
			List<Pair<String, T>> l = getComponents();
			int m = l.size();
			int k = 0;
			for (Pair<String, T> p : l){
				a += prefix + (k+++1 == m ? " └─ " : " ├─ ") + p.first+"\n";
				p.second.toString(prefix + " │ ");
			}
			return a;
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
		protected final List<Pair<String, T>> components;
		protected T parent = null;

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
}
