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
import wyil.lang.Type;
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
import wyvc.builder.LexicalElementTree.Tree;
import wyvc.builder.TypeCompiler.PrimitiveType;
import wyvc.builder.TypeCompiler.CompoundType;
import wyvc.builder.TypeCompiler.TypeTree;
import wyvc.builder.TypeCompiler.UnionType;
import wyvc.utils.CheckedGenerator;
import wyvc.utils.Generator;
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

	public static class CompoundNode extends Compound<NodeTree, HalfArrow> implements NodeTree {
		private final TypeTree type;
		private NodeTree parent = null;

		public CompoundNode(List<Pair<String, NodeTree>> components, TypeTree type) {
			super(components);
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

	public static class UnionNode extends CompoundNode {
		public final UnionType type;
		public final List<Pair<PrimitiveNode, NodeTree>> options;

		public UnionNode(List<Pair<PrimitiveNode, NodeTree>> options, UnionType type) {
			super(new Generator<Pair<String, NodeTree>>() {
				protected void generate() throws InterruptedException {
					int k = 0;
					for(Pair<PrimitiveNode, NodeTree> p : options){
						yield(new Pair<>(UnionType.FLAG_PREFIX+k, p.first));
						yield(new Pair<>(UnionType.TYPE_PREFIX+k++, p.second));
					}

				}}.toList(), type);
			this.type = type;
			this.options = options;
		}

	}


	private static List<Pair<PrimitiveNode, NodeTree>> options(CompoundNode node) {
		assert node.getType() instanceof UnionType;
		return new Generator<Pair<PrimitiveNode, NodeTree>>(){
			@Override
			protected void generate() throws InterruptedException {

			}}.toList();
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

	}


	public static class UnrelatedTypeCompilerError extends CompilerError {
		private final NodeTree value;
		private final UnionType type;

		public UnrelatedTypeCompilerError(UnionType type, NodeTree value) {
			this.value = value;
			this.type = type;
		}

		@Override
		public String info() {
			return "The value "+value+" of type "+value.getType().toString()+"\ncan't be interpreted as part of the UnionType "+type.toString();
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
				logger.debug("Return ? "+this+" "+ret+" "+tPart+" "+fPart);
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
		private Map<Type, wyvc.lang.Type> compiledTypes = new HashMap<>();

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

		private wyvc.lang.Type isNodeType(TypeTree type) throws CompilerException {
			if (type instanceof CompoundType)
				throw new CompilerException(new CompoundTypeCompilerError(type));
			return type.getValue();
		}

		private wyvc.lang.Type getNodeType(Type type) throws CompilerException {
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
				(Integer k, Type t) -> Utils.ignore(vars.put(k, buildParameter(
					((Location<Bytecode.VariableDeclaration>)func.getTree().getLocation(k)).getBytecode().getName(),
					TypeCompiler.compileType(logger, t, types)))));
			returnTypes = new TypeTree[func.type().returns().length];
			Utils.checkedConvert(
				Arrays.asList(func.type().returns()),
				(Type t) ->  TypeCompiler.compileType(logger, t, types)).toArray(returnTypes);
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

		private NodeTree buildParameter(String ident, TypeTree type) {
			return type instanceof PrimitiveType ? new PrimitiveNode(graph.new InputNode(ident, type.getValue()), ident)
			                                     : type instanceof UnionType ? new UnionNode(Utils.convert(
			                                    	 ((UnionType)type).getNamedOptions(),
			                                    	 (Triple<String,String,TypeTree> t) -> new Pair<PrimitiveNode, NodeTree>(
			                                    			 new PrimitiveNode(graph.new InputNode(ident+"_"+t.first, wyvc.lang.Type.Boolean), ident+"_"+t.first),
			                                    			 buildParameter(ident+"_"+t.second, t.third))), (UnionType)type)
			                                     : new CompoundNode(Utils.convert(type.getComponents(),
			                                    	 (Pair<String, TypeTree> p) -> new Pair<>(
			                                    			 p.first, buildParameter(ident+"_"+p.first, p.second))), type);
		}


		@SuppressWarnings("unchecked")
		public void build(Location<?> location) throws CompilerException {
			openLevel("Build");
			Utils.printLocation(logger, location, level);
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
				throw new CompilerException(new WyilUnsupportedCompilerError(location));
			closeLevel();
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
			throw new CompilerException(new WyilUnsupportedCompilerError(field));
		}

		private NodeTree buildField(NodeTree struct, NodeTree current, NodeTree compo) {
			ArrayList<Pair<String, NodeTree>> cmps = new ArrayList<>();
			cmps.addAll(struct.getComponents());
			for (Pair<String, NodeTree> c : cmps)
				if (c.second == current)
					c.second = compo;
			return new CompoundNode(cmps, struct.getType());
		}

		private void buildAssignField(int op, NodeTree var, NodeTree val) {
//			debug("Var "+op + " " +var);
			if (var == null || var.getParent() == null)
				vars.put(op, val);
			else
				buildAssignField(op, var.getParent(), buildField(var.getParent(), var, val));
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
			throw new CompilerException(new WyilUnsupportedCompilerError(field));
		}

		private NodeTree buildNamedHalfArrow(String ident, NodeTree node) {
			return node instanceof PrimitiveNode ? new PrimitiveNode(node.getValue(), ident)
			                                     : new CompoundNode(Utils.convert(node.getComponents(),
			                                    	 (Pair<String, NodeTree> p) -> new Pair<>(p.first, buildNamedHalfArrow(ident+"_"+p.first, p.second))), node.getType());
		}


		private void buildAssignValue(Location<?> acc, NodeTree val) throws CompilerException {
/**/			openLevel("Assign");
/**/			Utils.printLocation(logger, acc, level);
/**/			val.printStructure(logger, level);
			Pair<Location<Bytecode.VariableDeclaration>,String> p = buildAssignName(acc);
			Pair<Integer, NodeTree> q = recoverIdent(acc);
			buildAssignField(q.first, q.second , buildNamedHalfArrow(p.second, buildTypedValue(val, TypeCompiler.compileType(logger, acc.getType(), types))));
/**/			closeLevel();
		}

		private void buildAssign(Location<Bytecode.Assign> assign) throws CompilerException {
			Location<?>[] lhs = assign.getOperandGroup(SyntaxTree.LEFTHANDSIDE);
			Location<?>[] rhs = assign.getOperandGroup(SyntaxTree.RIGHTHANDSIDE);
			List<NodeTree> brhs = buildTuple(rhs);
			for(int k = 0; k < lhs.length; ++k)
				buildAssignValue(lhs[k], brhs.get(k));
		}
		private PrimitiveNode buildUndefinedValue(wyvc.lang.Type type) {
			return new PrimitiveNode(graph.new UndefConstNode(type));
		}

		private NodeTree buildUndefinedValue(TypeTree type) {
			if (type instanceof PrimitiveType) {
				return buildUndefinedValue(type.getValue());
			}
			return new CompoundNode(Utils.checkedConvert(
				type.getComponents(),
				(Pair<String, TypeTree> p) -> new Pair<>(p.first, buildUndefinedValue(p.second))), type);
		}

		private NodeTree buildUnionValue(UnionType type, NodeTree value) throws CompilerException {
			List<Pair<TypeTree, Pair<PrimitiveNode, NodeTree>>> cps =  value instanceof UnionNode
				? Utils.convert(((UnionNode) value).options, (Pair<PrimitiveNode, NodeTree> p) -> new Pair<>(p.second.getType(), p))
				: Collections.singletonList(new Pair<>(value.getType(), new Pair<>(new PrimitiveNode(graph.getTrue()),value)));
			return new UnionNode(new Generator<Pair<PrimitiveNode, NodeTree>>() {
				@Override
				protected void generate() throws InterruptedException {
					for (TypeTree t : type.getOptions()) {
						Pair<PrimitiveNode, NodeTree> n = null;
						for (int k = 0; k<cps.size(); ++k)
							if (cps.get(k).first.equals(t))
								n = cps.get(k).second;
						if (n == null)
							n = new Pair<>(new PrimitiveNode(graph.getFalse()),buildUndefinedValue(t));
						yield(n);
					}
				}}.toList(), type);
		}

		private NodeTree buildTypedValue(NodeTree node, TypeTree type) throws CompilerException {
			if (node.getType().equals(type))
				return node;
			if (type instanceof UnionType)
				return buildUnionValue((UnionType) type, node);
			throw new CompilerException(null);
		}

		private void buildReturn(Location<Bytecode.Return> ret) throws CompilerException {
			List<NodeTree> retv = Utils.checkedConvert(
				buildTuple(ret.getOperands()),
				(NodeTree t, Integer k) -> buildTypedValue(t, returnTypes[k]));
			if (partialReturn != null)
				partialReturn.completeReturn(retv);
			else
				partialReturn = new PartialReturn(retv);
		}

		private void buildDecl(Location<Bytecode.VariableDeclaration> decl) throws CompilerException {
/**/			debug("Declaration de "+decl.getBytecode().getName()+" en "+decl.getIndex());
			vars.put(decl.getIndex(), // compound + c'est pas ça
				decl.numberOfOperands() == 1 ? buildNamedHalfArrow(
					decl.getBytecode().getName(),
					buildTypedValue(buildExpression(decl.getOperand(0)), TypeCompiler.compileType(logger, decl.getType(), types)))
				                             : null);
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
			List<TypeTree> types = (type instanceof UnionType) ? ((UnionType) type).getOptions() : Collections.singletonList(type);
			DataNode cond = null;
			if (! (value instanceof UnionNode))
				return new PrimitiveNode(value.getType().equals(type) ? graph.getTrue() : graph.getFalse());
			for (Pair<PrimitiveNode, NodeTree> o : ((UnionNode) value).options)
				for (TypeTree t : types)
					if (t.equals(o.second.getType()))
						cond = cond == null
							? o.first.value.node
							: graph.new BinOpNode(is, wyvc.lang.Type.Boolean, new HalfArrow(cond), o.first.value);
			return new PrimitiveNode(cond == null ? graph.getFalse() : cond);
		}

		private NodeTree buildOperator(Location<Bytecode.Operator> op) throws CompilerException {
//			debug("Operator Compilation");
//			debug("Operator "+a);
//			a.printStructure(logger, "  ");
			if (op.getBytecode().kind() == OperatorKind.RECORDCONSTRUCTOR) {
				int k = 0;
				ArrayList<Pair<String, NodeTree>> fields = new ArrayList<>();
				for (String f : ((Type.EffectiveRecord)op.getType()).getFieldNames())
					fields.add(new Pair<>(f, buildExpression(op.getOperand(k++))));
				return new CompoundNode(Utils.convert(fields), TypeCompiler.compileType(logger, op.getType(), types));
			}
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
			if (bytecode instanceof VariableDeclaration)
				return vars.get(var.getOpcode());
			if (bytecode instanceof AliasDeclaration) {
				TypeTree type = TypeCompiler.compileType(logger, var.getType(), types);
				NodeTree node = buildVariableAccess(var.getOperand(0));
				if (! (node instanceof CompoundNode))
					return node;
				List<Pair<PrimitiveNode, NodeTree>> nc = new ArrayList<>();
				List<TypeTree> types = (type instanceof UnionType) ? ((UnionType) type).getOptions() : Collections.singletonList(type);
				debug("union"+node);
				node.printStructure(logger, "  ");
				for (Pair<PrimitiveNode, NodeTree> p : ((UnionNode) node).options)
					for (TypeTree t : types)
						if (t.equals(p.second.getType()))
							nc.add(p);
				return nc.size() == 1 ? nc.get(0).second : new UnionNode(nc, (UnionType) type);
			}
			throw new CompilerException(new WyilUnsupportedCompilerError(var));
		}



		private NodeTree buildCallReturn(String ident, TypeTree type, FuncCallNode func) {
			return type instanceof PrimitiveType ? new PrimitiveNode(graph.new FunctionReturnNode(ident, type.getValue(), func), ident)
			                                     : new CompoundNode(Utils.convert(type.getComponents(),
			                                    	 (Pair<String, TypeTree> p) -> new Pair<>(
			                                    			 p.first, buildCallReturn(ident+"_"+p.first, p.second, func))), type);
		}

		private List<NodeTree> buildInvoke(Location<Invoke> call) throws CompilerException {
			FuncCallNode c = graph.new FuncCallNode(call,
				Utils.checkedConvert(
					Arrays.asList(call.getOperands()),
				    (Location<?> l, Integer t) -> buildNamedHalfArrow("arg_"+t, buildExpression(l)).getValue()));
			return Utils.checkedConvert(
				Arrays.asList(call.getBytecode().type().returns()),
				(Type t, Integer i) -> buildCallReturn("ret_"+i, TypeCompiler.compileType(logger, t, types), c));
		}


		private  NodeTree buildEndIf(Location<If> ifs, HalfArrow ifn, NodeTree trueLab, NodeTree falseLab) throws CompilerException {
			//debug("Oh, end if ! "+trueLab+" "+falseLab);
			trueLab.checkIdenticalStructure(falseLab);
			return trueLab instanceof PrimitiveNode ? new PrimitiveNode(graph.new EndIfNode(ifs, ifn, trueLab.getValue(), falseLab.getValue()))
			                                        : new CompoundNode(Utils.checkedConvert(
		                                        		Utils.gather(trueLab.getComponents(),falseLab.getComponents()),
			                                        	(Pair<Pair<String, NodeTree>,Pair<String, NodeTree>> p) ->
			                                        	new Pair<>(p.first.first, buildEndIf(ifs, ifn, p.first.second, p.second.second))), trueLab.getType());
		}

		private NodeTree copyNamedHalfArrow(NodeTree name, NodeTree node) throws CompilerException {
			return node instanceof PrimitiveNode ? new PrimitiveNode(node.getValue(), name.getValue().ident)
			                                     : new CompoundNode(Utils.checkedConvert(node.getComponents(),
			                                    	 (Pair<String, NodeTree> p) -> new Pair<>(p.first, copyNamedHalfArrow(name.getComponent(p.first), p.second))), node.getType());
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
