package wyvc.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.Function;

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
import wyvc.builder.CompilerLogger.CompilerError;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.builder.CompilerLogger.CompilerNotice;
import wyvc.builder.CompilerLogger.LoggedBuilder;
import wyvc.builder.CompilerLogger.UnsupportedCompilerError;
import wyvc.builder.DataFlowGraph.DataNode;
import wyvc.builder.DataFlowGraph.FuncCallNode;
import wyvc.builder.DataFlowGraph.HalfArrow;
import wyvc.builder.DataFlowGraph.UndefConstNode;
import wyvc.builder.LexicalElementTree.Compound;
import wyvc.builder.LexicalElementTree.Primitive;
import wyvc.builder.LexicalElementTree.Structure;
import wyvc.builder.LexicalElementTree.Tree;
import wyvc.builder.TypeCompiler.PrimitiveType;
import wyvc.builder.TypeCompiler.RecordStructure;
import wyvc.builder.TypeCompiler.CompoundType;
import wyvc.builder.TypeCompiler.TypeTree;
import wyvc.builder.TypeCompiler.UnionStructure;
import wyvc.lang.Type;
import wyvc.utils.Generator;
import wyvc.utils.Generator.CheckedGenerator;
import wyvc.utils.Pair;
import wyvc.utils.Triple;
import wyvc.utils.Utils;

public final class DataFlowGraphBuilder {


	public static interface NodeTree extends Tree<NodeTree, HalfArrow> {
		public TypeTree getType();
		public NodeTree getParent();
		public void setParent(NodeTree parent);
	}

	public static class PrimitiveNode extends Primitive<NodeTree, HalfArrow> implements NodeTree {
		private final TypeTree type;
		private NodeTree parent = null;

		public PrimitiveNode(DataNode decl) {
			super(new HalfArrow(decl));
			type = new PrimitiveType(decl.type);
		}

		public PrimitiveNode(DataNode decl, String ident) {
			super(new HalfArrow(decl, ident));
			type = new PrimitiveType(decl.type);
		}

