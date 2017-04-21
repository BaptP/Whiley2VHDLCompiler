package wyvc.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import wyil.lang.Bytecode;
import wyil.lang.SyntaxTree;
import wyil.lang.Bytecode.Const;
import wyil.lang.Bytecode.FieldLoad;
import wyil.lang.Bytecode.If;
import wyil.lang.Bytecode.Invoke;
import wyil.lang.Bytecode.OperatorKind;
import wyil.lang.Bytecode.VariableAccess;
import wyil.lang.SyntaxTree.Location;
import wyil.lang.Type;
import wyil.lang.WyilFile;
import wyil.lang.WyilFile.FunctionOrMethod;
import wyvc.builder.ControlFlowGraph;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.builder.CompilerLogger.CompilerNotice;
import wyvc.builder.CompilerLogger.LoggedBuilder;
import wyvc.builder.CompilerLogger.UnsupportedCompilerError;
import wyvc.builder.ControlFlowGraph.DataNode;
import wyvc.builder.ControlFlowGraph.EndIfNode;
import wyvc.builder.ControlFlowGraph.FuncCallNode;
import wyvc.builder.ControlFlowGraph.IfNode;
import wyvc.builder.ControlFlowGraph.LabelNode;
import wyvc.builder.ControlFlowGraph.WyilNode;
import wyvc.builder.ControlFlowGraph.WyilSection;
import wyvc.builder.LexicalElementTree.Compound;
import wyvc.builder.LexicalElementTree.Primitive;
import wyvc.builder.LexicalElementTree.Tree;
import wyvc.builder.TypeCompiler.PrimitiveType;
import wyvc.builder.TypeCompiler.CompoundType;
import wyvc.builder.TypeCompiler.TypeTree;
import wyvc.utils.Pair;
import wyvc.utils.Utils;

public final class ControlFlowGraphBuilder {


	public static interface NodeTree<T extends DataNode> extends Tree<NodeTree<T>, T> {
		public TypeTree getType();
	}

	public static class PrimitiveNode<T extends DataNode> extends Primitive<NodeTree<T>, T> implements NodeTree<T> {
		private final TypeTree type;

		public PrimitiveNode(T decl) {
			super(decl);
			type = decl.type;
		}

		@Override
		public TypeTree getType() {
			return type;
		}
	}

	public static class CompoundNode<T extends DataNode> extends Compound<NodeTree<T>, T> implements NodeTree<T> {
		private final TypeTree type;

		public CompoundNode(List<Pair<String, NodeTree<T>>> components) {
			super(components);
			type = new CompoundType(Utils.convert(components,
				(Pair<String, NodeTree<T>> c) -> new Pair<String, TypeTree>(c.first,c.second.getType())));
		}

		@Override
		public TypeTree getType() {
			return type;
		}
	}

	public static interface LabelTree extends NodeTree<LabelNode> {
		public LabelTree getParent();
		public void setParent(LabelTree parent);
		public String getName();
	}

	public static class PrimitiveLabel extends PrimitiveNode<LabelNode> implements LabelTree {
		private LabelTree parent = null;
		private final String name;

		public PrimitiveLabel(String name, LabelNode decl) {
			super(decl);
			this.name = name;
		}

		// TODO Pas sur.
		public PrimitiveLabel(LabelNode decl) {
			super(decl);
			this.name = decl.label;
		}

		@Override
		public LabelTree getParent() {
			return parent;
		}

		@Override
		public void setParent(LabelTree parent) {
			this.parent = parent;
		}

		@Override
		public String getName() {
			return name;
		}
	}

	public static class CompoundLabel extends CompoundNode<LabelNode> implements LabelTree {
		private LabelTree parent = null;
		private final String name;

		public CompoundLabel(String name, List<Pair<String, LabelTree>> components) {
			super(Utils.convert(components));
			for (Pair<String, LabelTree> p : components)
				p.second.setParent(this);
			this.name = name;
		}

		@Override
		public LabelTree getParent() {
			return parent;
		}

		@Override
		public void setParent(LabelTree parent) {
			this.parent = parent;
		}

		@Override
		public String getName() {
			return name;
		}
	}


