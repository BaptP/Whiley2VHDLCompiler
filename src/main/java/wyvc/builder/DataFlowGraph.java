package wyvc.builder;

import java.util.List;
import java.util.Set;

import wyil.lang.Bytecode;
import wyil.lang.SyntaxTree.Location;
import wyvc.builder.CompilerLogger.CompilerError;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.builder.DataFlowGraph.DataArrow;
import wyvc.builder.DataFlowGraph.DataNode;
import wyvc.builder.DataFlowGraph.OutputNode;
import wyvc.io.GraphPrinter.PrintableGraph;
import wyvc.lang.Type;
import wyvc.lang.TypedValue.Port.Mode;
import wyvc.utils.FunctionalInterfaces.Function;
import wyvc.utils.BiMap;
import wyvc.utils.Generators;
import wyvc.utils.Generators.Generator;
import wyvc.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

public class DataFlowGraph extends PrintableGraph<DataFlowGraph.DataNode, DataFlowGraph.DataArrow> {

	public abstract class DataArrow extends PrintableGraph.PrintableArrow<DataArrow, DataNode> {
		public final DataNode from;
		public final DataNode to;

		public DataArrow(DataNode from, DataNode to) {
			super(DataFlowGraph.this, from, to);
			this.from = from;
			this.to = to;
		}

		@Override
		public List<String> getOptions() {
//			if (from.block != to.block)
//				return /*from.block.getParent() == to.block || from.block == to.block.getParent()
//					? */Arrays.asList("arrowhead=\"box\"","color=purple")/*
//					: Arrays.asList("arrowhead=\"box\"","color=green")*/;
			if (from instanceof BackRegister && ((BackRegister)from).getPreviousValue() == this)
				return Collections.singletonList("dir=\"back\"");
			return Collections.emptyList();
		}
	}

	public class UnamedDataArrow extends DataArrow {
		public UnamedDataArrow(DataNode from, DataNode to) {
			super(from, to);
		}

		@Override
		public String getIdent() {
			return "";
		}
	}

	public class NamedDataArrow extends DataArrow {
		public final String ident;

		public NamedDataArrow(String ident, DataNode from, DataNode to) {
			super(from, to);
			this.ident = ident;
		}

		@Override
		public String getIdent() {
			return ident;
		}
	}



	public static class HalfArrow {
		public final DataNode node;
		public final String ident;
		public DataArrow arrow = null;

		public HalfArrow(DataNode node, String ident) {
			this.node = node;
			this.ident = ident;
		}
		public HalfArrow(HalfArrow other, String ident) {
			this.node = other.node;
			this.ident = ident;
		}

		public HalfArrow(DataNode node) {
			this(node,null);
		}

		public DataArrow complete(DataNode toward) {
			DataArrow arrow = ident == null
					? node.getGraph().new UnamedDataArrow(node, toward)
					: node.getGraph().new NamedDataArrow(ident, node, toward);
			this.arrow = arrow;
			return arrow;
		}

		@Override
		public String toString() {
			return node.toString()+"--"+(ident == null ? "" : ident)+"-->";
		}
	}


	@FunctionalInterface
	public static interface Duplicator {
		public HalfArrow duplicate(DataArrow arrow) throws CompilerException;
	}

	public static class UnsupportedDuplicationCompilerError extends CompilerError {
		private final DataNode node;

		public UnsupportedDuplicationCompilerError(DataNode node) {
			this.node = node;
		}

		@Override
		public String info() {
			return "The duplication of the "+node.getClass().getSimpleName()+" <"+node+"> is unsupported";
		}

		public static CompilerException exception(DataNode node) {
			return new CompilerException(new UnsupportedDuplicationCompilerError(node));
		}

	}


	public abstract class DataNode extends PrintableGraph.PrintableNode<DataNode, DataArrow> {
		public final Type type;
		public final String nodeIdent;
		public final Location<?> location;


		public DataNode(String label, Type type, List<HalfArrow> sources, Location<?> location) throws CompilerException {
			super(DataFlowGraph.this);
			sources.forEach((HalfArrow a) -> a.complete(this));
			this.type = type;
			this.location = location;
			nodeIdent = label;
		}
		public DataNode(String label, Type type, List<HalfArrow> sources) throws CompilerException {
			this(label, type, sources, null);
		}
		public DataNode(String label, Type type, Location<?> location) throws CompilerException {
			this(label, type, Collections.emptyList(), location);
		}
		public DataNode(String label, Type type) throws CompilerException {
			this(label, type, Collections.emptyList(), null);
		}