		public PrimitiveNode(HalfArrow decl, String ident) {
			this(decl.node, ident);
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

	public static class CompoundNode<S extends Structure<NodeTree, HalfArrow>> extends Compound<NodeTree, HalfArrow, S> implements NodeTree {
		private final TypeTree type;
		private NodeTree parent = null;

//		public CompoundNode(List<Pair<String, NodeTree>> components, TypeTree type) {
//			super(components);
//			this.type = type;
//		}

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

//	public static class UnionNode extends CompoundNode {
//		public final UnionType type;
//		public final List<Pair<PrimitiveNode, NodeTree>> options;
//
//		public UnionNode(List<Pair<PrimitiveNode, NodeTree>> options, UnionType type) {
//			super(new Generator<Pair<String, NodeTree>>() {
//				protected void generate() throws InterruptedException {
//					int k = 0;
//					for(Pair<PrimitiveNode, NodeTree> p : options){
//						yield(new Pair<>(UnionType.FLAG_PREFIX+k, p.first));
//						yield(new Pair<>(UnionType.TYPE_PREFIX+k++, p.second));
//					}
//
//				}}.toList(), type);
//			this.type = type;
//			this.options = options;
//		}
//
//	}


//	private static List<Pair<PrimitiveNode, NodeTree>> options(CompoundNode node) {
//		assert node.getType() instanceof UnionType;
//		return new Generator<Pair<PrimitiveNode, NodeTree>>(){
//			@Override
//			protected void generate() throws InterruptedException {
//
//			}}.toList();
//	}



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
		private final CompoundType<UnionStructure<TypeTree,Type>> type;

		public UnrelatedTypeCompilerError(CompoundType<UnionStructure<TypeTree,Type>> type, NodeTree value) {
			this.value = value;
			this.type = type;
		}

		@Override
		public String info() {
			return "The value "+value+" of type "+value.getType().toString()+"\ncan't be interpreted as part of the UnionType "+type.toString();
		}

		public static CompilerException exception(CompoundType<UnionStructure<TypeTree,Type>> type, NodeTree value) {
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

	private static final class Builder extends LoggedBuilder {
		private class PartialReturn {
			private final HalfArrow cond;
			private final Location<If> ifs;
			private PartialReturn tPart;
			private PartialReturn fPart;
			private final List<NodeTree> ret;

			public PartialReturn(Location<If> ifs, HalfArrow cond, PartialReturn tPart, PartialReturn fPart) {
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


		private final Map<String, TypeTree> types;
		private Map<Integer, NodeTree> vars = new HashMap<>();
		private PartialReturn partialReturn = null;
		private final TypeTree[] returnTypes;
		private final DataFlowGraph graph;
		private Map<wyil.lang.Type, wyvc.lang.Type> compiledTypes = new HashMap<>();

		private String level = "";
		private Stack<String> block = new Stack<>();
		private void writeLevel(boolean open) {
			String a = "──────────────────────────────────────────────────";
			debug(level+(open ? "┌─" : "└─")+a.substring(0, Math.max(0, 30-level.length()))+" "+
					block.lastElement()+" "+a.substring(0, Math.max(0, 35-block.lastElement().length())));
		}
		private void openLevel(String n) {
			block.push(n);
			writeLevel(true);
			level = level+"│ ";
		}
		private void closeLevel() {
			level = level.substring(0, Math.max(0,level.length()-2));
			writeLevel(false);
			block.pop();
		}
		private <T> T end(T a) {
/**/			closeLevel();
			return a;
		}

		private Type isNodeType(TypeTree type) throws CompilerException {
			if (type instanceof CompoundType)
				CompoundTypeCompilerError.exception(type);
			return type.getValue();
		}

		private Type getNodeType(wyil.lang.Type type) throws CompilerException {
			return Utils.checkedAddIfAbsent(compiledTypes, type, () -> isNodeType(TypeCompiler.compileType(logger, type, types)));

		}


		public DataFlowGraph getGraph() {
			return graph;
		}


		@SuppressWarnings("unchecked")
		public Builder(CompilerLogger logger, Map<String, TypeTree> types, WyilFile.FunctionOrMethod func) throws CompilerException {
			super(logger);
			this.types = types;
			graph = new DataFlowGraph();
			Utils.checkedForEach(
				Arrays.asList(func.type().params()),
				(Integer k, wyil.lang.Type t) -> Utils.ignore(vars.put(k, buildParameter(
					((Location<Bytecode.VariableDeclaration>)func.getTree().getLocation(k)).getBytecode().getName(),
					TypeCompiler.compileType(logger, t, types)))));
			returnTypes = new TypeTree[func.type().returns().length];
			Utils.checkedConvert(
				Arrays.asList(func.type().returns()),
				(wyil.lang.Type t) ->  TypeCompiler.compileType(logger, t, types)).toArray(returnTypes);
			build(func.getBody());
			partialReturn.print("RET ");
			Utils.checkedForEach(
				partialReturn.getReturn(),
				(Integer k, NodeTree r) -> buildReturnValue("ret_"+ k, r));
		}

		private void buildReturnValue(String ident, NodeTree ret) {
			if (ret instanceof PrimitiveNode)
				graph.new OutputNode(ident, ret.getValue());
			else ret.getComponents().forEach((Pair<String, NodeTree> p) -> buildReturnValue(ident+"_"+p.first, p.second));
		}


		private DataNode buildParameter(String ident, Type type) {
			return graph.new InputNode(ident, type);
		}
		private Structure<NodeTree, HalfArrow> buildParameter(String ident, Structure<TypeTree, Type> structure) throws CompilerException {
			if (structure instanceof RecordStructure<?,?>)
				return new <CompilerException>RecordStructure<NodeTree, HalfArrow>(((RecordStructure<TypeTree, Type>) structure).getComponents().Map(
					(Pair<String, TypeTree> p) -> new Pair<>(p.first, buildParameter(ident+"_"+p.first, p.second))));
			if (structure instanceof UnionStructure<?,?>)
				return new <CompilerException>UnionStructure<NodeTree, HalfArrow>(((UnionStructure<TypeTree, Type>) structure).getOptions().Map(
					(Integer k, Pair<TypeTree,TypeTree> p) -> new Pair<>(
							buildParameter(ident+"_"+UnionStructure.FLG_PREFIX+k, p.first),
							buildParameter(ident+"_"+UnionStructure.VAL_PREFIX+k, p.second))));
			throw UnsupportedStructureConversionCompilerError.exception(structure);
		}
		private NodeTree buildParameter(String ident, TypeTree type) throws CompilerException {
			return type instanceof PrimitiveType ? new PrimitiveNode(buildParameter(ident, type.getValue()), ident)
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

		@SuppressWarnings("unchecked")
		private NodeTree buildField(CompoundNode<?> struct, NodeTree current, NodeTree compo) throws CompilerException {
			if (struct.getStructure() instanceof RecordStructure<?, ?>)
				return new CompoundNode<>(new RecordStructure<>(struct.getStructure().getComponents().map(
					(Pair<String,NodeTree> p) -> p.transformSecond((NodeTree n) -> n == current ? compo : n))), struct.getType());
			if (struct.getStructure() instanceof UnionStructure<?, ?>)
				return new CompoundNode<>(new UnionStructure<>(((UnionStructure<NodeTree, HalfArrow>)struct.getStructure()).getOptions().map(
					(Pair<NodeTree, NodeTree> p) -> new Pair<>(p.first == current ? compo : p.first, p.second == current ? compo : p.second)
						)), struct.getType());
			throw UnsupportedStructureConversionCompilerError.exception(struct.getStructure());
		}

		private void buildAssignField(int op, NodeTree var, NodeTree val) throws CompilerException {
//			debug("Var "+op + " " +var);
			if (var == null || var.getParent() == null)
				vars.put(op, val);
			else
				buildAssignField(op, var.getParent(), buildField((CompoundNode<?>) var.getParent(), var, val));
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


		private Structure<NodeTree, HalfArrow> buildNamedHalfArrow(String ident, Structure<NodeTree, HalfArrow> structure) throws CompilerException {
			if (structure instanceof RecordStructure<?,?>)
				return new <CompilerException>RecordStructure<NodeTree, HalfArrow>(((RecordStructure<NodeTree, HalfArrow>) structure).getComponents().Map(
					(Pair<String, NodeTree> p) -> new Pair<>(p.first, buildNamedHalfArrow(ident+"_"+p.first, p.second))));
			if (structure instanceof UnionStructure<?,?>)
				return new <CompilerException>UnionStructure<NodeTree, HalfArrow>(((UnionStructure<NodeTree, HalfArrow>) structure).getOptions().Map(
					(Integer k, Pair<NodeTree,NodeTree> p) -> new Pair<>(
							buildNamedHalfArrow(ident+"_"+UnionStructure.FLG_PREFIX+k, p.first),
							buildNamedHalfArrow(ident+"_"+UnionStructure.VAL_PREFIX+k, p.second))));
			throw UnsupportedStructureConversionCompilerError.exception(structure);
		}

		private NodeTree buildNamedHalfArrow(String ident, NodeTree node) throws CompilerException {
			return node instanceof PrimitiveNode ? new PrimitiveNode(node.getValue(), ident)
			                                     : new CompoundNode<>(buildNamedHalfArrow(ident, node.getStructure()), node.getType());
		}


		private void buildAssignValue(Location<?> acc, NodeTree val) throws CompilerException {
//			openLevel("Assign");
//			Utils.printLocation(logger, acc, level);
//			val.printStructure(logger, level);
			Pair<Location<Bytecode.VariableDeclaration>,String> p = buildAssignName(acc);
			Pair<Integer, NodeTree> q = recoverIdent(acc);
			buildAssignField(q.first, q.second , buildNamedHalfArrow(p.second, buildTypedValue(val, TypeCompiler.compileType(logger, acc.getType(), types))));
//			closeLevel();
		}

		private void buildAssign(Location<Bytecode.Assign> assign) throws CompilerException {
			Location<?>[] lhs = assign.getOperandGroup(SyntaxTree.LEFTHANDSIDE);
			Location<?>[] rhs = assign.getOperandGroup(SyntaxTree.RIGHTHANDSIDE);
			List<NodeTree> brhs = buildTuple(rhs);
			for(int k = 0; k < lhs.length; ++k)
				buildAssignValue(lhs[k], brhs.get(k));
		}

		private UndefConstNode buildUndefinedValue(Type type) {
			return graph.new UndefConstNode(type);
		}

		private Structure<NodeTree, HalfArrow> buildUndefinedValue(Structure<TypeTree, Type> structure) throws CompilerException {
			if (structure instanceof RecordStructure<?,?>)
				return new <CompilerException>RecordStructure<NodeTree, HalfArrow>(((RecordStructure<TypeTree, Type>) structure).getComponents().Map(
					(Pair<String, TypeTree> p) -> new Pair<>(p.first, buildUndefinedValue(p.second))));
			if (structure instanceof UnionStructure<?,?>)
				return new <CompilerException>UnionStructure<NodeTree, HalfArrow>(((UnionStructure<TypeTree, Type>) structure).getOptions().Map(
					(Pair<TypeTree,TypeTree> p) -> new Pair<>(
							buildUndefinedValue(p.first),
							buildUndefinedValue(p.second))));
			throw UnsupportedStructureConversionCompilerError.exception(structure);
		}

		private NodeTree buildUndefinedValue(TypeTree type) throws CompilerException {
			return type instanceof PrimitiveType ? new PrimitiveNode(buildUndefinedValue(type.getValue()))
			                                     : new CompoundNode<>(buildUndefinedValue(type.getStructure()), type);
		}

		private NodeTree buildUnionValue(CompoundType<UnionStructure<TypeTree,Type>> type, NodeTree value) throws CompilerException {
			List<Pair<TypeTree, Pair<NodeTree, NodeTree>>> cps =  value instanceof CompoundNode<?> && value.getStructure() instanceof UnionStructure<?,?>
				? ((UnionStructure<NodeTree, HalfArrow>) value.getStructure()).getOptions().map((Pair<NodeTree, NodeTree> p) -> new Pair<>(p.second.getType(), p)).toList()
				: Collections.singletonList(new Pair<>(value.getType(), new Pair<>(new PrimitiveNode(graph.getTrue()),value)));
			return new CompoundNode<>(new <CompilerException>UnionStructure<NodeTree, HalfArrow>(
				type.getStructure().getOptions().map(
					(Pair<TypeTree,TypeTree> p) -> new Pair<>(
						p.second,
						Generator.fromPairCollection(cps).find(
							(Pair<TypeTree, Pair<NodeTree, NodeTree>> c) -> c.first.equals(p.second)))).Map(
					(Pair<TypeTree, Pair<TypeTree, Pair<NodeTree, NodeTree>>> p) -> p.second == null
						? new Pair<>(new PrimitiveNode(graph.getFalse()),buildUndefinedValue(p.first))
						: p.second.second)), type);
		}

		@SuppressWarnings("unchecked")
		private NodeTree buildTypedValue(NodeTree node, TypeTree type) throws CompilerException {
//			debug("Val "+(node == null ? "NULL" : node.getType()));
			if (node.getType().equals(type))
				return node;
			if (type instanceof CompoundType<?> && type.getStructure() instanceof UnionStructure<?, ?>)
				return buildUnionValue((CompoundType<UnionStructure<TypeTree,Type>>) type, node);
			throw new CompilerException(null);
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
			vars.put(decl.getIndex(),
				decl.numberOfOperands() == 1 ? buildNamedHalfArrow(
					decl.getBytecode().getName(),
					buildTypedValue(buildExpression(decl.getOperand(0)), TypeCompiler.compileType(logger, decl.getType(), types)))
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
			if (!(value instanceof CompoundNode) || !(value.getStructure() instanceof UnionStructure))
				return new PrimitiveNode(value.getType().equals(type) ? graph.getTrue() : graph.getFalse());
			List<TypeTree> types = (type instanceof CompoundType<?> && type.getStructure() instanceof UnionStructure<?,?>)
					? ((UnionStructure<TypeTree, Type>) type.getStructure()).getOptions().takeSecond().toList() : Collections.singletonList(type);
			DataNode cond = ((UnionStructure<NodeTree, HalfArrow>) value.getStructure()).getOptions().fold(
				(DataNode d, Pair<NodeTree,NodeTree> p) -> {
					//System.out.println("Cas "+p+" : " + p.first);
					for (TypeTree t : types)
						if (t.equals(p.second.getType()))
							return d == null
								? p.first.getValue().node
								: graph.new BinOpNode(is, Type.Boolean, new HalfArrow(d), p.first.getValue());
					return d;

				}, null);
			return new PrimitiveNode(cond == null ? graph.getFalse() : cond);
		}

		private NodeTree buildOperator(Location<Bytecode.Operator> op) throws CompilerException {
//			debug("Operator Compilation");
//			debug("Operator "+a);
//			a.printStructure(logger, "  ");
			if (op.getBytecode().kind() == OperatorKind.RECORDCONSTRUCTOR)
				return new CompoundNode<>(new <CompilerException>RecordStructure<NodeTree, HalfArrow>(
						Generator.fromCollection(((wyil.lang.Type.EffectiveRecord)op.getType()).getFieldNames()).gather(
							Generator.fromCollection(op.getOperands())).Map((Pair<String, Location<?>> p) -> new Pair<>(p.first, buildExpression(p.second)))),
						TypeCompiler.compileType(logger, op.getType(), types));
			if (op.getBytecode().kind() == OperatorKind.IS)
				return buildIs(op, buildExpression(op.getOperand(0)), TypeCompiler.compileType(logger, ((Constant.Type)(((Const)(op.getOperand(1).getBytecode())).constant())).value(), types));
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
				TypeTree type = TypeCompiler.compileType(logger, var.getType(), types);
				NodeTree node = buildVariableAccess(var.getOperand(0));
				if (!(node instanceof CompoundNode<?>))
					return node;
				List<TypeTree> types = (type instanceof CompoundType<?> && type.getStructure() instanceof UnionStructure<?,?>)
						? ((UnionStructure<TypeTree, Type>) type.getStructure()).getOptions().takeSecond().toList()
						: Collections.singletonList(type);
//				debug("union "+node);
//				node.printStructure(logger, "  ");
				List<Pair<NodeTree, NodeTree>> newOpt = ((UnionStructure<NodeTree, HalfArrow>) node.getStructure()).getOptions().map((Pair<NodeTree, NodeTree> p) -> {
					for (TypeTree t : types)
						if (t.equals(p.second.getType()))
							return p;
					return null;}).remove(null).toList();
				return newOpt.size() == 1
						? newOpt.get(0).second
						: new CompoundNode<>(new UnionStructure<>(newOpt),type);
			}
			throw WyilUnsupportedCompilerError.exception(var);
		}


		private DataNode buildCallReturn(String ident, Type type, FuncCallNode func) {
			return graph.new FunctionReturnNode(ident, type, func);
		}
		private Structure<NodeTree, HalfArrow> buildCallReturn(String ident, Structure<TypeTree, Type> structure, FuncCallNode func) throws CompilerException {
			if (structure instanceof RecordStructure<?,?>)
				return new <CompilerException>RecordStructure<NodeTree, HalfArrow>(((RecordStructure<TypeTree, Type>) structure).getComponents().Map(
					(Pair<String, TypeTree> p) -> new Pair<>(p.first, buildCallReturn(ident+"_"+p.first, p.second, func))));
			if (structure instanceof UnionStructure<?,?>)
				return new <CompilerException>UnionStructure<NodeTree, HalfArrow>(((UnionStructure<TypeTree, Type>) structure).getOptions().Map(
					(Integer k, Pair<TypeTree,TypeTree> p) -> new Pair<>(
							buildCallReturn(ident+"_"+UnionStructure.FLG_PREFIX+k, p.first, func),
							buildCallReturn(ident+"_"+UnionStructure.VAL_PREFIX+k, p.second, func))));
			throw UnsupportedStructureConversionCompilerError.exception(structure);
		}
		private NodeTree buildCallReturn(String ident, TypeTree type, FuncCallNode func) throws CompilerException {
			return type instanceof PrimitiveType ? new PrimitiveNode(buildCallReturn(ident, type.getValue(), func), ident)
			                                     : new CompoundNode<>(buildCallReturn(ident, type.getStructure(), func), type);
		}

		private List<NodeTree> buildInvoke(Location<Invoke> call) throws CompilerException {
			FuncCallNode c = graph.new FuncCallNode(call,
				Utils.checkedConvert(
					Arrays.asList(call.getOperands()),
				    (Location<?> l, Integer t) -> buildNamedHalfArrow("arg_"+t, buildExpression(l)).getValue()));
			return Utils.checkedConvert(
				Arrays.asList(call.getBytecode().type().returns()),
				(wyil.lang.Type t, Integer i) -> buildCallReturn("ret_"+i, TypeCompiler.compileType(logger, t, types), c));
		}


		private  DataNode buildEndIf(Location<If> ifs, HalfArrow ifn, HalfArrow trueLab, HalfArrow falseLab) {
			return graph.new EndIfNode(ifs, ifn, trueLab, falseLab);
		}
		private  Structure<NodeTree, HalfArrow> buildEndIf(Location<If> ifs, HalfArrow ifn, Structure<NodeTree, HalfArrow> trueLab, Structure<NodeTree, HalfArrow> falseLab) throws CompilerException {
			if (trueLab instanceof RecordStructure<?,?> && falseLab instanceof RecordStructure<?,?>)
				return new <CompilerException>RecordStructure<NodeTree, HalfArrow>(((RecordStructure<NodeTree, HalfArrow>) trueLab).getComponents().gather(
					((RecordStructure<NodeTree, HalfArrow>) falseLab).getComponents()).Map(
					(Pair<Pair<String, NodeTree>,Pair<String, NodeTree>> p) -> new Pair<>(p.first.first, buildEndIf(ifs, ifn, p.first.second, p.second.second))));
			if (trueLab instanceof UnionStructure<?,?> && falseLab instanceof UnionStructure<?,?>)
				return new <CompilerException>UnionStructure<NodeTree, HalfArrow>(((UnionStructure<NodeTree, HalfArrow>) trueLab).getOptions().gather(
					((UnionStructure<NodeTree, HalfArrow>) falseLab).getOptions()).Map(
					(Pair<Pair<NodeTree,NodeTree>,Pair<NodeTree,NodeTree>> p) -> new Pair<>(
							buildEndIf(ifs, ifn, p.first.first, p.second.first),
							buildEndIf(ifs, ifn, p.first.second, p.second.second))));
			throw UncompatibleStructuresCompilerError.exception(trueLab, falseLab);
		}
		private  NodeTree buildEndIf(Location<If> ifs, HalfArrow ifn, NodeTree trueLab, NodeTree falseLab) throws CompilerException {
			trueLab.checkIdenticalStructure(falseLab);
			return trueLab instanceof PrimitiveNode ? new PrimitiveNode(buildEndIf(ifs, ifn, trueLab.getValue(), falseLab.getValue()))
			                                        : new CompoundNode<>(buildEndIf(ifs, ifn, trueLab.getStructure(), falseLab.getStructure()), trueLab.getType());
		}



		private Structure<NodeTree, HalfArrow> copyNamedHalfArrow(Structure<NodeTree, HalfArrow> name, Structure<NodeTree, HalfArrow> structure) throws CompilerException {
			if (structure instanceof RecordStructure<?,?> && name instanceof RecordStructure<?,?>)
				return new <CompilerException>RecordStructure<NodeTree, HalfArrow>(((RecordStructure<NodeTree, HalfArrow>) name).getComponents().gather(
					((RecordStructure<NodeTree, HalfArrow>) structure).getComponents()).Map(
					(Pair<Pair<String, NodeTree>,Pair<String, NodeTree>> p) -> new Pair<>(p.first.first, copyNamedHalfArrow(p.first.second, p.second.second))));
			if (structure instanceof UnionStructure<?,?> && name instanceof UnionStructure<?,?>)
				return new <CompilerException>UnionStructure<NodeTree, HalfArrow>(((UnionStructure<NodeTree, HalfArrow>) name).getOptions().gather(
					((UnionStructure<NodeTree, HalfArrow>) structure).getOptions()).Map(
					(Pair<Pair<NodeTree,NodeTree>,Pair<NodeTree,NodeTree>> p) -> new Pair<>(
							copyNamedHalfArrow(p.first.first, p.second.first),
							copyNamedHalfArrow(p.first.second, p.second.second))));
			throw UncompatibleStructuresCompilerError.exception(name, structure);
		}

		private NodeTree copyNamedHalfArrow(NodeTree name, NodeTree node) throws CompilerException {
			name.checkIdenticalStructure(node);
			return node instanceof PrimitiveNode ? new PrimitiveNode(node.getValue(), name.getValue().ident)
			                                     : new CompoundNode<>(copyNamedHalfArrow(name.getStructure(), node.getStructure()), node.getType());
		}

		private void buildIf(Location<If> ifs) throws CompilerException {
			NodeTree cond = buildExpression(ifs.getOperand(0));

			HalfArrow ifn = cond.getValue(); // TODO verif bool primitif.

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
			Utils.checkedForEach(
				state,
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






	public static DataFlowGraph buildGraph(CompilerLogger logger, FunctionOrMethod func, Map<String, TypeTree> types) throws CompilerException {
		return new Builder(logger, types, func).getGraph();
	}
}
