package wyvc.builder;


import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import wycc.util.Pair;
import wyil.lang.Bytecode;
import wyil.lang.Bytecode.Invoke;
import wyil.lang.SyntaxTree;
import wyil.lang.SyntaxTree.Location;
import wyvc.builder.VHDLCompileTask.VHDLCompilationException;
import wyvc.lang.Architecture;
import wyvc.lang.Component;
import wyvc.lang.Entity;
import wyvc.lang.Expression;
import wyvc.lang.LexicalElement.VHDLException;
import wyvc.lang.Statement.StatementGroup;
import wyvc.lang.Statement.ConcurrentStatement;
import wyvc.lang.Statement.SequentialStatement;
import wyvc.lang.Statement.SignalAssignment;
import wyvc.lang.Statement.Process;
import wyvc.lang.Type;
import wyvc.lang.TypedValue.Constant;
import wyvc.lang.TypedValue.Port;
import wyvc.lang.TypedValue.Port.Mode;
import wyvc.lang.TypedValue.PortException;
import wyvc.lang.TypedValue.Signal;
import wyvc.lang.TypedValue.Variable;
import wyvc.lang.Expression.TypesMismatchException;
import wyvc.builder.Utils;
import wyvc.builder.Utils;

public class ArchitectureCompiler {

	public static class ArchitectureData {
		public final Entity entity;
		public Map<Integer, SignalVariable> values = new HashMap<Integer, SignalVariable>();
		public ArrayList<Signal> signals = new ArrayList<>();
		public ArrayList<Signal> sensitive = new ArrayList<>();
		public ArrayList<Variable> variables = new ArrayList<>();
		public Map<String, Pair<Integer,Component>> components = new HashMap<>();
		public ArrayList<ConcurrentStatement> statements = new ArrayList<>();
		public ArrayList<SequentialStatement> processStatements = new ArrayList<>();

		public ArchitectureData(Entity entity) {
			this.entity = entity;
		}
	}

	public static class SignalVariable {
		private final ArchitectureData architecture;
		private final String ident;
		private int assignments = 0;
		private Signal current = null;

		public SignalVariable(String ident, Type type, ArchitectureData architecture) {
			this.architecture = architecture;
			this.ident = ident;
			signal("I"+ident, type);
		}

		public SignalVariable(Port port, ArchitectureData architecture) {
			this.architecture = architecture;
			this.ident = port.ident.substring(1);
			this.current = port;
			this.assignments = port.mode == Mode.IN ? 1 : 0;
		}

		private void signal(String ident, Type type) {
			this.current = new Signal(ident, type);
			architecture.signals.add(this.current);
		}

		public final Signal read() {
			return current;
		}

		public final SignalAssignment assign(Expression e) throws TypesMismatchException, PortException {
			if (assignments++ != 0)
				signal("N"+ident+"_"+assignments, current.type);
			return new SignalAssignment(current, e);
		}
	}

	private ArchitectureData architecture;

	public ArchitectureCompiler(Entity entity) throws TypesMismatchException, PortException {
		this.architecture = new ArchitectureData(entity);
		int inPort = 0;
		int outPort = 1;
		for(Port p : entity.interface_.ports) {
			SignalVariable s = new SignalVariable(p, this.architecture);
			if (p.mode == Mode.IN)
				architecture.values.put(inPort++, s);
			else
				architecture.values.put(-outPort++, s);
		}
	}

	public Architecture compile(Location<?> location) throws VHDLCompilationException, VHDLException {
		compileStatements(location);
		return new Architecture(architecture.entity, "Behavioural", architecture.signals.toArray(new Signal[0]), new Constant[0],
			Utils.toArray(architecture.components.values(), (Pair<Integer, Component> p) -> p.second(), new Component[0]),
			architecture.statements.toArray(new ConcurrentStatement[0]));
	}

	@SuppressWarnings("unchecked")
	private void compileStatements(Location<?> location) throws VHDLCompilationException, VHDLException {
		Bytecode bytecode = location.getBytecode();
		System.out.println(location.toString());
		if (bytecode instanceof Bytecode.VariableDeclaration)
			compileVariableDeclaration((Location<Bytecode.VariableDeclaration>) location);
		else if (bytecode instanceof Bytecode.Return)
			compileReturn((Location<Bytecode.Return>) location);
		else if (bytecode instanceof Bytecode.Block)
			compileBlock((Location<Bytecode.Block>) location);
		else if (bytecode instanceof Bytecode.Assign)
			compileAssign((Location<Bytecode.Assign>) location);
	}

	private void compileVariableDeclaration(Location<Bytecode.VariableDeclaration> location) throws VHDLCompilationException, VHDLException {
		Bytecode.VariableDeclaration var = location.getBytecode();
		SignalVariable s = new SignalVariable(var.getName(),new Type.Signed(31, 0), architecture);
		architecture.values.put(location.getIndex(), s);
		if (location.numberOfOperands() == 1) {
			ExpressionCompiler expr = new ExpressionCompiler(architecture);
			architecture.statements.add(s.assign(expr.compile(location.getOperand(0))));
		}
		// TODO Type !!
	}


	@SuppressWarnings("unchecked")
	private void compileAssign(Location<Bytecode.Assign> location) throws VHDLCompilationException, VHDLException {
		Location<?>[] lhs = location.getOperandGroup(SyntaxTree.LEFTHANDSIDE);
		Location<?>[] rhs = location.getOperandGroup(SyntaxTree.RIGHTHANDSIDE);
		Utils.printLocation(location, "");
		ExpressionCompiler expr = new ExpressionCompiler(architecture);
		ArrayList<Expression> crhs = new ArrayList<>();
		ArrayList<ConcurrentStatement> assignGroup = new ArrayList<>();
		for (Location<?> l : rhs) {
			if (l.getBytecode() instanceof Invoke)
				crhs.addAll(expr.compileInvoke((Location<Invoke>) l));
			else
				crhs.add(expr.compile(l));
		}
		for (int k = 0; k < lhs.length; ++k)
			assignGroup.add(architecture.values.get(lhs[k].getOperand(0).getIndex()).assign(crhs.get(k)));
		architecture.statements.add(new StatementGroup(assignGroup.toArray(new ConcurrentStatement[0])));
	}

	private void compileReturn(Location<Bytecode.Return> location) throws VHDLCompilationException, VHDLException {
		ExpressionCompiler expr = new ExpressionCompiler(architecture);
		int k = 0;
		ArrayList<ConcurrentStatement> returnGroup = new ArrayList<>();
		for(Location<?> l : location.getOperands())
			returnGroup.add(architecture.values.get(-++k).assign(expr.compile(l)));
		architecture.statements.add(new StatementGroup(returnGroup.toArray(new ConcurrentStatement[0])));
	}

	private void compileBlock(Location<Bytecode.Block> location) throws VHDLCompilationException, VHDLException {
		for(Location<?> l : location.getOperands())
			compileStatements(l);

	}


}