		private String getShortIdent() {
			return nodeIdent.length() < 19 ? nodeIdent : nodeIdent.substring(0, 12)+"..."+nodeIdent.substring(nodeIdent.length()-4);
		}

		@Override
		public String getIdent() {
			return getShortIdent()+"\n"+(type == null ? "Untyped" : type.toString());
		}

		@Override
		public List<String> getOptions() {
			return Collections.emptyList();
		}

		public DataFlowGraph getGraph() {
			return DataFlowGraph.this;
		}

		public final Type getType() {
			return type;
		}
		public final String getNodeIdent() {
			return nodeIdent;
		}


		protected void addSource(HalfArrow source) {
			source.complete(this);
		}

		public abstract boolean isStaticallyKnown();

		public abstract DataNode duplicate(DataFlowGraph graph, Duplicator duplicator) throws CompilerException;

	}

	public abstract class InterfaceNode extends DataNode {
		public final Mode mode;

		public InterfaceNode(String ident, Type type, Mode mode) throws CompilerException {
			super(ident, type);
			this.mode = mode;
		}

		public InterfaceNode(String ident, HalfArrow node) throws CompilerException {
			super(ident, node.node.type, Collections.singletonList(node));
			this.mode = Mode.OUT;
		}
		@Override
		public boolean isStaticallyKnown() {
			return false;
		}
	}

	public class InputNode extends InterfaceNode {
		public InputNode(String ident, Type type) throws CompilerException {
			super(ident, type, Mode.IN);
			inputs.add(this);
		}

		@Override
		public DataNode duplicate(DataFlowGraph graph, Duplicator duplicator) throws CompilerException {
			return graph.new InputNode(nodeIdent, type);
		}

		@Override
		protected void removed() {
			inputs.remove(this);
		}

	}

	public class OutputNode extends InterfaceNode {
		public final DataArrow source;

		public OutputNode(String ident, HalfArrow data) throws CompilerException {
			super(ident, data);
			outputs.add(this);
			this.source = data.arrow;
		}

		@Override
		public DataNode duplicate(DataFlowGraph graph, Duplicator duplicator) throws CompilerException {
			return graph.new OutputNode(nodeIdent, duplicator.duplicate(source));
		}


		@Override
		protected void removed() {
			outputs.remove(this);
		}
	}

	public class FunctionReturnNode extends DataNode {
		public final FuncCallNode fct;

		public FunctionReturnNode(String label, Type type, FuncCallNode fct) throws CompilerException {
			super(label, type, Collections.singletonList(new HalfArrow(fct)));
			this.fct = fct;
			fct.returns.add(this);
		}

		@Override
		public boolean isStaticallyKnown() {
			return false;
		}

		@Override
		public DataNode duplicate(DataFlowGraph graph, Duplicator duplicator) throws CompilerException {
			// TODO
			throw UnsupportedDuplicationCompilerError.exception(this);
		}
	}


	public class ConstNode extends DataNode {
		public ConstNode(Location<Bytecode.Const> decl, Type type) throws CompilerException {
			super(decl.getBytecode().constant().toString(), type, Collections.emptyList(), decl);
		}
		public ConstNode(String value, Type type) throws CompilerException {
			super(value, type, Collections.emptyList());
		}

		public ConstNode(String value, Type type, Location<?> location) throws CompilerException {
			super(value, type, Collections.emptyList(), location);
		}

		@Override
		public boolean isStaticallyKnown() {
			return true;
		}

		@Override
		public DataNode duplicate(DataFlowGraph graph, Duplicator duplicator) throws CompilerException {
			return graph.new ConstNode(nodeIdent, type, location);
		}
	}

	public class UndefConstNode extends ConstNode {
		public UndefConstNode(Type type) throws CompilerException {
			super(type.getDefault(), type);
		}

		@Override
		public boolean isStaticallyKnown() {
			return true;
		}
	}

	public final ConstNode getTrue() throws CompilerException {
		return new ConstNode("true", Type.Boolean);
	}
	public final ConstNode getFalse() throws CompilerException {
		return new ConstNode("false", Type.Boolean);
	}

	public static enum UnaryOperation {
		Not;
	}

	public final class UnaOpNode extends DataNode {
		public final UnaryOperation kind;
		public final DataArrow op;

		public UnaOpNode(UnaryOperation kind, Type type, HalfArrow op, Location<?> location) throws CompilerException {
			super(kind.name(), type, Collections.singletonList(op), location);
			this.kind = kind;
			this.op = op.arrow;
		}
		public UnaOpNode(UnaryOperation kind, Type type, HalfArrow op) throws CompilerException {
			this(kind, type, op, null);
		}


