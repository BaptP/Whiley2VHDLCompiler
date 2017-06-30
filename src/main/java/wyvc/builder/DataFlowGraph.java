package wyvc.builder;

import java.util.List;
import java.util.Set;
import java.util.Stack;

import wyil.lang.Bytecode;
import wyil.lang.SyntaxTree.Location;
import wyvc.builder.CompilerLogger.CompilerError;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.io.GraphPrinter.PrintableGraph;
import wyvc.lang.Type;
import wyvc.lang.TypedValue.Port.Mode;
import wyvc.utils.FunctionalInterfaces.Function;
import wyvc.utils.FunctionalInterfaces.Supplier;
import wyvc.utils.Generators;
import wyvc.utils.Generators.Generator;
import wyvc.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

public class DataFlowGraph extends PrintableGraph<DataFlowGraph.DataNode, DataFlowGraph.DataArrow<?,?>> {

	public abstract class DataArrow<T extends DataNode, U extends DataNode> extends PrintableGraph.PrintableArrow<DataArrow<?,?>, DataNode> {
		public final T from;
		public final U to;

		public DataArrow(T from, U to) {
			super(DataFlowGraph.this, from, to);
			this.from = from;
			this.to = to;
		}

		@Override
		public List<String> getOptions() {
			if (from.block != to.block)
				return /*from.block.getParent() == to.block || from.block == to.block.getParent()
					? */Arrays.asList("arrowhead=\"box\"","color=purple")/*
					: Arrays.asList("arrowhead=\"box\"","color=green")*/;
			if (from instanceof Register || to instanceof Register)
				return Collections.singletonList("dir=\"back\"");
			return Collections.emptyList();
		}
	}

	public class UnamedDataArrow<T extends DataNode, U extends DataNode> extends DataArrow<T,U> {
		public UnamedDataArrow(T from, U to) {
			super(from, to);
		}

		@Override
		public String getIdent() {
			return "";
		}
	}

	public class NamedDataArrow<T extends DataNode, U extends DataNode> extends DataArrow<T,U> {
		public final String ident;

		public NamedDataArrow(String ident, T from, U to) {
			super(from, to);
			this.ident = ident;
		}

		@Override
		public String getIdent() {
			return ident;
		}
	}



	public static class HalfArrow<T extends DataNode> {
		public final T node;
		public final String ident;
		public DataArrow<T,?> arrow = null;

		public HalfArrow(T node, String ident) {
			this.node = node;
			this.ident = ident;
		}
		public HalfArrow(HalfArrow<T> other, String ident) {
			this.node = other.node;
			this.ident = ident;
		}

		public HalfArrow(T node) {
			this(node,null);
		}

		public <U extends DataNode> DataArrow<T,U> complete(U toward) {
			DataArrow<T,U> arrow = ident == null
					? node.getGraph().new UnamedDataArrow<T,U>(node, toward)
					: node.getGraph().new NamedDataArrow<T,U>(ident, node, toward);
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
		public <T extends DataNode> HalfArrow<T> duplicate(DataArrow<T, ?> arrow);
	}

	public abstract class DataNode extends PrintableGraph.PrintableNode<DataNode, DataArrow<?,?>> {
		public final Type type;
		public final int part;
		public final String nodeIdent;
		public final NodeBlock block;


		public DataNode(String label, Type type) throws CompilerException {
			this(label, type, Collections.emptyList());
		}

