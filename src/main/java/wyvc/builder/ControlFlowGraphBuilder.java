package wyvc.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import wyil.lang.Bytecode;
import wyil.lang.SyntaxTree;
import wyil.lang.Bytecode.Const;
import wyil.lang.Bytecode.FieldLoad;
import wyil.lang.Bytecode.Invoke;
import wyil.lang.Bytecode.OperatorKind;
import wyil.lang.Bytecode.VariableAccess;
import wyil.lang.SyntaxTree.Location;
import wyil.lang.Type;
import wyil.lang.WyilFile;
import wyil.lang.WyilFile.FunctionOrMethod;
import wyvc.builder.ControlFlowGraph;
import wyvc.builder.ControlFlowGraph.DataNode;
import wyvc.builder.ControlFlowGraph.FuncCall;
import wyvc.builder.ControlFlowGraph.LabelNode;
import wyvc.builder.ControlFlowGraph.WyilNode;
import wyvc.builder.ControlFlowGraph.WyilSection;
import wyvc.builder.LexicalElementTree.Compound;
import wyvc.builder.LexicalElementTree.Primitive;
import wyvc.builder.LexicalElementTree.Tree;
import wyvc.builder.LexicalElementTree.TreeComponentException;
import wyvc.builder.TypeCompiler.NominalTypeException;
import wyvc.builder.TypeCompiler.PrimitiveType;
import wyvc.builder.TypeCompiler.TypeTree;
import wyvc.builder.TypeCompiler.UnsupportedTypeException;
import wyvc.builder.VHDLCompileTask.VHDLCompilationException;
import wyvc.utils.Pair;
import wyvc.utils.Utils;

public final class ControlFlowGraphBuilder {


	private static interface NodeTree<T extends DataNode> extends Tree<NodeTree<T>, T> {

	}

	public static class PrimitiveNode<T extends DataNode> extends Primitive<NodeTree<T>, T> implements NodeTree<T> {
		public PrimitiveNode(T decl) {
			super(decl);
		}
	}

	public static class CompoundNode<T extends DataNode> extends Compound<NodeTree<T>, T> implements NodeTree<T> {
		public CompoundNode(List<Pair<String, NodeTree<T>>> components) {
			super(components);
		}
	}

	private static interface LabelTree extends NodeTree<LabelNode> {
		public LabelTree getParent();
		public void setParent(LabelTree parent);
	}

	public static class PrimitiveLabel extends PrimitiveNode<LabelNode> implements LabelTree {
		private LabelTree parent = null;

		public PrimitiveLabel(LabelNode decl) {
			super(decl);
		}

		@Override
		public LabelTree getParent() {
			return parent;
		}

		@Override
		public void setParent(LabelTree parent) {
			this.parent = parent;
		}
	}

	public static class CompoundLabel extends CompoundNode<LabelNode> implements LabelTree {
		private LabelTree parent = null;

		public CompoundLabel(List<Pair<String, LabelTree>> components) {
			super(Utils.convert(components));
			for (Pair<String, LabelTree> p : components)
				p.second.setParent(this);
		}

		@Override
		public LabelTree getParent() {
			return parent;
		}

		@Override
		public void setParent(LabelTree parent) {
			this.parent = parent;
		}
	}


	private static class BMap<S,T> extends HashMap<S, T> {
		private static final long serialVersionUID = 1534890860087462075L;

		public T put(S o, T u) {
			System.out.println("### Modif "+u+" vers "+o);
			return super.put(o, u);
		}
	}

	private static final class Builder {
		private final Map<String, TypeTree> types;
		private BMap<Integer, LabelTree> vars = new BMap<>();
		private ArrayList<List<NodeTree<?>>> returns = new ArrayList<>();
		private final ControlFlowGraph graph;
		private ArrayList<DataNode> inputs = new ArrayList<>();

		private <T extends DataNode> List<DataNode> getEnclosingLabelHelper(NodeTree<T> t) {
			return Utils.convert(t.getValues());
		}

		private LabelNode getEnclosingLabel(String ident, NodeTree<?> val) {
			return graph.new LabelNode(ident, getEnclosingLabelHelper(val));
		}

