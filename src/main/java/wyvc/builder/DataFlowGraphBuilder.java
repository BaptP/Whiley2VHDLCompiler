package wyvc.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

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
import wyvc.builder.LexicalElementTree;
import wyvc.builder.LexicalElementTree.Compound;
import wyvc.builder.LexicalElementTree.Primitive;
import wyvc.builder.LexicalElementTree.Structure;
import wyvc.builder.LexicalElementTree.Tree;
import wyvc.builder.TypeCompiler.PrimitiveType;
import wyvc.builder.TypeCompiler.RecordStructure;
import wyvc.builder.TypeCompiler.RecordsUnionStructure;
import wyvc.builder.TypeCompiler.CompoundType;
import wyvc.builder.TypeCompiler.TypeTree;
import wyvc.builder.TypeCompiler.UnionStructure;
import wyvc.lang.Type;
import wyvc.utils.Generator;
import wyvc.utils.Pair;
import wyvc.utils.Utils;
import wyvc.utils.Generator.CheckedGenerator;

public final class DataFlowGraphBuilder extends LexicalElementTree {
	private final TypeCompiler typeCompiler;


	public DataFlowGraphBuilder(CompilerLogger logger, TypeCompiler typeCompiler) {
		super(logger);
		this.typeCompiler = typeCompiler;
	}






	public static interface NodeTree extends Tree<NodeTree, HalfArrow<?>> {
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

	public class CompoundNode<S extends Structure<NodeTree, HalfArrow<?>>> extends Compound<NodeTree, HalfArrow<?>, S> implements NodeTree {
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


	public static class UnsupportedStructureConversionCompilerError extends CompilerError {
		private final Structure<?,?> structure;

		public UnsupportedStructureConversionCompilerError(Structure<?,?> structure) {
			this.structure = structure;
		}

		@Override
		public String info() {
			return "The conversion of CompoundValue is not provided for the structure "+structure;
		}

