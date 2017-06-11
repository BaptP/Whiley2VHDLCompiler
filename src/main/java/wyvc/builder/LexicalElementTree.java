package wyvc.builder;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import wyvc.builder.CompilerLogger.CompilerDebug;
import wyvc.builder.CompilerLogger.CompilerError;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.builder.CompilerLogger.LoggedBuilder;
import wyvc.builder.CompilerLogger.LoggedContainer;
import wyvc.builder.TypeCompiler.TypeTree;
import wyvc.utils.Generators.Generator_;
import wyvc.utils.Generators.PairGenerator;
import wyvc.utils.Generators.PairGenerator_;
import wyvc.utils.Generators.CustomPairGenerator;
import wyvc.utils.Generators.EndOfGenerationException;
import wyvc.utils.Generators.Generator;
import wyvc.utils.Generators;
import wyvc.utils.Pair;

/**
 * The outer class of the tree representation classes.
 *
 * It enables an easy use of the logger in the inner classes.
 *
 *
 *
 * @author Baptiste Pauget
 * @see LoggedContainer
 */
public class LexicalElementTree extends LoggedBuilder {
	/*------- Compiler messages -------*/
	/*------- Classes/Interfaces -------*/




	public static interface Tree {
		/*------- Compiler messages -------*/

		/**
		 * A CompilerError reporting an incoherence between to trees that should
		 * have the same internal structure.
		 *
		 * @author Baptiste Pauget
		 */
		public static class TreeStructureCompilerError extends CompilerError {
			private final Tree expected;
			private final Tree encountered;

			public TreeStructureCompilerError(Tree expected, Tree encountered) {
				this.expected = expected;
				this.encountered = encountered;
			}

			@Override
			public String info() {
				return "Tree structures incoherent\n"+
						expected.toString(	 "  Expected :    ", "                ")+
						encountered.toString("  Encountered : ", "                ");
			}

			public static CompilerException exception(Tree expected, Tree encountered) {
				return new CompilerException(new TreeStructureCompilerError(expected, encountered));
			}
		}


		public static class UnexistingFieldCompilerError extends CompilerError {
			private final NamedNode<?> record;
			private final String field;

			public UnexistingFieldCompilerError(NamedNode<?> record, String field) {
				this.record = record;
				this.field = field;
			}

			@Override
			public String info() {
				return "No field \""+field+"\" exist in the node "+record.toString("  ");
			}

			public static CompilerException exception(NamedNode<?> record, String field) {
				return new CompilerException(new UnexistingFieldCompilerError(record, field));
			}
		}


		/*------- Interface content -------*/


		/**
		 * Compares the tree structure with the one of <code>other</code>.
		 *
		 * @param other 				The tree to compare with
		 * @return 						<code>true</code> if the structure matches, else <code>false</code>
		 */
		public boolean isStructuredAs(Tree other);

		/**
		 * Compares the tree with <code>other</code>.
		 *
		 * The comparison checks that structure are identical and that the
		 * leaves' values are equals.
		 *
		 * @param other					The tree to compare with
		 * @return						<code>true</code> if the trees are equals, else <code>false</code>
		 */
		public boolean equals(Tree other);

		/**
		 * Pretty-printer for the tree structure.
		 *
		 * @param prefix1		The prefix to add to the first line of the representation
		 * @param prefix2		The prefix to add to the next lines of the representation
		 * @return				A representation of the tree
		 */
		public String toString(String prefix1, String prefix2);

		/**
		 * Pretty-printer for the tree structure.
		 *
		 * @param prefix		The prefix to add to thes lines of the representation
		 * @return				A representation of the tree
		 */
		public default String toString(String prefix) {
			return toString(prefix, prefix);
		}

		/**
		 * Ensures that the <code>other</code> tree has the same structure than <code>this</code>.
		 *
		 * @param other 				The tree to compare with
		 * @throws CompilerException 	Thrown if the structures mismatch
		 */
		public default void checkIdenticalStructure(Tree other) throws CompilerException {
			if (!isStructuredAs(other))
				throw TreeStructureCompilerError.exception(this, other);
		}
	}

	public class Leaf<V> implements Tree {
		private final V value;

		public Leaf(V value) {
			this.value = value;
		}

		public V getValue() {
			return value;
		}

//		public boolean equals(T other) {
//			return other instanceof Leaf && value.equals(((Leaf<?,?>)other).value);
//		}

		@Override
		public String toString(String prefix1, String prefix2) {
			return prefix1+value;
		}

		@Override
		public boolean isStructuredAs(Tree other) {
			return other instanceof Leaf;
		}

		@Override
		public boolean equals(Tree other) {
			return other instanceof Leaf && ((Leaf<?>)other).getValue().equals(getValue());
		}


	}

	public interface Node<T extends Tree> extends Tree {
		public abstract int getNumberOfComponents();
		public abstract PairGenerator<String, T> getComponents();


//		public default boolean equals(T other) {
//			return other instanceof Node && isStructuredAs(other) && getComponents().takeSecond().gather(
//				((Node<?>)other).getComponents().takeSecond()).forAll(Tree::equals);
//		}



