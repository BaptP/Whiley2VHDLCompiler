package wyvc.builder;


import java.util.ArrayList;
import java.util.List;

import wyil.lang.Bytecode;
import wyil.lang.Type;
import wyil.lang.Bytecode.Const;
import wyil.lang.Bytecode.FieldLoad;
import wyil.lang.Bytecode.Operator;
import wyil.lang.Bytecode.OperatorKind;
import wyil.lang.Bytecode.VariableAccess;
import wyil.lang.Bytecode.Invoke;
import wyil.lang.SyntaxTree.Location;
import wyvc.utils.Pair;
import wyvc.utils.Triple;
import wyvc.utils.Utils;
import wyvc.lang.TypedValue.Signal;
import wyvc.builder.ElementCompiler.CompilationData;
import wyvc.builder.ElementCompiler.InterfacePattern;
import wyvc.builder.ElementCompiler.PrimitiveTypedIdentifier;
import wyvc.builder.ElementCompiler.TypedIdentifierTree;
import wyvc.builder.LexicalElementTree.Compound;
import wyvc.builder.LexicalElementTree.Primitive;
import wyvc.builder.LexicalElementTree.Tree;
import wyvc.builder.TypeCompiler.NominalTypeException;
import wyvc.builder.TypeCompiler.UnsupportedTypeException;
import wyvc.builder.VHDLCompileTask.VHDLCompilationException;
import wyvc.lang.Component;
import wyvc.lang.Expression;
import wyvc.lang.Expression.*;
import wyvc.lang.LexicalElement.VHDLException;
import wyvc.lang.Statement.ConcurrentStatement;
import wyvc.lang.Statement.StatementGroup;
import wyvc.lang.Statement.ComponentInstance;
import wyvc.lang.LexicalElement.UnsupportedException;

public class ExpressionCompiler {
	private CompilationData data;


	///////////////////////////////////////////////////////////////////////
	//                          Expressions                              //
	///////////////////////////////////////////////////////////////////////


	public static interface ExpressionTree extends Tree<ExpressionTree,Expression> {

	}

	public static class PrimitiveExpression extends Primitive<ExpressionTree,Expression> implements ExpressionTree {
		public PrimitiveExpression(Expression expression) {
			super(expression);
		}
	}

	public static class CompoundExpression extends Compound<ExpressionTree,Expression> implements ExpressionTree {
		public CompoundExpression(List<Pair<String, ExpressionTree>> components) {
			super(components);
		}

	}





	public ExpressionCompiler(CompilationData data) {
		this.data = data;
	}

	@SuppressWarnings("unchecked")
	public ExpressionTree compile(Location<?> location) throws VHDLException, VHDLCompilationException {
		Bytecode bytecode = location.getBytecode();
		if (bytecode instanceof VariableAccess)
			return compileVariableAccess((Location<VariableAccess>) location);
		if (bytecode instanceof Operator)
			return compileOperator((Location<Operator>) location);
		if (bytecode instanceof Const)
			return compileConst((Location<Const>) location);
		if (bytecode instanceof Invoke)
			return compileInvoke((Location<Invoke>) location).get(0);
		if (bytecode instanceof FieldLoad)
			return compileFieldLoad((Location<FieldLoad>) location);
		throw new UnsupportedException(bytecode.getClass());
	}

	public static ExpressionTree compileIdentifierAccess(TypedIdentifierTree id) throws VHDLException {
		if (id instanceof PrimitiveTypedIdentifier)
			return new PrimitiveExpression(new Access(id.getValue()));
		ArrayList<Pair<String,ExpressionTree>> acc = new ArrayList<>();
		for (Pair<String,TypedIdentifierTree> p : id.getComponents())
			acc.add(new Pair<String,ExpressionTree>(p.first, compileIdentifierAccess(p.second)));
		return new CompoundExpression(acc);
	}

	private ExpressionTree compileVariableAccess(Location<VariableAccess> location) throws VHDLException {
		assert(data.values.containsKey(location.getBytecode().getOperand(0)));
		return compileIdentifierAccess(data.values.get(location.getBytecode().getOperand(0)));
	}

