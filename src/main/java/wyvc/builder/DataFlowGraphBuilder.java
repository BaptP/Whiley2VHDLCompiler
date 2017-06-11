package wyvc.builder;

import java.io.ObjectInputStream.GetField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.CompletionException;

import javax.lang.model.type.UnionType;

import wyal.lang.WyalFile.Type.Record;
import wyil.lang.Bytecode;
import wyil.lang.Constant;
import wyil.lang.SyntaxTree;
import wyil.lang.Bytecode.AliasDeclaration;
import wyil.lang.Bytecode.Const;
import wyil.lang.Bytecode.FieldLoad;
import wyil.lang.Bytecode.If;
import wyil.lang.Bytecode.Invoke;
import wyil.lang.Bytecode.OperatorKind;
import wyil.lang.Bytecode.VariableAccess;
import wyil.lang.Bytecode.VariableDeclaration;
import wyil.lang.SyntaxTree.Location;
import wyil.lang.WyilFile;
import wyil.lang.WyilFile.FunctionOrMethod;
import wyvc.builder.DataFlowGraph;
import wyvc.builder.CompilerLogger.CompilerDebug;
import wyvc.builder.CompilerLogger.CompilerError;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.builder.CompilerLogger.CompilerNotice;
import wyvc.builder.CompilerLogger.LoggedBuilder;
import wyvc.builder.CompilerLogger.UnsupportedCompilerError;
import wyvc.builder.DataFlowGraph.DataNode;
import wyvc.builder.DataFlowGraph.FuncCallNode;
import wyvc.builder.DataFlowGraph.HalfArrow;
import wyvc.builder.DataFlowGraph.UndefConstNode;
import wyvc.builder.LexicalElementTree.Leaf;
import wyvc.builder.LexicalElementTree.Tree;
import wyvc.builder.TypeCompiler.AccessibleTypeTree;
import wyvc.builder.TypeCompiler.BooleanTypeLeaf;
import wyvc.builder.TypeCompiler.TypeLeaf;
import wyvc.builder.TypeCompiler.TypeOption;
import wyvc.builder.TypeCompiler.TypeRecord;
import wyvc.builder.TypeCompiler.TypeRecordUnion;
import wyvc.builder.TypeCompiler.TypeSimpleRecord;
import wyvc.builder.TypeCompiler.TypeSimpleUnion;
import wyvc.builder.TypeCompiler.TypeTree;
import wyvc.builder.TypeCompiler.TypeUnion;
import wyvc.builder.LexicalElementTree;
import wyvc.lang.Type;
import wyvc.utils.FunctionalInterfaces.BiFunction;
import wyvc.utils.FunctionalInterfaces.BiFunction_;
import wyvc.utils.FunctionalInterfaces.Function;
import wyvc.utils.FunctionalInterfaces.Supplier;
import wyvc.utils.Generators;
import wyvc.utils.Pair;
import wyvc.utils.Utils;
import wyvc.utils.Generators.Generator_;
import wyvc.utils.Generators.Generator;
import wyvc.utils.Generators.PairGenerator;
import wyvc.utils.Generators.PairGenerator_;

public final class DataFlowGraphBuilder extends LexicalElementTree {
	private final TypeCompiler typeCompiler;


	public DataFlowGraphBuilder(CompilerLogger logger, TypeCompiler typeCompiler) {
		super(logger);
		this.typeCompiler = typeCompiler;
	}



/*

	public static interface NodeTree extends Tree_<NodeTree, HalfArrow<?>> {
		public TypeTree getType();
	}

	public class PrimitiveNode extends Primitive<NodeTree, HalfArrow<?>> implements NodeTree {
		private final TypeTree type;

		public <T extends DataNode> PrimitiveNode(T decl) {
			super(new HalfArrow<>(decl));
			type = typeCompiler.new PrimitiveType(decl.type);
		}

		public <T extends DataNode> PrimitiveNode(T decl, String ident) {
			super(new HalfArrow<>(decl, ident));
			type = typeCompiler.new PrimitiveType(decl.type);
		}

		public <T extends DataNode> PrimitiveNode(HalfArrow<T> decl, String ident) {
			this(decl.node, ident);
		}

		@Override
		public TypeTree getType() {
			return type;
		}
	}

	public class CompoundNode<S extends Structure<NodeTree, HalfArrow<?>>> extends Compound_<NodeTree, HalfArrow<?>, S> implements NodeTree {
		private final TypeTree type;
		private NodeTree parent = null;

		public CompoundNode(S structure, TypeTree type) {
			super(structure);
			this.type = type;

		}

		@Override
		public TypeTree getType() {
			return type;
		}

		@Override
		public NodeTree getParent() {
			return parent;
		}

		@Override
		public void setParent(NodeTree parent) {
			this.parent = parent;
		}
	}

*/

	public static class NestedReturnCompilerNotice extends CompilerNotice {
		@Override
		public String info() {
			return "Nested returns can result in a large hardware configuration";
		}
	}

	public static class WyilUnsupportedCompilerError extends UnsupportedCompilerError {
		private final Location<?> location;
		public WyilUnsupportedCompilerError(Location<?> location) {
			this.location = location;
		}

		@Override
		public String info() {
			return location + " is currently unsupported";
		}

		public static CompilerException exception(Location<?> location) {
			return new CompilerException(new WyilUnsupportedCompilerError(location));
		}
	}

/*
	public static class CompoundTypeCompilerError extends CompilerError {
		private final TypeTree type;

		public CompoundTypeCompilerError(TypeTree type) {
			this.type = type;
		}

		@Override
		public String info() {
			return "This compound type can't be converted to VHDL type :\n"+type.toString("  ");
		}

		public static CompilerException exception(TypeTree type) {
			return new CompilerException(new CompoundTypeCompilerError(type));
		}

	}


	public static class UnrelatedTypeCompilerError extends CompilerError {
		private final NodeTree value;
		private final TypeTree type;

		public UnrelatedTypeCompilerError(TypeTree type, NodeTree value) {
			this.value = value;
			this.type = type;
		}

		@Override
		public String info() {
			return "The value "+value+" of type "+value.getType().toString()+"\ncan't be interpreted as part of the UnionType "+type.toString();
		}

		public static CompilerException exception(TypeTree type, NodeTree value) {
			return new CompilerException(new UnrelatedTypeCompilerError(type, value));
		}

	}

	public static class UncompatibleStructuresCompilerError extends CompilerError {
		private final Structure<?,?> structure1;
		private final Structure<?,?> structure2;

		public UncompatibleStructuresCompilerError(Structure<?,?> structure1, Structure<?,?> structure2) {
			this.structure1 = structure1;
			this.structure2 = structure2;
		}

		@Override
		public String info() {
			return "The structure "+structure1 + " is uncompatible with the structure "+structure2;
		}

		public static CompilerException exception(Structure<?,?> structure1, Structure<?,?> structure2) {
			return new CompilerException(new UncompatibleStructuresCompilerError(structure1, structure2));
		}
	}

*/


	public static class UnsupportedTreeNodeCompilerError extends CompilerError {
		private final Tree node;

		public UnsupportedTreeNodeCompilerError(Tree node) {
			this.node = node;
		}

		@Override
		public String info() {
			return "The conversion of the node "+node+" is unsupported";
		}

		public static CompilerException exception(Tree node) {
			return new CompilerException(new UnsupportedTreeNodeCompilerError(node));
		}
	}

	public static class UnrelatedTypeCompilerError extends CompilerError {
		private final VertexTree<?> value;
		private final TypeTree type;

		public UnrelatedTypeCompilerError(TypeTree type, VertexTree<?> value) {
			this.value = value;
			this.type = type;
		}

		@Override
		public String info() {
			return "The value <"+value+"> of type "+value.getType().toString()+"\ncan't be interpreted as part of the type "+type.toString();
		}

		public static CompilerException exception(TypeTree type, VertexTree<?> value) {
			return new CompilerException(new UnrelatedTypeCompilerError(type, value));
		}

	}

	public static class UnsupportedAliasCompilerError extends CompilerError {
		private final VertexTree<?> value;
		private final TypeTree type;

		public UnsupportedAliasCompilerError(TypeTree type, VertexTree<?> value) {
			this.value = value;
			this.type = type;
		}

		@Override
		public String info() {
			return "Unsupported alias of value <"+value+"> of type "+value.getType().toString("")+"\nto the type "+type.toString("");
		}

		public static CompilerException exception(TypeTree type, VertexTree<?> value) {
			return new CompilerException(new UnsupportedAliasCompilerError(type, value));
		}

	}



	private static interface VertexTree<T extends TypeTree> extends Tree {

