package wyvc.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.lang.model.type.PrimitiveType;

import wyvc.builder.CompilerLogger.CompilerError;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.builder.CompilerLogger.CompilerWarning;
import wyvc.builder.CompilerLogger.LoggedBuilder;
import wyvc.builder.CompilerLogger.UnsupportedCompilerError;
import wyvc.builder.ControlFlowGraph.BinOpNode;
import wyvc.builder.ControlFlowGraph.ConstNode;
import wyvc.builder.ControlFlowGraph.DataNode;
import wyvc.builder.ControlFlowGraph.EndIfNode;
import wyvc.builder.ControlFlowGraph.FuncCallNode;
import wyvc.builder.ControlFlowGraph.IfNode;
import wyvc.builder.ControlFlowGraph.LabelNode;
import wyvc.builder.ControlFlowGraph.WyilSection;
import wyvc.builder.TypeCompiler.CompoundType;
import wyvc.builder.TypeCompiler.TypeTree;
import wyvc.lang.Architecture;
import wyvc.lang.Component;
import wyvc.lang.Entity;
import wyvc.lang.Expression;
import wyvc.lang.Expression.Access;
import wyvc.lang.Expression.Value;
import wyvc.lang.Interface;
import wyvc.lang.Statement.ComponentInstance;
import wyvc.lang.Statement.ConcurrentStatement;
import wyvc.lang.Statement.ConditionalSignalAssignment;
import wyvc.lang.Statement.NotAStatement;
import wyvc.lang.Statement.Process;
import wyvc.lang.Statement.SequentialStatement;
import wyvc.lang.Statement.SignalAssignment;
import wyvc.lang.Statement.StatementGroup;
import wyvc.lang.Type;
import wyvc.lang.TypedValue.Signal;
import wyvc.lang.TypedValue.Variable;
import wyvc.lang.TypedValue.Constant;
import wyvc.lang.TypedValue.Port;
import wyvc.lang.TypedValue.Port.Mode;
import wyvc.utils.Pair;
import wyvc.utils.Utils;

public class EntityCompiler {
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
		private Map<String, Component> fcts;
		private List<Signal> signals = new ArrayList<>();
		private List<Port> ports = new ArrayList<>();
		private List<ConcurrentStatement> statements = new ArrayList<>();
		private Set<Component> components = new HashSet<>();
		private Set<FuncCallNode> compiled = new HashSet<>();
		private Set<String> used = new HashSet<>();
		private Set<String> args = new HashSet<>();
		private boolean portsIn;
		private boolean portsOut;

		public Compiler(CompilerLogger logger, Map<String, Component> fcts, List<String> inp) {
			super(logger);
			this.fcts = fcts;
			args.addAll(inp);
			used.addAll(inp);
		}

		public Signal getIO(LabelNode n) {
			return created.get(n);
		}

		public String getFreshLabel(String l) {
			if (used.contains(l)) {
				int k = 1;
				while (used.contains(l+"_"+ ++k));
				l = l+"_"+k;
			}
			used.add(l);
			return l;
		}

		private String getFreshLabel(String l, Mode mode) {
			return mode == Mode.IN && args.contains(l) ? l : getFreshLabel(l);
		}

		public Interface getInterface() {
			List<Port> ports = new ArrayList<>(this.ports);
			Collections.reverse(ports);
			return new Interface(ports.toArray(new Port[ports.size()]));
		}

		public Signal[] getSignals() {
			return signals.toArray(new Signal[signals.size()]);
		}

		public Component[] getComponent() {
			return components.toArray(new Component[components.size()]);
		}

		public Constant[] getConstants() {
			return new Constant[0];
		}

		private Type getPrimitiveType(TypeTree type) throws CompilerException {
			if (type instanceof CompoundType)
				throw new CompilerException(new CompoundTypeNodeError(type));
			return type.getValue();
		}

		private Signal createSignal(String ident, Type type, DataNode value){
			Signal s = new Signal(getFreshLabel(ident), type);
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
			Mode m = value == null ? Mode.IN : Mode.OUT;
			Port p = new Port(getFreshLabel(ident,m), type, m);
			ports.add(p);
			if (value != null)
				toCompile.add(new Pair<Signal, DataNode>(p, value));
			return p;
		}

