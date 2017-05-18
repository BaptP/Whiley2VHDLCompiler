package wyvc.builder;

import wyvc.builder.CompilerLogger.CompilerDebug;
import wyvc.builder.CompilerLogger.CompilerError;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.builder.CompilerLogger.LoggedBuilder;
import wyvc.builder.CompilerLogger.LoggedContainer;
import wyvc.utils.Generator.StandardGenerator;
import wyvc.utils.Generator.StandardPairGenerator;
import wyvc.utils.Generator;
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


	static class TreeComponentException extends CompilerError {
		private final String name;
		private final Tree<?,?> tree;

		public TreeComponentException(String name, Tree<?,?> tree) {
			this.name = name;
			this.tree = tree;
		}

		@Override
		public String info() {
			return "No such component : "+name+" in\n"+
					"  "+tree+"\n"+tree.toString("  ");
		}

		public static CompilerException exception(String name, Tree<?,?> tree) {
			return new CompilerException(new TreeComponentException(name, tree));
		}
	}



	/**
	 * Represents any node (leaf of inner node) of a tree structure that contains
	 * values only in leaves.
	 *
	 * <h2> Subclassing </h2>
	 *
	 * To provide highly usable common methods, recursive generic parameters are
	 * used.
	 *
	 * <pre>
	 * {@code
	 * interface MyTree extends Tree<MyTree,MyValue> {
	 *  	...
	 * }
	 *
	 * class MyPrimitive extends Primitive<MyTree, My Value> implements MyTree {
	 *  	...
	 * }
	 *
	 * class MyCompound extends Compound<MyTree, My Value> implements MyTree {
	 *  	...
	 * }
	 * }
	 * </pre>
	 *
	 * A <code>MyTree</code> object will looks like :
	 * <pre>
	 * {@code
	 * this : MyCompound
	 *  ├─ cmp_0 : MyCompound
	 *  │   ├─ cmp_0 : MyPrimitive
	 *  │   └─ cmp_1 : MyPrimitive
	 *  ├─ cmp_1 : MyCompound
	 *  │   └─ cmp_0 : MyPrimitive
	 *  └─ cmp_2 : MyPrimitive
	 * }
	 * </pre>
	 *
	 * @author Baptiste Pauget
	 *
	 * @param <T> The precise tree type, to enable easy sub-classing
	 * @param <V> The values' type contained in leaves
	 * @see Primitive
	 * @see Compound
	 */
	public interface Tree<T extends Tree<T,V>,V> {
		/*------- Compiler messages -------*/

		/**
		 * A CompilerError reporting an incoherence between to trees that should
		 * have the same internal structure.
		 *
		 * @author Baptiste Pauget
		 */
		public static class TreeStructureCompilerError extends CompilerError {
			private final Tree<?,?> expected;
			private final Tree<?,?> encountered;

			public TreeStructureCompilerError(Tree<?,?> expected, Tree<?,?> encountered) {
				this.expected = expected;
				this.encountered = encountered;
			}

			@Override
			public String info() {
				return "Tree structures incoherent\n"+
						" Expected :    *\n"+expected.toString("              ")+
						" Encountered : *\n"+encountered.toString("              ");
			}

			public static CompilerException exception(Tree<?,?> expected, Tree<?,?> encountered) {
				return new CompilerException(new TreeStructureCompilerError(expected, encountered));
			}
		}



		/*------- Interface Content -------*/

		/**
		 * Provides the value of a node.
		 *
		 * This method provided to avoid annoying type casting but should be
		 * only used after checking that the node is a leaf.
		 *
		 * @return The value contained in a leaf or <code>null</code> for a inner node
		 */
		public V getValue();

		/**
		 * Provides the children of a node.
		 *
		 * This method provided to avoid annoying type casting but should be
		 * only used after checking that the node is a inner node.
		 *
		 * @return A generator giving the subtrees of a node.
		 */
		public StandardPairGenerator<String,T> getComponents();

		/**
		 * Provides the number of children of a node.
		 *
		 * This method should not be use to determine whether a node is a leaf or not,
		 * since empty inner node can exist.
		 *
		 * @return The number of subtrees of the node.
		 */
		public int getNumberOfComponents();

		/**
		 * Compares the tree structure with the one of <code>other</code>
		 *
		 * @param other The tree used for comparison
		 * @return <code>true</code> if the structure matches, else <code>false</code>
		 */
		public boolean isStructuredAs(Tree<?,?> other);

		/**
		 * Provides the internal structure of a inner node.
		 *
		 * This method provided to avoid annoying type casting but should be
		 * only used after checking that the node is a inner node.
		 *
		 * @return The structure of a inner node or <code>null</code> for a leaf.
		 */
		public Structure<T,V> getStructure();

		public T getParent(); // TODO j'en veux pas.
		public void setParent(T parent); // TODO ça non plus !

		/**
		 * Checks that the <code>other</code> tree has the same structure than <code>this</code>
		 * @param other The tree used for comparison
		 * @throws CompilerException Thrown if the structures mismatch
		 */
		public default void checkIdenticalStructure(Tree<?,?> other) throws CompilerException {
			if (!isStructuredAs(other))
				throw TreeStructureCompilerError.exception(this, other);
		}

		/**
		 * Provides the values that are contained in the leaves of the tree.
		 *
		 * @return A generator giving the values of the leaves
		 */
		public default StandardGenerator<V> getValues() {
			if (this instanceof Primitive)
				return Generator.fromSingleton(getValue());
			return Generator.concat(getComponents().takeSecond().map(Tree::getValues));
		}

		/**
		 * Checks if a component <code>component</code> exists in the children of the node.
		 *
		 * @param component The named of the requested component
		 * @return
		 */
		public default boolean hasComponent(String component) {
			return getComponents().takeFirst().find(component) != null;
		}

		/**
		 * Provides the component named <code>component</code> of <code>this</code>.
		 *
		 * @param component
		 * @return The sub tree starting with the child named <code>component</code> node
		 * @throws CompilerException thrown is no component <code>component</code> exists
		 */
		public default T getComponent(String component) throws CompilerException {
			return getComponents().findOrThrow((Pair<String, T> c) ->c.first.equals(component), () -> TreeComponentException.exception(component, this)).second;
		}

		public default void printStructure(CompilerLogger logger, String start) { // TODO Salut !?
			logger.debug(toString(start));
		}

		/**
		 * Pretty-printer for the tree
		 *
		 * @param prefix A string to add at each line
		 * @return A representation of the tree's structure
		 */
		public default String toString(String prefix) {
			return this instanceof Primitive ? "" : getStructure().toString(prefix);
		}
	}



	public static class ReuseOfNodeCompilerDebug extends CompilerDebug {
		private final Tree<?,?> node;

		public ReuseOfNodeCompilerDebug(Tree<?,?> node) {
			this.node = node;
		}

		@Override
		public String info() {
			return "Setting parent for the node "+node+" that has already one";
		}
	}

	/**
	 *
	 * @author Baptiste Pauget
	 *
	 * @param <T> The precise tree type of the components
	 * @param <V> The values contains in the component trees
	 *
	 * @see Tree
	 * @see Compound
	 */
	public class Primitive<T extends Tree<T,V>,V> implements Tree<T,V> {
		protected V value;
		private T parent = null;

		public Primitive(V value) {
			this.value = value;
		}

		@Override
		public V getValue() {
			return value;
		}
		@Override
		public StandardPairGenerator<String,T> getComponents() {
			return Generator.emptyPairGenerator();
		}
		@Override
		public boolean isStructuredAs(Tree<?,?> other) {
			return other instanceof Primitive;
		}
		@Override
		public Structure<T, V> getStructure() {
			if (this.parent != null)
				logger.addMessage(new CompilerDebug() {
					@Override
					public String info() {
						return "Trying to get a structure from the primitive node "+Primitive.this+".";
					}
				});
			return null;
		}

		@Override
		public T getParent() {
			return parent;
		}

		@Override
		public void setParent(T parent) {
			if (this.parent != null)
				logger.addMessage(new ReuseOfNodeCompilerDebug(this));
			this.parent = parent;
		}

		@Override
		public int getNumberOfComponents() {
			return 0;
		}
	}



	/**
	 *
	 * @author Baptiste Pauget
	 *
	 * @param <T> The precise tree type of the components
	 * @param <V> The values contains in the component trees
	 *
	 * @see Primitive
	 * @see Compound
	 */
	public static interface Structure<T extends Tree<T,V>,V> {
		/*------- Compiler messages -------*/

		/**
		 * A CompilerError reporting an incoherence between to structures that should
		 * be the same.
		 *
		 * @author Baptiste Pauget
		 */
		public static class IncoherentStructureCompilerError extends CompilerError {
			private final Structure<?,?> expected;
			private final Structure<?,?> encountered;

			public IncoherentStructureCompilerError(Structure<?,?> expected, Structure<?,?> encountered) {
				this.expected = expected;
				this.encountered = encountered;
			}

			@Override
			public String info() {
				return "Structures incoherent\n"+
						" Expected :    "+expected+"\n"+expected.toString("              ")+
						" Encountered : "+encountered+"\n"+encountered.toString("              ");
			}

			public static CompilerException exception(Structure<?,?> expected, Structure<?,?> encountered) {
				return new CompilerException(new IncoherentStructureCompilerError(expected, encountered));
			}
		}


		/*------- Interface Content -------*/

		public int getNumberOfComponents();
		public StandardPairGenerator<String, T> getComponents();
		public String toString(String prefix);
		public boolean isStructureAs(Structure<?,?> other);
		public default void checkIdenticalStructure(Structure<?,?> other) throws CompilerException {
			if (! isStructureAs(other))
				IncoherentStructureCompilerError.exception(this, other);
		}
	}



	/**
	 *
	 *
	 * @author Baptiste Pauget
	 *
	 * @param <T> The precise tree type of the components
	 * @param <V> The values contains in the component trees
	 * @param <S> The structure type of the inner node
	 *
	 * @see Tree
	 * @see Compound
	 */
	public class Compound<T extends Tree<T,V>,V, S extends Structure<T,V>> implements Tree<T,V> {
		protected final S structure;
		private T parent = null;

		@SuppressWarnings("unchecked")
		public Compound(S structure) {
			this.structure = structure;
			structure.getComponents().forEach((String s, T t) -> t.setParent((T)this)); // TODO grrr
		}

		@Override
		public V getValue() {
			if (this.parent != null)
				logger.addMessage(new CompilerDebug() {
					@Override
					public String info() {
						return "Trying to get a value from the compound node "+Compound.this+".";
					}
				});
			return null;
		}
		@Override
		public StandardPairGenerator<String,T> getComponents() {
			return structure.getComponents();
		}
		@Override
		public boolean isStructuredAs(Tree<?,?> other) {
			return structure.isStructureAs(other.getStructure());
			/*if (getNumberOfComponents() != other.getNumberOfComponents())
				return false;
			List<Pair<String, R>> otherComponents = other.getComponents();
			for (int k = 0; k < components.size(); ++k)
				if (!components.get(k).first.equals(otherComponents.get(k).first) ||
					!components.get(k).second.isStructuredAs(otherComponents.get(k).second))
					return false;
			return true;*/
		}
		@Override
		public S getStructure() {
			return structure;
		}

		@Override
		public T getParent() {
			return parent;
		}

		@Override
		public void setParent(T parent) {
			if (this.parent != null)
				logger.addMessage(new ReuseOfNodeCompilerDebug(this));
			this.parent = parent;
		}

		@Override
		public int getNumberOfComponents() {
			return structure.getNumberOfComponents();
		}
	}



	public interface EffectiveRecordStructure<T extends Tree<T,V>,V> extends Structure<T,V> {
		/*------- Compiler messages -------*/

		static class UnexistingFieldCompilerError extends CompilerError {
			private final String name;
			private final EffectiveRecordStructure<?,?> record;

			public UnexistingFieldCompilerError(String name, EffectiveRecordStructure<?,?> record) {
				this.name = name;
				this.record = record;
			}

			@Override
			public String info() {
				return "No such field : "+name+" in\n"+
						"  "+record+"\n"+record.toString("  ");
			}

			public static CompilerException exception(String name, EffectiveRecordStructure<?,?> tree) {
				return new CompilerException(new UnexistingFieldCompilerError(name, tree));
			}
		}


		/*------- Interface Content -------*/

		public int getNumberOfFields();
		public StandardPairGenerator<String, T> getFields();
		default public StandardGenerator<String> getFieldNames() {
			return getFields().takeFirst();
		}
		default public T getField(String f) throws CompilerException {
			return getFields().findOrThrow((Pair<String, T> p) -> p.first.equals(f), () -> UnexistingFieldCompilerError.exception(f, this)).second;
		}
	}

	public static interface EffectiveUnionStructure<T extends Tree<T,V>,V> extends Structure<T,V> {
		/*------- Compiler messages -------*/


		/*------- Interface Content -------*/

		public final static String FLG_PREFIX = "is_t";
		public final static String VAL_PREFIX = "vl_t";
		public int getNumberOfOptions();
		public StandardPairGenerator<T, T> getOptions();
	}


	public LexicalElementTree(CompilerLogger logger) {
		super(logger);
	}
}