		public T getType();
	}

	private static interface AccessibleVertexTree<T extends AccessibleTypeTree> extends VertexTree<T> {

	}

	private class VertexLeaf<T extends TypeLeaf> extends Leaf<HalfArrow<?>> implements AccessibleVertexTree<T> {
		private final T type;

		public  <U extends DataNode> VertexLeaf(U value, T type) {
			super(new HalfArrow<>(value));
			this.type = type;
		}
		public  <U extends DataNode> VertexLeaf(U value, T type, String ident) {
			super(new HalfArrow<>(value, ident));
			this.type = type;
		}

//		public <U extends DataNode> VertexLeaf(U decl, String ident) {
//			super(new HalfArrow<>(decl, ident));
//			type = typeCompiler.new TypeLeaf(decl.type);
//		}

		@Override
		public T getType() {
			return type;
		}
	}

	private class BooleanVertexLeaf extends VertexLeaf<BooleanTypeLeaf> {
		public <U extends DataNode> BooleanVertexLeaf(U value) {
			super(value, typeCompiler.new BooleanTypeLeaf());
		}
		public <U extends DataNode> BooleanVertexLeaf(U value, String ident) {
			super(value, typeCompiler.new BooleanTypeLeaf(), ident);
		}
	}

	private static interface VertexNode<A extends VertexTree<?>, T extends TypeTree> extends VertexTree<T>, Node<A> {

	}

	private class VertexRecord<
		A extends TypeTree,
		B extends TypeRecord<? extends A>,
		T extends VertexTree<? extends A>
	> extends NamedNode<T> implements VertexNode<T,B> {

		private final B type;

		public <R extends T> VertexRecord(Generator<Pair<String, R>> fields, B type) {
			super(fields);
			this.type = type;
		}

		public <R extends T, E extends Exception> VertexRecord(Generator_<Pair<String, R>, E> fields, B type) throws E {
			super(fields);
			this.type = type;
		}

		@Override
		public B getType() {
			return type;
		}
	}

	private class VertexSimpleRecord extends VertexRecord<TypeTree, TypeSimpleRecord, AccessibleVertexTree<?>> implements AccessibleVertexTree<TypeSimpleRecord> {
		public <R extends AccessibleVertexTree<?>> VertexSimpleRecord(Generator<Pair<String, R>> fields, TypeSimpleRecord type) {
			super(fields, type);
		}

		public <R extends AccessibleVertexTree<?>, E extends Exception> VertexSimpleRecord(Generator_<Pair<String, R>, E> fields, TypeSimpleRecord type) throws E {
			super(fields, type);
		}
	}


	private class VertexOption<
		T extends AccessibleTypeTree,
		U extends VertexTree<? extends T>> extends BinaryNode<VertexTree<?>, BooleanVertexLeaf, U
	> implements VertexNode<VertexTree<?>, TypeOption<T>> {
		private final TypeOption<T> type;

		public VertexOption(BooleanVertexLeaf has, U val, TypeOption<T> type) {
			super(has,val);
			this.type = type;
		}

		@Override
		public TypeOption<T> getType() {
			return type;
		}

		@Override
		public String getFirstLabel() {
			return "has";
		}
		@Override
		public String getSecondLabel() {
			return "val";
		}
	}

	private class VertexSimpleUnion extends UnnamedNode<VertexOption<? extends TypeLeaf, ? extends VertexLeaf<? extends TypeLeaf>>>
	implements VertexNode<VertexOption<? extends TypeLeaf,
	                                   ? extends VertexLeaf<? extends TypeLeaf>>,TypeSimpleUnion>, AccessibleVertexTree<TypeSimpleUnion> {
		private final TypeSimpleUnion type;


		public VertexSimpleUnion(Generator<VertexOption<
		                                   ? extends TypeLeaf,
		                                   ? extends VertexLeaf<? extends TypeLeaf>>> options, TypeSimpleUnion type) {
			super(options);
			this.type = type;
		}

		public <E extends Exception> VertexSimpleUnion(Generator_<VertexOption<
				? extends TypeLeaf,
				? extends VertexLeaf<? extends TypeLeaf>>, E> options, TypeSimpleUnion type) throws E {
			super(options);
			this.type = type;
		}

		@Override
		public TypeSimpleUnion getType() {
			return type;
		}
	}

	private class VertexRecordUnion extends BinaryNode<VertexTree<?>,VertexSimpleRecord,
	VertexRecord<TypeOption<?>, TypeRecord<TypeOption<AccessibleTypeTree>>, VertexOption<AccessibleTypeTree,AccessibleVertexTree<?>>>> implements VertexNode<VertexTree<?>, TypeRecordUnion>, AccessibleVertexTree<TypeRecordUnion> {
		private final TypeRecordUnion type;

//		public VertexRecordUnion(VertexSimpleRecord shared, VertexRecord<TypeOption, VertexOption> specific, TypeRecordUnion type) {
//			super(shared, specific);
//			this.type = type;
//		}

		public <A extends AccessibleVertexTree<?>> VertexRecordUnion(
				Generator<Pair<String,A>> shared,
				Generator<Pair<String, VertexOption<AccessibleTypeTree,AccessibleVertexTree<?>>>> specific, TypeRecordUnion type) {
			super(new VertexSimpleRecord(shared, type.getFirstOperand()), new VertexRecord<>(specific, type.getSecondOperand()));
			this.type = type;
		}
		public <A extends AccessibleVertexTree<?>, E extends Exception> VertexRecordUnion(
				Generator_<Pair<String, A>,E> shared,
				Generator_<Pair<String, VertexOption<AccessibleTypeTree,AccessibleVertexTree<?>>>,E> specific, TypeRecordUnion type) throws E {
			super(new VertexSimpleRecord(shared, type.getFirstOperand()), new VertexRecord<>(specific, type.getSecondOperand()));
			this.type = type;
		}

		@Override
		public TypeRecordUnion getType() {
			return type;
		}



		public PairGenerator<String,AccessibleVertexTree<?>> getSharedFields() {
			return getFirstOperand().getFields();
		}
		public PairGenerator<String,VertexOption<AccessibleTypeTree, AccessibleVertexTree<?>>> getSpecificFields() {
			return getSecondOperand().getFields();
		}

		public boolean hasSharedField(String field) {
			return getFirstOperand().hasField(field);
		}
		public boolean hasSpecificField(String field) {
			return getSecondOperand().hasField(field);
		}

		public AccessibleVertexTree<?> getSharedField(String field) throws CompilerException {
			return getFirstOperand().getField(field);
		}
		public VertexOption<AccessibleTypeTree, AccessibleVertexTree<?>> getSpecificField(String field) throws CompilerException {
			return getSecondOperand().getField(field);
		}
	}


	private class VertexUnion extends BinaryNode<VertexTree<?>, VertexSimpleUnion, VertexRecordUnion> implements VertexNode<VertexTree<?>, TypeUnion>, AccessibleVertexTree<TypeUnion> {
		private final TypeUnion type;

		public VertexUnion(VertexSimpleUnion simpleOptions, VertexRecordUnion recordOptions, TypeUnion type) {
			super(simpleOptions, recordOptions);
			this.type = type;
		}

		@Override
		public TypeUnion getType() {
			return type;
		}

