package wyvc.builder;

import java.util.List;

import wyil.lang.Bytecode;
import wyil.lang.Bytecode.OperatorKind;
import wyil.lang.SyntaxTree.Location;
import wyvc.io.GraphPrinter.PrintableGraph;
import wyvc.lang.Type;
import wyvc.lang.TypedValue.Port.Mode;
import wyvc.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

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
			if (from.part != to.part)
				return Arrays.asList("arrowhead=\"box\"","color=purple");
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

	@FunctionalInterface
	public static interface Duplicator {
		public <T extends DataNode> HalfArrow<T> duplicate(DataArrow<T, ?> arrow);
	}

	public abstract class DataNode extends PrintableGraph.PrintableNode<DataNode, DataArrow<?,?>> {
		public final Type type;
		public final int part;
		public final String nodeIdent;


		public DataNode(String label, Type type) {
			this(label, type, Collections.emptyList());
		}

		public DataNode(String label, Type type, List<HalfArrow<?>> sources) {
			super(DataFlowGraph.this);
			sources.forEach((HalfArrow<?> a) -> a.complete(this));
			this.type = type;
			part = separation;
			nodeIdent = label;
		}

		@Override
		public String getIdent() {
			return nodeIdent+"\n"+(type == null ? "Untyped" : type.toString());
		}

		@Override
		public List<String> getOptions() {
			return Collections.emptyList();
		}

		public DataFlowGraph getGraph() {
			return DataFlowGraph.this;
		}

		public abstract boolean isStaticallyKnown();

		public abstract DataNode duplicate(Duplicator duplicator);
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
	}

	public abstract class InterfaceNode extends DataNode {
		public final Mode mode;

		public InterfaceNode(String ident, Type type, Mode mode) {
			super(ident, type);
			this.mode = mode;
		}

		public InterfaceNode(String ident, HalfArrow<?> node) {
			super(ident, node.node.type, Collections.singletonList(node));
			this.mode = Mode.OUT;
		}
		@Override
		public boolean isStaticallyKnown() {
			return false;
		}
	}

	public class InputNode extends InterfaceNode {
		public InputNode(String ident, Type type) {
			super(ident, type, Mode.IN);
			inputs.add(this);
		}

		@Override
		public DataNode duplicate(Duplicator duplicator) {
			return new InputNode(nodeIdent, type);
		}

	}

	public class OutputNode extends InterfaceNode {
		public final DataArrow<?,?> source;

		public OutputNode(String ident, HalfArrow<?> data) {
			super(ident, data);
			outputs.add(this);
			this.source = data.arrow;
		}

		@Override
		public DataNode duplicate(Duplicator duplicator) {
			return new OutputNode(nodeIdent, duplicator.duplicate(source));
		}

	}

	public class FunctionReturnNode extends DataNode {
		public final DataArrow<FuncCallNode, ?> fct;

		public FunctionReturnNode(String label, Type type, HalfArrow<FuncCallNode> fct) {
			super(label, type, Collections.singletonList(fct));
			this.fct = fct.arrow;
			fct.node.returns.add(this);
		}

		@Override
		public boolean isStaticallyKnown() {
			return false;
		}

		@Override
		public DataNode duplicate(Duplicator duplicator) {
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

		public WyilNode(Location<T> location, Type type, List<HalfArrow<?>> sources) {
			super(DataFlowGraph.toString(location.getBytecode()), type, sources);
			this.location = location;
		}

		public WyilNode(String ident, Location<T> location, Type type, List<HalfArrow<?>> sources) {
			super(ident, type, sources);
			this.location = location;
		}

	}
	public abstract class InstructionNode<T extends Bytecode> extends WyilNode<T> {
		public InstructionNode(Location<T> location, Type type, List<HalfArrow<?>> sources) {
			super(location, type, sources);
		}

	}

	public final class ConstNode extends InstructionNode<Bytecode.Const> {
		public ConstNode(Location<Bytecode.Const> decl, Type type) {
			super(decl, type, Collections.emptyList());
		}

		@Override
		public boolean isStaticallyKnown() {
			return true;
		}

		@Override
		public DataNode duplicate(Duplicator duplicator) {
			return new ConstNode(location, type);
		}
	}

	public class ExternConstNode extends DataNode {
		public ExternConstNode(Type type, String value) {
			super(value, type);
		}

		@Override
		public boolean isStaticallyKnown() {
			return true;
		}

		@Override
		public DataNode duplicate(Duplicator duplicator) {
			return new ExternConstNode(type, nodeIdent);
		}
	}

	public final class UndefConstNode extends ExternConstNode {
		public UndefConstNode(Type type) {
			super(type, type.getDefault());
		}

		@Override
		public boolean isStaticallyKnown() {
			return true;
		}
	}

	public final ExternConstNode getTrue() {
		return new ExternConstNode(Type.Boolean, "true");
	}
	public final ExternConstNode getFalse() {
		return new ExternConstNode(Type.Boolean, "false");
	}

	public final class UnaOpNode extends InstructionNode<Bytecode.Operator> {
		public final DataArrow<?,?> op;

		public UnaOpNode(Location<Bytecode.Operator> binOp, Type type, HalfArrow<?> op) {
			super(binOp, type, Arrays.asList(op));
			this.op = op.arrow;
		}

		@Override
		public boolean isStaticallyKnown() {
			return op.from.isStaticallyKnown();
		}

		@Override
		public DataNode duplicate(Duplicator duplicator) {
			return new UnaOpNode(location, type, duplicator.duplicate(op));
		}
	}

	public final class BinOpNode extends WyilNode<Bytecode.Operator> {
		public final DataArrow<?,?> op1;
		public final DataArrow<?,?> op2;

		public BinOpNode(Location<Bytecode.Operator> binOp, Type type, HalfArrow<?> op1, HalfArrow<?> op2){
			super(binOp, type, Arrays.asList(op1, op2));
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
		public DataNode duplicate(Duplicator duplicator) {
			return new BinOpNode(location, type, duplicator.duplicate(op1), duplicator.duplicate(op2));
		}
	}

	public final class FuncCallNode extends WyilNode<Bytecode.Invoke> {
		public final String funcName;
		public final List<FunctionReturnNode> returns = new ArrayList<>();
		public final List<DataArrow<?, ?>> args;



		public FuncCallNode(Location<Bytecode.Invoke> call, List<HalfArrow<?>> args) {
			super(call, null, args);
			invokes.add(this);
			funcName = call.getBytecode().name().name();
			this.args = Utils.convert(args, (HalfArrow<?> a) -> a.arrow);
		}

		@Override
		public boolean isStaticallyKnown() {
			return false;
		}

		@Override
		public DataNode duplicate(Duplicator duplicator) {
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

		public EndIfNode(Location<Bytecode.If> ifs, HalfArrow<?> condition, HalfArrow<?> trueNode, HalfArrow<?> falseNode) {
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
		public DataNode duplicate(Duplicator duplicator) {
			return new EndIfNode(location, duplicator.duplicate(condition), duplicator.duplicate(trueNode), duplicator.duplicate(falseNode));
		}
	}




	private int separation = 0;


	public List<InputNode> inputs = new ArrayList<>();
	public List<OutputNode> outputs = new ArrayList<>();
	public List<FuncCallNode> invokes = new ArrayList<>();


	static private String toString(Bytecode b) {
		if (b instanceof Bytecode.Operator)
			return ((Bytecode.Operator) b).kind() == OperatorKind.IS ? "or" : ((Bytecode.Operator) b).kind().toString();
		if (b instanceof Bytecode.Const)
			return ((Bytecode.Const) b).constant().toString();
		if (b instanceof Bytecode.If)
			return "If";
		if (b instanceof Bytecode.Invoke)
			return ((Bytecode.Invoke) b).name().toString()+"()";
		return b.toString();
	}

	public void addSeparation() {
		separation++;
	}

	@Override
	public List<DataNode> getInputs() {
		return Utils.convert(inputs);
	}

	@Override
	public List<DataNode> getOutputs() {
		// TODO Auto-generated method stub
		return Utils.convert(outputs);
	}
}
