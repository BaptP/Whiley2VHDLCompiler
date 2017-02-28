package wyvc.builder;


import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
import wyvc.lang.Statement.ConcurrentStatement;
import wyvc.lang.Statement.SequentialStatement;
import wyvc.lang.Statement.SignalAssignment;
import wyvc.lang.Statement.VariableAssignment;
import wyvc.lang.Statement.Process;
import wyvc.lang.Type;
import wyvc.lang.TypedValue;
import wyvc.lang.TypedValue.Constant;
import wyvc.lang.TypedValue.Port;
import wyvc.lang.TypedValue.Port.Mode;
import wyvc.lang.TypedValue.PortException;
import wyvc.lang.TypedValue.Signal;
import wyvc.lang.TypedValue.Variable;
import wyvc.lang.Expression.Access;
import wyvc.lang.Expression.TypesMismatchException;
import wyvc.builder.Utils;

public class ArchitectureCompiler {

	public static class ArchitectureData {
		public final Entity entity;
		public Map<Integer, TypedValue> values = new HashMap<Integer, TypedValue>();
		public ArrayList<Signal> signals = new ArrayList<>();
		public ArrayList<Signal> sensitive = new ArrayList<>();
		public ArrayList<Variable> variables = new ArrayList<>();
		public ArrayList<Constant> constants = new ArrayList<>();
		public Map<String, Component> components = new HashMap<String, Component>();
		public ArrayList<ConcurrentStatement> statements = new ArrayList<>();
		public ArrayList<SequentialStatement> processStatements = new ArrayList<>();

		public ArchitectureData(Entity entity) {
			this.entity = entity;
		}
	}


	private ArchitectureData architecture;

	public ArchitectureCompiler(Entity entity) throws TypesMismatchException, PortException {
		this.architecture = new ArchitectureData(entity);
		int inPort = 0;
		int outPort = 1;
		for(Port p : entity.interface_.ports) {
			if (p.mode == Mode.IN) {
				Variable v = new Variable("v_"+p.ident.substring(2), p.type);
				architecture.variables.add(v);
				architecture.sensitive.add(p);
				architecture.values.put(inPort++, v);
				architecture.processStatements.add(new VariableAssignment(v, new Access(p)));
			}
			else
				architecture.values.put(-outPort++, p);
		}
	}

	public Architecture compile(Location<?> location) throws VHDLCompilationException, VHDLException {
		compileStatements(location);
		architecture.statements.add(new Process("main", architecture.variables.toArray(new Variable[0]), architecture.sensitive.toArray(new Signal[0]),
			architecture.processStatements.toArray(new SequentialStatement[0])));
		return new Architecture(architecture.entity, "Behavioural", architecture.signals.toArray(new Signal[0]), architecture.constants.toArray(new Constant[0]),
			architecture.components.values().toArray(new Component[0]), architecture.statements.toArray(new ConcurrentStatement[0]));
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
		Variable v = new Variable(var.getName()+"_"+architecture.values.size(),new Type.Signed(31, 0));
		architecture.values.put(location.getIndex(), v);
		architecture.variables.add(v);
		if (location.numberOfOperands() == 1) {
			ExpressionCompiler expr = new ExpressionCompiler(architecture);
			architecture.processStatements.add(new VariableAssignment(v, expr.compile(location.getOperand(0))));
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
		for (Location<?> l : rhs) {
			if (l.getBytecode() instanceof Invoke)
				crhs.addAll(expr.compileInvoke((Location<Invoke>) l));
			else
				crhs.add(expr.compile(l));
		}
		for (int k = 0; k < lhs.length; ++k)
			architecture.processStatements.add(new VariableAssignment((Variable) architecture.values.get(lhs[k].getOperand(0).getIndex()),
				crhs.get(k)));
	}

	private void compileReturn(Location<Bytecode.Return> location) throws VHDLCompilationException, VHDLException {
		ExpressionCompiler expr = new ExpressionCompiler(architecture);
		int k = 0;
		for(Location<?> l : location.getOperands())
			architecture.processStatements.add(new SignalAssignment((Port) architecture.values.get(-++k), expr.compile(l)));
	}

	private void compileBlock(Location<Bytecode.Block> location) throws VHDLCompilationException, VHDLException {
		for(Location<?> l : location.getOperands())
			compileStatements(l);

	}


}