		@Override
		public String getFirstLabel() {
			return "sha";
		}
		@Override
		public String getSecondLabel() {
			return "spe";
		}

	}


/*


	private static interface NodeVertex<T extends TypeTree> extends Node<VertexTree<?>>,VertexTree<T> {

	}

	private class RecordVertex extends RecordNode<VertexTree<?>> implements NodeVertex<TypeRecord> {
		private final TypeRecord type;

		public <E extends Exception> RecordVertex(
				Generator_<Pair<String, VertexTree<?>>, E> fields,
				TypeRecord type) throws E {
			super(fields);
			this.type = type;
		}

		public RecordVertex(
				Generator<Pair<String, VertexTree<?>>> fields,
				TypeRecord type) {
			super(fields);
			this.type = type;
		}

		@Override
		public TypeRecord getType() {
			return type;
		}
	}

	private abstract class UnionVertex<T extends VertexTree<?>> extends UnionNode<VertexTree<?>, LeafVertex, T> {
		public UnionVertex(Generator<Pair<LeafVertex, T>> options) {
			super(options);
		}

		public <E extends Exception> UnionVertex(Generator_<Pair<LeafVertex, T>, E> options) throws E {
			super(options);
		}
	}

	private class SimpleUnionVertex extends UnionVertex<VertexTree<?>> implements NodeVertex<SimpleTypeUnion> {
		private final SimpleTypeUnion type;

		public SimpleUnionVertex(
				Generator<Pair<LeafVertex, VertexTree<?>>> options,
				SimpleTypeUnion type) {
			super(options);
			this.type = type;
		}

		public <E extends Exception> SimpleUnionVertex(
				Generator_<Pair<LeafVertex, VertexTree<?>>, E> options,
				SimpleTypeUnion type) throws E {
			super(options);
			this.type = type;
		}


		@Override
		public SimpleTypeUnion getType() {
			return type;
		}

		@Override
		protected PairGenerator<VertexTree<?>, VertexTree<?>> getTypedOptions() {
			return getOptions().map((LeafVertex a) -> a, (VertexTree<?> b) -> b);
		}
	}

	private class RecordUnionVertex extends UnionVertex<RecordVertex> implements NodeVertex<TypeRecordUnion>{
		private final TypeRecordUnion type;

		public RecordUnionVertex(
				Generator<Pair<LeafVertex, RecordVertex>> options,
				TypeRecordUnion type) {
			super(options);
			this.type = type;
		}
		public <E extends Exception> RecordUnionVertex(
				Generator_<Pair<LeafVertex, RecordVertex>, E> options,
				TypeRecordUnion type) throws E {
			super(options);
			this.type = type;
		}

		@Override
		public TypeRecordUnion getType() {
			return type;
		}

		@Override
		protected PairGenerator<VertexTree<?>, VertexTree<?>> getTypedOptions() {
			return getOptions().map((LeafVertex a) -> a, (RecordVertex b) -> b);
		}
	}
*/

	private final class Builder extends LoggedBuilder {

		/*------- Classes/Interfaces -------*/

		private class PartialReturn {
			private final HalfArrow<?> cond;
			private final Location<If> ifs;
			private PartialReturn tPart;
			private PartialReturn fPart;
			private final List<AccessibleVertexTree<?>> ret;


			public PartialReturn(Location<If> ifs, HalfArrow<?> cond, PartialReturn tPart, PartialReturn fPart) {
				this.cond = cond;
				this.ifs = ifs;
				this.tPart = tPart;
				this.fPart = fPart;
				this.ret = null;
			}
			public PartialReturn(List<AccessibleVertexTree<?>> ret) {
				assert ret != null;
				this.ifs = null;
				this.cond = null;
				this.tPart = null;
				this.fPart = null;
				this.ret = ret;
			}

// TODO
			public Generator<VertexTree<?>> getReturn() throws CompilerException {
				assert !isPartial();
//				logger.debug("Return ? "+this+" "+ret+" "+tPart+" "+fPart);
				return null;//ret == null ? tPart.getReturn().gather(fPart.getReturn()).map(
					//(Pair<NodeTree<?>, NodeTree<?>> p) -> buildEndIf(ifs, cond, p.first, p.second)) : Generator.fromCollection(ret);
			}

			public void completeReturn(List<AccessibleVertexTree<?>> rem) {
				completeReturn(new PartialReturn(rem));
			}

			public PartialReturn completeReturn(PartialReturn rem) {
				if (ret != null) return this;
				if (tPart == null) tPart = rem;
				else tPart.completeReturn(rem);
				if (fPart == null) fPart = rem;
				else fPart.completeReturn(rem);
				return this;
			}

			public boolean isPartial() {
				return ret == null && ((fPart == null || fPart.isPartial()) || (tPart == null || tPart.isPartial()));
			}

			public void print(String pref) {
				debug(pref+(ret != null ? "Val "+ret : "Mux "+ifs));
				if (ret==null) {
					debug(pref+" t─ "+tPart);
					if (tPart != null)
						tPart.print(pref+" │  ");
					debug(pref+" f─ "+fPart);
					if (fPart != null)
						fPart.print(pref+" │  ");
				}
			}
		}


		private Map<Integer, AccessibleVertexTree<?>> vars = new HashMap<>();
		private PartialReturn partialReturn = null;
		private final List<AccessibleTypeTree> returnTypes;
		private final DataFlowGraph graph;
		private Map<wyil.lang.Type, wyvc.lang.Type> compiledTypes = new HashMap<>();

		/*------- Constructor -------*/

		@SuppressWarnings("unchecked")
		public Builder(WyilFile.FunctionOrMethod func) throws CompilerException {
			super(DataFlowGraphBuilder.this.logger);
			graph = new DataFlowGraph();
			Generators.fromCollection(func.type().params()).enumerate().forEach_((Integer k, wyil.lang.Type t) ->
				vars.put(k, buildParameter(
					((Location<Bytecode.VariableDeclaration>)func.getTree().getLocation(k)).getBytecode().getName(),
					buildType(t))));
			returnTypes = Generators.fromCollection(func.type().returns()).map_(this::buildType).toList();
			//build(func.getBody());
/**/			partialReturn.print("RET ");
			partialReturn.getReturn().enumerate().mapFirst(Object::toString).mapFirst("ret_"::concat).forEach_(this::buildReturnValue);
		}


		private void buildReturnValue(String ident, VertexTree<?> ret) {
			if (ret instanceof VertexLeaf)
				graph.new OutputNode(ident, ((VertexLeaf<?>)ret).getValue());
			else if (ret instanceof VertexNode)
				((VertexNode<?,?>)ret).getComponents().forEach((String s, VertexTree<?> t) -> buildReturnValue(ident+"_"+s, t));
		}


		/*------- Classe content -------*/

//		private Type isNodeType(TypeTree type) throws CompilerException {
//			if (type instanceof CompoundType)
//				CompoundTypeCompilerError.exception(type);
//			return type.getValue();
//		}
//
//		private Type getNodeType(wyil.lang.Type type) throws CompilerException {
//			return Utils.checkedAddIfAbsent(compiledTypes, type, () -> isNodeType(buildType(type)));
//
//		}


		public DataFlowGraph getGraph() {
			return graph;
		}


		public AccessibleTypeTree buildType(wyil.lang.Type type) throws CompilerException {
			return typeCompiler.compileType(type);
		}





		private VertexLeaf<TypeLeaf> buildUndefinedValue(TypeLeaf type) {
			return new VertexLeaf<>(graph.new UndefConstNode(type.getValue()), type);
		}
		private BooleanVertexLeaf buildUndefinedValue(BooleanTypeLeaf type) {
			return new BooleanVertexLeaf(graph.new UndefConstNode(type.getValue()));
		}
		private VertexSimpleRecord buildUndefinedValue(TypeSimpleRecord type) throws CompilerException {
			return new VertexSimpleRecord(type.getFields().mapSecond_(this::buildUndefinedValue), type);
		}
		private VertexSimpleUnion buildUndefinedValue(TypeSimpleUnion type) throws CompilerException {
			return new VertexSimpleUnion(type.getOptions().map_((TypeOption<TypeLeaf> t) -> new VertexOption<TypeLeaf, VertexLeaf<TypeLeaf>>(
					buildBoolean(false),
					buildUndefinedValue(t.getSecondOperand()), t)), type);
		}
		private VertexRecordUnion buildUndefinedValue(TypeRecordUnion type) throws CompilerException {
			return new VertexRecordUnion(
				type.getSharedFields().mapSecond_(this::buildUndefinedValue),
				type.getSpecificFields().mapSecond_((TypeOption<AccessibleTypeTree> t) -> new VertexOption<AccessibleTypeTree, AccessibleVertexTree<?>>(
						buildBoolean(false),
						buildUndefinedValue(t.getSecondOperand()), t)), type);
		}
		private VertexUnion buildUndefinedValue(TypeUnion type) throws CompilerException {
			return new VertexUnion(
				buildUndefinedValue(type.getFirstOperand()),
				buildUndefinedValue(type.getSecondOperand()), type);
		}
		private AccessibleVertexTree<?> buildUndefinedValue(AccessibleTypeTree type) throws CompilerException {
			if (type instanceof BooleanTypeLeaf)
				return buildUndefinedValue((BooleanTypeLeaf)type);
			if (type instanceof TypeLeaf)
				return buildUndefinedValue((TypeLeaf)type);
			if (type instanceof TypeSimpleRecord)
				return buildUndefinedValue((TypeSimpleRecord)type);
			if (type instanceof TypeUnion)
				return buildUndefinedValue((TypeUnion)type);
			if (type instanceof TypeRecordUnion)
				return buildUndefinedValue((TypeRecordUnion)type);
			if (type instanceof TypeSimpleUnion)
				return buildUndefinedValue((TypeSimpleUnion)type);
			throw UnsupportedTreeNodeCompilerError.exception(type);
		}


		private BooleanVertexLeaf buildBoolean(Boolean value) {
			return new BooleanVertexLeaf(graph.new ExternConstNode(Type.Boolean, value.toString()));
		}










