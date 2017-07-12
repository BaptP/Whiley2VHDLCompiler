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

import wyvc.builder.CompilerLogger.CompilerError;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.builder.CompilerLogger.CompilerWarning;
import wyvc.builder.CompilerLogger.LoggedBuilder;
import wyvc.builder.CompilerLogger.UnsupportedCompilerError;
import wyvc.builder.DataFlowGraph.BinOpNode;
import wyvc.builder.DataFlowGraph.ConstNode;
import wyvc.builder.DataFlowGraph.DataArrow;
import wyvc.builder.DataFlowGraph.DataNode;
import wyvc.builder.DataFlowGraph.EndIfNode;
import wyvc.builder.DataFlowGraph.FuncCallNode;
import wyvc.builder.DataFlowGraph.FunctionReturnNode;
import wyvc.builder.DataFlowGraph.InputNode;
import wyvc.builder.DataFlowGraph.InterfaceNode;
import wyvc.builder.DataFlowGraph.NamedDataArrow;
import wyvc.builder.DataFlowGraph.OutputNode;
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
import wyvc.lang.Type.VectorType;
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



	private static class Compiler extends LoggedBuilder {
		private Queue<Pair<Signal, DataNode>> toCompile = new LinkedList<>();
		private Map<DataNode, Signal> created = new HashMap<>();
		private Map<String, Component> fcts;
		private List<Signal> signals = new ArrayList<>();
		private List<Port> ports = new ArrayList<>();
		private List<ConcurrentStatement> statements = new ArrayList<>();
		private Set<Component> components = new HashSet<>();
		private Set<FuncCallNode> compiled = new HashSet<>();
		private Set<String> used = new HashSet<>();
		private boolean portsIn;
		private boolean portsOut;
		//private String fctName;

		public Compiler(CompilerLogger logger, Map<String, Component> fcts) {
			super(logger);
			this.fcts = fcts;
			used.addAll(fcts.keySet());
		}

		public Signal getIO(InterfaceNode n) {
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

		public Interface getInterface() {
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





		private Port createPort(String ident, DataNode node, Mode mode) {
			Port port = new Port(getFreshLabel(ident), node.type, mode);
			ports.add(port);
			created.put(node, port);
			return port;
		}

		private Signal createSignal(String ident, DataNode node) {
			Signal signal = new Signal(getFreshLabel(ident), node.type);
			created.put(node, signal);
			signals.add(signal);
			return signal;
		}

		private Signal createPort(InputNode input) {
			return portsIn ? createPort(input.nodeIdent, input, Mode.IN)
			               : createSignal(input.nodeIdent, input);
		}

		private Signal createPort(OutputNode output) {
			Signal port = portsOut ? createPort(output.nodeIdent, output, Mode.OUT)
				                   : createSignal(output.nodeIdent, output);
			return planCompilation(port, output.source.from);
		}

		/**
		 * Returns a signal linked to the source circuit.
		 * If the arrow is not named, a name signal is created.
		 * @param source
		 * @param name
		 * @return
		 */
		private Signal getSignal(DataArrow source, String name) { // TODO Obsolete ?
			return Utils.addIfAbsent(created, source.from, () -> planCompilation(createSignal(name, source.from), source.from));
		}
		private Signal getSignal(DataNode source, String name) {
			return Utils.addIfAbsent(created, source, () -> planCompilation(createSignal(name, source), source));
		}

		private Signal getSignal(NamedDataArrow source) {
			return getSignal(source, source.ident);
		}


		private Signal planCompilation(Signal signal, DataNode source) {
			toCompile.add(new Pair<Signal, DataNode>(signal, source));
			return signal;
		}




		private Expression compileAcces(NamedDataArrow arrow) throws CompilerException {
			return new Access(getSignal(arrow));
		}

		private Expression compileExpression(DataArrow arrow) throws CompilerException {
			if (arrow instanceof NamedDataArrow)
				return compileAcces((NamedDataArrow) arrow);
			return compileExpression(arrow.from);
		}

		private Expression compileInput(InputNode input) throws CompilerException {
			return new Access(created.get(input));
		}

		private Expression compileExpression(DataNode node) throws CompilerException {
			if (node instanceof BinOpNode)
				return compileBinOp((BinOpNode) node);
			if (node instanceof ConstNode)
				return compileConst((ConstNode) node);
			if (node instanceof InputNode)
				return compileInput((InputNode) node);
			throw new CompilerException(new UnsupportedDataNodeError(node));
		}

		private Expression compileConst(ConstNode node) throws CompilerException {
			return new Value(node.type, node.nodeIdent);
		}



		private Expression compileBinOp(BinOpNode node) throws CompilerException {
			Expression e1 = compileExpression(node.op1);
			Expression e2 = compileExpression(node.op2);
			switch (node.kind) {
			case Add: return new Expression.Add(e1, e2);
			case Sub: return new Expression.Sub(e1, e2);
			case Mul: return new Expression.SubVector(new Expression.Mul(e1, e2), ((VectorType)node.type).lenght()-1, 0);
			case And: return new Expression.And(e1, e2);
			case Or:  return new Expression.Or (e1, e2);
			case Xor: return new Expression.Xor(e1, e2);
			case Eq:  return new Expression.Eq(e1, e2);
			case Ne:  return new Expression.Ne(e1, e2);
			case Lt:  return new Expression.Lt(e1, e2);
			case Le:  return new Expression.Le(e1, e2);
			case Gt:  return new Expression.Gt(e1, e2);
			case Ge:  return new Expression.Ge(e1, e2);
			default:  throw new CompilerException(new UnsupportedDataNodeError(node));
			}
		}

		private ConditionalSignalAssignment compileEndIf(Signal s, EndIfNode endIf) throws CompilerException {
			Signal st = getSignal(endIf.trueNode, s.ident+"1");
			Signal sf = getSignal(endIf.falseNode, s.ident+"0");
			Signal cd = getSignal(endIf.condition, s.ident+"2");
			return new ConditionalSignalAssignment(s,
				Arrays.asList(new Pair<Expression, Expression>(new Access(st), new Access(cd))),
				new Access(sf));
		}


		private ConcurrentStatement compileInvoke(FuncCallNode call) throws CompilerException {
			if (compiled.contains(call))
				return new NotAStatement();
			compiled.add(call);
			Component fct = fcts.get(call.funcName);
			components.add(fct);
			List<Signal> args = call.getSources().enumerate().map((k,n) -> getSignal(n, call.funcName+"_arg_"+k)).toList();
			List<Signal> rets = call.getReturns().map(n -> Utils.addIfAbsent(created, n, () -> createSignal(n.nodeIdent, n))).toList();
			return new ComponentInstance(getFreshLabel(call.funcName), fct, Utils.concat(args, rets).toArray(new Signal[args.size()+rets.size()]));
		}

		private ConcurrentStatement compileSource(Pair<Signal, DataNode> source) throws CompilerException {
			debug("Nouvelle source "+source.first.ident + " <= " + source.second.nodeIdent);
			if (source.second != null) {
				if (source.second instanceof EndIfNode)
					return compileEndIf(source.first, (EndIfNode) source.second);
				if (source.second instanceof FunctionReturnNode)
					return compileInvoke(((FunctionReturnNode) source.second).fct);
				return new SignalAssignment(source.first, compileExpression(source.second));
			}
			throw new CompilerException(new UnsupportedDataNodeError(source.second));
		}

		private void compileSignals() throws CompilerException {
			while (!toCompile.isEmpty())
				statements.add(compileSource(toCompile.remove()));
		}

//		private void checkInputs(InputNode n) throws CompilerException {
//			if (!created.containsKey(n)){
//				addMessage(new UnusedInputSignalWarning(n.nodeIdent, fctName));
//				createPort(n);
//			}
//		} TODO move !

		public List<ConcurrentStatement> compile(String name, DataFlowGraph sec, boolean portsIn, boolean portsOut) throws CompilerException {
			this.portsIn = portsIn;
			this.portsOut = portsOut;
			//this.fctName = name;
			sec.getInputNodes().forEach(this::createPort);
			sec.getOutputNodes().forEach(this::createPort);
			compileSignals();
			Collections.reverse(statements);
			return statements;
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

	public static class UnusedInputSignalWarning extends CompilerWarning { // TODO à déplacer.
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

	public static Entity compile(CompilerLogger logger, String name, DataFlowGraph graph, Map<String, Component> fcts) throws CompilerException {
		if (parts.isEmpty())
			throw new CompilerException(new EmptyEntityError(name));
		Compiler cmp = new Compiler(logger, fcts);
		List<ConcurrentStatement> statements = new ArrayList<>();
		DataFlowGraph pSec = parts.get(0);
		statements.addAll(cmp.compile(name, pSec, true, parts.size() == 1));
		for (DataFlowGraph sec : parts.subList(1, parts.size())) {
			statements.addAll(cmp.compile(name, sec, false, sec == parts.get(parts.size()-1)));
			statements.add(new Process(cmp.getFreshLabel("Cycle"), new Variable[0], new Signal[0],
				pSec.getOutputNodes().map(cmp::getIO).gather(sec.getInputNodes().map(cmp::getIO)).
				mapSecond_(Access::new).map(SignalAssignment::new).toList().toArray(new SequentialStatement[pSec.outputs.size()])));
			pSec = sec;
		}
		Entity entity = new Entity(name, cmp.getInterface());
		entity.addArchitectures(new Architecture(entity, "Behavioural", cmp.getSignals(), cmp.getConstants(), cmp.getComponent(),
			statements.toArray(new ConcurrentStatement[statements.size()])));
		fcts.put(name, new Component(name, cmp.getInterface()));
		return entity;
	}
}