		@Override
		public boolean isStaticallyKnown() {
			return op.from.isStaticallyKnown();
		}

		@Override
		public DataNode duplicate(DataFlowGraph graph, Duplicator duplicator) throws CompilerException {
			return graph.new UnaOpNode(kind, type, duplicator.duplicate(op), location);
		}
	}

	public static enum BinaryOperation {
		Add, Sub, Mul, Div, Rem,
		And, Or, Xor,
		Eq, Ne,
		Lt, Le, Gt, Ge;
	}

	public final class BinOpNode extends DataNode {
		public final BinaryOperation kind;
		public final DataArrow op1;
		public final DataArrow op2;

		public BinOpNode(BinaryOperation kind, Type type, HalfArrow op1, HalfArrow op2, Location<?> location) throws CompilerException{
			super(kind.toString(), type, Arrays.asList(op1, op2), location);
			this.kind = kind;
			this.op1 = op1.arrow;
			this.op2 = op2.arrow;
		}
		public BinOpNode(BinaryOperation kind, Type type, HalfArrow op1, HalfArrow op2) throws CompilerException{
			this(kind, type, op1, op2, null);
		}


		@Override
		public List<String> getOptions() {
			return Arrays.asList("shape=\"rectangle\"","style=filled","fillcolor=lemonchiffon");
		}

		@Override
		public boolean isStaticallyKnown() {
			return op1.from.isStaticallyKnown() && op2.from.isStaticallyKnown();
		}

		@Override
		public DataNode duplicate(DataFlowGraph graph, Duplicator duplicator) throws CompilerException {
			return graph.new BinOpNode(kind, type, duplicator.duplicate(op1), duplicator.duplicate(op2), location);
		}
	}

	public final class FuncCallNode extends DataNode {
		public final String funcName;
		public final List<FunctionReturnNode> returns = new ArrayList<>();
		public final List<DataArrow> args;

		public FuncCallNode(String funcName, List<HalfArrow> args, Location<?> location) throws CompilerException {
			super(funcName + "()", null, args, location);
			invokes.add(this);
			this.funcName = funcName;
			this.args = Utils.convert(args, (HalfArrow a) -> a.arrow);
		}


		public FuncCallNode(Location<Bytecode.Invoke> call, List<HalfArrow> args) throws CompilerException {
			this(call.getBytecode().name().name(), args, call);
		}

		public final Generator<FunctionReturnNode> getReturns() {
			return Generators.fromCollection(returns);
		}

		@Override
		public boolean isStaticallyKnown() {
			return false;
		}

		@Override
		public DataNode duplicate(DataFlowGraph graph, Duplicator duplicator) throws CompilerException {
			return graph.new FuncCallNode(funcName, Generators.fromCollection(args).map_(duplicator::duplicate).toList(), location);
		}

		@Override
		protected void removed() {
			invokes.remove(this);
		}
	}

	public final class EndIfNode extends DataNode {
		public final DataArrow condition;
		public final DataArrow trueNode;
		public final DataArrow falseNode;

		public EndIfNode(HalfArrow condition, HalfArrow trueNode, HalfArrow falseNode, Location<?> location) throws CompilerException {
			super("mux", trueNode.node.type, Arrays.asList(condition, trueNode, falseNode), location);
			this.condition = condition.arrow;
			this.trueNode  =  trueNode.arrow;
			this.falseNode = falseNode.arrow;

		}

		public EndIfNode(HalfArrow condition, HalfArrow trueNode, HalfArrow falseNode) throws CompilerException {
			this(condition, trueNode, falseNode, null);
		}

		@Override
		public List<String> getOptions() {
			return Arrays.asList("shape=\"rectangle\"","style=filled","fillcolor=lemonchiffon");
		}

		@Override
		public boolean isStaticallyKnown() {
			return false;//TODO complex
		}


		@Override
		public DataNode duplicate(DataFlowGraph graph, Duplicator duplicator) throws CompilerException {
			return graph.new EndIfNode(duplicator.duplicate(condition), duplicator.duplicate(trueNode), duplicator.duplicate(falseNode), location);
		}
	}

	public final class WhileResultNode extends DataNode {
		public final WhileNode whileNode;

		public WhileResultNode(WhileNode node, Type type, String ident) throws CompilerException {
			super(ident, type, Collections.singletonList(new HalfArrow(node)));
			whileNode = node;
		}

