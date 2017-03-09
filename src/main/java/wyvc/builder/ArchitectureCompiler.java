package wyvc.builder;


import java.util.ArrayList;

import wyil.lang.Bytecode;
import wyil.lang.Bytecode.Invoke;
import wyil.lang.SyntaxTree;
import wyil.lang.SyntaxTree.Location;
import wyvc.utils.Triple;
import wyvc.builder.VHDLCompileTask.VHDLCompilationException;
import wyvc.lang.Architecture;
import wyvc.lang.Component;
import wyvc.lang.Entity;
import wyvc.lang.LexicalElement.VHDLException;
import wyvc.lang.Statement.StatementGroup;
import wyvc.lang.Statement.ConcurrentStatement;
import wyvc.lang.TypedValue.Constant;
import wyvc.utils.Utils;
import wyvc.lang.TypedValue.PortException;
import wyvc.lang.TypedValue.Signal;
import wyvc.lang.Expression.TypesMismatchException;
import wyvc.builder.ElementCompiler.CompilationData;
import wyvc.builder.ElementCompiler.InterfacePattern;
import wyvc.builder.ElementCompiler.TypedIdentifierTree;
import wyvc.builder.ExpressionCompiler.ExpressionTree;
import wyvc.builder.LexicalElementTree.IdentifierComponentException;

public class ArchitectureCompiler {
	private final CompilationData data;
	private final Entity entity;


	public ArchitectureCompiler(Entity entity, CompilationData data) throws TypesMismatchException, PortException {
		this.data = data;
		this.entity = entity;
	}

	public Architecture compile(Location<?> location) throws VHDLCompilationException, VHDLException {
		compileStatements(location);
		return new Architecture(entity, "Behavioural", data.signals.toArray(new Signal[0]), new Constant[0],
			Utils.toArray(data.components.values(), (Triple<Integer, Component, InterfacePattern> p) -> p.second, new Component[0]),
			data.statements.toArray(new ConcurrentStatement[0]));
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
		TypedIdentifierTree v = TypedIdentifierTree.createSignal(var.getName(), TypeCompiler.compileType(location.getType(), data), data);
		data.values.put(location.getIndex(), v);
		if (location.numberOfOperands() == 1) {
			ExpressionCompiler expr = new ExpressionCompiler(data);
			data.statements.add(v.assign(expr.compile(location.getOperand(0))));
		}
		// TODO Type !!
	}

	public static class AssignmentException extends VHDLCompilationException {
		private static final long serialVersionUID = -2618011322731674457L;

		@Override
		protected void details() {
			System.err.println("    Bad assignment left hand side.");
		}
	}

	private TypedIdentifierTree compileLeftHandSide(Location<?> location) throws AssignmentException, VHDLException, IdentifierComponentException {
		if (location.getBytecode() instanceof Bytecode.FieldLoad)
			return compileLeftHandSide(location.getOperand(0)).getComponent(((Bytecode.FieldLoad) location.getBytecode()).fieldName());
		if (location.getBytecode() instanceof Bytecode.VariableAccess)
			return data.values.get(location.getBytecode().getOperand(0));
		throw new AssignmentException();
	}

	@SuppressWarnings("unchecked")
	private void compileAssign(Location<Bytecode.Assign> location) throws VHDLCompilationException, VHDLException {
		Location<?>[] lhs = location.getOperandGroup(SyntaxTree.LEFTHANDSIDE);
		Location<?>[] rhs = location.getOperandGroup(SyntaxTree.RIGHTHANDSIDE);
		Utils.printLocation(location, "");
		ExpressionCompiler expr = new ExpressionCompiler(data);
		ArrayList<ExpressionTree> crhs = new ArrayList<>();
		ArrayList<ConcurrentStatement> assignGroup = new ArrayList<>();
		for (Location<?> l : rhs) {
			if (l.getBytecode() instanceof Invoke)
				crhs.addAll(expr.compileInvoke((Location<Invoke>) l));
			else
				crhs.add(expr.compile(l));
		}
		/*
		for (int k = 0; k < lhs.length; ++k){
			Utils.printLocation(lhs[k], "v"+k+"  ");
			assignGroup.add(data.values.get(lhs[k].getOperand(0).getIndex()).assign(crhs.get(k)));
		}
		data.statements.add(new StatementGroup(assignGroup.toArray(new ConcurrentStatement[0])));
		/*/
		for (int k = 0; k < lhs.length; ++k){
			Utils.printLocation(lhs[k], "v"+k+"  ");
			assignGroup.add(compileLeftHandSide(lhs[k]).assign(crhs.get(k)));
		}
		data.statements.add(new StatementGroup(assignGroup.toArray(new ConcurrentStatement[0])));
		//*/
}



	private void compileReturn(Location<Bytecode.Return> location) throws VHDLCompilationException, VHDLException {
		ExpressionCompiler expr = new ExpressionCompiler(data);
		int k = 0;
		ArrayList<ConcurrentStatement> returnGroup = new ArrayList<>();
		for(Location<?> l : location.getOperands())
			returnGroup.add(data.values.get(-++k).assign(expr.compile(l)));
		data.statements.add(new StatementGroup(returnGroup.toArray(new ConcurrentStatement[0])));
	}

	private void compileBlock(Location<Bytecode.Block> location) throws VHDLCompilationException, VHDLException {
		for(Location<?> l : location.getOperands())
			compileStatements(l);

	}

}