		private VertexUnion buildTypedValue(AccessibleVertexTree<?> node, TypeUnion type) throws CompilerException {
			if (node instanceof VertexUnion)
				return new VertexUnion(
					buildTypedValue(((VertexUnion) node).getFirstOperand(), type.getFirstOperand()),
					buildTypedValue(((VertexUnion) node).getSecondOperand(), type.getSecondOperand()),
					type);
			if (node instanceof VertexLeaf || node instanceof VertexSimpleUnion)
				return new VertexUnion(
					buildUndefinedValue(type.getFirstOperand()),
					buildTypedValue(node, type.getSecondOperand()),
					type);
			if (node instanceof VertexSimpleRecord || node instanceof VertexRecordUnion)
				return new VertexUnion(
					buildTypedValue(node, type.getFirstOperand()),
					buildUndefinedValue(type.getSecondOperand()),
					type);
			throw UnrelatedTypeCompilerError.exception(type, node);
		}
		private VertexRecordUnion buildTypedValue(AccessibleVertexTree<?> node, TypeRecordUnion type) throws CompilerException {
			Map<String, AccessibleVertexTree<?>> shared = new HashMap<>();
			Map<String, VertexOption<AccessibleTypeTree, AccessibleVertexTree<?>>> specific = new HashMap<>();
			if (node instanceof VertexRecordUnion) {
				((VertexRecordUnion) node).getSharedFields().forEach(shared::put);
				((VertexRecordUnion) node).getSpecificFields().forEach(specific::put);
			}
			else if (node instanceof VertexSimpleRecord)
				((VertexSimpleRecord) node).getFields().map(shared::put);
			else throw UnrelatedTypeCompilerError.exception(type, node);
			return new VertexRecordUnion(
				type.getSharedFields().map((String s, AccessibleTypeTree t) -> new Pair<>(s,buildTypedValue(shared.get(s), t))),
				type.getSpecificFields().map((String s, TypeOption<AccessibleTypeTree> o) -> new Pair<>(s, shared.containsKey(s)
						? new VertexOption<AccessibleTypeTree, AccessibleVertexTree<?>>(buildBoolean(true), buildTypedValue(shared.get(s), o.getSecondOperand()), new TypeOption<>(shared.get(s).getType()))
						: specific.containsKey(s) ? new VertexOption<AccessibleTypeTree, AccessibleVertexTree<?>>(specific.get(s).getFirstOperand(), buildTypedValue(specific.get(s).getSecondOperand(), o.getSecondOperand()), specific.get(s).getType())
						                          : new VertexOption<AccessibleTypeTree, AccessibleVertexTree<?>>(buildBoolean(false), buildUndefinedValue(o.getSecondOperand()), o))),
				type);
		}
		private VertexSimpleUnion buildTypedValue(AccessibleVertexTree<?> node, TypeSimpleUnion type) throws CompilerException {
			if (!(node instanceof VertexSimpleUnion) || !(node instanceof VertexLeaf))
				throw UnrelatedTypeCompilerError.exception(type, node);

			List<VertexOption<? extends TypeLeaf, ? extends VertexLeaf<? extends TypeLeaf>>> cpn = new ArrayList<>();
			if (node instanceof VertexSimpleUnion)
				cpn.addAll(((VertexSimpleUnion)node).getOptions().toList());
			else if (node instanceof VertexLeaf)
				cpn.add(new VertexOption<>(buildBoolean(true), (VertexLeaf<?>) node, new TypeOption<>(((VertexLeaf<?>)node).getType())));
			else throw UnrelatedTypeCompilerError.exception(type, node);
			return new VertexSimpleUnion(buildUndefinedValue(type).getOptions().map(
				(VertexOption<? extends TypeLeaf, ? extends VertexLeaf<? extends TypeLeaf>> o) -> {
					for (VertexOption<? extends TypeLeaf, ? extends VertexLeaf<? extends TypeLeaf>> c : cpn)
						if (o.getType().equals(c.getType()))
							return c;
					return o;
				}), type);
		}
		private VertexSimpleRecord buildTypedValue(AccessibleVertexTree<?> node, TypeSimpleRecord type) throws CompilerException {
			if (node instanceof VertexSimpleRecord && ((VertexSimpleRecord) node).hasSameFields(type))
				return new VertexSimpleRecord(Generators.toPairGenerator(((VertexSimpleRecord) node).getFields().gather(type.getFields().takeSecond()).map(
					(Pair<String, AccessibleVertexTree<?>> c, AccessibleTypeTree t) -> new Pair<>(c.first ,buildTypedValue(c.second, t)))), type);
			throw UnrelatedTypeCompilerError.exception(type, node);
		}
		private AccessibleVertexTree<?> buildTypedValue(AccessibleVertexTree<?> node, TypeTree type) throws CompilerException {
//			debug("Val "+(node == null ? "NULL" : node.getType()));
			if (node.getType().equals(type))
				return node;
			if (type instanceof TypeSimpleRecord)
				return buildTypedValue(node, (TypeSimpleRecord)type);
			if (type instanceof TypeUnion)
				return buildTypedValue(node, (TypeUnion)type);
			if (type instanceof TypeSimpleUnion)
				return buildTypedValue(node, (TypeSimpleUnion)type);
			if (type instanceof TypeRecordUnion)
				return buildTypedValue(node, (TypeRecordUnion)type);
			throw UnrelatedTypeCompilerError.exception(type, node);
		}



		private VertexLeaf<?> buildParameter(String ident, TypeLeaf type) {
			return new VertexLeaf<>(graph.new InputNode(ident, type.getValue()), type);
		}
		private BooleanVertexLeaf buildParameter(String ident, BooleanTypeLeaf type) {
			return new BooleanVertexLeaf(graph.new InputNode(ident, type.getValue()));
		}
		private VertexSimpleRecord buildParameter(String ident, TypeSimpleRecord type) throws CompilerException {
			return new VertexSimpleRecord(type.getFieldNames().gather_(type.getFields().mapFirst((ident+"_")::concat).map_(this::buildParameter)), type);
		}
		private VertexSimpleUnion buildParameter(String ident, TypeSimpleUnion type) throws CompilerException {
			return new VertexSimpleUnion(type.getOptions().enumerate().mapFirst(type::getLabel).mapFirst((ident+"_")::concat).map(
				(String s, TypeOption<TypeLeaf> t) -> new VertexOption<TypeLeaf, VertexLeaf<?>>(
					buildParameter(s+"_"+t.getFirstLabel(), t.getFirstOperand()),
					buildParameter(s+"_"+t.getSecondLabel(), t.getSecondOperand()), t)), type);
		}
		private VertexRecordUnion buildParameter(String ident, TypeRecordUnion type) throws CompilerException {
			return new VertexRecordUnion(
				type.getSharedFields().biMap_((Pair<String,AccessibleTypeTree> p) -> p.first,  (Pair<String,AccessibleTypeTree> p) -> buildParameter(ident+"_"+p.first, p.second)),
				type.getSpecificFields().mapSecond_((TypeOption<AccessibleTypeTree> t) -> new VertexOption<AccessibleTypeTree, AccessibleVertexTree<?>>(
						buildParameter(ident+"_"+t.getFirstLabel(), t.getFirstOperand()),
						buildParameter(ident+"_"+t.getSecondLabel(), t.getSecondOperand()), t)), type);
		}
		private VertexUnion buildParameter(String ident, TypeUnion type) throws CompilerException {
			return new VertexUnion(
				buildParameter(ident+"_"+type.getFirstLabel(), type.getFirstOperand()),
				buildParameter(ident+"_"+type.getSecondLabel(), type.getSecondOperand()), type);
		}
		private AccessibleVertexTree<?> buildParameter(String ident, AccessibleTypeTree type) throws CompilerException {
			if (type instanceof BooleanTypeLeaf)
				return buildParameter(ident, (BooleanTypeLeaf)type);
			if (type instanceof TypeLeaf)
				return buildParameter(ident, (TypeLeaf)type);
			if (type instanceof TypeSimpleRecord)
				return buildParameter(ident, (TypeSimpleRecord)type);
			if (type instanceof TypeUnion)
				return buildParameter(ident, (TypeUnion)type);
			if (type instanceof TypeRecordUnion)
				return buildParameter(ident, (TypeRecordUnion)type);
			if (type instanceof TypeSimpleUnion)
				return buildParameter(ident, (TypeSimpleUnion)type);
			throw UnsupportedTreeNodeCompilerError.exception(type);
		}



