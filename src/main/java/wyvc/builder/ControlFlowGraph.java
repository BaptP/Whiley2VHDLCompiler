package wyvc.builder;

import java.util.List;
import java.util.Map;

import wyil.lang.Bytecode;
import wyil.lang.SyntaxTree.Location;
import wyvc.builder.TypeCompiler.TypeTree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class ControlFlowGraph {

	public ControlFlowGraph(Map<String, TypeTree> nominalTypes) {
		//this.nominalTypes = nominalTypes;
	}


	public static class GraphNode<T extends GraphNode<T>> {
		public final List<T> sources;
		private ArrayList<T> targets = new ArrayList<>();
		public final String label;

		@SuppressWarnings("unchecked")
		public GraphNode(String label, List<T> sources) {
			for(T t: sources) System.out.println("Source "+t+(t == null ? "" : " : "+t.label));
			this.label = label;
			this.sources = sources;
			for (GraphNode<T> n : sources)
				n.addTarget((T)this);
		}

		private void addTarget(T target) {
			targets.add(target);
		}

		public List<T> getTargets() {
			return targets;
		}

		public List<String> getOptions() {
			return Arrays.asList("shape=\"box\"","color=black");
		}

	}


	public abstract class DataNode extends GraphNode<DataNode> {
		public DataNode(String label, List<DataNode> sources) {
			super(label, sources);
		}
	}

	public static class WyilSection {
		public final List<DataNode> inputs;
		public final List<DataNode> outputs;

		public WyilSection(List<DataNode> inputs, List<DataNode> outputs) {
			this.inputs = inputs;
			this.outputs = outputs;
		}
	}

	public class LabelNode extends DataNode {
		public LabelNode(String ident, List<DataNode> sources) {
			super(ident, sources);
		}

		public LabelNode(String ident, DataNode source) {
			super(ident, Collections.singletonList(source));
		}

		public LabelNode(String ident) {
			super(ident, Collections.emptyList());
		}

		@Override
		public List<String> getOptions() {
			return Arrays.asList("shape=\"ellipse\"","color=green");
		}
	}

	public abstract class WyilNode<T extends Bytecode> extends DataNode {
		public final Location<T> location;
//		public final TypeTree type;

		public WyilNode(Location<T> location, List<DataNode> sources) {
			super(location.toString(), sources);
			this.location = location;
//			Type t = location.getType();
//			if (!types.containsKey(t))
//				types.put(t, TypeCompiler.compileType(t, nominalTypes));
//			this.type = types.get(t);
		}
	}



	public abstract class InstructionNode<T extends Bytecode> extends WyilNode<T> {
		public InstructionNode(Location<T> location, List<DataNode> sources) {
			super(location, sources);
		}

	}

	public final class ConstNode extends InstructionNode<Bytecode.Const> {
		public ConstNode(Location<Bytecode.Const> decl) {
			super(decl, Collections.emptyList());
		}
	}

	public final class UnaOpNode extends InstructionNode<Bytecode.Operator> {
		public final DataNode op;

		public UnaOpNode(Location<Bytecode.Operator> binOp, DataNode op) {
			super(binOp, Arrays.asList(op));
			this.op = op;
		}
	}

	public final class BinOpNode extends WyilNode<Bytecode.Operator> {
		public final DataNode op1;
		public final DataNode op2;

		public BinOpNode(Location<Bytecode.Operator> binOp, DataNode op1, DataNode op2) {
			super(binOp, Arrays.asList(op1, op2));
			this.op1 = op1;
			this.op2 = op2;
		}
	}

	public final class FuncCall extends WyilNode<Bytecode.Invoke> {
		public FuncCall(Location<Bytecode.Invoke> call, List<DataNode> args) {
			super(call, args);
		}
	}

}