		@Override
		default String toString(String prefix1, String prefix2) {
			final int l = getNumberOfComponents();
			return getComponents().enumerate().fold(
				(String s, Pair<Integer, Pair<String, T>> c) -> s+
				c.second.second.toString(
					prefix2 + (c.first+1 == l ? "└─ " : "├─ ") + (c.second.first.length() != 0 ? c.second.first+" : " : ""),
					prefix2 + (c.first+1 == l ? "   " : "│  ")),
				prefix1 + getClass().getSimpleName()+"\n");
		}

		@Override
		default boolean equals(Tree other) {
			return other instanceof Node && isStructuredAs(other) &&
					((Node<?>)other).getComponents().takeSecond().gather(getComponents().takeSecond()).forAll(Tree::equals);
		}
	}

	public class UnaryNode<T extends Tree, A extends T> implements Node<T> {
		private final A operand;

		public UnaryNode(A operand) {
			this.operand = operand;
		}

		protected String getLabel() {
			return "op";
		}

		public final A getOperand() {
			return operand;
		}

		@Override
		public final boolean isStructuredAs(Tree other) {
			return other instanceof UnaryNode && getOperand().isStructuredAs(((UnaryNode<?, ?>)other).getOperand());
		}

		@Override
		public final int getNumberOfComponents() {
			return 1;
		}

		@Override
		public final PairGenerator<String, T> getComponents() {
			return Generators.fromPairSingleton(getLabel(), getOperand());
		}
	}

	public class BinaryNode<T extends Tree, A extends T, B extends T> implements Node<T> {
		private final A firstOperand;
		private final B secondOperand;

		public BinaryNode(A firstOperand, B secondOperand) {
			this.firstOperand = firstOperand;
			this.secondOperand = secondOperand;
		}

		public BinaryNode(Pair<A, B> operands) {
			this(operands.first, operands.second);
		}

		protected String getFirstLabel() {
			return "op_0";
		}
		protected String getSecondLabel() {
			return "op_1";
		}

		public final A getFirstOperand() {
			return firstOperand;
		}
		public final B getSecondOperand() {
			return secondOperand;
		}

		@Override
		public final boolean isStructuredAs(Tree other) {
			return other instanceof BinaryNode &&
					getFirstOperand().isStructuredAs(((BinaryNode<?, ?, ?>)other).getFirstOperand()) &&
					getSecondOperand().isStructuredAs(((BinaryNode<?, ?, ?>)other).getSecondOperand());
		}

		@Override
		public final int getNumberOfComponents() {
			return 2;
		}

		@Override
		public final PairGenerator<String, T> getComponents() {
			return Generators.fromPairCollection(Arrays.asList(
				new Pair<>(getFirstLabel(), getFirstOperand()),
				new Pair<>(getSecondLabel(), getSecondOperand())));
		}
	}

	public class UnnamedNode<T extends Tree> implements Node<T> {
		private final List<T> options;

		public UnnamedNode(Generator<? extends T> options) {
			this.options = options.map((T t) -> t).toList();
		}
		public <E extends Exception> UnnamedNode(Generator_<? extends T, E> options) throws E {
			this.options = options.map((T t) -> t).toList();
		}
		public UnnamedNode(T option) {
			this.options = Collections.singletonList(option);
		}
		public UnnamedNode() {
			this.options = Collections.emptyList();
		}

		protected String getLabel(int k) {
			return "opt_"+k;
		}

		public Generator<T> getOptions() {
			return Generators.fromCollection(options);
		}

		public int getNumberOfOptions() {
			return options.size();
		}

		@Override
		public final boolean isStructuredAs(Tree other) {
			return other instanceof UnnamedNode &&
					getNumberOfOptions() == ((UnnamedNode<?>)other).getNumberOfOptions() &&
					getOptions().gather(((UnnamedNode<?>)other).getOptions()).forAll(Tree::isStructuredAs);
		}

		@Override
		public final int getNumberOfComponents() {
			return getNumberOfOptions();
		}

		@Override
		public final PairGenerator<String, T> getComponents() {
			return getOptions().enumerate().mapFirst(this::getLabel);
		}

	}

	public class NamedNode<T extends Tree> implements Node<T> {
		private final List<Pair<String,T>> fields;

		public <A extends T> NamedNode(Generator<Pair<String,A>> fields) {
			this.fields = Generators.toPairGenerator(fields).mapSecond((T t) -> t).toList();
		}
		public <A extends T, E extends Exception> NamedNode(Generator_<Pair<String,A>, E> fields) throws E {
			this.fields = Generators.toPairGenerator(fields).mapSecond((T t) -> t).toList();
		}
		public NamedNode(String name, T field) {
			this.fields = Collections.singletonList(new Pair<>(name, field));
		}
		public NamedNode() {
			this.fields = Collections.emptyList();
		}

		protected String getLabel(int k) {
			return "opt_"+k;
		}

		public PairGenerator<String, T> getFields() {
			return Generators.fromPairCollection(fields);
		}

		public Generator<String> getFieldNames() {
			return getFields().takeFirst();
		}

		public int getNumberOfFields() {
			return fields.size();
		}

		public boolean hasField(String field) {
			return getFieldNames().find(fields::equals) != null;
		}

		public T getField(String field) throws CompilerException {
			for (Pair<String,T> f : fields)
				if (f.first.equals(field))
					return f.second;
			throw UnexistingFieldCompilerError.exception(this, field);
		}