		public static CompilerException exception(Structure<?,?> structure) {
			return new CompilerException(new UnsupportedStructureConversionCompilerError(structure));
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



	private final class Builder extends LoggedBuilder {
		private class PartialReturn {
			private final HalfArrow<?> cond;
			private final Location<If> ifs;
			private PartialReturn tPart;
			private PartialReturn fPart;
			private final List<NodeTree> ret;

			public PartialReturn(Location<If> ifs, HalfArrow<?> cond, PartialReturn tPart, PartialReturn fPart) {
				this.cond = cond;
				this.ifs = ifs;
				this.tPart = tPart;
				this.fPart = fPart;
				this.ret = null;
			}
			public PartialReturn(List<NodeTree> ret) {
				assert ret != null;
				this.ifs = null;
				this.cond = null;
				this.tPart = null;
				this.fPart = null;
				this.ret = ret;
			}


			public List<NodeTree> getReturn() throws CompilerException {
				assert !isPartial();
//				logger.debug("Return ? "+this+" "+ret+" "+tPart+" "+fPart);
				return ret == null ? Utils.checkedConvert(Utils.gather(tPart.getReturn(), fPart.getReturn()),
					(Pair<NodeTree, NodeTree> p) -> buildEndIf(ifs, cond, p.first, p.second)) : ret;
			}

			public void completeReturn(List<NodeTree> rem) {
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


		private Map<Integer, NodeTree> vars = new HashMap<>();
		private PartialReturn partialReturn = null;
		private final TypeTree[] returnTypes;
		private final DataFlowGraph graph;
		private Map<wyil.lang.Type, wyvc.lang.Type> compiledTypes = new HashMap<>();



		private Type isNodeType(TypeTree type) throws CompilerException {
			if (type instanceof CompoundType)
				CompoundTypeCompilerError.exception(type);
			return type.getValue();
		}

		private Type getNodeType(wyil.lang.Type type) throws CompilerException {
			return Utils.checkedAddIfAbsent(compiledTypes, type, () -> isNodeType(buildType(type)));

		}


		public DataFlowGraph getGraph() {
			return graph;
		}


		public TypeTree buildType(wyil.lang.Type type) throws CompilerException {
			return typeCompiler.compileType(type);
		}




		@SuppressWarnings("unchecked")
		public Builder(WyilFile.FunctionOrMethod func) throws CompilerException {
			super(DataFlowGraphBuilder.this.logger);
			graph = new DataFlowGraph();
			Utils.checkedForEach(
				Arrays.asList(func.type().params()),
				(Integer k, wyil.lang.Type t) -> Utils.ignore(vars.put(k, buildParameter(
					((Location<Bytecode.VariableDeclaration>)func.getTree().getLocation(k)).getBytecode().getName(),
					buildType(t)))));
			returnTypes = new TypeTree[func.type().returns().length];
			Utils.checkedConvert(
				Arrays.asList(func.type().returns()),
				(wyil.lang.Type t) ->  buildType(t)).toArray(returnTypes);
			build(func.getBody());
			partialReturn.print("RET ");
			Utils.checkedForEach(
				partialReturn.getReturn(),
				(Integer k, NodeTree r) -> buildReturnValue("ret_"+ k, r));
		}




		private UndefConstNode buildUndefinedValue(Type type) {
			return graph.new UndefConstNode(type);
		}

		private Structure<NodeTree, HalfArrow<?>> buildUndefinedValue(Structure<TypeTree, Type> structure) throws CompilerException {
			if (structure instanceof RecordStructure)
				return new RecordStructure<>(((RecordStructure<TypeTree, Type>) structure).getComponents().Map(
					(String s, TypeTree t) -> new Pair<>(s, buildUndefinedValue(t))));
			if (structure instanceof UnionStructure)
				return new UnionStructure<>(((UnionStructure<TypeTree, Type>) structure).getOptions().Map(
					(TypeTree t,TypeTree v) -> new Pair<>(buildUndefinedValue(t),buildUndefinedValue(v))));
			throw UnsupportedStructureConversionCompilerError.exception(structure);
		}

		private NodeTree buildUndefinedValue(TypeTree type) throws CompilerException {
			return type instanceof PrimitiveType
					? new PrimitiveNode(buildUndefinedValue(type.getValue()))
					: new CompoundNode<>(buildUndefinedValue(type.getStructure()), type);
		}
		private RecordStructure<NodeTree, HalfArrow<?>> buildUnionValue(RecordStructure<TypeTree,Type> type, NodeTree value) throws CompilerException {
			//if (type.getComponentNumber() != value.getComponentNumber() || )
			// TODO Test ?
			return new RecordStructure<>(type.getComponents().gather(value.getStructure().getComponents()).Map(
				(Pair<Pair<String, TypeTree>, Pair<String,NodeTree>> p) ->
				new Pair<>(p.first.first,buildTypedValue(p.second.second, p.first.second))));
		}

		private UnionStructure<NodeTree, HalfArrow<?>> buildUnionValue(UnionStructure<TypeTree,Type> type, NodeTree value) throws CompilerException {
			List<Pair<TypeTree, Pair<NodeTree, NodeTree>>> cps =  value instanceof CompoundNode<?> && value.getStructure() instanceof UnionStructure<?,?>
				? ((UnionStructure<NodeTree, HalfArrow<?>>) value.getStructure()).getOptions().map((Pair<NodeTree, NodeTree> p) -> new Pair<>(p.second.getType(), p)).toList()
				: Collections.singletonList(new Pair<>(value.getType(), new Pair<>(new PrimitiveNode(graph.getTrue()),value)));
			return new UnionStructure<>(type.getOptions().map(
					(TypeTree t,TypeTree v) -> new Pair<>(v,Generator.fromPairCollection(cps).find(
							(Pair<TypeTree, Pair<NodeTree, NodeTree>> c) -> c.first.equals(v)))).Map(
					(Pair<TypeTree, Pair<TypeTree, Pair<NodeTree, NodeTree>>> p) -> p.second == null
						? new Pair<>(new PrimitiveNode(graph.getFalse()),buildUndefinedValue(p.first))
						: p.second.second));
		}

		private Structure<NodeTree, HalfArrow<?>> buildTypedValue(Structure<TypeTree, Type> type, NodeTree node) throws CompilerException {
			if (type instanceof UnionStructure)
				return buildUnionValue((UnionStructure<TypeTree,Type>) type, node);
			if (type instanceof RecordStructure)
				return buildUnionValue((RecordStructure<TypeTree,Type>) type, node);
			throw UnsupportedStructureConversionCompilerError.exception(type);
		}

		private NodeTree buildTypedValue(NodeTree node, TypeTree type) throws CompilerException {
//			debug("Val "+(node == null ? "NULL" : node.getType()));
			if (node.getType().equals(type))
				return node;
			if (type instanceof CompoundType)
				return new CompoundNode<>(buildTypedValue(type.getStructure(), node), type);
			throw UnrelatedTypeCompilerError.exception(type, node);
		}




		private void buildReturnValue(String ident, NodeTree ret) {
			if (ret instanceof PrimitiveNode)
				graph.new OutputNode(ident, ret.getValue());
			else ret.getComponents().forEach((Pair<String, NodeTree> p) -> buildReturnValue(ident+"_"+p.first, p.second));
		}









		private Structure<NodeTree, HalfArrow<?>> buildUnionNode(NodeTree is_t1, NodeTree vl_t1, NodeTree is_t2, NodeTree vl_t2) throws CompilerException {
			return buildUnionNode(Generator.fromCollection(Arrays.asList(new Pair<>(is_t1,vl_t1), new Pair<>(is_t2,vl_t2))).toCheckedGenerator());
		}

		private Structure<NodeTree, HalfArrow<?>> buildUnionNode(CheckedGenerator<Pair<NodeTree, NodeTree>, CompilerException> options) throws CompilerException {
			final List<Pair<NodeTree, NodeTree>> nodes = options.toList();
//			if (nodes.isEmpty())
//				throw EmptyUnionTypeCompilerError.exception();
			if (nodes.size() == 1)
				logger.addMessage(new CompilerDebug(){
					@Override
					public String info() {
						return "Union node of one option should not exist here.";
					}});
			if (!Generator.fromCollection(nodes).forAll((Pair<NodeTree, NodeTree> p) -> p.second instanceof CompoundType && p.second.getStructure() instanceof RecordStructure))
				return new UnionStructure<>(Generator.fromPairCollection(nodes));

			List<CompoundType<RecordStructure<TypeTree,Type>>> recordTypes = Utils.convert(nodes);
			final Map<String, TypeTree> cps0 = new HashMap<>();
			final Map<String, TypeTree> cps1 = new HashMap<>();
			recordTypes.get(0).getStructure().getFields().forEach((String n, TypeTree t) -> cps0.put(n, t));
			for (int k = 1; k<recordTypes.size() && !cps0.isEmpty(); ++k) {
				recordTypes.get(k).getStructure().getFields().ForEach((String n, TypeTree t) -> {
					if(cps0.containsKey(n))
						cps1.put(n, buildUnionNode(cps0.get(n), t));
				});
				cps0.clear();
				cps0.putAll(cps1);
				cps1.clear();
			}
			return new CompoundType<>(new RecordsUnionStructure<>(
					Generator.fromCollection(cps0.entrySet()).map((Entry<String, TypeTree> e) -> new Pair<>(e.getKey(), e.getValue())),
					Generator.fromCollection(nodes).map((TypeTree t) -> new Pair<>(getBoolean(), t))));
		}


		private DataNode buildParameter(String ident, Type type) {
			return graph.new InputNode(ident, type);
		}
		private Structure<NodeTree, HalfArrow<?>> buildParameter(String ident, Structure<TypeTree, Type> structure) throws CompilerException {
			if (structure instanceof RecordStructure)
				return new RecordStructure<>(((RecordStructure<TypeTree, Type>) structure).getComponents().Map(
					(String s, TypeTree t) -> new Pair<>(s, buildParameter(ident+"_"+s, t))));
			if (structure instanceof UnionStructure)
				return new UnionStructure<>(((UnionStructure<TypeTree, Type>) structure).getOptions().EnumMap(
					(Integer k, Pair<TypeTree,TypeTree> p) -> new Pair<>(
							buildParameter(ident+"_"+EffectiveUnionStructure.FLG_PREFIX+k, p.first),
							buildParameter(ident+"_"+EffectiveUnionStructure.VAL_PREFIX+k, p.second))));
			if (structure instanceof EffectiveUnionStructure)
				return buildUnionNode(((UnionStructure<TypeTree, Type>) structure).getOptions().EnumMap(
					(Integer k, Pair<TypeTree,TypeTree> p) -> new Pair<>(
							buildParameter(ident+"_"+EffectiveUnionStructure.FLG_PREFIX+k, p.first),
							buildParameter(ident+"_"+EffectiveUnionStructure.VAL_PREFIX+k, p.second))));
			throw UnsupportedStructureConversionCompilerError.exception(structure);
		}
		private NodeTree buildParameter(String ident, TypeTree type) throws CompilerException {
			return type instanceof PrimitiveType
					? new PrimitiveNode(buildParameter(ident, type.getValue()), ident)
					: new CompoundNode<>(buildParameter(ident, type.getStructure()), type);
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
		private List<NodeTree> buildTuple(Location<?>[] elem) throws CompilerException {
			ArrayList<NodeTree> exprs = new ArrayList<>();
			for (Location<?> e : elem){
				if (e.getBytecode() instanceof Invoke)
					exprs.addAll(buildInvoke((Location<Invoke>) e));
				else
					exprs.add(buildExpression(e));
			}
			return exprs;
		}

		@SuppressWarnings("unchecked")
		private Pair<Location<Bytecode.VariableDeclaration>,String> buildAssignName(Location<?> field) throws CompilerException {
			Bytecode bytecode = field.getBytecode();
			if (bytecode instanceof Bytecode.FieldLoad)
				return buildAssignName(field.getOperand(0)).transformSecond((String a) -> a+"_"+((Bytecode.FieldLoad) bytecode).fieldName());
			if (bytecode instanceof Bytecode.VariableDeclaration)
				return new Pair<>(
						(Location<Bytecode.VariableDeclaration>) field,
						((Bytecode.VariableDeclaration) bytecode).getName());
			if (bytecode instanceof Bytecode.VariableAccess)
				return buildAssignName(field.getOperand(0));
			throw WyilUnsupportedCompilerError.exception(field);
		}

		private Structure<NodeTree, HalfArrow<?>> buildCopy(Structure<NodeTree, HalfArrow<?>> structure) throws CompilerException {
			if (structure instanceof RecordStructure)
				return new RecordStructure<>(((RecordStructure<NodeTree, HalfArrow<?>>)structure).getComponents().Map(
					(String s, NodeTree n) -> new Pair<>(s, buildCopy(n))));
			if (structure instanceof UnionStructure)
				return new UnionStructure<>(((UnionStructure<NodeTree, HalfArrow<?>>)structure).getOptions().Map(
					(NodeTree t, NodeTree n) -> new Pair<>(buildCopy(t), buildCopy(n))));
			throw UnsupportedStructureConversionCompilerError.exception(structure);
		}

		private NodeTree buildCopy(NodeTree node) throws CompilerException {
			return node instanceof PrimitiveNode
					? new PrimitiveNode(node.getValue().node, node.getValue().ident)
					: new CompoundNode<>(buildCopy(node.getStructure()), node.getType());
		}

		private Structure<NodeTree, HalfArrow<?>> buildField(Structure<NodeTree, HalfArrow<?>> struct, NodeTree current, NodeTree compo) throws CompilerException {
			if (struct instanceof RecordStructure)
				return new RecordStructure<>(((RecordStructure<NodeTree, HalfArrow<?>>)struct).getComponents().Map(
					(String s,NodeTree n) -> new Pair<>(s, n == current ? compo : buildCopy(n))));
			if (struct instanceof UnionStructure)
				return new UnionStructure<>(((UnionStructure<NodeTree, HalfArrow<?>>)struct).getOptions().Map(
					(NodeTree test, NodeTree val) -> new Pair<>(
							test == current ? compo : buildCopy(test),
							val == current ? compo : buildCopy(val))));
			throw UnsupportedStructureConversionCompilerError.exception(struct);
		}

		private void buildAssignField(int op, NodeTree var, NodeTree val) throws CompilerException {
			debug("Var "+op + " " +var);
			if (var != null)
				var.printStructure(logger, var.getParent()+" --->  ");
			if (var == null || var.getParent() == null)
				vars.put(op, val);
			else
				buildAssignField(op, var.getParent(), new CompoundNode<>(buildField(var.getParent().getStructure(), var, val), var.getParent().getType()));
		}

		private Pair<Integer, NodeTree> recoverIdent(Location<?> field) throws CompilerException {
			Bytecode bytecode = field.getBytecode();
			if (bytecode instanceof Bytecode.FieldLoad)
				return recoverIdent(field.getOperand(0)).transformSecondChecked(
					(NodeTree l) -> l.getComponent(((Bytecode.FieldLoad) bytecode).fieldName()));
			if (bytecode instanceof Bytecode.VariableDeclaration)
				return new Pair<>(field.getIndex(), vars.get(field.getIndex()));
			if (bytecode instanceof Bytecode.VariableAccess)
				return recoverIdent(field.getOperand(0));
			throw WyilUnsupportedCompilerError.exception(field);
		}


		private Structure<NodeTree, HalfArrow<?>> buildNamedHalfArrow(String ident, Structure<NodeTree, HalfArrow<?>> structure) throws CompilerException {
			if (structure instanceof RecordStructure)
				return new RecordStructure<>(((RecordStructure<NodeTree, HalfArrow<?>>) structure).getComponents().Map(
					(String s, NodeTree n) -> new Pair<>(s, buildNamedHalfArrow(ident+"_"+s, n))));
			if (structure instanceof UnionStructure)
				return new UnionStructure<>(((UnionStructure<NodeTree, HalfArrow<?>>) structure).getOptions().EnumMap(
					(Integer k, Pair<NodeTree,NodeTree> p) -> new Pair<>(
							buildNamedHalfArrow(ident+"_"+UnionStructure.FLG_PREFIX+k, p.first),
							buildNamedHalfArrow(ident+"_"+UnionStructure.VAL_PREFIX+k, p.second))));
			throw UnsupportedStructureConversionCompilerError.exception(structure);
		}

		private NodeTree buildNamedHalfArrow(String ident, NodeTree node) throws CompilerException {
			return node instanceof PrimitiveNode
					? new PrimitiveNode(node.getValue(), ident)
					: new CompoundNode<>(buildNamedHalfArrow(ident, node.getStructure()), node.getType());
		}


		private void buildAssignValue(Location<?> acc, NodeTree val) throws CompilerException {
/**/			openLevel("Assign");
//			Utils.printLocation(logger, acc, level);
			val.printStructure(logger, level);
			Pair<Location<Bytecode.VariableDeclaration>,String> p = buildAssignName(acc);
			Pair<Integer, NodeTree> q = recoverIdent(acc);
			buildAssignField(q.first, q.second , buildNamedHalfArrow(p.second, buildTypedValue(val, buildType(acc.getType()))));
/**/			closeLevel();
		}

		private void buildAssign(Location<Bytecode.Assign> assign) throws CompilerException {
			Location<?>[] lhs = assign.getOperandGroup(SyntaxTree.LEFTHANDSIDE);
			Location<?>[] rhs = assign.getOperandGroup(SyntaxTree.RIGHTHANDSIDE);
			List<NodeTree> brhs = buildTuple(rhs);
			for(int k = 0; k < lhs.length; ++k)
				buildAssignValue(lhs[k], brhs.get(k));
		}

		private void buildReturn(Location<Bytecode.Return> ret) throws CompilerException {
//			debug("Return.....");
			List<NodeTree> retv = Utils.checkedConvert(
				buildTuple(ret.getOperands()),
				(NodeTree t, Integer k) -> buildTypedValue(t, returnTypes[k]));
//			debug("Return ?.....");
			if (partialReturn != null)
				partialReturn.completeReturn(retv);
			else
				partialReturn = new PartialReturn(retv);
		}

		private void buildDecl(Location<Bytecode.VariableDeclaration> decl) throws CompilerException {
//			debug("Declaration de "+decl.getBytecode().getName()+" en "+decl.getIndex());
			vars.put(decl.getIndex(), decl.numberOfOperands() == 1
				? buildNamedHalfArrow(decl.getBytecode().getName(),
					buildTypedValue(buildExpression(decl.getOperand(0)), buildType(decl.getType())))
				: null);
//			debug("Ajouté !");
		}

		@SuppressWarnings("unchecked")
		private NodeTree buildExpression(Location<?> location) throws CompilerException {
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


		private NodeTree buildIs(Location<Bytecode.Operator> is, NodeTree value, TypeTree type) {
			debug("IS "+value.getType()+" -> "+type);
			if (!(value instanceof CompoundNode) || !(value.getStructure() instanceof UnionStructure))
				return new PrimitiveNode(value.getType().equals(type) ? graph.getTrue() : graph.getFalse());
			List<TypeTree> types = (type instanceof CompoundType && type.getStructure() instanceof UnionStructure)
					? ((UnionStructure<TypeTree, Type>) type.getStructure()).getOptions().takeSecond().toList() : Collections.singletonList(type);
			debug("Il y a " + types.size() +" cas");
			DataNode cond = ((UnionStructure<NodeTree, HalfArrow<?>>) value.getStructure()).getOptions().fold(
				(DataNode d, Pair<NodeTree,NodeTree> p) -> {
					debug("Cas "+ p.second.getType());
					for (TypeTree t : types)
						if (t.equals(p.second.getType()))
							return d == null
								? p.first.getValue().node
								: graph.new BinOpNode(is, Type.Boolean, new HalfArrow<>(d), p.first.getValue());
					debug("  ---- abandonné");
					return d;

				}, null);
			debug("Donc "+cond);
			return new PrimitiveNode(cond == null ? graph.getFalse() : cond);
		}

		private NodeTree buildOperator(Location<Bytecode.Operator> op) throws CompilerException {
//			debug("Operator Compilation");
//			debug("Operator "+a);
//			a.printStructure(logger, "  ");
			if (op.getBytecode().kind() == OperatorKind.RECORDCONSTRUCTOR)
				return new CompoundNode<>(new <CompilerException>RecordStructure<NodeTree, HalfArrow<?>>(
						Generator.fromCollection(((wyil.lang.Type.EffectiveRecord)op.getType()).getFieldNames()).gather(
							Generator.fromCollection(op.getOperands())).Map((Pair<String, Location<?>> p) -> new Pair<>(p.first, buildExpression(p.second)))),
						buildType(op.getType()));
			if (op.getBytecode().kind() == OperatorKind.IS)
				return buildIs(op, buildExpression(op.getOperand(0)), buildType(((Constant.Type)(((Const)(op.getOperand(1).getBytecode())).constant())).value()));
			if (op.numberOfOperands() == 1)
				return new PrimitiveNode(graph.new UnaOpNode(op, getNodeType(op.getType()), buildExpression(op.getOperand(0)).getValue()));
			if (op.numberOfOperands() == 2)
				return new PrimitiveNode(graph.new BinOpNode(op, getNodeType(op.getType()), buildExpression(op.getOperand(0)).getValue(), buildExpression(op.getOperand(1)).getValue()));

			throw new CompilerException(new WyilUnsupportedCompilerError(op));
		}

		private NodeTree buildConst(Location<Const> val) throws CompilerException {
			return new PrimitiveNode(graph.new ConstNode(val, getNodeType(val.getType())));
		}


		@SuppressWarnings("unchecked")
		private NodeTree buildAccess(Location<?> location) throws CompilerException {
/**/			openLevel("Access");
/**/			Utils.printLocation(logger, location, level);
			Bytecode bytecode = location.getBytecode();
			if (bytecode instanceof FieldLoad)
				return end(buildFieldLoad((Location<FieldLoad>) location));
			if (bytecode instanceof VariableAccess)
				return end(buildVariableAccess(location.getOperand(0)));
			throw new CompilerException(new WyilUnsupportedCompilerError(location));
		}

		private NodeTree buildFieldLoad(Location<FieldLoad> field) throws CompilerException {
			return buildAccess(field.getOperand(0)).getComponent(field.getBytecode().fieldName());
		}

		private NodeTree buildVariableAccess(Location<?> var) throws CompilerException {
			Bytecode bytecode = var.getBytecode();
//			debug("var access"+var+" "+var.getIndex());
			if (bytecode instanceof VariableDeclaration)
				return vars.get(var.getIndex());
			if (bytecode instanceof AliasDeclaration) {
				TypeTree type = buildType(var.getType());
				NodeTree node = buildVariableAccess(var.getOperand(0));
				if (!(node instanceof CompoundNode))
					return node;
				List<TypeTree> types = type.getStructure() instanceof UnionStructure
						? ((UnionStructure<TypeTree, Type>) type.getStructure()).getOptions().takeSecond().toList()
						: Collections.singletonList(type);
//				debug("union "+node);
//				node.printStructure(logger, "  ");
				List<Pair<NodeTree, NodeTree>> newOpt = ((UnionStructure<NodeTree, HalfArrow<?>>) node.getStructure()).getOptions().Map(
					(NodeTree p, NodeTree v) -> {
						for (TypeTree t : types)
							if (t.equals(v.getType()))
								return new Pair<>(buildCopy(p),buildCopy(v));
						return null;}).remove(null).toList();
				return newOpt.size() == 1
						? newOpt.get(0).second
						: new CompoundNode<>(new UnionStructure<>(Generator.fromCollection(newOpt)),type);
			}
			throw WyilUnsupportedCompilerError.exception(var);
		}


		private DataNode buildCallReturn(String ident, Type type, FuncCallNode func) {
			return graph.new FunctionReturnNode(ident, type, new HalfArrow<>(func));
		}
		private Structure<NodeTree, HalfArrow<?>> buildCallReturn(String ident, Structure<TypeTree, Type> structure, FuncCallNode func) throws CompilerException {
			if (structure instanceof RecordStructure)
				return new RecordStructure<>(((RecordStructure<TypeTree, Type>) structure).getComponents().Map(
					(String s, TypeTree t) -> new Pair<>(s, buildCallReturn(ident+"_"+s, t, func))));
			if (structure instanceof UnionStructure)
				return new UnionStructure<>(((UnionStructure<TypeTree, Type>) structure).getOptions().EnumMap(
					(Integer k, Pair<TypeTree,TypeTree> p) -> new Pair<>(
							buildCallReturn(ident+"_"+UnionStructure.FLG_PREFIX+k, p.first, func),
							buildCallReturn(ident+"_"+UnionStructure.VAL_PREFIX+k, p.second, func))));
			throw UnsupportedStructureConversionCompilerError.exception(structure);
		}
		private NodeTree buildCallReturn(String ident, TypeTree type, FuncCallNode func) throws CompilerException {
			return type instanceof PrimitiveType
					? new PrimitiveNode(buildCallReturn(ident, type.getValue(), func), ident)
					: new CompoundNode<>(buildCallReturn(ident, type.getStructure(), func), type);
		}

		private List<NodeTree> buildInvoke(Location<Invoke> call) throws CompilerException {
			FuncCallNode c = graph.new FuncCallNode(call,
				Utils.checkedConvert(Arrays.asList(call.getOperands()),
				    (Location<?> l, Integer t) -> buildNamedHalfArrow("arg_"+t, buildExpression(l)).getValue()));
			return Utils.checkedConvert(Arrays.asList(call.getBytecode().type().returns()),
				(wyil.lang.Type t, Integer i) -> buildCallReturn("ret_"+i, buildType(t), c));
		}


		private  DataNode buildEndIf(Location<If> ifs, HalfArrow<?> ifn, HalfArrow<?> trueLab, HalfArrow<?> falseLab) {
			return trueLab.node == falseLab.node ? trueLab.node : graph.new EndIfNode(ifs, ifn, trueLab, falseLab);
		}
		private  Structure<NodeTree, HalfArrow<?>> buildEndIf(Location<If> ifs, HalfArrow<?> ifn, Structure<NodeTree, HalfArrow<?>> trueLab, Structure<NodeTree, HalfArrow<?>> falseLab) throws CompilerException {
			if (trueLab instanceof RecordStructure && falseLab instanceof RecordStructure)
				return new RecordStructure<>(((RecordStructure<NodeTree, HalfArrow<?>>) trueLab).getComponents().gather(
					((RecordStructure<NodeTree, HalfArrow<?>>) falseLab).getComponents()).Map(
					(Pair<String, NodeTree> p1,Pair<String, NodeTree> p2) -> new Pair<>(p1.first, buildEndIf(ifs, ifn, p1.second, p2.second))));
			if (trueLab instanceof UnionStructure && falseLab instanceof UnionStructure)
				return new UnionStructure<>(((UnionStructure<NodeTree, HalfArrow<?>>) trueLab).getOptions().gather(
					((UnionStructure<NodeTree, HalfArrow<?>>) falseLab).getOptions()).Map(
					(Pair<NodeTree,NodeTree> p1,Pair<NodeTree,NodeTree> p2) -> new Pair<>(
							buildEndIf(ifs, ifn, p1.first, p2.first),
							buildEndIf(ifs, ifn, p1.second, p2.second))));
			throw UncompatibleStructuresCompilerError.exception(trueLab, falseLab);
		}
		private  NodeTree buildEndIf(Location<If> ifs, HalfArrow<?> ifn, NodeTree trueLab, NodeTree falseLab) throws CompilerException {
			trueLab.checkIdenticalStructure(falseLab); // Test Type
			return trueLab instanceof PrimitiveNode
					? new PrimitiveNode(buildEndIf(ifs, ifn, trueLab.getValue(), falseLab.getValue()))
					: new CompoundNode<>(buildEndIf(ifs, ifn, trueLab.getStructure(), falseLab.getStructure()), trueLab.getType());
		}



		private Structure<NodeTree, HalfArrow<?>> copyNamedHalfArrow(Structure<NodeTree, HalfArrow<?>> name, Structure<NodeTree, HalfArrow<?>> structure) throws CompilerException {
			if (structure instanceof RecordStructure && name instanceof RecordStructure)
				return new RecordStructure<>(((RecordStructure<NodeTree, HalfArrow<?>>) name).getComponents().gather(
					((RecordStructure<NodeTree, HalfArrow<?>>) structure).getComponents()).Map(
					(Pair<String, NodeTree> p1,Pair<String, NodeTree> p2) -> new Pair<>(p1.first, copyNamedHalfArrow(p1.second, p2.second))));
			if (structure instanceof UnionStructure && name instanceof UnionStructure)
				return new UnionStructure<>(((UnionStructure<NodeTree, HalfArrow<?>>) name).getOptions().gather(
					((UnionStructure<NodeTree, HalfArrow<?>>) structure).getOptions()).Map(
					(Pair<NodeTree,NodeTree> p1,Pair<NodeTree,NodeTree> p2) -> new Pair<>(
							copyNamedHalfArrow(p1.first, p2.first),
							copyNamedHalfArrow(p1.second, p2.second))));
			throw UncompatibleStructuresCompilerError.exception(name, structure);
		}

		private NodeTree copyNamedHalfArrow(NodeTree name, NodeTree node) throws CompilerException {
			name.checkIdenticalStructure(node);
			return node instanceof PrimitiveNode
					? new PrimitiveNode(node.getValue(), name.getValue().ident)
					: new CompoundNode<>(copyNamedHalfArrow(name.getStructure(), node.getStructure()), node.getType());
		}

		private void buildIf(Location<If> ifs) throws CompilerException {
			NodeTree cond = buildExpression(ifs.getOperand(0));

			HalfArrow<?> ifn = cond.getValue(); // TODO verif bool primitif.

			HashMap<Integer, NodeTree> state = new HashMap<>();
			vars.forEach((Integer i, NodeTree t) -> state.put(i, t));
			HashMap<Integer, NodeTree> tbc = new HashMap<>();

			PartialReturn prevReturn = partialReturn;
			partialReturn = null;
			build(ifs.getBlock(0));
			state.forEach((Integer i, NodeTree t) -> {if (vars.get(i) != t) tbc.put(i, vars.get(i));});

			vars.clear();
			state.forEach((Integer i, NodeTree t) -> vars.put(i, t));


			HashMap<Integer, NodeTree> fbc = new HashMap<>();

			PartialReturn trueReturn = partialReturn;
			partialReturn = null;
			if (ifs.getBytecode().hasFalseBranch())
				build(ifs.getBlock(1));
			state.forEach((Integer i, NodeTree t) -> {if (vars.get(i) != t) fbc.put(i, vars.get(i));});

			vars.clear();
			Utils.checkedForEach(state,
				(Integer i, NodeTree t) -> vars.put(i, tbc.containsKey(i) || fbc.containsKey(i)
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
		return new Builder(func).getGraph();
	}
}
