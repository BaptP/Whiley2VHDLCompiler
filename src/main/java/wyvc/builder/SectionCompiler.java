package wyvc.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.lang.model.type.PrimitiveType;

import wyvc.builder.CompilerLogger.CompilerError;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.builder.CompilerLogger.LoggedBuilder;
import wyvc.builder.CompilerLogger.UnsupportedCompilerError;
import wyvc.builder.ControlFlowGraph.BinOpNode;
import wyvc.builder.ControlFlowGraph.ConstNode;
import wyvc.builder.ControlFlowGraph.DataNode;
import wyvc.builder.ControlFlowGraph.EndIfNode;
import wyvc.builder.ControlFlowGraph.IfNode;
import wyvc.builder.ControlFlowGraph.LabelNode;
import wyvc.builder.ControlFlowGraph.WyilSection;
import wyvc.builder.TypeCompiler.CompoundType;
import wyvc.builder.TypeCompiler.TypeTree;
import wyvc.lang.Expression;
import wyvc.lang.Statement.ConcurrentStatement;
import wyvc.lang.Statement.SignalAssignment;
import wyvc.lang.Type;
import wyvc.lang.Statement.ConditionalSignalAssignment;
import wyvc.lang.Expression.Access;
import wyvc.lang.Expression.Value;
import wyvc.lang.TypedValue.Signal;
import wyvc.utils.Pair;
import wyvc.lang.TypedValue.Port;
import wyvc.lang.TypedValue.Port.Mode;

public class SectionCompiler {
	private static class UnsupportedDataNodeError extends UnsupportedCompilerError {
		private final DataNode node;

		public UnsupportedDataNodeError(DataNode node) {
			this.node = node;
		}

		@Override
		public String info() {
			return "Production of VHDL from data node "+node+" currently unsupported"+(node == null ? "" : " "+node.nodeIdent);
		}
	}

	private static class CompoundTypeNodeError extends CompilerError {
		private final TypeTree type;

		public CompoundTypeNodeError(TypeTree type) {
			this.type = type;
		}

		@Override
		public String info() {
			return "Converting a compound type is unsupported\n"+type.toString("  ");
		}

	}

	private static class Compiler extends LoggedBuilder {
		private Queue<Pair<Signal, DataNode>> toCompile = new LinkedList<>();
		private Map<LabelNode, Signal> created = new HashMap<>();
		private List<Signal> signals = new ArrayList<>();
		private List<Port> ports = new ArrayList<>();
		private List<ConcurrentStatement> statements = new ArrayList<>();
		final boolean portsIn;
		final boolean portsOut;

		public Compiler(CompilerLogger logger, boolean portsIn, boolean portsOut) {
			super(logger);
			this.portsIn = portsIn;
			this.portsOut = portsOut;
		}

		private Type getPrimitiveType(TypeTree type) throws CompilerException {
			if (type instanceof CompoundType)
				throw new CompilerException(new CompoundTypeNodeError(type));
			return type.getValue();
		}

		private Signal createSignal(String ident, Type type, DataNode value){
			Signal s = new Signal(ident, type);
			signals.add(s);
			if (value != null)
				toCompile.add(new Pair<Signal, DataNode>(s, value));
			return s;
		}
		private Signal createPort(String ident, Type type, DataNode value) {
			if (value == null && !portsIn)
				return createSignal(ident, type, value);
			if (value != null && !portsOut)
				return createSignal(ident, type, value);
			Port p = new Port(ident, type, value == null ? Mode.IN : Mode.OUT);
			ports.add(p);
			if (value != null)
				toCompile.add(new Pair<Signal, DataNode>(p, value));
			return p;
		}

		private Signal getSignal(LabelNode label) throws CompilerException {
			if (!created.containsKey(label))
				created.put(label, label.getSource() != null && !(label.getSource().type instanceof CompoundType)
					? createSignal(label.nodeIdent, getPrimitiveType(label.type), label.getSource())
					: createPort(label.nodeIdent, getPrimitiveType(label.type), null));
			return created.get(label);
		}



		private Expression compileExpression(DataNode node) throws CompilerException {
			if (node instanceof LabelNode)
				return compileLabel((LabelNode) node);
			if (node instanceof BinOpNode)
				return compileBinOp((BinOpNode) node);
			if (node instanceof ConstNode)
				return compileConst((ConstNode) node);
			if (node instanceof IfNode)
				return compileExpression(node.sources.get(0));
			throw new CompilerException(new UnsupportedDataNodeError(node));
		}

		private Expression compileConst(ConstNode node) throws CompilerException {
			return new Value(getPrimitiveType(node.type), node.nodeIdent);
		}

		private Expression compileLabel(LabelNode label) throws CompilerException {
			DataNode source = label.getSource();
			if (source != null && source instanceof LabelNode && source.type instanceof PrimitiveType)
				return compileLabel((LabelNode) source);
			return new Access(getSignal(label));
		}

		private Expression compileBinOp(BinOpNode node) throws CompilerException {
			Expression e1 = compileExpression(node.op1);
			Expression e2 = compileExpression(node.op2);
			switch (node.location.getBytecode().kind()) {
			case ADD:
				return new Expression.Add(e1, e2);
			case SUB:
				return new Expression.Sub(e1, e2);
			case BITWISEAND:
			case AND:
				return new Expression.And(e1, e2);
			case BITWISEOR:
			case OR:
				return new Expression.Or (e1, e2);
			case BITWISEXOR:
				return new Expression.Xor(e1, e2);
			case EQ:
				return new Expression.Eq(e1, e2); // TODO comparaisons...
			default:
				throw new CompilerException(new UnsupportedDataNodeError(node));
			}
		}

		private ConditionalSignalAssignment compile(Signal s, EndIfNode endIf) throws CompilerException {
			Signal st = createSignal(s.ident+"1", s.type, endIf.getTrueNode());
			Signal sf = createSignal(s.ident+"0", s.type, endIf.getFalseNode());
			return new ConditionalSignalAssignment(s,
				Arrays.asList(new Pair<Expression, Expression>(new Access(st),
						compileExpression(endIf.getConditionNode()))),
				new Access(sf));
		}

		private ConcurrentStatement compileSource(Pair<Signal, DataNode> source) throws CompilerException {
			debug("Nouvelle source "+source.first.ident + " <= " + source.second.nodeIdent);
			if (source.second != null) {
				if (source.second instanceof EndIfNode)
					return compile(source.first, (EndIfNode) source.second);
				else
					return new SignalAssignment(source.first, compileExpression(source.second));
			}
			throw new CompilerException(new UnsupportedDataNodeError(source.second));
		}

		private void compileSignal() throws CompilerException {
			while (!toCompile.isEmpty())
				statements.add(compileSource(toCompile.remove()));
		}

		public List<ConcurrentStatement> compile(WyilSection sec) throws CompilerException {
			Collections.reverse(sec.outputs);
			for (LabelNode l : sec.outputs)
				createPort(l.nodeIdent, getPrimitiveType(l.type), l.getSource());
			compileSignal();
			return statements;
		}
	}

	public static List<ConcurrentStatement> compile(CompilerLogger logger, WyilSection sec, boolean portsIn, boolean portsOut) throws CompilerException {
		Compiler cmp = new Compiler(logger, portsIn, portsOut);
		return cmp.compile(sec);
	}
}