		@SuppressWarnings("unchecked")
		public void build(Location<?> location) throws CompilerException {
/**/			openLevel("Build");
/**/			Utils.printLocation(logger, location, level);
			Bytecode bytecode = location.getBytecode();
			if (bytecode instanceof Bytecode.Block)
				buildBlock((Location<Bytecode.Block>) location);
			else if (bytecode instanceof Bytecode.VariableDeclaration)
				buildDecl((Location<Bytecode.VariableDeclaration>) location);
			else if (bytecode instanceof Bytecode.Assign)
				buildAssign((Location<Bytecode.Assign>) location);
			else if (bytecode instanceof Bytecode.Return)
				buildReturn((Location<Bytecode.Return>) location);
			else if (bytecode instanceof Bytecode.If)
				buildIf((Location<Bytecode.If>) location);
			else if (bytecode instanceof Bytecode.Skip)
				graph.addSeparation();
			else
				WyilUnsupportedCompilerError.exception(location);
/**/			closeLevel();
		}

		private void buildBlock(Location<Bytecode.Block> block) throws CompilerException {
			for (Location<?> l : block.getOperands())
				build(l);
		}




		@SuppressWarnings("unchecked")
		private List<AccessibleVertexTree<?>> buildTuple(Location<?>[] elem) throws CompilerException {
			ArrayList<AccessibleVertexTree<?>> exprs = new ArrayList<>();
			for (Location<?> e : elem){
				if (e.getBytecode() instanceof Invoke)
					exprs.addAll(buildInvoke((Location<Invoke>) e));
				else
					exprs.add(buildExpression(e));
			}
			return exprs;
		}
//
//		@SuppressWarnings("unchecked")
//		private Pair<Location<Bytecode.VariableDeclaration>,String> buildAssignName(Location<?> field) throws CompilerException {
//			Bytecode bytecode = field.getBytecode();
//			if (bytecode instanceof Bytecode.FieldLoad)
//				return buildAssignName(field.getOperand(0)).transformSecond((String a) -> a+"_"+((Bytecode.FieldLoad) bytecode).fieldName());
//			if (bytecode instanceof Bytecode.VariableDeclaration)
//				return new Pair<>(
//						(Location<Bytecode.VariableDeclaration>) field,
//						((Bytecode.VariableDeclaration) bytecode).getName());
//			if (bytecode instanceof Bytecode.VariableAccess)
//				return buildAssignName(field.getOperand(0));
//			throw WyilUnsupportedCompilerError.exception(field);
//		}
//
//		private Structure<NodeTree, HalfArrow<?>> buildCopy(Structure<NodeTree, HalfArrow<?>> structure) throws CompilerException {
//			if (structure instanceof RecordStructure)
//				return new RecordStructure<>(((RecordStructure<NodeTree, HalfArrow<?>>)structure).getComponents().Map(
//					(String s, NodeTree n) -> new Pair<>(s, buildCopy(n))));
//			if (structure instanceof UnionStructure)
//				return new UnionStructure<>(((UnionStructure<NodeTree, HalfArrow<?>>)structure).getOptions().Map(
//					(NodeTree t, NodeTree n) -> new Pair<>(buildCopy(t), buildCopy(n))));
//			throw UnsupportedStructureConversionCompilerError.exception(structure);
//		}
//
//		private NodeTree buildCopy(NodeTree node) throws CompilerException {
//			return node instanceof PrimitiveNode
//					? new PrimitiveNode(node.getValue().node, node.getValue().ident)
//					: new CompoundNode<>(buildCopy(node.getStructure()), node.getType());
//		}
//
//		private Structure<NodeTree, HalfArrow<?>> buildField(Structure<NodeTree, HalfArrow<?>> struct, NodeTree current, NodeTree compo) throws CompilerException {
//			if (struct instanceof RecordStructure)
//				return new RecordStructure<>(((RecordStructure<NodeTree, HalfArrow<?>>)struct).getComponents().Map(
//					(String s,NodeTree n) -> new Pair<>(s, n == current ? compo : buildCopy(n))));
//			if (struct instanceof UnionStructure)
//				return new UnionStructure<>(((UnionStructure<NodeTree, HalfArrow<?>>)struct).getOptions().Map(
//					(NodeTree test, NodeTree val) -> new Pair<>(
//							test == current ? compo : buildCopy(test),
//							val == current ? compo : buildCopy(val))));
//			throw UnsupportedStructureConversionCompilerError.exception(struct);
//		}
//
//		private void buildAssignField(int op, NodeTree var, NodeTree val) throws CompilerException {
//			debug("Var "+op + " " +var);
//			if (var != null)
//				var.printStructure(logger, var.getParent()+" --->  ");
//			if (var == null || var.getParent() == null)
//				vars.put(op, val);
//			else
//				buildAssignField(op, var.getParent(), new CompoundNode<>(buildField(var.getParent().getStructure(), var, val), var.getParent().getType()));
//		}
//
//		private Pair<Integer, NodeTree> recoverIdent(Location<?> field) throws CompilerException {
//			Bytecode bytecode = field.getBytecode();
//			if (bytecode instanceof Bytecode.FieldLoad)
//				return recoverIdent(field.getOperand(0)).transformSecondChecked(
//					(NodeTree l) -> l.getComponent(((Bytecode.FieldLoad) bytecode).fieldName()));
//			if (bytecode instanceof Bytecode.VariableDeclaration)
//				return new Pair<>(field.getIndex(), vars.get(field.getIndex()));
//			if (bytecode instanceof Bytecode.VariableAccess)
//				return recoverIdent(field.getOperand(0));
//			throw WyilUnsupportedCompilerError.exception(field);
//		}
//



		private BooleanVertexLeaf buildNamedHalfArrow(String ident, BooleanVertexLeaf val) {
			return new BooleanVertexLeaf(val.getValue().node, ident);
		}
		private VertexLeaf<?> buildNamedHalfArrow(String ident, VertexLeaf<?> val) {
			return new VertexLeaf<>(val.getValue().node, val.getType(), ident);
		}

		private VertexSimpleRecord buildNamedHalfArrow(String ident, VertexSimpleRecord val) throws CompilerException {
			return new VertexSimpleRecord(val.getFieldNames().gather_(val.getFields().mapFirst((ident+"_")::concat).map_(this::buildNamedHalfArrow)), val.getType());
		}
		private VertexSimpleUnion buildNamedHalfArrow(String ident, VertexSimpleUnion val) throws CompilerException {
			VertexOption<? extends TypeLeaf, ? extends VertexLeaf<?>> o;
			return new VertexSimpleUnion(val.getOptions().enumerate().mapFirst(val::getLabel).mapFirst((ident+"_")::concat).map(
				(String s, VertexOption<? extends TypeLeaf, ? extends VertexLeaf<?>> t) -> new VertexOption<>(
					buildNamedHalfArrow(s+"_"+t.getFirstLabel(), t.getFirstOperand()),
					buildNamedHalfArrow(s+"_"+t.getSecondLabel(), t.getSecondOperand()), new TypeOption<>(t.getSecondOperand().getType()))), val.getType());
		}
		private VertexRecordUnion buildNamedHalfArrow(String ident, VertexRecordUnion val) throws CompilerException {
			return new VertexRecordUnion(
				val.getSharedFields().biMap_((Pair<String,AccessibleVertexTree<?>> p) -> p.first,  (Pair<String,AccessibleVertexTree<?>> p) -> buildNamedHalfArrow(ident+"_"+p.first, p.second)),
				val.getSpecificFields().mapSecond_((VertexOption<AccessibleTypeTree, AccessibleVertexTree<?>> t) -> new VertexOption<AccessibleTypeTree, AccessibleVertexTree<?>>(
						buildNamedHalfArrow(ident+"_"+t.getFirstLabel(), t.getFirstOperand()),
						buildNamedHalfArrow(ident+"_"+t.getSecondLabel(), t.getSecondOperand()), t.getType())), val.getType());
		}
		private VertexUnion buildNamedHalfArrow(String ident, VertexUnion val) throws CompilerException {
			return new VertexUnion(
				buildNamedHalfArrow(ident+"_"+val.getFirstLabel(), val.getFirstOperand()),
				buildNamedHalfArrow(ident+"_"+val.getSecondLabel(), val.getSecondOperand()), val.getType());
		}
		private AccessibleVertexTree<?> buildNamedHalfArrow(String ident, AccessibleVertexTree<?> val) throws CompilerException {
			if (val instanceof BooleanVertexLeaf)
				return buildNamedHalfArrow(ident, (BooleanVertexLeaf)val);
			if (val instanceof VertexLeaf)
				return buildNamedHalfArrow(ident, (VertexLeaf<?>)val);
			if (val instanceof VertexSimpleRecord)
				return buildNamedHalfArrow(ident, (VertexSimpleRecord)val);
			if (val instanceof VertexSimpleUnion)
				return buildNamedHalfArrow(ident, (VertexSimpleUnion)val);
			if (val instanceof VertexRecordUnion)
				return buildNamedHalfArrow(ident, (VertexRecordUnion)val);
			throw UnsupportedTreeNodeCompilerError.exception(val);
		}



