package wyvc.builder;

import java.util.List;
import java.util.Map;

import wyil.lang.Bytecode;
import wyil.lang.SyntaxTree.Location;
import wyil.lang.Type;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.builder.TypeCompiler.TypeTree;
import wyvc.utils.CheckedFunctionalInterface.CheckedFunction;
import wyvc.utils.GraphNode;
import wyvc.utils.Pair;
import wyvc.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

public class ControlFlowGraph {
	private HashMap<Type, TypeTree> types = new HashMap<>();
	private Map<String, TypeTree> nominalTypes;
	public List<FuncCallNode> invokes = new ArrayList<>();
	int separation = 0;
	private final CompilerLogger logger;


	public ControlFlowGraph(CompilerLogger logger, Map<String, TypeTree> nominalTypes) {
		this.nominalTypes = nominalTypes;
		this.logger = logger;
	}

	static private String toString(Bytecode b) {
		if (b instanceof Bytecode.Operator)
			return ((Bytecode.Operator) b).kind().toString();
		if (b instanceof Bytecode.Const)
			return ((Bytecode.Const) b).constant().toString();
		if (b instanceof Bytecode.If)
			return "If";
		if (b instanceof Bytecode.Invoke)
			return ((Bytecode.Invoke) b).name().toString()+"()";
		return b.toString();
	}

	TypeTree getType(Type t) throws CompilerException {
		if (!types.containsKey(t))
			types.put(t, TypeCompiler.compileType(logger, t, nominalTypes));
		return types.get(t);
	}

	public void addSeparation() {
		separation++;
	}




	public abstract class DataNode extends GraphNode<DataNode> {
		public final TypeTree type;
		public final int part;
		public final String nodeIdent;

		public DataNode(String label, TypeTree type, List<DataNode> sources) {
			super(label+"\n"+(type == null ? "Untyped" : type.toString()), sources);
			this.type = type;
			part = separation;
			nodeIdent = label;
		}

		@Override
		public List<String> getArrowOptions(DataNode other) {
			if (other.part != part)
				return Arrays.asList("arrowhead=\"box\"","color=purple");
			return Collections.emptyList();
		}

		public abstract DataNode duplicate(List<DataNode> sources) throws CompilerException;
	}

	public class WyilSection {
		public final List<LabelNode> inputs;
		public final List<LabelNode> outputs;
		public final List<FuncCallNode> invokes;

		public WyilSection(List<LabelNode> inputs, List<LabelNode> outputs, List<FuncCallNode> invokes) {
			this.inputs = inputs;
			this.outputs = outputs;
			this.invokes = invokes;
		}

		public WyilSection duplicate() throws CompilerException {
			Map<DataNode, DataNode> duplication = new HashMap<>();
			CheckedFunction<DataNode, DataNode, CompilerException> dpl = (DataNode d) -> duplicate(d, duplication, null);
			return new WyilSection(
				Utils.convert(Utils.checkedConvert(inputs, dpl)),
				Utils.convert(Utils.checkedConvert(outputs, dpl)),
				Utils.convert(Utils.checkedConvert(invokes, dpl)));
		}

		private DataNode duplicate(DataNode d, Map<DataNode, DataNode> duplication, DataNode p) throws CompilerException {
			if (duplication.containsKey(d))
				return duplication.get(d);
			DataNode c = d.duplicate(Utils.checkedConvert(d.sources, (DataNode n) -> duplicate(n, duplication, null)));
			duplication.put(d, c);
			for (DataNode t : d.targets)
				if (t != p)
					duplicate(t, duplication, null);
			return c;
		}
	}

	public class LabelNode extends DataNode { // TODO Garrantir 1 source max ?
		public LabelNode(String ident, Type type, List<DataNode> sources) throws CompilerException {
			super(ident, getType(type), sources);
		}
		public LabelNode(String ident, TypeTree type, List<DataNode> sources) {
			super(ident, type, sources);
		}

		public LabelNode(String ident, DataNode source) {
			super(ident, source.type, Collections.singletonList(source));
		}
		public LabelNode(String ident, Type type, DataNode source) throws CompilerException {
			super(ident, getType(type), Collections.singletonList(source));
		}
		public LabelNode(String ident, TypeTree type, DataNode source) {
			super(ident, type, Collections.singletonList(source));
		}

		public LabelNode(String ident, Type type) throws CompilerException {
			super(ident, getType(type), Collections.emptyList());
		}
		public LabelNode(String ident, TypeTree type) {
			super(ident, type, Collections.emptyList());
		}

		@Override
		public List<String> getNodeOptions() {
			return Arrays.asList("shape=\"ellipse\"","color=green");
		}

		@Override
		public DataNode duplicate(List<DataNode> sources) {
			return new LabelNode(nodeIdent, type, sources);
		}

		public DataNode getSource() {
			return sources.isEmpty() ? null : sources.get(0);
		}
	}

	public abstract class WyilNode<T extends Bytecode> extends DataNode {
		public final Location<T> location;

