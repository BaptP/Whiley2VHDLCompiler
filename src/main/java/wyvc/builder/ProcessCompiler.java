package wyvc.builder;

import wyvc.builder.DataFlowGraph;
import wyvc.lang.Statement.Process;
import wyvc.lang.Statement.SequentialStatement;
import wyvc.lang.TypedValue.Signal;
import wyvc.lang.TypedValue.Variable;
import wyvc.utils.Pair;;

public class ProcessCompiler {
/*

	public static interface Tree<T extends Tree<T>> {
		public boolean isStructuredAs(T other);
	}


	public static class Leaf<T extends Tree<T>, V> implements Tree<T> {
		private final V value;
		public Leaf(V value) {
			this.value = value;
		}

		public V getValue() {
			return value;
		}

		public boolean isStructuredAs(T other) {
			return other instanceof Leaf;
		}
	}



	public static class Record<T extends Tree<T>> implements Tree<T> {
		private final T field;
		public Record(T field) {
			this.field = field;
		}

		public T getField() {
			return field;
		}

		public boolean isStructuredAs(T other) {
			return other instanceof Leaf;
		}
	}

	public static class Union<T extends Tree<T>, L extends Leaf<T,?>, R extends Record<T>> implements Tree<T> {
		private final Pair<L,R> option;

		public Union(Pair<L,R> option) {
			this.option = option;
		}

		public Pair<L,R> getOptions() {
			return option;
		}

		public boolean isStructuredAs(T other) {
			return other instanceof Union;
		}
	}



	public static interface Type {}
	public static interface TypeTree extends Tree<TypeTree> {

	}
	public static class TypeLeaf extends Leaf<TypeTree, Type> implements TypeTree {
		public TypeLeaf(Type value) {
			super(value);
		}
	}
	public static class TypeRecord extends Record<TypeTree> implements TypeTree {
		public TypeRecord(TypeTree field) {
			super(field);
		}

	}
	public static class TypeUnion extends Union<TypeTree, TypeLeaf, TypeRecord> implements TypeTree {
		public TypeUnion(Pair<TypeLeaf, TypeRecord> option) {
			super(option);
		}

	}



	public static interface Vertex {}
	public static interface VertexTree extends Tree<VertexTree> {
		public TypeTree getType();

	}
	public static class VertexLeaf extends Leaf<VertexTree, Vertex> implements VertexTree {
		private final TypeTree type;

		public VertexLeaf(Vertex value, TypeLeaf type) {
			super(value);
			this.type = type;
		}

		@Override
		public TypeTree getType() {
			return type;
		}
	}
	public static class VertexRecord extends Record<VertexTree> implements VertexTree {
		private final TypeTree type;

		public VertexRecord(VertexTree field, TypeRecord type) {
			super(field);
			this.type = type;
		}

		@Override
		public TypeTree getType() {
			return type;
		}
	}
	public static class VertexUnion extends Union<VertexTree, VertexLeaf, VertexRecord> implements VertexTree {
		private final TypeTree type;

		public VertexUnion(Pair<VertexLeaf, VertexRecord> option, TypeUnion type) {
			super(option);
			this.type = type;
		}

		@Override
		public TypeTree getType() {
			return type;
		}
	}


	public void a(VertexUnion v) {
		v.getOptions().first.getType();
	}
*/


	static Process compileProcess(String ident, DataFlowGraph section) {
		return new Process(ident, new Variable[0], new Signal[0], new SequentialStatement[0]);
	}
}