		private void buildAssignValue(Location<?> acc, AccessibleVertexTree<?> val) throws CompilerException {
/**/			openLevel("Assign");
//			Utils.printLocation(logger, acc, level);
//			val.printStructure(logger, level);
			Pair<Location<Bytecode.VariableDeclaration>,String> p = buildAssignName(acc);
			Pair<Integer, NodeTree> q = recoverIdent(acc);
			buildAssignField(q.first, q.second , buildNamedHalfArrow(p.second, buildTypedValue(val, buildType(acc.getType()))));
/**/			closeLevel();
		}

		private void buildAssign(Location<Bytecode.Assign> assign) throws CompilerException {
//			Generators.fromCollection(assign.getOperandGroup(SyntaxTree.LEFTHANDSIDE)).gather(
//				Generators.fromCollection(assign.getOperandGroup(SyntaxTree.RIGHTHANDSIDE))).mapSecond(function)
			Location<?>[] lhs = assign.getOperandGroup(SyntaxTree.LEFTHANDSIDE);
			Location<?>[] rhs = assign.getOperandGroup(SyntaxTree.RIGHTHANDSIDE);
			List<AccessibleVertexTree<?>> brhs = buildTuple(rhs);
			for(int k = 0; k < lhs.length; ++k)
				buildAssignValue(lhs[k], brhs.get(k));
		}








		private void buildReturn(Location<Bytecode.Return> ret) throws CompilerException {
//			debug("Return.....");
			Generator_<AccessibleVertexTree<?>,CompilerException> retv = Generators.fromCollection(
				buildTuple(ret.getOperands())).enumerate().mapFirst(returnTypes::get).swap().map_(this::buildTypedValue);
//			debug("Return ?.....");
			if (partialReturn != null)
				partialReturn.completeReturn(retv.toList());
			else
				partialReturn = new PartialReturn(retv.toList());
		}

		private void buildDecl(Location<Bytecode.VariableDeclaration> decl) throws CompilerException {
//			debug("Declaration de "+decl.getBytecode().getName()+" en "+decl.getIndex());
			vars.put(decl.getIndex(), decl.numberOfOperands() == 1
				? buildNamedHalfArrow(decl.getBytecode().getName(),
					buildTypedValue(buildExpression(decl.getOperand(0)), buildType(decl.getType())))
				: null);
//			debug("Ajouté !");
		}
//
//
//		private NodeTree buildIs(Location<Bytecode.Operator> is, NodeTree value, TypeTree type) {
//			debug("IS "+value.getType()+" -> "+type);
//			if (!(value instanceof CompoundNode) || !(value.getStructure() instanceof UnionStructure))
//				return new PrimitiveNode(value.getType().equals(type) ? graph.getTrue() : graph.getFalse());
//			List<TypeTree> types = (type instanceof CompoundType && type.getStructure() instanceof UnionStructure)
//					? ((UnionStructure<TypeTree, Type>) type.getStructure()).getOptions().takeSecond().toList() : Collections.singletonList(type);
//			debug("Il y a " + types.size() +" cas");
//			DataNode cond = ((UnionStructure<NodeTree, HalfArrow<?>>) value.getStructure()).getOptions().fold(
//				(DataNode d, Pair<NodeTree,NodeTree> p) -> {
//					debug("Cas "+ p.second.getType());
//					for (TypeTree t : types)
//						if (t.equals(p.second.getType()))
//							return d == null
//								? p.first.getValue().node
//								: graph.new BinOpNode(is, Type.Boolean, new HalfArrow<>(d), p.first.getValue());
//					debug("  ---- abandonné");
//					return d;
//
//				}, null);
//			debug("Donc "+cond);
//			return new PrimitiveNode(cond == null ? graph.getFalse() : cond);
//		}
//

		private AccessibleVertexTree<?> buildRecordConstruction(Location<Bytecode.Operator> op) throws CompilerException {
			AccessibleTypeTree type = buildType(op.getType());
			if (type instanceof TypeSimpleRecord)
				return new VertexSimpleRecord(
						Generators.fromCollection(((wyil.lang.Type.EffectiveRecord)op.getType()).getFieldNames()).gather(
							Generators.fromCollection(op.getOperands())).map_((Pair<String, Location<?>> p) -> new Pair<>(p.first, buildExpression(p.second))),
						(TypeSimpleRecord) type);
			throw WyilUnsupportedCompilerError.exception(op); // TODO Record Union ? + Type verification
		}



		private BooleanVertexLeaf buildIs(Location<Bytecode.Operator> is) throws CompilerException {
			AccessibleVertexTree<?> arg = buildExpression(is.getOperand(0));
			AccessibleTypeTree type = buildType(((Constant.Type)(((Const)(is.getOperand(1).getBytecode())).constant())).value());
			// TODO
		}

		private AccessibleVertexTree<?> buildUnaryOperation(Location<Bytecode.Operator> op) throws CompilerException {
			AccessibleTypeTree type = buildType(op.getType());
			AccessibleVertexTree<?> arg = buildTypedValue(buildExpression(op.getOperand(0)), buildType(op.getType()));
			if (type instanceof TypeLeaf && arg instanceof VertexLeaf)
				return new VertexLeaf<>(graph.new UnaOpNode(op, ((TypeLeaf) type).getValue(), ((VertexLeaf<?>) arg).getValue()), (TypeLeaf) type);
			throw WyilUnsupportedCompilerError.exception(op);
		}
		private AccessibleVertexTree<?> buildBinaryOperation(Location<Bytecode.Operator> op) throws CompilerException {
			AccessibleTypeTree type = buildType(op.getType());
			AccessibleVertexTree<?> arg1 = buildTypedValue(buildExpression(op.getOperand(0)), buildType(op.getType()));
			AccessibleVertexTree<?> arg2 = buildTypedValue(buildExpression(op.getOperand(0)), buildType(op.getType()));
			if (type instanceof TypeLeaf && arg1 instanceof VertexLeaf && arg2 instanceof VertexLeaf)
				return new VertexLeaf<>(graph.new BinOpNode(op,
					((TypeLeaf) type).getValue(), ((VertexLeaf<?>) arg1).getValue(), ((VertexLeaf<?>) arg2).getValue()), (TypeLeaf) type);
			throw WyilUnsupportedCompilerError.exception(op);
		}
		private AccessibleVertexTree<?> buildOperator(Location<Bytecode.Operator> op) throws CompilerException {
//			debug("Operator Compilation");
//			debug("Operator "+a);
//			a.printStructure(logger, "  ");
			if (op.getBytecode().kind() == OperatorKind.RECORDCONSTRUCTOR)
				return buildRecordConstruction(op);
			if (op.getBytecode().kind() == OperatorKind.IS)
				return buildIs(op);
			if (op.numberOfOperands() == 1)
				return buildUnaryOperation(op);
			if (op.numberOfOperands() == 2)
				return buildBinaryOperation(op);
			throw WyilUnsupportedCompilerError.exception(op);
		}

		private AccessibleVertexTree<?> buildConst(Location<Const> val) throws CompilerException {
			TypeTree t = buildType(val.getType());
			if (t instanceof BooleanTypeLeaf)
				return new BooleanVertexLeaf(graph.new ConstNode(val, ((TypeLeaf) t).getValue()));
			if (t instanceof TypeLeaf)
				return new VertexLeaf<>(graph.new ConstNode(val, ((TypeLeaf) t).getValue()), (TypeLeaf) t);
			throw WyilUnsupportedCompilerError.exception(val);
		}




		private AccessibleVertexTree<?> buildFieldAccess(Location<FieldLoad> field) throws CompilerException {
			AccessibleVertexTree<?> record = buildAccess(field.getOperand(0));
			if (record instanceof VertexSimpleRecord)
				return ((VertexSimpleRecord)record).getField(field.getBytecode().fieldName());
			if (record instanceof VertexRecordUnion)
				return ((VertexRecordUnion)record).getFirstOperand().getField(field.getBytecode().fieldName());
			throw WyilUnsupportedCompilerError.exception(field);
		}