		public WyilSection getWyilSection() {
			if (returns.size() == 0)
				return new WyilSection(inputs, Collections.emptyList());
			int l = returns.get(0).size();
			ArrayList<Pair<String, ArrayList<DataNode>>> outputs = new ArrayList<>(l);
			for (int k=0; k < l; ++k)
				outputs.add(new Pair<String, ArrayList<DataNode>>("out_"+k, new ArrayList<>()));
			int k = 0;
			for (List<NodeTree<?>> r : returns){
				int j = 0;
				for (NodeTree<?> n : r)
					outputs.get(j).second.add(getEnclosingLabel("out_"+j+++"_"+k, n));
				k++;
			}
			return new WyilSection(inputs, Utils.convert(outputs, (Pair<String, ArrayList<DataNode>> re) -> graph.new LabelNode(re.first, re.second)));
		}

		public Builder(Map<String, TypeTree> types, WyilFile.FunctionOrMethod func) throws VHDLCompilationException {
			this.types = types;
			graph = new ControlFlowGraph(types);
			int k = 0;
			for (Type t : func.type().params()) {
				@SuppressWarnings("unchecked")
				String ident = ((Location<Bytecode.VariableDeclaration>)func.getTree().getLocation(k)).getBytecode().getName();
				LabelNode l = graph.new LabelNode(ident);
				inputs.add(l);
				TypeTree ty = TypeCompiler.compileType(t, types);
				vars.put(k++, ty instanceof PrimitiveType ? new PrimitiveLabel(l)
				                                          : buildParameter(ident, ty, l));
			}
			build(func.getBody());
		}

		private LabelTree buildParameter(String ident, TypeTree type, DataNode input) {
			return type instanceof PrimitiveType ? new PrimitiveLabel(graph.new LabelNode(ident, input))
			                                     : new CompoundLabel(Utils.convert(type.getComponents(),
			                                    	 (Pair<String, TypeTree> p) -> new Pair<String, LabelTree>(
			                                    			 p.first, buildParameter(ident+"."+p.first, p.second, input))));
		}

		@SuppressWarnings("unchecked")
		public void build(Location<?> location) throws VHDLCompilationException {
			System.out.println("┌───────────── Build ──────────────");
			Utils.printLocation(location, "│ ");
			Bytecode bytecode = location.getBytecode();
			if (bytecode instanceof Bytecode.Block)
				buildBlock((Location<Bytecode.Block>) location);
			else if (bytecode instanceof Bytecode.VariableDeclaration)
				buildDecl((Location<Bytecode.VariableDeclaration>) location);
			else if (bytecode instanceof Bytecode.Assign)
				buildAssign((Location<Bytecode.Assign>) location);
			else if (bytecode instanceof Bytecode.Return)
				buildReturn((Location<Bytecode.Return>) location);
		}

		private void buildBlock(Location<Bytecode.Block> block) throws VHDLCompilationException {
			for (Location<?> l : block.getOperands())
				build(l);
		}




		@SuppressWarnings("unchecked")
		private List<NodeTree<?>> buildTuple(Location<?>[] elem) throws VHDLCompilationException {
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
			return expr instanceof PrimitiveNode<?> ? new PrimitiveLabel(graph.new LabelNode(ident, expr.getValue()))
			                                        : new CompoundLabel(Utils.convert(expr.getComponents(),
			                                        	(Pair<String, NodeTree<T>> p) -> new Pair<String, LabelTree>(
			                                        			p.first, buildNamedValueHelper(ident+"."+p.first, p.second))));
		}

		private LabelTree buildNamedValue(Location<Bytecode.VariableDeclaration> var, String name, NodeTree<? extends DataNode> expr) throws VHDLCompilationException {
			return buildNamedValueHelper(name, expr);
		}