		@Override
		public final boolean isStructuredAs(Tree other) {
			return other instanceof NamedNode && hasSameFields((NamedNode<?>)other) &&
					getFields().takeSecond().gather(((NamedNode<?>)other).getFields().takeSecond()).forAll(Tree::isStructuredAs);
		}

		public final boolean hasSameFields(NamedNode<?> other) {
			return getNumberOfFields() == other.getNumberOfFields() && getFields().takeFirst().gather(other.getFields().takeFirst()).forAll(String::equals);
		}

		@Override
		public final int getNumberOfComponents() {
			return getNumberOfFields();
		}

		@Override
		public final PairGenerator<String, T> getComponents() {
			return getFields();
		}
	}

//
//
//
//
//
//	public class RecordNode<T extends Tree> extends NamedNode<T> {
//
//		public RecordNode(Generator<Pair<String,T>> fields) {
//			super(fields);
//		}
//		public <E extends Exception> RecordNode(Generator_<Pair<String,T>, E> fields) throws E {
//			super(fields);
//		}
//		public RecordNode(String name, T field) {
//			super(name, field);
//		}
//	}
//
//	public class UnionNode<T extends Tree> extends UnnamedNode<T> {
//
//		public UnionNode(Generator<T> options) {
//			super(options);
//		}
//		public <E extends Exception> UnionNode(Generator_<T, E> options) throws E {
//			super(options);
//		}
//		public UnionNode(T option) {
//			super(option);
//		}
//	}

//
//
//
//	public static interface EffectiveRecordNode<T extends Tree> extends Node<T> {
//
//		/*------- Compiler messages -------*/
//
//		/**
//		 * A {@link CompilerError} reporting a try to access a nonexistent field.
//		 *
//		 * @author Baptiste Pauget
//		 *
//		 */
//		public static class NonexistentFieldCompilerError extends CompilerError {
//			private final String name;
//			private final EffectiveRecordNode<?> record;
//
//			public NonexistentFieldCompilerError(String name, EffectiveRecordNode<?> record) {
//				this.name = name;
//				this.record = record;
//			}
//
//			@Override
//			public String info() {
//				return "No such field : "+name+" in\n"+
//						"  "+record+"\n"+record.toString("  ");
//			}
//
//			public static CompilerException exception(String name, EffectiveRecordNode<?> tree) {
//				return new CompilerException(new NonexistentFieldCompilerError(name, tree));
//			}
//		}
//
//		/*------- Interface content -------*/
//
//		public int getNumberOfFields();
//		public Generator<String> getFieldNames();
//	}
//
//	public abstract class RecordNode<T extends Tree> implements EffectiveRecordNode<T>, Tree {
//		private final Map<String, T> fields = new HashMap<>();
//
//		public RecordNode(Generator<Pair<String, T>> fields) {
//			Generators.toPairGenerator(fields).forEach(this.fields::put);
//		}
//
//		public <E extends Exception> RecordNode(Generator_<Pair<String, T>, E> fields) throws E {
//			Generators.toPairGenerator(fields).forEach(this.fields::put);
//		}
//
//		@Override
//		public int getNumberOfComponents() {
//			return fields.size();
//		}
//
//		@Override
//		public int getNumberOfFields() {
//			return fields.size();
//		}
//
//		@Override
//		public Generator<String> getFieldNames() {
//			return Generators.fromCollection(fields.entrySet()).map(Entry::getKey);
//		}
//
//		public PairGenerator<String, T> getFields() {
//			return Generators.fromCollection(fields.entrySet()).biMap(Entry::getKey, Entry::getValue);
//		}
//
//
//		@Override
//		public PairGenerator<String, T> getComponents() {
//			return getFields();
//		}
//
//
//		private final <U extends Tree> boolean isStructuredAsHelper(RecordNode<U> other) {
//			return other.getNumberOfComponents() == getNumberOfComponents() &&
//					getComponents().gather(other.getComponents()).forAll((Pair<String, T> c1, Pair<String, U> c2) ->
//					c1.first.equals(c2.first) && c1.second.equals(c2.second));
//		}
//
//		@Override
//		public boolean isStructuredAs(Tree other) {
//			if (other instanceof RecordNode)
//				return isStructuredAsHelper((RecordNode<?>) other);
//			return false;
//		}
//
//		public T getField(String name) throws CompilerException {
//			if (!fields.containsKey(name))
//				throw NonexistentFieldCompilerError.exception(name, this);
//			return fields.get(name);
//		}
//	}
//
//	public abstract class UnionNode<T extends Tree> implements Node<T> {
//		public final static String FLG_PREFIX = "is_t";
//		public final static String VAL_PREFIX = "vl_t"; // TODO Ailleurs
//
//		protected final List<T> options;
//
//		public UnionNode(Generator<T> options) {
//			this.options = options.toList();
//		}
//		public <E extends Exception> UnionNode(Generator_<T, E> options) throws E{
//			this.options = options.toList();
//		}
//
//		public int getNumberOfComponents() {
//			return 2*options.size();
//		}
//
//		public final int getNumberOfOptions() {
//			return options.size();
//		}
//
//		public final Generator<T> getOptions() {
//			return Generators.fromCollection(options);
//		}
//
//		public PairGenerator<String, T> getComponents() {
//			return getOptions().enumerate().mapFirst(Object::toString).mapFirst("opt_"::concat);
//		}
//
//		private final <U extends Tree, B extends Tree> boolean isStructuredAsHelper(UnionNode<U> other) {
//			return other.getNumberOfOptions() == getNumberOfOptions() &&
//					getOptions().gather(other.getOptions()).forAll(Tree::isStructuredAs);
//		}
//
//		@Override
//		public boolean isStructuredAs(Tree other) {
//			if (other instanceof UnionNode)
//				return isStructuredAsHelper((UnionNode<?>) other);
//			return false;
//		}
//	}
//
//
//	public abstract class RecordUnionNode<T extends Tree, B extends RecordNode<T>> implements EffectiveRecordNode<T> {
//		private final Map<String, T> sharedComponents = new HashMap<>();
//		private final Map<String, T> specificComponents = new HashMap<>();
//		private final Set<String> coexisting = new HashSet<>();
//
//		public RecordUnionNode(Generator<B> options) {
//			scanComponents(options.toList());
//		}
//
//		public <E extends Exception> RecordUnionNode(Generator_<B, E> options) throws E {
//			scanComponents(options.toList());
//		}
//
//		private void makeCoexisting(List<String> c) {
//			for (int k = 0; k < c.size()-1; ++k)
//				for (int l = k+1; k < c.size(); ++l)
//					coexisting.add(c.get(k).compareTo(c.get(l)) < 0 ? c.get(k) + "#" + c.get(l) : c.get(l) + "#" + c.get(k));
//		}
//
//		private void scanComponents(List<B> options) {
//			final Map<String, List<T>> sCps1 = new HashMap<>();
//			final Map<String, List<T>> sCps2 = new HashMap<>();
//			final Map<String, List<T>> spCps = new HashMap<>();
//			B opt = options.get(0);
//			makeCoexisting(opt.getFieldNames().toList());
//			opt.getFields().mapSecond(Collections::singletonList).forEach(sCps1::put);;
//			for (int k = 1; k<options.size() && !sCps1.isEmpty(); ++k) {
//				opt = options.get(k);
//				makeCoexisting(opt.getFieldNames().toList());
//				opt.getFields().forEach((String n, T t) -> {
//					if(sCps1.containsKey(n)) {
//						List<T> l = sCps1.remove(n);
//						l.add(t);
//						sCps2.put(n, l);
//					}
//					else
//						sCps1.put(n, Collections.singletonList(t));
//				});
//				for (Entry<String, List<T>> s : sCps1.entrySet()) {
//					if(spCps.containsKey(s.getKey()))
//						spCps.get(s.getKey()).addAll(s.getValue());
//					else
//						spCps.put(s.getKey(), s.getValue());
//				}
//				sCps1.clear();
//				sCps1.putAll(sCps2);
//				sCps2.clear();
//			}
//			Generators.fromMap(sCps1).mapSecond(Generators::fromCollection).mapSecond(this::getUnion).map(sharedComponents::put);
//			Generators.fromMap(spCps).mapSecond(Generators::fromCollection).mapSecond(this::getUnion).map(specificComponents::put);
//		}
//
//		protected abstract T getUnion(Generator<T> options);
//
//		private <U extends Tree> boolean isStructuredAsHelper(RecordUnionNode<U, ?> other) {
//			return sharedComponents.size() == other.sharedComponents.size() && specificComponents.size() == other.specificComponents.size() &&
//					coexisting.equals(other.coexisting) &&
//					Generators.fromMap(sharedComponents).gather(Generators.fromMap(other.sharedComponents)).
//					forAll((Pair<String,T> a, Pair<String, U> b) -> a.first.equals(b.first) && a.second.isStructuredAs(b.second)) &&
//					Generators.fromMap(specificComponents).gather(Generators.fromMap(other.specificComponents)).
//					forAll((Pair<String,T> a, Pair<String, U> b) -> a.first.equals(b.first) && a.second.isStructuredAs(b.second));
//		}
//
//		@Override
//		public boolean isStructuredAs(Tree other) {
//			return other instanceof RecordUnionNode ? isStructuredAsHelper((RecordUnionNode<?, ?>) other) : false;
//		}
//
//		@Override
//		public int getNumberOfFields() {
//			return sharedComponents.size();
//		}
//
//		@Override
//		public Generator<String> getFieldNames() {
//			return Generators.fromCollection(sharedComponents.keySet());
//		}
//
//		public int getNumberOfPotentialFields() {
//			return specificComponents.size();
//		}
//
//		public Generator<String> getPotentialFieldNames() {
//			return Generators.fromCollection(specificComponents.keySet());
//		}
//
//		public PairGenerator<String,T> getFields() {
//			return Generators.fromMap(sharedComponents);
//		}
//
//		public PairGenerator<String,T> getPotentialFields() {
//			return Generators.fromMap(specificComponents);
//		}
//
//
//		public T getField(String name) throws CompilerException {
//			if (!sharedComponents.containsKey(name))
//				throw NonexistentFieldCompilerError.exception(name, this);
//			return sharedComponents.get(name);
//		}
//
//		public T getPotentialField(String name) throws CompilerException {
//			if (!specificComponents.containsKey(name))
//				throw NonexistentFieldCompilerError.exception(name, this);
//			return specificComponents.get(name);
//		}
//
//		public boolean coextist(String pField1, String pField2) {
//			return coexisting.contains(pField2+"#"+pField2) || coexisting.contains(pField2+"#"+pField1);
//		}
//
//		@Override
//		public String toString(String prefix1, String prefix2) {
//			final int l = getNumberOfFields();
//			final int m = getNumberOfPotentialFields();
//			return getFields().append(getPotentialFields()).enumerate().fold(
//				(String s, Pair<Integer, Pair<String, T>> c) -> s+
//				prefix2 + (c.first+1 == l+m ? " └" : " ├") + (c.first < l ? "─ " : "╌ ") + c.second.first+"\n"+c.second.second.toString(prefix2 + " │ "),
//				prefix1 + getClass().getSimpleName());
//		}
//
//
//
//		@Override
//		public int getNumberOfComponents() {
//			// TODO Auto-generated method stub
//			return 0;
//		}
//
//		@Override
//		public PairGenerator<String, T> getComponents() {
//			// TODO Auto-generated method stub
//			return null;
//		}
//	}
//
//
//	/**
//	 * Represents any node (leaf of inner node) of a tree structure that contains
//	 * values only in leaves.
//	 *
//	 * <h2> Subclassing </h2>
//	 *
//	 * To provide highly usable common methods, recursive generic parameters are
//	 * used.
//	 *
//	 * <pre>
//	 * {@code
//	 * interface MyTree extends Tree<MyTree,MyValue> {
//	 *  	...
//	 * }
//	 *
//	 * class MyPrimitive extends Primitive<MyTree, My Value> implements MyTree {
//	 *  	...
//	 * }
//	 *
//	 * class MyCompound extends Compound<MyTree, My Value> implements MyTree {
//	 *  	...
//	 * }
//	 * }
//	 * </pre>
//	 *
//	 * A <code>MyTree</code> object will looks like :
//	 * <pre>
//	 * {@code
//	 * this : MyCompound
//	 *  ├─ cmp_0 : MyCompound
//	 *  │   ├─ cmp_0 : MyPrimitive
//	 *  │   └─ cmp_1 : MyPrimitive
//	 *  ├─ cmp_1 : MyCompound
//	 *  │   └─ cmp_0 : MyPrimitive
//	 *  └─ cmp_2 : MyPrimitive
//	 * }
//	 * </pre>
//	 *
//	 * @author Baptiste Pauget
//	 *
//	 * @param <T> The precise tree type, to enable easy sub-classing
//	 * @param <V> The values' type contained in leaves
//	 * @see Primitive
//	 * @see Compound_
//	 */
//	public interface Tree_<T extends Tree_<T,V>,V> {
//
//
//		/*------- Interface Content -------*/
//
//		/**
//		 * Provides the value of a node.
//		 *
//		 * This method provided to avoid annoying type casting but should be
//		 * only used after checking that the node is a leaf.
//		 *
//		 * @return The value contained in a leaf or <code>null</code> for a inner node
//		 */
//		public V getValue();
//
//		/**
//		 * Provides the children of a node.
//		 *
//		 * This method provided to avoid annoying type casting but should be
//		 * only used after checking that the node is a inner node.
//		 *
//		 * @return A generator giving the subtrees of a node.
//		 */
//		public StandardPairGenerator<String,T> getComponents();
//
//		/**
//		 * Provides the number of children of a node.
//		 *
//		 * This method should not be use to determine whether a node is a leaf or not,
//		 * since empty inner node can exist.
//		 *
//		 * @return The number of subtrees of the node.
//		 */
//		public int getNumberOfComponents();
//
//		/**
//		 * Compares the tree structure with the one of <code>other</code>
//		 *
//		 * @param other The tree used for comparison
//		 * @return <code>true</code> if the structure matches, else <code>false</code>
//		 */
//		public boolean isStructuredAs(Tree_<?,?> other);
//
//		/**
//		 * Provides the internal structure of a inner node.
//		 *
//		 * This method provided to avoid annoying type casting but should be
//		 * only used after checking that the node is a inner node.
//		 *
//		 * @return The structure of a inner node or <code>null</code> for a leaf.
//		 */
//		public Structure<T,V> getStructure();
//
//		public T getParent(); // TODO j'en veux pas.
//		public void setParent(T parent); // TODO ça non plus !
//
//		/**
//		 * Checks that the <code>other</code> tree has the same structure than <code>this</code>.
//		 *
//		 * @param other The tree used for comparison
//		 * @throws CompilerException Thrown if the structures mismatch
//		 */
//		public default void checkIdenticalStructure(Tree_<?,?> other) throws CompilerException {
//			if (!isStructuredAs(other))
//				throw TreeStructureCompilerError.exception(this, other);
//		}
//
//		/**
//		 * Provides the values that are contained in the leaves of the tree.
//		 *
//		 * @return A generator giving the values of the leaves
//		 */
//		public default StandardGenerator<V> getValues() {
//			if (this instanceof Primitive)
//				return Generator.fromSingleton(getValue());
//			return Generator.concat(getComponents().takeSecond().map(Tree_::getValues));
//		}
//
//		/**
//		 * Checks if a component <code>component</code> exists in the children of the node.
//		 *
//		 * @param component The named of the requested component
//		 * @return
//		 */
//		public default boolean hasComponent(String component) {
//			return getComponents().takeFirst().find(component) != null;
//		}
//
//		/**
//		 * Provides the component named <code>component</code> of <code>this</code>.
//		 *
//		 * @param component
//		 * @return The sub tree starting with the child named <code>component</code> node
//		 * @throws CompilerException thrown is no component <code>component</code> exists
//		 */
//		public default T getComponent(String component) throws CompilerException {
//			return getComponents().findOrThrow((Pair<String, T> c) ->c.first.equals(component), () -> TreeComponentException.exception(component, this)).second;
//		}
//
////		public default void printStructure(CompilerLogger logger, String start) { // TODO Salut !?
////			logger.debug(toString(start));
////		}
//
//		/**
//		 * Pretty-printer for the tree
//		 *
//		 * @param prefix A string to add at each line
//		 * @return A representation of the tree's structure
//		 */
//		public default String toString(String prefix) {
//			return this instanceof Primitive ? "" : getStructure().toString(prefix);
//		}
//	}
//
//
//
//	public static class ReuseOfNodeCompilerDebug extends CompilerDebug {
//		private final Tree_<?,?> node;
//
//		public ReuseOfNodeCompilerDebug(Tree_<?,?> node) {
//			this.node = node;
//		}
//
//		@Override
//		public String info() {
//			return "Setting parent for the node "+node+" that has already one";
//		}
//	}
//
//	/**
//	 *
//	 * @author Baptiste Pauget
//	 *
//	 * @param <T> The precise tree type of the components
//	 * @param <V> The values contains in the component trees
//	 *
//	 * @see Tree_
//	 * @see Compound_
//	 */
//	public class Primitive<T extends Tree_<T,V>,V> implements Tree_<T,V> {
//		protected V value;
//		private T parent = null;
//
//		public Primitive(V value) {
//			this.value = value;
//		}
//
//		@Override
//		public V getValue() {
//			return value;
//		}
//		@Override
//		public StandardPairGenerator<String,T> getComponents() {
//			return Generator.emptyPairGenerator();
//		}
//		@Override
//		public boolean isStructuredAs(Tree_<?,?> other) {
//			return other instanceof Primitive;
//		}
//		@Override
//		public Structure<T, V> getStructure() {
//			if (this.parent != null)
//				logger.addMessage(new CompilerDebug() {
//					@Override
//					public String info() {
//						return "Trying to get a structure from the primitive node "+Primitive.this+".";
//					}
//				});
//			return null;
//		}
//
//		@Override
//		public T getParent() {
//			return parent;
//		}
//
//		@Override
//		public void setParent(T parent) {
//			if (this.parent != null)
//				logger.addMessage(new ReuseOfNodeCompilerDebug(this));
//			this.parent = parent;
//		}
//
//		@Override
//		public int getNumberOfComponents() {
//			return 0;
//		}
//	}
//
//
//
//	/**
//	 *
//	 * @author Baptiste Pauget
//	 *
//	 * @param <T> The precise tree type of the components
//	 * @param <V> The values contains in the component trees
//	 *
//	 * @see Primitive
//	 * @see Compound_
//	 */
//	public static interface Structure<T extends Tree_<T,V>,V> {
//		/*------- Compiler messages -------*/
//
//		/**
//		 * A CompilerError reporting an incoherence between to structures that should
//		 * be the same.
//		 *
//		 * @author Baptiste Pauget
//		 */
//		public static class IncoherentStructureCompilerError extends CompilerError {
//			private final Structure<?,?> expected;
//			private final Structure<?,?> encountered;
//
//			public IncoherentStructureCompilerError(Structure<?,?> expected, Structure<?,?> encountered) {
//				this.expected = expected;
//				this.encountered = encountered;
//			}
//
//			@Override
//			public String info() {
//				return "Structures incoherent\n"+
//						" Expected :    "+expected+"\n"+expected.toString("              ")+
//						" Encountered : "+encountered+"\n"+encountered.toString("              ");
//			}
//
//			public static CompilerException exception(Structure<?,?> expected, Structure<?,?> encountered) {
//				return new CompilerException(new IncoherentStructureCompilerError(expected, encountered));
//			}
//		}
//
//
//		/*------- Interface Content -------*/
//
//		public int getNumberOfComponents();
//		public StandardPairGenerator<String, T> getComponents();
//		public String toString(String prefix);
//		public boolean isStructureAs(Structure<?,?> other);
//		public default void checkIdenticalStructure(Structure<?,?> other) throws CompilerException {
//			if (!isStructureAs(other))
//				IncoherentStructureCompilerError.exception(this, other);
//		}
//	}
//
//
//
//	private static interface Compound<T extends Tree_<T,V>,V, S extends Structure<T,V>> extends Tree_<T,V>{}
//	/**
//	 *
//	 *
//	 * @author Baptiste Pauget
//	 *
//	 * @param <T> The precise tree type of the components
//	 * @param <V> The values contains in the component trees
//	 * @param <S> The structure type of the inner node
//	 *
//	 * @see Tree_
//	 * @see Compound_
//	 */
//	public class Compound_<T extends Tree_<T,V>,V, S extends Structure<T,V>> implements Compound<T,V,S> {
//		protected final S structure;
//		private T parent = null;
//
//		@SuppressWarnings("unchecked")
//		public Compound_(S structure) {
//			this.structure = structure;
//			structure.getComponents().forEach((String s, T t) -> t.setParent((T)this)); // TODO grrr
//		}
//
//		@Override
//		public V getValue() {
//			if (this.parent != null)
//				logger.addMessage(new CompilerDebug() {
//					@Override
//					public String info() {
//						return "Trying to get a value from the compound node "+Compound_.this+".";
//					}
//				});
//			return null;
//		}
//		@Override
//		public StandardPairGenerator<String,T> getComponents() {
//			return structure.getComponents();
//		}
//		@Override
//		public boolean isStructuredAs(Tree_<?,?> other) {
//			return structure.isStructureAs(other.getStructure());
//			/*if (getNumberOfComponents() != other.getNumberOfComponents())
//				return false;
//			List<Pair<String, R>> otherComponents = other.getComponents();
//			for (int k = 0; k < components.size(); ++k)
//				if (!components.get(k).first.equals(otherComponents.get(k).first) ||
//					!components.get(k).second.isStructuredAs(otherComponents.get(k).second))
//					return false;
//			return true;*/
//		}
//		@Override
//		public S getStructure() {
//			return structure;
//		}
//
//		@Override
//		public T getParent() {
//			return parent;
//		}
//
//		@Override
//		public void setParent(T parent) {
//			if (this.parent != null)
//				logger.addMessage(new ReuseOfNodeCompilerDebug(this));
//			this.parent = parent;
//		}
//
//		@Override
//		public int getNumberOfComponents() {
//			return structure.getNumberOfComponents();
//		}
//	}
//
//
//
//	public interface EffectiveRecordStructure<T extends Tree_<T,V>,V> extends Structure<T,V> {
//		/*------- Compiler messages -------*/
//
//
//
//		/*------- Interface Content -------*/
//
//		public int getNumberOfFields();
//		public StandardGenerator<String> getFieldNames();
//		public T getReadingField(String f) throws CompilerException;
//		public StandardGenerator<T> getWrittingField(String f) throws CompilerException;
//	}
//
//
//
//
//
//
//
//	public static class RecordStructure<T extends Tree_<T,V>,V> implements EffectiveRecordStructure<T,V> {
//		private final Map<String, T> fields = new HashMap<>();
//
//
//		//---- Constructors
//
//		public RecordStructure(StandardGenerator<Pair<String, T>> fields) {
//			Generator.toPairGenerator(fields).forEach(this.fields::put);
//		}
//
//		public <E extends Exception> RecordStructure(CheckedGenerator<Pair<String, T>, E> fields) throws E {
//			Generator.toPairGenerator(fields).forEach(this.fields::put);
//		}
//
//
//		//---- Override methods from Structure
//
//		@Override
//		public boolean isStructureAs(Structure<?, ?> other) {
//			if (other instanceof RecordStructure)
//				return getNumberOfFields() == ((RecordStructure<?,?>)other).getNumberOfFields() && getFields().takeFirst().gather(
//					((RecordStructure<?,?>)other).getFields().takeFirst()).forAll((String s1, String s2) -> s1.equals(s2));
//			return false;
//		}
//
//		@Override
//		public StandardPairGenerator<String, T> getComponents() {
//			return getFields();
//		}
//
//		@Override
//		public int getNumberOfComponents() {
//			return getNumberOfFields();
//		}
//
//		@Override
//		public String toString(String prefix) {
//			final int l = getNumberOfFields();
//			return getFields().enumerate().fold(
//				(String s, Pair<Integer, Pair<String, T>> c)
//					-> s+prefix + (c.first+1 == l ? " └─ " : " ├─ ") + c.second.first+"\n"+c.second.second.toString(prefix + " │ "),
//				"");
//		}
//
//
//		//---- Override methods from EffectiveRecordStructure
//
//		@Override
//		public int getNumberOfFields() {
//			return fields.size();
//		}
//
//		@Override
//		public T getReadingField(String field) throws CompilerException {
//			return getField(field);
//		}
//
//		@Override
//		public StandardGenerator<T> getWrittingField(String field) throws CompilerException {
//			return Generator.fromSingleton(getField(field));
//		}
//
//		@Override
//		public StandardGenerator<String> getFieldNames() {
//			return Generator.fromCollection(fields.entrySet()).map(Entry::getKey);
//		}
//
//
//		//---- Proper methods
//
//		public StandardPairGenerator<String, T> getFields() {
//			return Generator.fromCollection(fields.entrySet()).biMap(Entry::getKey, Entry::getValue);
//		}
//
//		public T getField(String field) throws CompilerException {
//			if (fields.containsKey(field))
//				return fields.get(field);
//			throw UnexistingFieldCompilerError.exception(field, this);
//		}
//
//	}
//
//	public static class UnionStructure<T extends Tree_<T,V>,V> implements Structure<T,V> {
//
//		public final static String FLG_PREFIX = "is_t";
//		public final static String VAL_PREFIX = "vl_t";
//
//		public final List<Pair<T,T>> options;
//
//		//---- Constructors
//
//		public UnionStructure(StandardGenerator<Pair<T,T>> options) {
//			this.options = options.toList();
//		}
//
//		public <E extends Exception> UnionStructure(CheckedGenerator<Pair<T,T>, E> options) throws E {
//			this.options = options.toList();
//		}
//
//
//		//---- Override methods from Structure
//
//		@Override
//		public StandardPairGenerator<String, T> getComponents() {
//			return new StandardPairGenerator<String,T>(){
//				@Override
//				protected void generate() throws InterruptedException, wyvc.utils.Generator.EndOfGenerationException {
//					int k = 0;
//					for (Pair<T,T> p : options) {
//						yield(new Pair<>(FLG_PREFIX + k, p.first));
//						yield(new Pair<>(VAL_PREFIX + k++, p.second));
//					}
//				}};
//		}
//
//		@Override
//		public String toString(String prefix) {
//			final int l = getNumberOfOptions();
//			return getOptions().enumerate().fold(
//				(String s, Pair<Integer, Pair<T, T>> c) -> s+
//				prefix + (c.first+1 == l ? " └┰ " : " ├┰ ") + FLG_PREFIX + c.first +"\n"+c.second.first.toString(prefix + " │  ") +
//				prefix + (c.first+1 == l ? "  ┖ " : " │┖ ") + VAL_PREFIX + c.first+"\n"+c.second.second.toString(prefix + (c.first+1 == l ? "    " : " │  ")),
//				"");
//		}
//
//		public <U extends Tree_<U,W>,W> boolean isStructureAsHelper(Structure<U, W> other) {
//			if (other instanceof UnionStructure)
//				return getNumberOfOptions() == ((UnionStructure<U,W>)other).getNumberOfOptions() && getOptions().takeSecond().gather(
//					((UnionStructure<U,W>)other).getOptions().takeSecond()).forAll((T t, U u) -> t.isStructuredAs(u));
//			return false;
//		}
//
//		@Override
//		public boolean isStructureAs(Structure<?, ?> other) {
//			return isStructureAsHelper(other);
//		}
//
//		@Override
//		public int getNumberOfComponents() {
//			return 2*options.size();
//		}
//
//
//		//---- Proper methods
//
//		public StandardPairGenerator<T,T> getOptions() {
//			return Generator.fromPairCollection(options);
//		}
//
//		public int getNumberOfOptions() {
//			return options.size();
//		}
//	}
//
//
//	public abstract static class RecordsUnionStructure<T extends Tree_<T,V>, V>  extends UnionStructure<T,V> implements EffectiveRecordStructure<T,V> {
//		//protected final List<Pair<String,Compound<T,V,EffectiveRecordStructure<T,V>>>> components;
//
//		public <C extends T&Compound<T,V,?>> RecordsUnionStructure(StandardGenerator<Pair<T,C>> options) {
//			super(options);
//			//this.components = components.<>convert().toList();
//			//this.options = options.toList();
//		}
//
//		@Override
//		public int getNumberOfComponents() {
//			return 2*options.size();
//		}
//
//		@Override
//		public StandardPairGenerator<String, T> getComponents() {
//			return new StandardPairGenerator<String,T>(){
//				@Override
//				protected void generate() throws InterruptedException, wyvc.utils.Generator.EndOfGenerationException {
//					int k = 0;
//					for (Pair<T,T> p : options) {
//						yield(new Pair<>(FLG_PREFIX + k, p.first));
//						yield(new Pair<>(VAL_PREFIX + k++, p.second));
//					}
//				}};
//		}
//
//		@Override
//		public String toString(String prefix) {
//			final int l = getNumberOfFields();
//			final int m = getNumberOfOptions();
//			return getOptions().enumerate().fold(
//				(String s, Pair<Integer, Pair<T, T>> c) -> s+
//				prefix + (c.first+1 == m ? " └┰ " : " ├┰ ") + FLG_PREFIX + c.first +"\n"+c.second.first.toString(prefix + " │  ") +
//				prefix + (c.first+1 == m ? "  ┖ " : " │┖ ") + VAL_PREFIX + c.first+"\n"+c.second.second.toString(prefix + (c.first+1 == l ? "    " : " │  ")),
//				"");
//		}
//
//		public <U extends Tree_<U,W>,W> boolean isStructureAsHelper(Structure<U, W> other) {
//			if (other instanceof RecordsUnionStructure)
//				return getNumberOfOptions() == ((RecordsUnionStructure<U,W>)other).getNumberOfOptions() && getOptions().takeSecond().gather(
//					((RecordsUnionStructure<U,W>)other).getOptions().takeSecond()).forAll((T t, U u) -> t.isStructuredAs(u));
//			return false;
//		}
//
//		@Override
//		public boolean isStructureAs(Structure<?, ?> other) {
//			return isStructureAsHelper(other);
//		}
//
//		@Override
//		public int getNumberOfFields() {
//			return components.size();
//		}
//
//		@Override
//		public int getNumberOfOptions() {
//			return options.size();
//		}
//
//		@Override
//		public StandardPairGenerator<T, T> getOptions() {
//			return Generator.fromPairCollection(options);
//		}
//	}
//



	public LexicalElementTree(CompilerLogger logger) {
		super(logger);
	}
}