		private VertexLeaf<?> buildAlias(TypeLeaf type, VertexSimpleUnion node) throws CompilerException {
			return node.getOptions().map(VertexOption::getSecondOperand).find((VertexLeaf<?> o) -> o.getType().equals(type));
		}
		private VertexSimpleRecord buildAlias(TypeSimpleRecord type, VertexSimpleRecord node) throws CompilerException {
			if (! type.hasSameFields(node))
				throw UnsupportedAliasCompilerError.exception(type, node);
			return new VertexSimpleRecord(type.getFieldNames().gather_(type.getFields().takeSecond().gather(node.getFields().takeSecond()).map_(this::buildAlias)),type);
		}
		private VertexSimpleRecord buildAlias(TypeSimpleRecord type, VertexRecordUnion node) throws CompilerException {
			Map<String, AccessibleVertexTree<?>> cpn = new HashMap<>();
			node.getSharedFields().map(cpn::put);
			node.getSpecificFields().mapSecond(VertexOption::getSecondOperand).map(cpn::put);
			if (! type.getFields().forAll(cpn::containsKey))
				throw UnsupportedAliasCompilerError.exception(type, node);
			return new VertexSimpleRecord(type.getFieldNames().gather_(type.getFields().swap().mapSecond(cpn::get).map_(this::buildAlias)), type);
		}
		private VertexSimpleUnion buildAlias(TypeSimpleUnion type, VertexSimpleUnion node) throws CompilerException {
			List<VertexOption<? extends TypeLeaf, ? extends VertexLeaf<? extends TypeLeaf>>> cpn = node.getOptions().toList();
			return new VertexSimpleUnion(type.getOptions().map(TypeOption::getSecondOperand).map((TypeLeaf l) -> Generators.fromCollection(cpn).find(
				(VertexOption<? extends TypeLeaf, ? extends VertexLeaf<? extends TypeLeaf>> o) -> l.equals(o.getSecondOperand().getType()))), type);
		}
		private VertexRecordUnion buildAlias(TypeRecordUnion type, VertexRecordUnion node) throws CompilerException {
//			return new VertexRecordUnion(
				type.getSharedFields().mapSecond_((String f, AccessibleTypeTree t) -> buildAlias(t, node.hasSharedField(f)
					? node.getSharedField(f)
					: node.getSpecificField(f).getSecondOperand()));
				type.getSpecificFields().mapSecond_(
					(String f, TypeOption<AccessibleTypeTree> t) -> new VertexOption<>(
							node.getSpecificField(f).getFirstOperand(), buildAlias(t.getSecondOperand(), node.getSpecificField(f).getSecondOperand()), t));
//				type);
		}
		private VertexUnion buildAlias(TypeUnion type, VertexUnion node) throws CompilerException {
			return new VertexUnion(
				buildAlias(type.getFirstOperand(), node.getFirstOperand()),
				buildAlias(type.getSecondOperand(), node.getSecondOperand()), type);
		}
		private AccessibleVertexTree<?> buildAlias(AccessibleTypeTree type, AccessibleVertexTree<?> node) throws CompilerException {
			// TODO BooleanLeaf ?
			if (((VertexLeaf<?>)node).getType().equals(type))
				return node;
			if (type instanceof TypeLeaf && node instanceof VertexSimpleUnion)
				return buildAlias((TypeLeaf) type, (VertexSimpleUnion) node);
			if (type instanceof TypeLeaf && node instanceof VertexUnion)
				return buildAlias((TypeLeaf) type, ((VertexUnion) node).getFirstOperand());
			if (type instanceof TypeSimpleRecord && node instanceof VertexSimpleRecord)
				return buildAlias((TypeSimpleRecord) type, (VertexSimpleRecord) node);
			if (type instanceof TypeSimpleRecord && node instanceof VertexRecordUnion)
				return buildAlias((TypeSimpleRecord) type, (VertexRecordUnion) node);
			if (type instanceof TypeSimpleRecord && node instanceof VertexUnion)
				return buildAlias((TypeSimpleRecord) type, ((VertexUnion) node).getSecondOperand());
			if (type instanceof TypeSimpleUnion && node instanceof VertexSimpleUnion)
				return buildAlias((TypeSimpleUnion) type, (VertexSimpleUnion) node);
			if (type instanceof TypeSimpleUnion && node instanceof VertexUnion)
				return buildAlias((TypeSimpleUnion) type, ((VertexUnion) node).getFirstOperand());
			if (type instanceof TypeRecordUnion && node instanceof VertexRecordUnion)
				return buildAlias((TypeRecordUnion) type, (VertexRecordUnion) node);
			if (type instanceof TypeRecordUnion && node instanceof VertexUnion)
				return buildAlias((TypeRecordUnion) type, ((VertexUnion) node).getSecondOperand());
			if (type instanceof TypeUnion && node instanceof VertexUnion)
				return buildAlias((TypeUnion) type, (VertexUnion) node);
			throw UnsupportedAliasCompilerError.exception(type, node);
		}

		private AccessibleVertexTree<?> buildVariableAccess(Location<?> var) throws CompilerException {
			Bytecode bytecode = var.getBytecode();
//			debug("var access"+var+" "+var.getIndex());
			if (bytecode instanceof VariableDeclaration)
				return vars.get(var.getIndex());
			if (bytecode instanceof AliasDeclaration)
				return buildAlias(buildType(var.getType()), buildVariableAccess(var.getOperand(0)));
			throw WyilUnsupportedCompilerError.exception(var);
		}
//

		@SuppressWarnings("unchecked")
		private AccessibleVertexTree<?> buildAccess(Location<?> location) throws CompilerException {
/**/			openLevel("Access");
/**/			Utils.printLocation(logger, location, level);
			Bytecode bytecode = location.getBytecode();
			if (bytecode instanceof FieldLoad)
				return end(buildFieldAccess((Location<FieldLoad>) location));
			if (bytecode instanceof VariableAccess)
				return end(buildVariableAccess(location.getOperand(0)));
			throw new CompilerException(new WyilUnsupportedCompilerError(location));
		}

//
//		private DataNode buildCallReturn(String ident, Type type, FuncCallNode func) {
//			return graph.new FunctionReturnNode(ident, type, new HalfArrow<>(func));
//		}
//		private Structure<NodeTree, HalfArrow<?>> buildCallReturn(String ident, Structure<TypeTree, Type> structure, FuncCallNode func) throws CompilerException {
//			if (structure instanceof RecordStructure)
//				return new RecordStructure<>(((RecordStructure<TypeTree, Type>) structure).getComponents().Map(
//					(String s, TypeTree t) -> new Pair<>(s, buildCallReturn(ident+"_"+s, t, func))));
//			if (structure instanceof UnionStructure)
//				return new UnionStructure<>(((UnionStructure<TypeTree, Type>) structure).getOptions().EnumMap(
//					(Integer k, Pair<TypeTree,TypeTree> p) -> new Pair<>(
//							buildCallReturn(ident+"_"+UnionStructure.FLG_PREFIX+k, p.first, func),
//							buildCallReturn(ident+"_"+UnionStructure.VAL_PREFIX+k, p.second, func))));
//			throw UnsupportedStructureConversionCompilerError.exception(structure);
//		}
//		private NodeTree buildCallReturn(String ident, TypeTree type, FuncCallNode func) throws CompilerException {
//			return type instanceof PrimitiveType
//					? new PrimitiveNode(buildCallReturn(ident, type.getValue(), func), ident)
//					: new CompoundNode<>(buildCallReturn(ident, type.getStructure(), func), type);
//		}
//
		private List<AccessibleVertexTree<?>> buildInvoke(Location<Invoke> call) throws CompilerException {
			FuncCallNode c = graph.new FuncCallNode(call,
				Generators.fromCollection(call.getOperands()).enumerate().map(
				    (Integer t, Location<?> l) -> buildNamedHalfArrow("arg_"+t, buildExpression(l)).getValue()));
			return Utils.checkedConvert(Arrays.asList(call.getBytecode().type().returns()),
				(wyil.lang.Type t, Integer i) -> buildCallReturn("ret_"+i, buildType(t), c));
		}


		@SuppressWarnings("unchecked")
		private AccessibleVertexTree<?> buildExpression(Location<?> location) throws CompilerException {
			openLevel("Expression");
			Utils.printLocation(logger, location, level);
			Bytecode bytecode = location.getBytecode();
			if (bytecode instanceof Bytecode.Operator)
				return end(buildOperator((Location<Bytecode.Operator>) location));
			if (bytecode instanceof Const)
				return end(buildConst((Location<Const>) location));
			if (bytecode instanceof VariableAccess || bytecode instanceof FieldLoad)
				return end(buildAccess(location));
			if (bytecode instanceof Invoke)
				return end(buildInvoke((Location<Invoke>) location).get(0));
			throw new CompilerException(new WyilUnsupportedCompilerError(location));
		}