		@SuppressWarnings("unchecked")
		private Pair<Location<Bytecode.VariableDeclaration>,String> buildAssignName(Location<?> field) {
			Bytecode bytecode = field.getBytecode();
			System.out.println("Assign Name");
			Utils.printLocation(field, "o ");
			if (bytecode instanceof Bytecode.FieldLoad)
				return buildAssignName(field.getOperand(0)).transformSecond((String a) -> a+"."+((Bytecode.FieldLoad) bytecode).fieldName());
			if (bytecode instanceof Bytecode.VariableDeclaration)
				return new Pair<Location<Bytecode.VariableDeclaration>,String>(
						(Location<Bytecode.VariableDeclaration>) field,
						((Bytecode.VariableDeclaration) bytecode).getName());
			if (bytecode instanceof Bytecode.VariableAccess)
				return buildAssignName(field.getOperand(0));
			System.out.println("Houla");
			assert false;
			return null;
		}

		private LabelTree buildField(LabelTree struct, LabelTree current, LabelTree compo) {
			ArrayList<Pair<String, LabelTree>> cmps = new ArrayList<>();
			cmps.addAll(Utils.convert(struct.getComponents()));
			for (Pair<String, LabelTree> c : cmps)
				if (c.second == current)
					c.second = compo;
			return new CompoundLabel(cmps);
		}

		private void buildAssignField(int op, LabelTree var, LabelTree val) {
			System.out.println("Var "+op + " " +var);
			if (var.getParent() == null)
				vars.put(op, val);
			else
				buildAssignField(op, var.getParent(), buildField(var.getParent(), var, val));
		}

		private Pair<Integer, LabelTree> recoverIdent(Location<?> field) {
			Bytecode bytecode = field.getBytecode();
			if (bytecode instanceof Bytecode.FieldLoad)
				return recoverIdent(field.getOperand(0)).transformSecond((LabelTree c) -> {
					try {
						return (LabelTree) c.getComponent(((Bytecode.FieldLoad) bytecode).fieldName());
					} catch (TreeComponentException e) {
						e.printStackTrace();
					}
					System.out.println("STOP !!!!!");
					return null;
				});
			if (bytecode instanceof Bytecode.VariableDeclaration)
				return new Pair<Integer, LabelTree>(field.getIndex(), vars.get(field.getIndex()));
			if (bytecode instanceof Bytecode.VariableAccess)
				return recoverIdent(field.getOperand(0));
			assert(false);
			System.out.println("Assert false.......");
			return null;
		}
		private void buildAssignValue(Location<?> acc, NodeTree<?> val) throws VHDLCompilationException {
			System.out.println("│ ┌──────────── Assign ────────────");
			Utils.printLocation(acc, "│ │ ");
			val.printStructure(System.out, "│ │ ");
			Pair<Location<Bytecode.VariableDeclaration>,String> p = buildAssignName(acc);
			System.out.println("Trouvé "+p.first + " - " + p.second );
			Pair<Integer, LabelTree> q = recoverIdent(acc);
			buildAssignField(q.first, q.second , buildNamedValue(p.first, p.second, val));
		}

		private void buildAssign(Location<Bytecode.Assign> assign) throws VHDLCompilationException {
			Location<?>[] lhs = assign.getOperandGroup(SyntaxTree.LEFTHANDSIDE);
			Location<?>[] rhs = assign.getOperandGroup(SyntaxTree.RIGHTHANDSIDE);
			List<NodeTree<?>> brhs = buildTuple(rhs);
			for(int k = 0; k < lhs.length; ++k)
				buildAssignValue(lhs[k], brhs.get(k));
		}

		private void buildReturn(Location<Bytecode.Return> ret) throws VHDLCompilationException {
			returns.add(buildTuple(ret.getOperands()));
		}

		private void buildDecl(Location<Bytecode.VariableDeclaration> decl) throws VHDLCompilationException {
			System.out.println("Declaration de "+decl.getBytecode().getName()+" en "+decl.getIndex());
			vars.put(decl.getIndex(),
				decl.numberOfOperands() == 1 ? buildNamedValue(decl, decl.getBytecode().getName(), buildExpression(decl.getOperand(0)))
				                             : new PrimitiveLabel(graph.new LabelNode(decl.getBytecode().getName())));
		}