		@Override
		public boolean isStaticallyKnown() {
			return false;
		}

		@Override
		public DataNode duplicate(DataFlowGraph graph, Duplicator duplicator) throws CompilerException {
			throw UnsupportedDuplicationCompilerError.exception(this);
		}

	}

	public final class WhileNode extends DataNode {
		public final DataFlowGraph condition;
		public final BiMap<DataNode, InputNode> cInputs;
		public final DataNode conditionValue;
		public final DataFlowGraph body;
		public final BiMap<DataNode, InputNode> bInputs;
		public final BiMap<OutputNode, WhileResultNode> bOutputs = new BiMap<>();

		public WhileNode(DataFlowGraph condition, BiMap<DataNode, InputNode> cInputs, DataNode conditionValue,
				DataFlowGraph body, BiMap<DataNode, InputNode> bInputs, Location<Bytecode.While> location) throws CompilerException {
			super("While", null,  cInputs.getValues().appendPair(bInputs.getValues().filter((i,f) -> !cInputs.containsKey(i))).mapSecond(InputNode::getNodeIdent).map_((n,s) -> new HalfArrow(n,s)).toList(), location);
			this.condition = condition;
			this.cInputs = cInputs;
			this.conditionValue = conditionValue;
			this.body = body;
			this.bInputs = bInputs;
			whileNodes.add(this);
			updateLatency(Latency.UnknownDelay);
		}

		@Override
		public List<String> getOptions() {
			return Arrays.asList("shape=\"octagon\"","style=filled","fillcolor=green");
		}

		@Override
		public boolean isStaticallyKnown() {
			return false;
		}

		@Override
		public DataNode duplicate(DataFlowGraph graph, Duplicator duplicator) throws CompilerException {
			throw UnsupportedDuplicationCompilerError.exception(this);
		}


		public DataNode createResult(OutputNode node) throws CompilerException {
			WhileResultNode rNode = new WhileResultNode(this, node.type, node.nodeIdent);
			bOutputs.put(node, rNode);
			return rNode;
		}

	}


	public final class BackRegister extends DataNode {
		private DataArrow previousValue = null;

		public BackRegister(Type type) throws CompilerException {
			super("Reg", type, Collections.emptyList());
		}

		@Override
		public List<String> getOptions() {
			return Arrays.asList("shape=\"hexagon\"","style=filled","fillcolor=bisque");
		}

		@Override
		public boolean isStaticallyKnown() {
			return false;
		}

		DataArrow getPreviousValue() {
			return previousValue;
		}

		public void setPreviousValue(DataArrow value) {
			// TODO throw exception if already set !
			this.previousValue = value;
		}

		@Override
		public DataNode duplicate(DataFlowGraph graph, Duplicator duplicator) throws CompilerException {
			throw UnsupportedDuplicationCompilerError.exception(this);
//			return graph.new BackRegister(duplicator.duplicate(previousValue));
		}
	}

	public final class Register extends DataNode {
		public final DataArrow previousValue;
		public final int delay;

		public Register(HalfArrow previousValue) throws CompilerException {
			super("Reg", previousValue.node.type, Collections.singletonList(previousValue));
			this.previousValue  =  previousValue.arrow;
			this.delay = 1;
		}

		public Register(HalfArrow previousValue, int delay) throws CompilerException {
			super("Reg", previousValue.node.type, Collections.singletonList(previousValue));
			this.previousValue  =  previousValue.arrow;
			this.delay = delay; // TODO check delay > 0
		}

		@Override
		public List<String> getOptions() {
			return Arrays.asList("shape=\"hexagon\"","style=filled","fillcolor=bisque");
		}

		@Override
		public boolean isStaticallyKnown() {
			return false;
		}


		@Override
		public DataNode duplicate(DataFlowGraph graph, Duplicator duplicator) throws CompilerException {
			return graph.new Register(duplicator.duplicate(previousValue), delay);
		}
	}
	public final class BufferFlag extends DataNode {
		public final DataArrow buffer;

		public BufferFlag(HalfArrow buffer) throws CompilerException {
			super("bFl", Type.Boolean, Collections.singletonList(buffer));
			this.buffer = buffer.arrow;
		}

		@Override
		public boolean isStaticallyKnown() {
			return false;
		}

		@Override
		public DataNode duplicate(DataFlowGraph graph, Duplicator duplicator) throws CompilerException {
			throw UnsupportedDuplicationCompilerError.exception(this);
		}
	}

