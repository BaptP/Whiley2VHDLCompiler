package wyvc.builder;


import java.util.ArrayList;

import wycc.util.Pair;
import wyil.lang.Bytecode;
import wyil.lang.Bytecode.Const;
import wyil.lang.Bytecode.Operator;
import wyil.lang.Bytecode.VariableAccess;
import wyil.lang.Bytecode.Invoke;
import wyil.lang.SyntaxTree.Location;
import wyvc.lang.Type.*;
import wyvc.lang.TypedValue.Port;
import wyvc.lang.TypedValue.Port.Mode;
import wyvc.lang.TypedValue.Signal;
import wyvc.builder.ArchitectureCompiler.ArchitectureData;
import wyvc.lang.Component;
import wyvc.lang.Expression;
import wyvc.lang.Expression.*;
import wyvc.lang.LexicalElement.VHDLException;
import wyvc.lang.Statement.ConcurrentStatement;
import wyvc.lang.Statement.StatementGroup;
import wyvc.lang.Statement.ComponentInstance;
import wyvc.lang.Statement.SignalAssignment;
import wyvc.lang.LexicalElement.UnsupportedException;

public class ExpressionCompiler {
	private ArchitectureData architecture;

	public ExpressionCompiler(ArchitectureData architecture) {
		this.architecture = architecture;
	}

	@SuppressWarnings("unchecked")
	public Expression compile(Location<?> location) throws VHDLException {
		Bytecode bytecode = location.getBytecode();
		if (bytecode instanceof VariableAccess)
			return compileVariableAccess((Location<VariableAccess>) location);
		if (bytecode instanceof Operator)
			return compileOperator((Location<Operator>) location);
		if (bytecode instanceof Const)
			return compileConst((Location<Const>) location);
		if (bytecode instanceof Invoke)
			return compileInvoke((Location<Invoke>) location).get(0);
		throw new UnsupportedException(bytecode.getClass());
	}

	private Expression compileVariableAccess(Location<VariableAccess> location) throws VHDLException {
		assert(architecture.values.containsKey(location.getBytecode().getOperand(0)));
		return new Access(architecture.values.get(location.getBytecode().getOperand(0)).read());
	}

	private Expression compileOperator(Location<Operator> location) throws VHDLException {
		switch (location.getBytecode().kind()) {
		case ADD:
			return new Add(compile(location.getOperand(0)), compile(location.getOperand(1)));
		case SUB:
			return new Sub(compile(location.getOperand(0)), compile(location.getOperand(1)));
		case BITWISEAND:
		case AND:
			return new And(compile(location.getOperand(0)), compile(location.getOperand(1)));
		case BITWISEOR:
		case OR:
			return new Or (compile(location.getOperand(0)), compile(location.getOperand(1)));
		case BITWISEXOR:
			return new Xor(compile(location.getOperand(0)), compile(location.getOperand(1)));


		default:
			throw new UnsupportedException(Expression.class);
		}
	}

	private Expression compileConst(Location<Const> location) throws VHDLException {
		return new Value(new Signed(31,0), location.getBytecode().constant().toString());
	}

	public ArrayList<Expression> compileInvoke(Location<Invoke> location) throws VHDLException {
		String fct = location.getBytecode().name().name();
		if (! architecture.components.containsKey(fct))
			architecture.components.put(fct, new Pair<>(0,new Component(fct, ElementCompiler.compileInterface(fct, location.getBytecode().type()))));
		ArrayList<Signal> ports = new ArrayList<>();
		ArrayList<ConcurrentStatement> funGroup = new ArrayList<>();
		int inp = 0;
		int out = 0;
		ArrayList<Expression> output = new ArrayList<>();
		Pair<Integer, Component> cmp = architecture.components.get(fct);
		int callNb = cmp.first().intValue();
		architecture .components.put(fct, new Pair<>(callNb+1, cmp.second()));
		int k = 0;
		for (Port p : cmp.second().interface_.ports){
			if (p.mode == Mode.IN) {
				Expression expr = compile(location.getOperand(inp));
				if (expr instanceof Access && ((Access) expr).value instanceof Signal)
					ports.add((Signal) ((Access) expr).value);
				else {
					Signal s = new Signal(fct+"_"+callNb+"_in_"+inp++, p.type);
					architecture.signals.add(s);
					ports.add(s);
					funGroup.add(new SignalAssignment(s, expr));
				}
			}
			else{
				Signal s = new Signal(fct+"_"+callNb+"_in_"+inp++, p.type);
				architecture.signals.add(s);
				ports.add(s);
				output.add(new Access(s));
			}
			k++;
		}
		funGroup.add(new ComponentInstance(fct+"_"+callNb, cmp.second(), ports.toArray(new Signal[0])));
		architecture.statements.add(new StatementGroup(funGroup.toArray(new ConcurrentStatement[0])));

		return output;
	}
}
