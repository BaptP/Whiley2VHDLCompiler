package wyvc.builder;

import java.util.List;
import java.util.Map;

import wyil.lang.Bytecode;
import wyil.lang.Bytecode.OperatorKind;
import wyil.lang.SyntaxTree.Location;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.io.GraphPrinter.PrintableGraph;
import wyvc.lang.Type;
import wyvc.lang.TypedValue.Port.Mode;
import wyvc.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

public class DataFlowGraph extends PrintableGraph<DataFlowGraph.DataNode, DataFlowGraph.DataArrow> {

	public abstract class DataArrow extends PrintableGraph.PrintableArrow<DataArrow, DataNode> {
		public DataArrow(DataNode from, DataNode to) {
			super(DataFlowGraph.this, from, to);
		}

		@Override
		public List<String> getOptions() {
			if (from.part != to.part)
				return Arrays.asList("arrowhead=\"box\"","color=purple");
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

	public abstract class DataNode extends PrintableGraph.PrintableNode<DataNode, DataArrow> {
		public final Type type;
		public final int part;
		public final String nodeIdent;

		protected final Map<HalfArrow, DataArrow> builtArrows = new HashMap<>();

		public DataNode(String label, Type type) {
			this(label, type, Collections.emptyList());
		}

		public DataNode(String label, Type type, List<HalfArrow> sources) {
			super(DataFlowGraph.this);
			sources.forEach((HalfArrow a) -> builtArrows.put(a, a.complete(this)));
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

		public DataNode duplicate(List<DataNode> sources) {return null;}; // TODO was abstract
		public abstract boolean isStaticallyKnown();
	}

	public static class HalfArrow {
		public final DataNode node;
		public final String ident;

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
			return ident == null ? node.getGraph().new UnamedDataArrow(node, toward)
			                     : node.getGraph().new NamedDataArrow(ident, node, toward);
		}
	}

	public abstract class InterfaceNode extends DataNode {
		public final Mode mode;

		public InterfaceNode(String ident, Type type, Mode mode) {
			super(ident, type);
			this.mode = mode;
		}

		public InterfaceNode(String ident, HalfArrow node) {
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
		public DataNode duplicate(List<DataNode> sources) {
			return null;
		}

	}

	public class OutputNode extends InterfaceNode {
		public final DataArrow source;

		public OutputNode(String ident, HalfArrow data) {
			super(ident, data);
			outputs.add(this);
			this.source = builtArrows.get(data);
		}

		@Override
		public DataNode duplicate(List<DataNode> sources) {
			return null;
		}
	}

	public class FunctionReturnNode extends DataNode {
		public final FuncCallNode fct;

		public FunctionReturnNode(String label, Type type, FuncCallNode fct) {
			super(label, type, Collections.singletonList(new HalfArrow(fct)));
			this.fct = fct;
			fct.returns.add(this);
		}

		@Override
		public boolean isStaticallyKnown() {
			return false;
		}
	}

	public abstract class WyilNode<T extends Bytecode> extends DataNode {
		public final Location<T> location;

		/*public WyilNode(Location<T> location, List<DataNode> sources) throws CompilerException {
			super(DataFlowGraph.toString(location.getBytecode()),
				getType(location.getType()), sources);
			this.location = location;
		}*/

		public WyilNode(Location<T> location, Type type, List<HalfArrow> sources) {
			super(DataFlowGraph.toString(location.getBytecode()), type, sources);
			this.location = location;
		}

		public WyilNode(String ident, Location<T> location, Type type, List<HalfArrow> sources) {
			super(ident, type, sources);
			this.location = location;
		}

	}

	/*public class WyilSection {
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
	}*/

	/*public class LabelNode extends DataNode {
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
		public List<String> getOptions() {
			return Arrays.asList("shape=\"ellipse\"","color=green");
		}

		@Override
		public DataNode duplicate(List<DataNode> sources) {
			return new LabelNode(nodeIdent, type, sources);
		}

		public DataNode getSource() {
			return sources.isEmpty() ? null : sources.get(0).from;
		}
	}*/



	public abstract class InstructionNode<T extends Bytecode> extends WyilNode<T> {
		public InstructionNode(Location<T> location, Type type, List<HalfArrow> sources) {
			super(location, type, sources);
		}

	}

	public final class ConstNode extends InstructionNode<Bytecode.Const> {
		public ConstNode(Location<Bytecode.Const> decl, Type type) throws CompilerException {
			super(decl, type, Collections.emptyList());
		}

		@Override
		public boolean isStaticallyKnown() {
			return true;
		}

//		@Override
//		public DataNode duplicate(List<DataNode> sources) throws CompilerException {
//			return new ConstNode(location);
//		}
	}

	public class ExternConstNode extends DataNode {
		public ExternConstNode(Type type, String value) {
			super(value, type);
		}

		@Override
		public boolean isStaticallyKnown() {
			return true;
		}

//		@Override
//		public DataNode duplicate(List<DataNode> sources) throws CompilerException {
//			return new ConstNode(location);
//		}
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
		public final DataArrow op;

		public UnaOpNode(Location<Bytecode.Operator> binOp, Type type, HalfArrow op) {
			super(binOp, type, Arrays.asList(op));
			this.op = builtArrows.get(op);
		}

		@Override
		public boolean isStaticallyKnown() {
			return op.from.isStaticallyKnown();
		}
//
//		@Override
//		public DataNode duplicate(List<DataNode> sources) throws CompilerException {
//			return new UnaOpNode(location, sources.get(0));
//		}
	}

	public final class BinOpNode extends WyilNode<Bytecode.Operator> {
		public final DataArrow op1;
		public final DataArrow op2;

		public BinOpNode(Location<Bytecode.Operator> binOp, Type type, HalfArrow op1, HalfArrow op2){
			super(binOp, type, Arrays.asList(op1, op2));
			this.op1 = builtArrows.get(op1);
			this.op2 = builtArrows.get(op2);
		}


		@Override
		public List<String> getOptions() {
			return Arrays.asList("shape=\"rectangle\"","style=filled","fillcolor=lemonchiffon");
		}

		@Override
		public boolean isStaticallyKnown() {
			return op1.from.isStaticallyKnown() && op2.from.isStaticallyKnown();
		}

//		@Override
//		public DataNode duplicate(List<DataNode> sources) throws CompilerException {
//			return new BinOpNode(location, sources.get(0), sources.get(1));
//		}
	}

	public final class FuncCallNode extends WyilNode<Bytecode.Invoke> {
		public final String funcName;
		public final List<FunctionReturnNode> returns = new ArrayList<>();

		public FuncCallNode(Location<Bytecode.Invoke> call, List<HalfArrow> args) {
			super(call, null, args);
			invokes.add(this);
			funcName = call.getBytecode().name().name();
		}

		@Override
		public boolean isStaticallyKnown() {
			return false;
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
//
//		@Override
//		public DataNode duplicate(List<HalfArrow> sources) {
//			return new FuncCallNode(location, sources);
//		}
	}


	/*public final class IfNode extends LabelNode {
		public final Location<Bytecode.If> location;

		public IfNode(Location<Bytecode.If> ifs, DataNode cond) {
			super("if_cond", cond.type, Collections.singletonList(cond));
			location = ifs;
		}

		@Override
		public List<String> getOptions() {
			return Arrays.asList("shape=\"hexagon\"","color=yellow");
		}

		@Override
		public DataNode duplicate(List<DataNode> sources) {
			return new IfNode(location, sources.get(0));
		}
	}*/

	public final class EndIfNode extends WyilNode<Bytecode.If> {
		public final DataArrow condition;
		public final DataArrow trueNode;
		public final DataArrow falseNode;

		public EndIfNode(Location<Bytecode.If> ifs, HalfArrow condition, HalfArrow trueNode, HalfArrow falseNode) {
			super("mux", ifs, trueNode.node.type, Arrays.asList(condition, trueNode, falseNode));
			this.condition = builtArrows.get(condition);
			this.trueNode = builtArrows.get(trueNode);
			this.falseNode = builtArrows.get(falseNode);
		}

		@Override
		public List<String> getOptions() {
			return Arrays.asList("shape=\"rectangle\"","style=filled","fillcolor=lemonchiffon");
		}

		@Override
		public boolean isStaticallyKnown() {
			return false;//TODO complex
		}

//		@Override
//		public DataNode duplicate(List<DataNode> sources) {
//			return new EndIfNode(sources.get(0), sources.get(1), sources.get(2));
//		}
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