		private Signal getSignal(LabelNode label) throws CompilerException {
			DataNode source = label.getSource();
			if (source != null && source instanceof LabelNode && source.type instanceof PrimitiveType)
				return getSignal((LabelNode) source);
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
				return new Expression.Eq(e1, e2);
			case NEQ:
				return new Expression.Ne(e1, e2);
			case LT:
				return new Expression.Lt(e1, e2);
			case LTEQ:
				return new Expression.Le(e1, e2);
			case GT:
				return new Expression.Gt(e1, e2);
			case GTEQ:
				return new Expression.Ge(e1, e2);
			default:
				throw new CompilerException(new UnsupportedDataNodeError(node));
			}
		}

		private ConditionalSignalAssignment compileEndIf(Signal s, EndIfNode endIf) throws CompilerException {
			Signal st = createSignal(s.ident+"1", s.type, endIf.getTrueNode());
			Signal sf = createSignal(s.ident+"0", s.type, endIf.getFalseNode());
			return new ConditionalSignalAssignment(s,
				Arrays.asList(new Pair<Expression, Expression>(new Access(st),
						compileExpression(endIf.getConditionNode()))),
				new Access(sf));
		}


		private ConcurrentStatement compileInvoke(FuncCallNode call) throws CompilerException {
			if (compiled.contains(call))
				return new NotAStatement();
			compiled.add(call);
			Component fct = fcts.get(call.funcName);
			components.add(fct);
			List<Signal> args = Utils.checkedConvert(
				call.sources,(DataNode n) -> getSignal((LabelNode) n));
			args.addAll(Utils.checkedConvert(call.targets, (DataNode n) -> getSignal((LabelNode) n)));
			return new ComponentInstance(getFreshLabel(call.funcName), fct, args.toArray(new Signal[args.size()]));
		}

		private ConcurrentStatement compileSource(Pair<Signal, DataNode> source) throws CompilerException {
			debug("Nouvelle source "+source.first.ident + " <= " + source.second.nodeIdent);
			if (source.second != null) {
				if (source.second instanceof EndIfNode)
					return compileEndIf(source.first, (EndIfNode) source.second);
				if (source.second instanceof FuncCallNode)
					return compileInvoke((FuncCallNode) source.second);
				return new SignalAssignment(source.first, compileExpression(source.second));
			}
			throw new CompilerException(new UnsupportedDataNodeError(source.second));
		}

		private void compileSignal() throws CompilerException {
			while (!toCompile.isEmpty())
				statements.add(compileSource(toCompile.remove()));
		}

		private void checkInputs(String name, LabelNode l) throws CompilerException {
			if (l.type instanceof CompoundType)
				for (DataNode c : l.getTargets())
					checkInputs(name, (LabelNode) c);
			else if (!created.containsKey(l)){
					addMessage(new UnusedInputSignalWarning(l.nodeIdent, name));
					createPort(l.nodeIdent, getPrimitiveType(l.type), null);
				}
		}

		public StatementGroup compile(String name, WyilSection sec, boolean portsIn, boolean portsOut) throws CompilerException {
			this.portsIn = portsIn;
			this.portsOut = portsOut;
			Collections.reverse(sec.outputs);
			for (LabelNode l : sec.outputs)
				createPort(l.nodeIdent, getPrimitiveType(l.type), l.getSource());
			compileSignal();
			for (LabelNode l : sec.inputs)
				checkInputs(name, l);
			Collections.reverse(statements);
			return new StatementGroup(statements.toArray(new ConcurrentStatement[statements.size()]));
		}
	}

	public static class EmptyEntityError extends CompilerError {
		private final String name;

		public EmptyEntityError(String name) {
			this.name = name;
		}

		@Override
		public String info() {
			return "Compilation of the empty function "+name+" unsupported";
		}
	}

	public static class UnusedInputSignalWarning extends CompilerWarning {
		private final String arg;
		private final String fun;

		public UnusedInputSignalWarning(String arg, String fun) {
			this.arg = arg;
			this.fun = fun;
		}

		@Override
		public String info() {
			return "The argument "+arg+" of function "+fun+" is useless for the computation of the ouputs.\nIt was kept to preserve the signature.";
		}

	}

	public static Entity compile(CompilerLogger logger, String name, List<WyilSection> parts, Map<String, Component> fcts) throws CompilerException {
		if (parts.isEmpty())
			throw new CompilerException(new EmptyEntityError(name));

		Compiler cmp = new Compiler(logger, fcts, Utils.convert(parts.get(0).inputs, (LabelNode n) -> n.nodeIdent));
		List<ConcurrentStatement> statements = new ArrayList<>();
		List<Signal> out = new ArrayList<>();
		WyilSection pSec = parts.get(0);
		statements.add(cmp.compile(name, pSec, true, parts.size() == 1));
		for (WyilSection sec : parts.subList(1, parts.size())) {
			out = Utils.convert(pSec.outputs, (LabelNode o) -> cmp.getIO(o));
			statements.add(cmp.compile(name, sec, false, sec == parts.get(parts.size()-1)));
			pSec = sec;
			List<Signal> in = Utils.convert(pSec.inputs, (LabelNode o) -> cmp.getIO(o));
			statements.add(new Process(cmp.getFreshLabel("Cycle"), new Variable[0], new Signal[0],
				Utils.checkedConvert(
					Utils.gather(out, in),
					(Pair<Signal, Signal> p) -> new SignalAssignment(p.second, new Access(p.first))
					).toArray(new SequentialStatement[in.size()])));
		}
		Entity entity = new Entity(name, cmp.getInterface());
		entity.addArchitectures(new Architecture(entity, "Behavioural", cmp.getSignals(), cmp.getConstants(), cmp.getComponent(),
			statements.toArray(new ConcurrentStatement[statements.size()])));
		fcts.put(name, new Component(name, cmp.getInterface()));
		return entity;
	}
}
