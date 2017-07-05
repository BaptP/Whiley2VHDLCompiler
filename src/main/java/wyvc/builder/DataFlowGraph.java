package wyvc.builder;

import java.util.List;
import java.util.Set;

import wyil.lang.Bytecode;
import wyil.lang.SyntaxTree.Location;
import wyvc.builder.CompilerLogger.CompilerError;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.io.GraphPrinter.PrintableGraph;
import wyvc.lang.Type;
import wyvc.lang.TypedValue.Port.Mode;
import wyvc.utils.FunctionalInterfaces.Function;
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
		public final int part;
		public final String nodeIdent;
		public final NodeBlock block;
		public final Location<?> location;


		public DataNode(String label, Type type, List<HalfArrow> sources, Location<?> location) throws CompilerException {
			super(DataFlowGraph.this);
			sources.forEach((HalfArrow a) -> a.complete(this));
			this.type = type;
			this.location = location;
			part = separation;
			nodeIdent = label;
			block = DataFlowGraph.this.block;
			block.addNode(this);
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

		@Override
		protected void removed() {
			try {
				block.removeNode(this);
			} catch (CompilerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println(e.error.info());
			}
		}
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
		public ConstNode(Type type, String value) throws CompilerException {
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
			super(type, type.getDefault());
		}

		@Override
		public boolean isStaticallyKnown() {
			return true;
		}
	}

	public final ConstNode getTrue() throws CompilerException {
		return new ConstNode(Type.Boolean, "true");
	}
	public final ConstNode getFalse() throws CompilerException {
		return new ConstNode(Type.Boolean, "false");
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


	public final class WhileNode extends DataNode {
		public final DataArrow value;
		public final BackRegister register;


		private WhileNode(HalfArrow value, BackRegister register, Location<?> location) throws CompilerException {
			super("While", value.node.type, Arrays.asList(value, new HalfArrow(register, "reg_"+value.ident)), location);
			this.value = value.arrow;
			this.register = register;
		}
		public WhileNode(HalfArrow value, Location<?> location) throws CompilerException {
			this(value, new BackRegister(value.node.type), location);
		}
		public WhileNode(HalfArrow value) throws CompilerException {
			this(value, null);
		}


		@Override
		public List<String> getOptions() {
			return Arrays.asList("shape=\"octagon\"","style=filled","fillcolor=bisque");
		}

		@Override
		public boolean isStaticallyKnown() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public DataNode duplicate(DataFlowGraph graph, Duplicator duplicator) throws CompilerException {
			return graph.new WhileNode(duplicator.duplicate(value), location);
		}
	}

	public final class EndWhileNode extends DataNode {
		public final DataArrow condition;
		public final WhileNode previousValue;
		public final DataArrow nextValue;
		public final DataArrow register;

		private EndWhileNode(HalfArrow condition, WhileNode previousValue, HalfArrow nextValue, HalfArrow register, Location<?> location) throws CompilerException {
			super("EndWhile", previousValue.type, Arrays.asList(condition, new HalfArrow(previousValue, "pre_"+nextValue.ident), nextValue, register), location);
			this.condition = condition.arrow;
			this.previousValue  =  previousValue;
			this.nextValue = nextValue.arrow;
			this.register = register.arrow;
			previousValue.register.setPreviousValue(this.register);
			updateLatency(Latency.UnknownDelay);
		}
		public EndWhileNode(HalfArrow condition, WhileNode previousValue, HalfArrow nextValue, Location<?> location) throws CompilerException {
			this(condition, previousValue, nextValue, new HalfArrow(previousValue.register, "reg_"+nextValue.ident), location);
		}
		public EndWhileNode(HalfArrow condition, WhileNode previousValue, HalfArrow nextValue) throws CompilerException {
			this(condition, previousValue, nextValue, null);
		}

		@Override
		public List<String> getOptions() {
			return Arrays.asList("shape=\"octagon\"","style=filled","fillcolor=bisque");
		}

		@Override
		public boolean isStaticallyKnown() {
			return false;//TODO complex
		}


		@Override
		public DataNode duplicate(DataFlowGraph graph, Duplicator duplicator) throws CompilerException {
			throw UnsupportedDuplicationCompilerError.exception(this);
//			return graph.new EndWhileNode(duplicator.duplicate(condition), duplicator.duplicate(previousValue), duplicator.duplicate(nextValue), duplicator.duplicate(register));
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








	public static class UnsuitableNodeCompilerError extends CompilerError {
		private final NodeBlock block;
		private final DataNode node;

		public UnsuitableNodeCompilerError(NodeBlock block, DataNode node) {
			this.block = block;
			this.node = node;
		}

		@Override
		public String info() {
			return "The node "+node.getClass().getSimpleName()+" <"+node+"> is not compatible with the "+block.getClass().getSimpleName()+" <"+block+">";
		}

		public static CompilerException exception(NodeBlock block, DataNode node) {
			return new CompilerException(new UnsuitableNodeCompilerError(block, node));
		}
	}

	public abstract class NodeBlock {
		private final NodeBlock parent;

		public NodeBlock(NodeBlock parent) {
			this.parent = parent;
			if (parent instanceof GraphBlock)
				((GraphBlock)parent).addNestedBlock(this);
		}

		public NodeBlock getParent() {
			return parent;
		}

		public void setCurrent() {
			setBlock(this);
		}

		public void close() throws CompilerException {
			if (block == this && block.parent != null)
				setBlock(this.getParent());
			else
				throw InvalidBlockOperationCompilationError.exception(this);
		}


		public void addNode(DataNode node) throws CompilerException {
			throw UnsuitableNodeCompilerError.exception(this, node);
		}

		public void removeNode(DataNode node) throws CompilerException { // TODO Use CompilerDebug ?
			throw UnsuitableNodeCompilerError.exception(this, node);
		}

		public abstract Generator<? extends NodeBlock> getNestedBlocks();
		public abstract int getNumberOfNestedBlocks();
		public abstract Generator<DataNode> getNodes();
		public abstract int getNumberOfNodes();
	}


//	public class CallBlock implements InnerBlock {
//		private final GraphBlock call;
//
//		public CallBlock(GraphBlock call) {
//			this.call = call;
//		}
//
//		@Override
//		public int getNumberOfNestedBlocks() {
//			return 1;
//		}
//
//		@Override
//		public Generator<GraphBlock> getNestedBlocks() {
//			return Generators.fromSingleton(call);
//		}
//
//	}

	public class WhileBlock extends NodeBlock {
		public final ComputationBlock condition = new ComputationBlock(this);
		public final ComputationBlock body = new ComputationBlock(this);
		private final Set<BackRegister> registerNodes = new HashSet<>();
		private final Set<WhileNode> whileNodes = new HashSet<>();
		private final Set<EndWhileNode> endWhileNodes = new HashSet<>();

		public WhileBlock(GraphBlock parent) {
			super(parent);
		}

		@Override
		public int getNumberOfNestedBlocks() {
			return 2;
		}

		@Override
		public Generator<ComputationBlock> getNestedBlocks() {
			return Generators.fromCollection(Arrays.asList(condition, body));
		}

		@Override
		public void addNode(DataNode node) throws CompilerException {
			if (node instanceof WhileNode)
				whileNodes.add((WhileNode) node);
			else if (node instanceof EndWhileNode)
				endWhileNodes.add((EndWhileNode) node);
			else if (node instanceof BackRegister)
				registerNodes.add((BackRegister) node);
			else
				throw UnsuitableNodeCompilerError.exception(this, node);
		}

		@Override
		public void removeNode(DataNode node) throws CompilerException {
			if (node instanceof WhileNode)
				whileNodes.remove((WhileNode) node);
			else if (node instanceof EndWhileNode)
				endWhileNodes.remove((EndWhileNode) node);
			else if (node instanceof BackRegister)
				registerNodes.remove((BackRegister) node);
			else
				throw UnsuitableNodeCompilerError.exception(this, node);
		}

		public void openCondition() {
			setBlock(condition);
		}

		public void openBody() {
			setBlock(body);
		}

		public Generator<WhileNode> getWhileNodes() {
			return Generators.fromCollection(whileNodes);
		}
		public Generator<EndWhileNode> getEndWhileNodes() {
			return Generators.fromCollection(endWhileNodes);
		}
		public Generator<BackRegister> getRegisters() {
			return Generators.fromCollection(registerNodes);
		}
		@Override
		public Generator<DataNode> getNodes() {
			return Generators.<DataNode>fromCollection(whileNodes).append(Generators.fromCollection(registerNodes)).append(Generators.fromCollection(endWhileNodes));
		}

		@Override
		public int getNumberOfNodes() {
			return whileNodes.size()+registerNodes.size()+endWhileNodes.size();
		}
	}


	public class IfBlock extends NodeBlock {
		public final ComputationBlock condition = new ComputationBlock(this);;
		public final ComputationBlock trueBranch = new ComputationBlock(this);
		public final ComputationBlock falseBranch = new ComputationBlock(this);
		public final Set<EndIfNode> endIfNode = new HashSet<>();
		public HalfArrow conditionNode = null;

		public IfBlock(GraphBlock parent) {
			super(parent);
		}

		@Override
		public int getNumberOfNestedBlocks() {
			return 3;
		}

		@Override
		public Generator<ComputationBlock> getNestedBlocks() {
			return Generators.fromCollection(Arrays.asList(condition, trueBranch, falseBranch));
		}

		@Override
		public Generator<DataNode> getNodes() {
			return Generators.<DataNode>fromCollection(endIfNode);
		}

		@Override
		public int getNumberOfNodes() {
			return endIfNode.size();
		}

		@Override
		public void addNode(DataNode node) throws CompilerException {
			if (node instanceof EndIfNode)
				endIfNode.add((EndIfNode) node);
			else
				throw UnsuitableNodeCompilerError.exception(this, node);
		}

		@Override
		public void removeNode(DataNode node) throws CompilerException {
			if (node instanceof EndIfNode)
				endIfNode.remove((EndIfNode) node);
			else
				throw UnsuitableNodeCompilerError.exception(this, node);
		}

		public void openCondition() {
			setBlock(condition);
		}

		public void openTrueBranch() {
			setBlock(trueBranch);
		}

		public void openFalseBranch() {
			setBlock(falseBranch);
		}

	}

	public class GraphBlock extends NodeBlock {
		public final ComputationBlock parent;
		public final int index;
		private final Set<DataNode> nodes = new HashSet<>();
		private final List<NodeBlock> nestedBlocks = new ArrayList<>();

		public GraphBlock(ComputationBlock parent, int index) {
			super(parent);
			this.index = index;
			this.parent = parent;
			parent.addNestedBlock(this);
		}

		@Override
		public final Generator<NodeBlock> getNestedBlocks() {
			return Generators.fromCollection(nestedBlocks);
		}

		@Override
		public final int getNumberOfNestedBlocks() {
			return nestedBlocks.size();
		}

		public final void addNestedBlock(NodeBlock block) {
			nestedBlocks.add(block);
		}

		@Override
		public void addNode(DataNode node) throws CompilerException {
			nodes.add(node);
		}

		@Override
		public void removeNode(DataNode node) throws CompilerException {
			nodes.remove(node);
		}

		@Override
		public Generator<DataNode> getNodes() {
			return Generators.fromCollection(nodes);
		}

		@Override
		public int getNumberOfNodes() {
			return nodes.size();
		}

		@Override
		public void close() throws CompilerException {
			if (block.parent != null)
				setBlock(block.getParent());
			else
				throw InvalidBlockOperationCompilationError.exception(this);
		}

	}




	public class ComputationBlock extends NodeBlock {
		private final List<GraphBlock> nestedBlocks = new ArrayList<>();
		private final List<Set<Register>> registers = new ArrayList<>();

		public ComputationBlock(NodeBlock parent) {
			super(parent);
		}

		@Override
		public final Generator<GraphBlock> getNestedBlocks() {
			return Generators.fromCollection(nestedBlocks);
		}

		@Override
		public final int getNumberOfNestedBlocks() {
			return nestedBlocks.size();
		}

		public final void addNestedBlock(GraphBlock block) {
			nestedBlocks.add(block);
		}

		@Override
		public void addNode(DataNode node) throws CompilerException {
			System.out.println("Add "+node);
			if (node instanceof Register)
				registers.get(registers.size()-1).add((Register) node);
			else
				throw UnsuitableNodeCompilerError.exception(this, node);
		}

		@Override
		public void removeNode(DataNode node) throws CompilerException {
			System.out.println("remove "+node);
			if (node instanceof Register)
				registers.remove((Register) node);
			else
				throw UnsuitableNodeCompilerError.exception(this, node);
		}

		@Override
		public Generator<DataNode> getNodes() {
			return Generators.emptyGenerator();
		}

		@Override
		public int getNumberOfNodes() {
			return 0;
		}

		@Override
		public void setCurrent() {
			registers.add(new HashSet<>());
			setBlock(this);
		}
	}



	public class FunctionBlock extends NodeBlock {
		public final ComputationBlock inputs = new ComputationBlock(this);
		public final GraphBlock inputsGraph = new GraphBlock(inputs, 0);
		public final ComputationBlock body = new ComputationBlock(this);
		public final ComputationBlock outputs = new ComputationBlock(this);
		public final GraphBlock outputsGraph = new GraphBlock(outputs, 0);

		public FunctionBlock() {
			super(null);
		}

		public void openInputs() {
			setBlock(inputsGraph);
		}

		public void openBody() {
			setBlock(body);
		}

		public void openOutputs() {
			setBlock(outputsGraph);
		}

		@Override
		public Generator<ComputationBlock> getNestedBlocks() {
			return Generators.fromCollection(Arrays.asList(inputs, body, outputs));
		}

		@Override
		public int getNumberOfNestedBlocks() {
			return 3;
		}

		@Override
		public Generator<DataNode> getNodes() {
			return Generators.emptyGenerator();
		}

		@Override
		public int getNumberOfNodes() {
			return 0;
		}
	}





	public static class InvalidBlockOperationCompilationError extends CompilerError {
		private final NodeBlock block;

		public InvalidBlockOperationCompilationError(NodeBlock block) {
			this.block = block;
		}

		@Override
		public String info() {
			return "The requested operation is not permitted with the "+block.getClass().getSimpleName()+" <"+block+">";
		}

		public static CompilerException exception(NodeBlock block) {
			return new CompilerException(new InvalidBlockOperationCompilationError(block));
		}
	}



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
	public FunctionBlock topLevelBlock = new FunctionBlock();

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

	private NodeBlock block = topLevelBlock;
	private <T extends NodeBlock> T setBlock(T block) {
		this.block = block;
//		System.out.println("Block Changed : " +block);
		return block;
	}


	private int separation = 0;



	public WhileBlock openWhileBlock() throws CompilerException {
		if (block instanceof GraphBlock)
			return setBlock(new WhileBlock((GraphBlock)block));
		throw InvalidBlockOperationCompilationError.exception(block);
	}
	public IfBlock openIfBlock() throws CompilerException {
		if (block instanceof GraphBlock)
			return setBlock(new IfBlock((GraphBlock)block));
		throw InvalidBlockOperationCompilationError.exception(block);
	}
	public GraphBlock openNestedBlock() throws CompilerException {
		if (block instanceof ComputationBlock)
			return setBlock(new GraphBlock((ComputationBlock)block, 0));
		throw InvalidBlockOperationCompilationError.exception(block);
	}


	public void openFollowingBlock() throws CompilerException {
//		System.out.println(block + " --->");
		if (block instanceof ComputationBlock)
			setBlock(new GraphBlock((ComputationBlock)block, ((ComputationBlock)block).nestedBlocks.size()));
		else
			throw InvalidBlockOperationCompilationError.exception(block);
//		System.out.println("  ---> "+block);
	}

	public void closeCurrentBlock() throws CompilerException {
//		System.out.println(block + " --->");
		if (block instanceof GraphBlock && block.parent != null)
			((GraphBlock)block).parent.setCurrent();
		else
			throw InvalidBlockOperationCompilationError.exception(block);
//		System.out.println("  ---> "+block);
	}

//	public void openConcurrentBlock() throws CompilerException {
//		if (block instanceof GraphBlock)
//			block = new GraphBlock(block.parent, ((GraphBlock)block).index);
//		else
//			throw InvalidBlockOperationCompilationError.exception(block);
//	}


	public Generator<InputNode> getInputNodes() {
		return Generators.fromCollection(inputs);
	}
	public Generator<OutputNode> getOutputNodes() {
		return Generators.fromCollection(outputs);
	}
	public Generator<FuncCallNode> getInvokesNodes() {
		return Generators.fromCollection(invokes);
	}



	public void addSeparation() {
		separation++;
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