		@SuppressWarnings("unchecked")
		private NodeTree<? extends DataNode> buildExpression(Location<?> location) throws VHDLCompilationException {
			System.out.println("│ ┌────────── Expression ──────────");
			Utils.printLocation(location, "│ │ ");
			Bytecode bytecode = location.getBytecode();
			if (bytecode instanceof Bytecode.Operator)
				return buildOperator((Location<Bytecode.Operator>) location);
			if (bytecode instanceof Const)
				return buildConst((Location<Const>) location);
			if (bytecode instanceof VariableAccess ||
				bytecode instanceof FieldLoad)
				return buildAccess(location);
			if (bytecode instanceof Invoke)
				return buildInvoke((Location<Invoke>) location).get(0);
			System.out.println("############### OUPS #############");
			return null;
		}


		private NodeTree<?> buildOperator(Location<Bytecode.Operator> op) throws VHDLCompilationException {
			System.out.println("Operator Compilation");
			NodeTree<?> a = buildExpression(op.getOperand(0));
			System.out.println("Operator "+a);
			a.printStructure(System.out, "  ");
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
				return new PrimitiveNode<WyilNode<?>>(graph.new BinOpNode(op, a.getValue(), buildExpression(op.getOperand(1)).getValue()));
			return null;
		}

		private NodeTree<?> buildConst(Location<Const> val) {
			return new PrimitiveNode<DataNode>(graph.new ConstNode(val));
		}


		@SuppressWarnings("unchecked")
		private LabelTree buildAccess(Location<?> location) throws TreeComponentException {
			System.out.println("│ │ ┌─────────── Access ───────────");
			Utils.printLocation(location, "│ │ │ ");
			Bytecode bytecode = location.getBytecode();
			if (bytecode instanceof FieldLoad)
				return buildFieldLoad((Location<FieldLoad>) location);
			if (bytecode instanceof VariableAccess)
				return buildVariableAccess((Location<VariableAccess>) location);
			assert(false);
			return null;
		}

		private LabelTree buildFieldLoad(Location<FieldLoad> field) throws TreeComponentException {
//			NodeTree<LabelNode> a = buildAccess(field.getOperand(0));
//			a.printStructure(System.out, "Fields  ");
//			System.out.println(field.getBytecode().fieldName());
//			NodeTree<LabelNode> b = a.getComponent(field.getBytecode().fieldName());
//			System.out.println("C'est "+b);
//			return b;
			return (LabelTree) buildAccess(field.getOperand(0)).getComponent(field.getBytecode().fieldName());
		}

		private LabelTree buildVariableAccess(Location<VariableAccess> var) {
			System.out.println("Access at "+vars.get(var.getBytecode().getOperand(0))+" via "+var.getBytecode().getOperand(0));
			Utils.printLocation(var, "}- ");
			vars.get(var.getBytecode().getOperand(0)).printStructure(System.out, "}- ");
			return vars.get(var.getBytecode().getOperand(0));
		}

		private List<LabelTree> buildInvoke(Location<Invoke> call) {
			FuncCall c = graph.new FuncCall(call, Utils.convertInd(Arrays.asList(call.getOperands()),
				(Pair<Location<?>,Integer> a) -> {
					try {
						return getEnclosingLabel("arg_"+a.second, buildExpression(a.first));
					} catch (VHDLCompilationException e) {
						e.printStackTrace();
					}
					return null;
				}));
			return Utils.convertInd(
				Utils.convert(Arrays.asList(call.getBytecode().type().returns()),
					(Type t) -> {
						try {
							return TypeCompiler.compileType(t, types);
						} catch (UnsupportedTypeException | NominalTypeException e) {
							e.printStackTrace();
						}
						return null;
					}),
				(Pair<TypeTree, Integer> p) -> p.first instanceof PrimitiveType ? new PrimitiveLabel(graph.new LabelNode("ret_"+p.second, c))
				                                                                : buildParameter("ret_"+p.second, p.first, c));

		}
	}






	public static WyilSection buildGraph(FunctionOrMethod func, Map<String, TypeTree> types) throws VHDLCompilationException {
		return new Builder(types, func).getWyilSection();
	}
}