		private VertexLeaf buildEndIf(Location<If> ifs, HalfArrow<?> ifn, VertexLeaf trueLab, VertexLeaf falseLab) {
			return trueLab.getValue().node == falseLab.getValue().node
					? trueLab
					: new VertexLeaf(graph.new EndIfNode(ifs, ifn, trueLab.getValue(), falseLab.getValue()), trueLab.getType());
		}
		private VertexSimpleRecord buildEndIf(Location<If> ifs, HalfArrow<?> ifn, VertexSimpleRecord trueLab, VertexSimpleRecord falseLab) throws CompilerException {
			return new VertexSimpleRecord(falseLab.getFieldNames().gather_(falseLab.getFields().takeSecond().gather(falseLab.getFields().takeSecond()).
				map_((VertexTree<?> t, VertexTree<?> f) -> buildEndIf(ifs, ifn, t,f))), trueLab.getType());
		}
		private VertexOption buildEndIf(Location<If> ifs, HalfArrow<?> ifn, VertexOption trueLab, VertexOption falseLab) throws CompilerException {
			return new VertexOption(
				buildEndIf(ifs, ifn, trueLab.getFirstOperand(), falseLab.getFirstOperand()),
				buildEndIf(ifs, ifn, trueLab.getSecondOperand(), falseLab.getSecondOperand()), trueLab.getType());
		}
		private VertexSimpleUnion buildEndIf(Location<If> ifs, HalfArrow<?> ifn, VertexSimpleUnion trueLab, VertexSimpleUnion falseLab) throws CompilerException {
			return new VertexSimpleUnion(trueLab.getOptions().gather(falseLab.getOptions()).map_(
				(VertexOption t, VertexOption f) -> buildEndIf(ifs, ifn, t,f)), trueLab.getType());
		}
//		private VertexRecordUnion buildEndIf(Location<If> ifs, HalfArrow<?> ifn, String name, TypeRecordUnion falseLab) throws CompilerException {
//			return new VertexRecordUnion(
//				type.getSharedFields().biMap_((Pair<String,TypeTree> p) -> p.first,  (Pair<String,TypeTree> p) -> buildParameter(ident+"_"+p.first, p.second)),
//				type.getSpecificFields().mapSecond_((TypeOption t) -> new VertexOption(
//						buildBoolean(false),
//						buildUndefinedValue(t.getSecondOperand()), t)), type);
//		}
		private VertexTree<?> buildEndIf(Location<If> ifs, HalfArrow<?> ifn, VertexTree<?> trueLab, VertexTree<?> falseLab) throws CompilerException {
			trueLab.checkIdenticalStructure(falseLab);
			if (trueLab instanceof VertexLeaf 			&& falseLab instanceof VertexLeaf)
				return buildEndIf(ifs, ifn, (VertexLeaf)trueLab, (VertexLeaf)falseLab);
			if (trueLab instanceof VertexSimpleRecord 	&& falseLab instanceof VertexRecord)
				return buildEndIf(ifs, ifn, (VertexSimpleRecord)trueLab, (VertexSimpleRecord)falseLab);
			if (trueLab instanceof VertexSimpleUnion 			&& falseLab instanceof VertexSimpleUnion)
				return buildEndIf(ifs, ifn, (VertexSimpleUnion)trueLab, (VertexSimpleUnion)falseLab);
//			if (trueLab instanceof VertexRecordUnion 	&& falseLab instanceof VertexRecordUnion)
//				return buildEndIf(ifs, ifn, (VertexRecordUnion)trueLab, (VertexRecordUnion)falseLab);
			throw UnsupportedTreeNodeCompilerError.exception(trueLab);
		}




		private VertexLeaf copyNamedHalfArrow(VertexLeaf name, VertexLeaf node) {
			return new VertexLeaf(node.getValue().node, name.getValue().ident);
		}
		private VertexSimpleRecord copyNamedHalfArrow(VertexSimpleRecord name, VertexSimpleRecord node) throws CompilerException {
			return new VertexSimpleRecord(name.getFieldNames().gather_(node.getFields().takeSecond().gather(name.getFields().takeSecond()).
				map_(this::copyNamedHalfArrow)), node.getType());
		}
		private VertexOption copyNamedHalfArrow(VertexOption name, VertexOption node) throws CompilerException {
			return new VertexOption(
				copyNamedHalfArrow(name.getFirstOperand(), node.getFirstOperand()),
				copyNamedHalfArrow(name.getSecondOperand(), node.getSecondOperand()), node.getType());
		}
		private VertexSimpleUnion copyNamedHalfArrow(VertexSimpleUnion name, VertexSimpleUnion node) throws CompilerException {
			return new VertexSimpleUnion(name.getOptions().gather(node.getOptions()).map_(this::copyNamedHalfArrow), node.getType());
		}
//		private VertexRecordUnion copyNamedHalfArrow(String name, TypeRecordUnion node) throws CompilerException {
//			return new VertexRecordUnion(
//				type.getSharedFields().biMap_((Pair<String,TypeTree> p) -> p.first,  (Pair<String,TypeTree> p) -> buildParameter(ident+"_"+p.first, p.second)),
//				type.getSpecificFields().mapSecond_((TypeOption t) -> new VertexOption(
//						buildBoolean(false),
//						buildUndefinedValue(t.getSecondOperand()), t)), type);
//		}
		private VertexTree<?> copyNamedHalfArrow(VertexTree<?> name, VertexTree<?> node) throws CompilerException {
			name.checkIdenticalStructure(node);
			if (name instanceof VertexLeaf 			&& node instanceof VertexLeaf)
				return copyNamedHalfArrow((VertexLeaf)name, (VertexLeaf)node);
			if (name instanceof VertexSimpleRecord 	&& node instanceof VertexRecord)
				return copyNamedHalfArrow((VertexSimpleRecord)name, (VertexSimpleRecord)node);
			if (name instanceof VertexSimpleUnion 		&& node instanceof VertexSimpleUnion)
				return copyNamedHalfArrow((VertexSimpleUnion)name, (VertexSimpleUnion)node);
//			if (name instanceof VertexRecordUnion 	&& node instanceof VertexRecordUnion)
//				return copyNamedHalfArrow((VertexRecordUnion)name, (VertexRecordUnion)node);
			throw UnsupportedTreeNodeCompilerError.exception(node);
		}



		private void buildIf(Location<If> ifs) throws CompilerException {
			VertexTree<?> cond = buildExpression(ifs.getOperand(0));

			if (!(cond instanceof VertexLeaf))
				throw new CompilerException(new CompilerError() {
					@Override
					public String info() {
						return "The condition is not a Leaf...";
					}
				});
			HalfArrow<?> ifn = ((VertexLeaf)cond).getValue(); // TODO verif bool primitif.

			HashMap<Integer, VertexTree<?>> state = new HashMap<>();
			vars.forEach((Integer i, VertexTree<?> t) -> state.put(i, t));
			HashMap<Integer, VertexTree<?>> tbc = new HashMap<>();

			PartialReturn prevReturn = partialReturn;
			partialReturn = null;
			build(ifs.getBlock(0));
			state.forEach((Integer i, VertexTree<?> t) -> {if (vars.get(i) != t) tbc.put(i, vars.get(i));});

			vars.clear();
			state.forEach((Integer i, VertexTree<?> t) -> vars.put(i, t));


			HashMap<Integer, VertexTree<?>> fbc = new HashMap<>();

			PartialReturn trueReturn = partialReturn;
			partialReturn = null;
			if (ifs.getBytecode().hasFalseBranch())
				build(ifs.getBlock(1));
			state.forEach((Integer i, VertexTree<?> t) -> {if (vars.get(i) != t) fbc.put(i, vars.get(i));});

			vars.clear();
			Generators.fromMap(state).forEach_(
				(Integer i, VertexTree<?> t) -> vars.put(i, tbc.containsKey(i) || fbc.containsKey(i)
					? copyNamedHalfArrow(t, buildEndIf(ifs, ifn, tbc.getOrDefault(i, t), fbc.getOrDefault(i, t)))
					: t));
			if (trueReturn != null || partialReturn != null) {
				addMessage(new NestedReturnCompilerNotice());
				trueReturn = new PartialReturn(ifs, ifn, trueReturn, partialReturn);
			}
			partialReturn = prevReturn != null ? prevReturn.completeReturn(trueReturn) : trueReturn;
		}
	}






	public DataFlowGraph buildGraph(FunctionOrMethod func) throws CompilerException {
		return null;// new Builder(func).getGraph();
	}
}