	private Expression operator(Location<Operator> location, Expression e1, Expression e2) throws VHDLException{
		switch (location.getBytecode().kind()) {
		case ADD:
			return new Add(e1, e2);
		case SUB:
			return new Sub(e1, e2);
		case BITWISEAND:
		case AND:
			return new And(e1, e2);
		case BITWISEOR:
		case OR:
			return new Or (e1, e2);
		case BITWISEXOR:
			return new Xor(e1, e2);
		default:
			throw new UnsupportedException(Expression.class);
		}
	}

	static public class BadOperatorArgumentStructureException extends VHDLCompilationException {
		private static final long serialVersionUID = 2495210249219832612L;
		private final Tree<?,?> t;

		public BadOperatorArgumentStructureException(Tree<?,?> t) {
			this.t = t;
		}

		@Override
		protected void details() {
			System.err.println("    Bad operand structure : *");
			t.printStructure(System.err,"                           ");
		}
	}

	private ExpressionTree compileOperator(Location<Operator> location) throws VHDLException, VHDLCompilationException {
		if (location.getBytecode().kind() == OperatorKind.RECORDCONSTRUCTOR) {
			int k = 0;
			ArrayList<Pair<String, ExpressionTree>> fields = new ArrayList<>();
			for (String f : ((Type.EffectiveRecord)location.getType()).getFieldNames())
				fields.add(new Pair<String, ExpressionTree>(f, compile(location.getOperand(k++))));
			return new CompoundExpression(fields);
		}
		ExpressionTree e1 = compile(location.getOperand(0));
		ExpressionTree e2 = compile(location.getOperand(1));
		if (e1 instanceof CompoundExpression)
			throw new BadOperatorArgumentStructureException(e1); // TODO mieux
		if (e2 instanceof CompoundExpression)
			throw new BadOperatorArgumentStructureException(e2); // TODO mieux
		return new PrimitiveExpression(operator(location, e1.getValue(), e2.getValue()));
	}

	private ExpressionTree compileConst(Location<Const> location) throws VHDLException, UnsupportedTypeException, NominalTypeException {
		return new PrimitiveExpression(new Value(TypeCompiler.compileType(location.getType(), data.types).getValue(), location.getBytecode().constant().toString()));
	}

	public ArrayList<ExpressionTree> compileInvoke(Location<Invoke> location) throws VHDLException, VHDLCompilationException {
		String fct = location.getBytecode().name().name();
		if (! data.components.containsKey(fct)) {
			InterfacePattern in = ElementCompiler.compileInterface(fct, location.getBytecode().type(), data);
			data.components.put(fct, new Triple<Integer, Component, InterfacePattern>(0,new Component(fct,in.interface_), in));
		}
		ArrayList<Signal> ports = new ArrayList<>();
		ArrayList<ConcurrentStatement> funGroup = new ArrayList<>();
		ArrayList<ExpressionTree> output = new ArrayList<>();
		Triple<Integer, Component, InterfacePattern> cmp = data.components.get(fct);
		int callNb = cmp.first.intValue();
		data.components.put(fct, new Triple<>(callNb+1, cmp.second, cmp.third));
		int k = 0;
		for (TypedIdentifierTree p : cmp.third.inputs){
			ExpressionTree expr = compile(location.getOperand(k));
			p.checkIdenticalStructure(expr);
			TypedIdentifierTree s = TypedIdentifierTree.createSignal(fct+"_"+callNb+"_in_"+k++, p.getType(), data);
			ports.addAll(Utils.convert(s.getValues()));
			funGroup.add(s.assign(expr));
		}
		k = 0;
		for (TypedIdentifierTree p : cmp.third.outputs){
			TypedIdentifierTree s = TypedIdentifierTree.createSignal(fct+"_"+callNb+"_out_"+k++, p.getType(), data);
			ports.addAll(Utils.convert(s.getValues()));
			output.add(compileIdentifierAccess(s));
			k++;
		}
		funGroup.add(new ComponentInstance(fct+"_"+callNb, cmp.second, ports.toArray(new Signal[0])));
		data.statements.add(new StatementGroup(funGroup.toArray(new ConcurrentStatement[0])));
		return output;
	}


	public ExpressionTree compileFieldLoad(Location<FieldLoad> location) throws VHDLException, VHDLCompilationException {
		ExpressionTree t = compile(location.getOperand(0));
		return t.getComponent(location.getBytecode().fieldName());
	}
}