		public DataNode(String label, Type type, List<HalfArrow<?>> sources) throws CompilerException {
			super(DataFlowGraph.this);
			sources.forEach((HalfArrow<?> a) -> a.complete(this));
			this.type = type;
			part = separation;
			nodeIdent = label;
			block = DataFlowGraph.this.block;
			block.addNode(this);
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

		protected void addSource(HalfArrow<?> source) {
			source.complete(this);
		}

		public abstract boolean isStaticallyKnown();

		public abstract DataNode duplicate(Duplicator duplicator) throws CompilerException;

		@Override
		protected void removed() {
			try {
				block.removeNode(this);
			} catch (CompilerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public abstract class InterfaceNode extends DataNode {
		public final Mode mode;

		public InterfaceNode(String ident, Type type, Mode mode) throws CompilerException {
			super(ident, type);
			this.mode = mode;
		}

		public InterfaceNode(String ident, HalfArrow<?> node) throws CompilerException {
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
		public DataNode duplicate(Duplicator duplicator) throws CompilerException {
			return new InputNode(nodeIdent, type);
		}

	}

	public class OutputNode extends InterfaceNode {
		public final DataArrow<?,?> source;

		public OutputNode(String ident, HalfArrow<?> data) throws CompilerException {
			super(ident, data);
			outputs.add(this);
			this.source = data.arrow;
		}

		@Override
		public DataNode duplicate(Duplicator duplicator) throws CompilerException {
			return new OutputNode(nodeIdent, duplicator.duplicate(source));
		}

	}

	public class FunctionReturnNode extends DataNode {
		public final DataArrow<FuncCallNode, ?> fct;

		public FunctionReturnNode(String label, Type type, HalfArrow<FuncCallNode> fct) throws CompilerException {
			super(label, type, Collections.singletonList(fct));
			this.fct = fct.arrow;
			fct.node.returns.add(this);
		}
		public FunctionReturnNode(String label, Type type, FuncCallNode fct) throws CompilerException {
			this(label, type, new HalfArrow<>(fct));
		}

		@Override
		public boolean isStaticallyKnown() {
			return false;
		}

		@Override
		public DataNode duplicate(Duplicator duplicator) throws CompilerException {
			return new FunctionReturnNode(nodeIdent, type, duplicator.duplicate(fct));
		}
	}

	public abstract class WyilNode<T extends Bytecode> extends DataNode {
		public final Location<T> location;

		/*public WyilNode(Location<T> location, List<DataNode> sources) throws CompilerException {
			super(DataFlowGraph.toString(location.getBytecode()),
				getType(location.getType()), sources);
			this.location = location;
		}*/

		public WyilNode(Location<T> location, Type type, List<HalfArrow<?>> sources) throws CompilerException {
			super(DataFlowGraph.toString(location.getBytecode()), type, sources);
			this.location = location;
		}

		public WyilNode(String ident, Location<T> location, Type type, List<HalfArrow<?>> sources) throws CompilerException {
			super(ident, type, sources);
			this.location = location;
		}

	}
//	public abstract class InstructionNode<T extends Bytecode> extends WyilNode<T> {
//		public InstructionNode(Location<T> location, Type type, List<HalfArrow<?>> sources) throws CompilerException {
//			super(location, type, sources);
//		}
//
//	}

	public final class ConstNode extends WyilNode<Bytecode.Const> {
		public ConstNode(Location<Bytecode.Const> decl, Type type) throws CompilerException {
			super(decl, type, Collections.emptyList());
		}

		@Override
		public boolean isStaticallyKnown() {
			return true;
		}

		@Override
		public DataNode duplicate(Duplicator duplicator) throws CompilerException {
			return new ConstNode(location, type);
		}
	}

	public class ExternConstNode extends DataNode {
		public ExternConstNode(Type type, String value) throws CompilerException {
			super(value, type);
		}

		@Override
		public boolean isStaticallyKnown() {
			return true;
		}

		@Override
		public DataNode duplicate(Duplicator duplicator) throws CompilerException {
			return new ExternConstNode(type, nodeIdent);
		}
	}

	public final class UndefConstNode extends ExternConstNode {
		public UndefConstNode(Type type) throws CompilerException {
			super(type, type.getDefault());
		}

		@Override
		public boolean isStaticallyKnown() {
			return true;
		}
	}

	public final ExternConstNode getTrue() throws CompilerException {
		return new ExternConstNode(Type.Boolean, "true");
	}
	public final ExternConstNode getFalse() throws CompilerException {
		return new ExternConstNode(Type.Boolean, "false");
	}

	public static enum UnaryOperation {
		Not;
	}

	public final class UnaOpNode extends WyilNode<Bytecode.Operator> {
		public final UnaryOperation kind;
		public final DataArrow<?,?> op;

		public UnaOpNode(Location<Bytecode.Operator> binOp, UnaryOperation kind, Type type, HalfArrow<?> op) throws CompilerException {
			super(binOp, type, Arrays.asList(op));
			this.kind = kind;
			this.op = op.arrow;
		}

		@Override
		public boolean isStaticallyKnown() {
			return op.from.isStaticallyKnown();
		}

		@Override
		public DataNode duplicate(Duplicator duplicator) throws CompilerException {
			return new UnaOpNode(location, kind, type, duplicator.duplicate(op));
		}
	}

	public static enum BinaryOperation {
		Add, Sub, Mul, Div, Rem,
		And, Or, Xor,
		Eq, Ne,
		Lt, Le, Gt, Ge;
	}

	public final class BinOpNode extends WyilNode<Bytecode.Operator> {
		public final BinaryOperation kind;
		public final DataArrow<?,?> op1;
		public final DataArrow<?,?> op2;

		public BinOpNode(Location<Bytecode.Operator> binOp, BinaryOperation kind, Type type, HalfArrow<?> op1, HalfArrow<?> op2) throws CompilerException{
			super(kind.toString(), binOp, type, Arrays.asList(op1, op2));
			this.kind = kind;
			this.op1 = op1.arrow;
			this.op2 = op2.arrow;
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
		public DataNode duplicate(Duplicator duplicator) throws CompilerException {
			return new BinOpNode(location, kind, type, duplicator.duplicate(op1), duplicator.duplicate(op2));
		}
	}

	public final class FuncCallNode extends WyilNode<Bytecode.Invoke> {
		public final String funcName;
		public final List<FunctionReturnNode> returns = new ArrayList<>();
		public final List<DataArrow<?, ?>> args;



		public FuncCallNode(Location<Bytecode.Invoke> call, List<HalfArrow<?>> args) throws CompilerException {
			super(call, null, args);
			invokes.add(this);
			funcName = call.getBytecode().name().name();
			this.args = Utils.convert(args, (HalfArrow<?> a) -> a.arrow);
		}

		public final Generator<FunctionReturnNode> getReturns() {
			return Generators.fromCollection(returns);
		}

		@Override
		public boolean isStaticallyKnown() {
			return false;
		}

		@Override
		public DataNode duplicate(Duplicator duplicator) throws CompilerException {
			return new FuncCallNode(location, Utils.convert(args, (DataArrow<?, ?> a) -> duplicator.duplicate(a)));
		}

		/*public void inline(WyilSection func) throws CompilerException {
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
		}*/
	}

	public final class EndIfNode extends WyilNode<Bytecode.If> {
		public final DataArrow<?,?> condition;
		public final DataArrow<?,?> trueNode;
		public final DataArrow<?,?> falseNode;

		public EndIfNode(Location<Bytecode.If> ifs, HalfArrow<?> condition, HalfArrow<?> trueNode, HalfArrow<?> falseNode) throws CompilerException {
			super("mux", ifs, trueNode.node.type, Arrays.asList(condition, trueNode, falseNode));
			this.condition = condition.arrow;
			this.trueNode  =  trueNode.arrow;
			this.falseNode = falseNode.arrow;
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
		public DataNode duplicate(Duplicator duplicator) throws CompilerException {
			return new EndIfNode(location, duplicator.duplicate(condition), duplicator.duplicate(trueNode), duplicator.duplicate(falseNode));
		}
	}


	public final class WhileNode extends WyilNode<Bytecode.While> {
		public final DataArrow<?,?> value;

		public WhileNode(Location<Bytecode.While> whiles, HalfArrow<?> value) throws CompilerException {
			super("While", whiles, value.node.type, Collections.singletonList(value));
			this.value = value.arrow;
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
		public DataNode duplicate(Duplicator duplicator) throws CompilerException {
			return new WhileNode(location, duplicator.duplicate(value));
		}
	}

	public final class EndWhileNode extends WyilNode<Bytecode.While> {
		public final DataArrow<?,?> condition;
		public final DataArrow<?,?> previousValue;
		public final DataArrow<?,?> nextValue;

		public EndWhileNode(Location<Bytecode.While> whiles, HalfArrow<?> condition, HalfArrow<?> previousValue, HalfArrow<?> nextValue) throws CompilerException {
			super("EndWhile", whiles, previousValue.node.type, Arrays.asList(condition, previousValue, nextValue));
			this.condition = condition.arrow;
			this.previousValue  =  previousValue.arrow;
			this.nextValue = nextValue.arrow;
			updateLatency(Latency.UnknownDelay);
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
		public DataNode duplicate(Duplicator duplicator) throws CompilerException {
			return new EndWhileNode(location, duplicator.duplicate(condition), duplicator.duplicate(previousValue), duplicator.duplicate(nextValue));
		}
	}


	public final class Register extends DataNode {
		public final DataArrow<?,?> previousValue;
		public final int delay;

		public Register(HalfArrow<?> previousValue) throws CompilerException {
			super("Reg", previousValue.node.type, Collections.singletonList(previousValue));
			this.previousValue  =  previousValue.arrow;
			this.delay = 1;
		}

		public Register(HalfArrow<?> previousValue, int delay) throws CompilerException {
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
		public DataNode duplicate(Duplicator duplicator) throws CompilerException {
			return new Register(duplicator.duplicate(previousValue), delay);
		}
	}


//	public final class Queue extends DataNode {
//		public final DataArrow<?,?> clock;
//		public final DataArrow<?,?> write;
//		public final DataArrow<?,?> read;
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
		private final Set<Register> registerNodes = new HashSet<>();
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
			else if (node instanceof Register)
				registerNodes.add((Register) node);
			else
				throw UnsuitableNodeCompilerError.exception(this, node);
		}

		@Override
		public void removeNode(DataNode node) throws CompilerException {
			if (node instanceof WhileNode)
				whileNodes.remove((WhileNode) node);
			else if (node instanceof EndWhileNode)
				endWhileNodes.remove((EndWhileNode) node);
			else if (node instanceof Register)
				registerNodes.remove((Register) node);
			else
				throw UnsuitableNodeCompilerError.exception(this, node);
		}

		public void openCondition() {
			setBlock(condition);
		}

		public void openBody() {
			setBlock(body);
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
		public HalfArrow<?> conditionNode = null;

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
			throw UnsuitableNodeCompilerError.exception(this, node);
		}

		@Override
		public void removeNode(DataNode node) throws CompilerException {
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
		if (block instanceof GraphBlock && block.parent != null)
			setBlock(new GraphBlock(((GraphBlock)block).parent, ((GraphBlock)block).index+1));
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
}