	private static class BMap<S,T> extends HashMap<S, T> {
		private static final long serialVersionUID = 1534890860087462075L;

		public T put(S o, T u) {
			//System.out.println("### Modif "+u+" vers "+o);
			return super.put(o, u);
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
	}

	private static final class Builder extends LoggedBuilder {
		private class PartialReturn {
			private final IfNode ifn;
			private PartialReturn tPart;
			private PartialReturn fPart;
			private final List<NodeTree<?>> ret;

			public PartialReturn(IfNode ifn, PartialReturn tPart, PartialReturn fPart) {
				this.ifn = ifn;
				this.tPart = tPart;
				this.fPart = fPart;
				this.ret = null;
			}
			public PartialReturn(List<NodeTree<?>> ret) {
				assert ret != null;
				this.ifn = null;
				this.tPart = null;
				this.fPart = null;
				this.ret = ret;
			}

			public List<NodeTree<?>> getReturn() {
				assert !isPartial();
				return ret == null ? Utils.convert(Utils.gather(tPart.getReturn(), fPart.getReturn()),
					(Pair<NodeTree<?>, NodeTree<?>> p) -> buildEndIf(ifn, p.first, p.second)) : ret;
			}

			public void completeReturn(List<NodeTree<?>> rem) {
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
		}


		private final Map<String, TypeTree> types;
		private Map<Integer, LabelTree> vars = new BMap<>();
		private PartialReturn partialReturn = null;
		private final ControlFlowGraph graph;
		private ArrayList<LabelNode> inputs = new ArrayList<>();

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
			closeLevel();
			return a;
		}





		private <T extends DataNode> List<DataNode> getEnclosingLabelHelper(NodeTree<T> t) {
			return Utils.convert(t.getValues());
		}

		private LabelNode getEnclosingLabel(String ident, NodeTree<?> val) {
			return graph.new LabelNode(ident, val.getType(), getEnclosingLabelHelper(val));
		}

		public WyilSection getWyilSection() {
			if (partialReturn == null)
				return graph.new WyilSection(inputs, Collections.emptyList(), Collections.emptyList());
			List<LabelNode> outputs = new ArrayList<>();
			int l = 0;
			for (NodeTree<?> r : partialReturn.getReturn())
				for (LabelNode c : buildNamedValue("out_"+l++, r).getValues())
					outputs.add(c);
			return graph.new WyilSection(inputs, outputs, graph.invokes);
		}

		public Builder(CompilerLogger logger, Map<String, TypeTree> types, WyilFile.FunctionOrMethod func) throws CompilerException {
			super(logger);
			this.types = types;
			graph = new ControlFlowGraph(logger, types);
			int k = 0;
			for (Type t : func.type().params()) {
				@SuppressWarnings("unchecked")
				String ident = ((Location<Bytecode.VariableDeclaration>)func.getTree().getLocation(k)).getBytecode().getName();
				LabelNode l = graph.new LabelNode(ident, t);
				inputs.add(l);
				vars.put(k++, l.type instanceof PrimitiveType ? new PrimitiveLabel(ident, l)
				                                              : buildParameter(ident, l.type, l));
			}
			build(func.getBody());
		}