	public final class Buffer extends DataNode {
		public final DataArrow write;
		public final DataArrow read;
		public final DataArrow previousValue;
		public final BufferFlag isEmpty = new BufferFlag(new HalfArrow(this, "isEmpty"));
		public final BufferFlag isFull = new BufferFlag(new HalfArrow(this, "isFull"));
		public final int size;

		public Buffer(HalfArrow write, HalfArrow read, HalfArrow previousValue, int size) throws CompilerException {
			super("Reg", previousValue.node.type, Arrays.asList(write, read, previousValue));
			this.write  =  write.arrow;
			this.read  =  read.arrow;
			this.previousValue  =  previousValue.arrow;
			this.size = size;
		}

		@Override
		public List<String> getOptions() {
			return Arrays.asList("shape=\"hexagon\"","style=filled","fillcolor=bisque");
		}

		@Override
		public boolean isStaticallyKnown() {
			return false;
		}

		@Override
		public DataNode duplicate(DataFlowGraph graph, Duplicator duplicator) throws CompilerException {
			return graph.new Buffer(duplicator.duplicate(write), duplicator.duplicate(read), duplicator.duplicate(previousValue), size);
		}
	}


//	public final class Queue extends DataNode {
//		public final DataArrow clock;
//		public final DataArrow write;
//		public final DataArrow read;
//
//
//		public Queue(Type type) throws CompilerException {
//			super("LReg", type);
//			// TODO Auto-generated constructor stub
//		}
//
//		@Override
//		public List<String> getOptions() {
//			return Arrays.asList("shape=\"hexagon\"","style=filled","fillcolor=bisque");
//		}
//
//		@Override
//		public boolean isStaticallyKnown() {
//			return false;
//		}
//		@Override
//		public DataNode duplicate(Duplicator duplicator) throws CompilerException {
//			return new Queue(duplicator.duplicate(previousValue));
//		}
//
//	}
//
//
//









	public static class UncompleteLatencyCompilerError extends CompilerError {
		private final DataFlowGraph graph;

		public UncompleteLatencyCompilerError(DataFlowGraph graph) {
			this.graph = graph;
		}

		@Override
		public String info() {
			return "The computation of the latency of "+graph+" is not yet completed";
		}

		public static CompilerException exception(DataFlowGraph graph) {
			return new CompilerException(new UncompleteLatencyCompilerError(graph));
		}
	}



	public List<InputNode> inputs = new ArrayList<>();
	public List<OutputNode> outputs = new ArrayList<>();
	public List<FuncCallNode> invokes = new ArrayList<>();
	public List<WhileNode> whileNodes = new ArrayList<>();

	public enum Latency {
		NullDelay,
		KnownDelay,
		UnknownDelay;
	}
	private Latency latency = Latency.NullDelay;
	private boolean latencyCompleted = false;
	private void updateLatency(Latency latency) {
		if (latency.ordinal() > this.latency.ordinal())
			this.latency = latency;
	}
	public Latency getLatency() throws CompilerException {
		if (latencyCompleted)
			return latency;
		throw UncompleteLatencyCompilerError.exception(this);
	}
	public void updateInvokeLatency(Function<String, DataFlowGraph> graphs) throws CompilerException {
		getInvokesNodes().map(f -> f.funcName).map(graphs).forEach_(g -> {
			if (g == null)
				throw UncompleteLatencyCompilerError.exception(g);
			updateLatency(g.getLatency());
		});
		latencyCompleted = true;
	}


	private void checkNodeUsefull(DataNode node) {
		if (!(node instanceof InterfaceNode) && node.targets.size() == 0) {
			List<DataArrow> sources = new ArrayList<>(node.sources);
			removeNode(node);
			for (DataArrow a : sources)
				checkNodeUsefull(a.from);
		}
	}

	public void removeUselessNodes() {
		List<DataNode> nodes = new ArrayList<>();
		nodes.addAll(this.nodes);
		for (DataNode n : nodes)
			checkNodeUsefull(n);
	}

	public Generator<InputNode> getInputNodes() {
		return Generators.fromCollection(new ArrayList<>(inputs));
	}
	public Generator<OutputNode> getOutputNodes() {
		return Generators.fromCollection(outputs);
	}
	public Generator<FuncCallNode> getInvokesNodes() {
		return Generators.fromCollection(invokes);
	}



	public void addSeparation() {
		updateLatency(Latency.KnownDelay);
	}

	@Override
	public List<DataNode> getInputs() {
		return Utils.convert(inputs);
	}

	@Override
	public List<DataNode> getOutputs() {
		return Utils.convert(outputs);
	}
}