		public WyilNode(Location<T> location, List<DataNode> sources) throws CompilerException {
			super(ControlFlowGraph.toString(location.getBytecode()),
				getType(location.getType()), sources);
			this.location = location;
		}

		public WyilNode(Location<T> location, TypeTree type, List<DataNode> sources) {
			super(ControlFlowGraph.toString(location.getBytecode()), type, sources);
			this.location = location;
		}

		public WyilNode(String ident, Location<T> location, TypeTree type, List<DataNode> sources) {
			super(ident, type, sources);
			this.location = location;
		}

	}



	public abstract class InstructionNode<T extends Bytecode> extends WyilNode<T> {
		public InstructionNode(Location<T> location, List<DataNode> sources) throws CompilerException {
			super(location, sources);
		}

	}

	public final class ConstNode extends InstructionNode<Bytecode.Const> {
		public ConstNode(Location<Bytecode.Const> decl) throws CompilerException {
			super(decl, Collections.emptyList());
		}

		@Override
		public DataNode duplicate(List<DataNode> sources) throws CompilerException {
			return new ConstNode(location);
		}
	}

	public final class UnaOpNode extends InstructionNode<Bytecode.Operator> {
		public final DataNode op;

		public UnaOpNode(Location<Bytecode.Operator> binOp, DataNode op) throws CompilerException {
			super(binOp, Arrays.asList(op));
			this.op = op;
		}

		@Override
		public DataNode duplicate(List<DataNode> sources) throws CompilerException {
			return new UnaOpNode(location, sources.get(0));
		}
	}

	public final class BinOpNode extends WyilNode<Bytecode.Operator> {
		public final DataNode op1;
		public final DataNode op2;

		public BinOpNode(Location<Bytecode.Operator> binOp, DataNode op1, DataNode op2) throws CompilerException {
			super(binOp, Arrays.asList(op1, op2));
			this.op1 = op1;
			this.op2 = op2;
		}

		@Override
		public List<String> getNodeOptions() {
			return Arrays.asList("shape=\"rectangle\"","style=filled","fillcolor=lemonchiffon");
		}

		@Override
		public DataNode duplicate(List<DataNode> sources) throws CompilerException {
			return new BinOpNode(location, sources.get(0), sources.get(1));
		}
	}

	public final class FuncCallNode extends WyilNode<Bytecode.Invoke> {
		public final String funcName;

		public FuncCallNode(Location<Bytecode.Invoke> call, List<DataNode> args) {
			super(call, null, args);
			invokes.add(this);
			funcName = call.getBytecode().name().name();
		}

		public void inline(WyilSection func) throws CompilerException {
			WyilSection cfunc = func.duplicate();
			for (Pair<DataNode,LabelNode> a : Utils.gather(sources, cfunc.inputs)){
				a.first.targets.replaceAll((DataNode n) -> n == this ? a.second : n);
				a.second.sources.add(a.first);
				//System.out.println("Argu "+a.first.label+" "+a.second.label+" "+a.first.targets.get(0)+" "+a.second);
			}
			for (Pair<DataNode,LabelNode> r : Utils.gather(getTargets(), cfunc.outputs)){
				r.first.sources.replaceAll((DataNode n) -> n == this ? r.second : n);
				r.second.targets.add(r.first);
				//System.out.println("Sortie "+r.first.label+" "+r.second.label+" "+r.first.sources.get(0)+" "+r.second);
			}
		}

		@Override
		public DataNode duplicate(List<DataNode> sources) {
			return new FuncCallNode(location, sources);
		}
	}


	public final class IfNode extends LabelNode {
		public final Location<Bytecode.If> location;

		public IfNode(Location<Bytecode.If> ifs, DataNode cond) {
			super("if_cond", cond.type, Collections.singletonList(cond));
			location = ifs;
		}

		@Override
		public List<String> getNodeOptions() {
			return Arrays.asList("shape=\"hexagon\"","color=yellow");
		}

		@Override
		public DataNode duplicate(List<DataNode> sources) {
			return new IfNode(location, sources.get(0));
		}
	}

	public final class EndIfNode extends WyilNode<Bytecode.If> {
		public EndIfNode(IfNode condition, DataNode trueNode, DataNode falseNode) {
			super("mux", condition.location, trueNode.type, Arrays.asList(condition, trueNode, falseNode));
		}

		@Override
		public List<String> getNodeOptions() {
			return Arrays.asList("shape=\"rectangle\"","style=filled","fillcolor=lemonchiffon");
		}

		@Override
		public DataNode duplicate(List<DataNode> sources) {
			return new EndIfNode((IfNode) sources.get(0), (LabelNode) sources.get(1), (LabelNode) sources.get(2));
		}

		public DataNode getConditionNode() {
			return sources.get(0);
		}
		public DataNode getTrueNode() {
			return sources.get(1);
		}
		public DataNode getFalseNode() {
			return sources.get(2);
		}
	}
}