		private LabelTree buildParameter(String ident, TypeTree type, DataNode input) {
			return type instanceof PrimitiveType ? new PrimitiveLabel(ident, graph.new LabelNode(ident, type,  input))
			                                     : new CompoundLabel(ident, Utils.convert(type.getComponents(),
			                                    	 (Pair<String, TypeTree> p) -> new Pair<String, LabelTree>(
			                                    			 p.first, buildParameter(ident+"_"+p.first, p.second, input))));
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
		private List<NodeTree<?>> buildTuple(Location<?>[] elem) throws CompilerException {
			ArrayList<NodeTree<?>> exprs = new ArrayList<>();
			for (Location<?> e : elem){
				if (e.getBytecode() instanceof Invoke)
					exprs.addAll(buildInvoke((Location<Invoke>) e));
				else
					exprs.add(buildExpression(e));
			}
			return exprs;
		}

		private <T extends DataNode> LabelTree buildNamedValueHelper(String ident, NodeTree<T> expr) {
			return expr instanceof PrimitiveNode<?> ? new PrimitiveLabel(ident, graph.new LabelNode(ident, expr.getValue()))
			                                        : new CompoundLabel(ident, Utils.convert(expr.getComponents(),
			                                        	(Pair<String, NodeTree<T>> p) -> new Pair<String, LabelTree>(
			                                        			p.first, buildNamedValueHelper(ident+"_"+p.first, p.second))));
		}

		private LabelTree buildNamedValue(String name, NodeTree<?> expr) {
			return buildNamedValueHelper(name, expr);
		}

		@SuppressWarnings("unchecked")
		private Pair<Location<Bytecode.VariableDeclaration>,String> buildAssignName(Location<?> field) throws CompilerException {
			Bytecode bytecode = field.getBytecode();
//			debug("Assign Name");
//			Utils.printLocation(logger, field, "o ");
			if (bytecode instanceof Bytecode.FieldLoad)
				return buildAssignName(field.getOperand(0)).transformSecond((String a) -> a+"_"+((Bytecode.FieldLoad) bytecode).fieldName());
			if (bytecode instanceof Bytecode.VariableDeclaration)
				return new Pair<Location<Bytecode.VariableDeclaration>,String>(
						(Location<Bytecode.VariableDeclaration>) field,
						((Bytecode.VariableDeclaration) bytecode).getName());
			if (bytecode instanceof Bytecode.VariableAccess)
				return buildAssignName(field.getOperand(0));
			throw new CompilerException(new WyilUnsupportedCompilerError(field));
		}

		private LabelTree buildField(LabelTree struct, LabelTree current, LabelTree compo) {
			ArrayList<Pair<String, LabelTree>> cmps = new ArrayList<>();
			cmps.addAll(Utils.convert(struct.getComponents()));
			for (Pair<String, LabelTree> c : cmps)
				if (c.second == current)
					c.second = compo;
			return new CompoundLabel(struct.getName(), cmps);
		}

		private void buildAssignField(int op, LabelTree var, LabelTree val) {
//			debug("Var "+op + " " +var);
			if (var.getParent() == null)
				vars.put(op, val);
			else
				buildAssignField(op, var.getParent(), buildField(var.getParent(), var, val));
		}

		private Pair<Integer, LabelTree> recoverIdent(Location<?> field) throws CompilerException {
			Bytecode bytecode = field.getBytecode();
			if (bytecode instanceof Bytecode.FieldLoad)
				return recoverIdent(field.getOperand(0)).transformSecondChecked(
					(LabelTree l) -> (LabelTree) l.getComponent(((Bytecode.FieldLoad) bytecode).fieldName()));
			if (bytecode instanceof Bytecode.VariableDeclaration)
				return new Pair<Integer, LabelTree>(field.getIndex(), vars.get(field.getIndex()));
			if (bytecode instanceof Bytecode.VariableAccess)
				return recoverIdent(field.getOperand(0));
			throw new CompilerException(new WyilUnsupportedCompilerError(field));
		}
		private void buildAssignValue(Location<?> acc, NodeTree<?> val) throws CompilerException {
			openLevel("Assign");
			Utils.printLocation(logger, acc, level);
			val.printStructure(logger, level);
			Pair<Location<Bytecode.VariableDeclaration>,String> p = buildAssignName(acc);
			Pair<Integer, LabelTree> q = recoverIdent(acc);
			buildAssignField(q.first, q.second , buildNamedValue(p.second, val));
			closeLevel();
		}

		private void buildAssign(Location<Bytecode.Assign> assign) throws CompilerException {
			Location<?>[] lhs = assign.getOperandGroup(SyntaxTree.LEFTHANDSIDE);
			Location<?>[] rhs = assign.getOperandGroup(SyntaxTree.RIGHTHANDSIDE);
			List<NodeTree<?>> brhs = buildTuple(rhs);
			for(int k = 0; k < lhs.length; ++k)
				buildAssignValue(lhs[k], brhs.get(k));
		}

		private void buildReturn(Location<Bytecode.Return> ret) throws CompilerException {
			List<NodeTree<?>> retv = buildTuple(ret.getOperands());
			if (partialReturn != null)
				partialReturn.completeReturn(retv);
			else
				partialReturn = new PartialReturn(retv);
		}

		private void buildDecl(Location<Bytecode.VariableDeclaration> decl) throws CompilerException {
			debug("Declaration de "+decl.getBytecode().getName()+" en "+decl.getIndex());
			vars.put(decl.getIndex(),
				decl.numberOfOperands() == 1 ? buildNamedValue(decl.getBytecode().getName(), buildExpression(decl.getOperand(0)))
				                             : new PrimitiveLabel(decl.getBytecode().getName(), graph.new LabelNode(decl.getBytecode().getName(), decl.getType())));
		}

		@SuppressWarnings("unchecked")
		private NodeTree<? extends DataNode> buildExpression(Location<?> location) throws CompilerException {
			openLevel("Expression");
			Utils.printLocation(logger, location, level);
			Bytecode bytecode = location.getBytecode();
			if (bytecode instanceof Bytecode.Operator)
				return end(buildOperator((Location<Bytecode.Operator>) location));
			if (bytecode instanceof Const)
				return end(buildConst((Location<Const>) location));
			if (bytecode instanceof VariableAccess ||
				bytecode instanceof FieldLoad)
				return end(buildAccess(location));
			if (bytecode instanceof Invoke)
				return end(buildInvoke((Location<Invoke>) location).get(0));
			throw new CompilerException(new WyilUnsupportedCompilerError(location));
		}


		private NodeTree<?> buildOperator(Location<Bytecode.Operator> op) throws CompilerException {
//			debug("Operator Compilation");
//			debug("Operator "+a);
//			a.printStructure(logger, "  ");
			if (op.getBytecode().kind() == OperatorKind.RECORDCONSTRUCTOR) {
				int k = 0;
				ArrayList<Pair<String, NodeTree<?>>> fields = new ArrayList<>();
				for (String f : ((Type.EffectiveRecord)op.getType()).getFieldNames())
					fields.add(new Pair<String, NodeTree<?>>(f, buildExpression(op.getOperand(k++))));
				return new CompoundNode<DataNode>(Utils.convert(fields));
			}
			if (op.numberOfOperands() == 1)
				return new PrimitiveNode<WyilNode<?>>(graph.new UnaOpNode(op, buildExpression(op.getOperand(0)).getValue()));
			if (op.numberOfOperands() == 2)
				return new PrimitiveNode<WyilNode<?>>(graph.new BinOpNode(op, buildExpression(op.getOperand(0)).getValue(), buildExpression(op.getOperand(1)).getValue()));

			throw new CompilerException(new WyilUnsupportedCompilerError(op));
		}

		private NodeTree<?> buildConst(Location<Const> val) throws CompilerException {
			return new PrimitiveNode<DataNode>(graph.new ConstNode(val));
		}


		@SuppressWarnings("unchecked")
		private LabelTree buildAccess(Location<?> location) throws CompilerException {
			openLevel("Access");
			Utils.printLocation(logger, location, level);
			Bytecode bytecode = location.getBytecode();
			if (bytecode instanceof FieldLoad)
				return end(buildFieldLoad((Location<FieldLoad>) location));
			if (bytecode instanceof VariableAccess)
				return end(buildVariableAccess((Location<VariableAccess>) location));
			throw new CompilerException(new WyilUnsupportedCompilerError(location));
		}

		private LabelTree buildFieldLoad(Location<FieldLoad> field) throws CompilerException {
//			NodeTree<LabelNode> a = buildAccess(field.getOperand(0));
//			a.printStructure(System.out, "Fields  ");
//			System.out.println(field.getBytecode().fieldName());
//			NodeTree<LabelNode> b = a.getComponent(field.getBytecode().fieldName());
//			System.out.println("C'est "+b);
//			return b;
			return (LabelTree) buildAccess(field.getOperand(0)).getComponent(field.getBytecode().fieldName());
		}

		private LabelTree buildVariableAccess(Location<VariableAccess> var) {
//			System.out.println("Access at "+vars.get(var.getBytecode().getOperand(0))+" via "+var.getBytecode().getOperand(0));
//			Utils.printLocation(var, "}- ");
//			vars.get(var.getBytecode().getOperand(0)).printStructure(System.out, "}- ");
			return vars.get(var.getBytecode().getOperand(0));
		}

		private List<LabelTree> buildInvoke(Location<Invoke> call) throws CompilerException {
			FuncCallNode c = graph.new FuncCallNode(call,
				Utils.checkedConvert(
					Arrays.asList(call.getOperands()),
				    (Location<?> l, Integer t) -> getEnclosingLabel("arg_"+t, buildExpression(l))));
			return Utils.checkedConvert(
				Arrays.asList(call.getBytecode().type().returns()),
				(Type t, Integer i) -> t instanceof PrimitiveType ? new PrimitiveLabel(graph.new LabelNode("ret_"+i, t, c))
				                                                  : buildParameter("ret_"+i, TypeCompiler.compileType(logger, t, types), c));

		}


		private <T extends DataNode, U extends DataNode> NodeTree<EndIfNode> buildEndIfHelper(IfNode ifn, NodeTree<T> trueLab, NodeTree<U> falseLab) {
			return trueLab instanceof PrimitiveNode ? new PrimitiveNode<EndIfNode>(graph.new EndIfNode(ifn, trueLab.getValue(), falseLab.getValue()))
			                                        : new CompoundNode<EndIfNode>(Utils.convert(
		                                        		Utils.gather(trueLab.getComponents(),falseLab.getComponents()),
			                                        	(Pair<Pair<String, NodeTree<T>>,Pair<String, NodeTree<U>>> p) ->
			                                        	new Pair<String, NodeTree<EndIfNode>>(p.first.first, buildEndIf(ifn, p.first.second, p.second.second))));
		}
		private NodeTree<EndIfNode> buildEndIf(IfNode ifn, NodeTree<?> trueLab, NodeTree<?> falseLab) {
			return buildEndIfHelper(ifn, trueLab, falseLab);
		}

		private void buildIf(Location<If> ifs) throws CompilerException {
			//debug("début if "+ifs+" "+ifs.numberOfOperands()+" "+ifs.numberOfBlocks());
			NodeTree<?> cond = buildExpression(ifs.getOperand(0));
			//debug("condition "+cond);
			IfNode ifn = graph.new IfNode(ifs, cond.getValue());

			HashMap<Integer, LabelTree> state = new HashMap<>();
			vars.forEach((Integer i, LabelTree t) -> state.put(i, t));
			HashMap<Integer, LabelTree> tbc = new HashMap<>();

			PartialReturn prevReturn = partialReturn;
			partialReturn = null;
			build(ifs.getBlock(0));
			state.forEach((Integer i, LabelTree t) -> {if (vars.get(i) != t) tbc.put(i, vars.get(i));});

			vars.clear();
			state.forEach((Integer i, LabelTree t) -> vars.put(i, t));


			HashMap<Integer, LabelTree> fbc = new HashMap<>();

			PartialReturn trueReturn = partialReturn;
			partialReturn = null;
			if (ifs.getBytecode().hasFalseBranch())
				build(ifs.getBlock(1));
			state.forEach((Integer i, LabelTree t) -> {if (vars.get(i) != t) fbc.put(i, vars.get(i));});

			vars.clear();
			state.forEach((Integer i, LabelTree t) -> {
				vars.put(i,
					tbc.containsKey(i) || fbc.containsKey(i) ? buildNamedValue(t.getName(),
						buildEndIf(ifn, tbc.getOrDefault(i, t), fbc.getOrDefault(i, t)))
					                                         : t);
			});
			if (trueReturn != null || partialReturn != null) {
				addMessage(new NestedReturnCompilerNotice());
				trueReturn = new PartialReturn(ifn, trueReturn, partialReturn);
			}
			partialReturn = prevReturn != null ? prevReturn.completeReturn(trueReturn) : trueReturn;
		}
	}






	public static WyilSection buildGraph(CompilerLogger logger, FunctionOrMethod func, Map<String, TypeTree> types) throws CompilerException {
		return new Builder(logger, types, func).getWyilSection();
	}
}
