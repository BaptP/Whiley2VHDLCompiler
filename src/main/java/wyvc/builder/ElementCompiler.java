package wyvc.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import wyvc.utils.Pair;
import wyvc.utils.Triple;
import wyvc.utils.Utils;
import wyil.lang.Type;
import wyil.lang.WyilFile.FunctionOrMethod;
import wyvc.builder.LexicalElementTree.Compound;
import wyvc.builder.ExpressionCompiler.ExpressionTree;
import wyvc.builder.ExpressionCompiler.PrimitiveExpression;
import wyvc.builder.LexicalElementTree.IdentifierStructureException;
import wyvc.builder.LexicalElementTree.Primitive;
import wyvc.builder.LexicalElementTree.Tree;
import wyvc.builder.TypeCompiler.PrimitiveType;
import wyvc.builder.TypeCompiler.TypeTree;
import wyvc.builder.TypeCompiler.CompoundType;
import wyvc.builder.TypeCompiler.UnsupportedTypeException;
import wyvc.builder.VHDLCompileTask.VHDLCompilationException;
import wyvc.lang.Component;
import wyvc.lang.Entity;
import wyvc.lang.Expression;
import wyvc.lang.Expression.TypesMismatchException;
import wyvc.lang.Interface;
import wyvc.lang.TypedValue;
import wyvc.lang.LexicalElement.UnsupportedException;
import wyvc.lang.LexicalElement.VHDLException;
import wyvc.lang.Statement.ConcurrentStatement;
import wyvc.lang.Statement.StatementGroup;
import wyvc.lang.Statement.SignalAssignment;
import wyvc.lang.TypedValue.Port;
import wyvc.lang.TypedValue.Signal;
import wyvc.lang.TypedValue.Port.Mode;
import wyvc.lang.TypedValue.PortException;

public class ElementCompiler {
	///////////////////////////////////////////////////////////////////////
	//                        TypedIdentifiers                           //
	///////////////////////////////////////////////////////////////////////

	public static interface TypedIdentifierTree extends Tree<TypedIdentifierTree,TypedValue> {
		public int getAssignmentsNumber();
		public StatementGroup assign(ExpressionTree expression) throws IdentifierStructureException, TypesMismatchException, PortException;
		public TypeTree getType();
		public String getIdent();


		public static TypedIdentifierTree createPort(String ident, TypeTree type, Mode mode, ArrayList<Port> ports, CompilationData data) {
			if (type instanceof PrimitiveType){
				Port port = new Port(ident, type.getValue(), mode);
				ports.add(port);
				return new PrimitiveTypedIdentifier(port, type, data);
			}
			ArrayList<Pair<String, TypedIdentifierTree>> cmp = new ArrayList<>(type.getComponentNumber());
			for (Pair<String, TypeTree> p : type.getComponents())
				cmp.add(new Pair<String, TypedIdentifierTree>(p.first, createPort(ident+"_"+p.first, p.second, mode, ports, data)));
			return new CompoundTypedIdentifier(ident, cmp, type, data);
		}

		public static TypedIdentifierTree createSignal(String ident, TypeTree type, CompilationData data) {
			if (type instanceof PrimitiveType){
				Signal signal = new Signal(ident, type.getValue());
				data.signals.add(signal);
				return new PrimitiveTypedIdentifier(signal, type, data);
			}
			ArrayList<Pair<String, TypedIdentifierTree>> cmp = new ArrayList<>(type.getComponentNumber());
			for (Pair<String, TypeTree> p : type.getComponents())
				cmp.add(new Pair<String, TypedIdentifierTree>(p.first, createSignal(ident+"_"+p.first, p.second, data)));
			return new CompoundTypedIdentifier(ident, cmp, type, data);
		}
/*
		public static TypedIdentifierTree createAccess(String ident, ExpressionTree acc, CompilationData data) throws UnsupportedException {
			if (acc instanceof PrimitiveExpression){
				Expression expr = acc.getValue();
				if (expr instanceof Expression.Access)
					return new PrimitiveTypedIdentifier(((Expression.Access)expr).value, new PrimitiveType(((Expression.Access)expr).value.type), data);
				throw new UnsupportedException(expr.getClass()); // TODO mieux ?;
			}
			ArrayList<Pair<String, TypedIdentifierTree>> cmp = new ArrayList<>(acc.getComponentNumber());
			ArrayList<Pair<String, TypeTree>> typ = new ArrayList<>(acc.getComponentNumber());
			for (Pair<String, ExpressionTree> p : acc.getComponents()){
				TypedIdentifierTree t = createAccess(ident+"_"+p.first, p.second, data);
				cmp.add(new Pair<String, TypedIdentifierTree>(p.first, t));
				typ.add(new Pair<String, TypeTree>(p.first, t.getType()));
			}
			return new CompoundTypedIdentifier(ident, cmp, new CompoundType(typ), data);
		}*/
	}

	public static class PrimitiveTypedIdentifier extends Primitive<TypedIdentifierTree,TypedValue> implements TypedIdentifierTree {
		private final CompilationData data;
		private int assignmentsNb;
		private final String ident;
		private final TypeTree type;

		public PrimitiveTypedIdentifier(TypedValue value, TypeTree type, CompilationData data) {
			super(value);
			this.data = data;
			assignmentsNb = value.isWritable() ? 0 : 1;
			ident = value.ident;
			this.type = type;
		}

		@Override
		public int getAssignmentsNumber() {
			return assignmentsNb;
		}

		@Override
		public TypeTree getType() {
			return type;
		}

		@Override
		public String getIdent() {
			return assignmentsNb == 0 ? ident : ident+"_"+assignmentsNb;
		}

		@Override
		public StatementGroup assign(ExpressionTree expression) throws IdentifierStructureException, TypesMismatchException, PortException {
			checkIdenticalStructure(expression);
			++assignmentsNb;
			if (assignmentsNb != 1) {
				Signal s = new Signal(ident + "_" + assignmentsNb, value.type);
				value = s;
				data.signals.add(s);
			}
			ConcurrentStatement[] st = {new SignalAssignment((Signal) value, expression.getValue())};
			return new StatementGroup(st);
		}
	}

	public static class CompoundTypedIdentifier extends Compound<TypedIdentifierTree,TypedValue> implements TypedIdentifierTree {
		private final CompilationData data;
		private int assignmentsNb;
		private final String ident;
		private final TypeTree type;

		public CompoundTypedIdentifier(String ident, List<Pair<String, TypedIdentifierTree>> components, TypeTree type, CompilationData data) {
			super(components);
			this.data = data;
			this.ident = ident;
			this.type = type;
			for (Pair<String, TypedIdentifierTree> p : components)
				assignmentsNb = Math.max(assignmentsNb, p.second.getAssignmentsNumber());
		}

		@Override
		public int getAssignmentsNumber() {
			return assignmentsNb;
		}

		@Override
		public TypeTree getType() {
			return type;
		}

		@Override
		public String getIdent() {
			return assignmentsNb == 0 ? ident : ident+"_"+assignmentsNb;
		}

		@Override
		public StatementGroup assign(ExpressionTree expression) throws IdentifierStructureException, TypesMismatchException, PortException {
			checkIdenticalStructure(expression);
			++assignmentsNb;
			if (assignmentsNb != 1)
				for (Pair<String, TypedIdentifierTree> p : getComponents())
					p.second = TypedIdentifierTree.createSignal(ident+"_"+assignmentsNb+"_"+p.first, p.second.getType(), data);
			StatementGroup st = new StatementGroup(new ConcurrentStatement[0]);
			for (Pair<TypedIdentifierTree, ExpressionTree> p : Utils.join(Utils.takeSecond(getComponents()), Utils.takeSecond(expression.getComponents())))
				st = st.concat(p.first.assign(p.second));
			return st;

		}
	}



	public static class CompilationData {
		public Map<Integer, TypedIdentifierTree> values = new HashMap<>();
		public Map<String, Triple<Integer, Component, InterfacePattern>> components = new HashMap<>();
		public ArrayList<Signal> signals = new ArrayList<>();
		public ArrayList<ConcurrentStatement> statements = new ArrayList<>();
		public Map<String, TypeTree> types = new HashMap<>();

		public void entityReset() {
			values = new HashMap<>();
			components = new HashMap<>();
			signals = new ArrayList<>();
			statements = new ArrayList<>();
		}
	}




	public static Entity compileEntity(FunctionOrMethod function, CompilationData data) throws VHDLException, VHDLCompilationException{
		data.entityReset();
		InterfacePattern interface_ = compileInterface(function.name(), function.type(), data);
		Entity entity =  new Entity(
			function.name(),
			interface_.interface_
		);
		int i = 0;
		for (TypedIdentifierTree id : interface_.inputs)
			data.values.put(i++, id);
		i = 0;
		for (TypedIdentifierTree id : interface_.outputs)
			data.values.put(--i, id);
		ArchitectureCompiler ac = new ArchitectureCompiler(entity, data);
		entity.addArchitectures(ac.compile(function.getBody()));
		return entity;
	}

	public static class InterfacePattern {
		public final Interface interface_;
		public final List<TypedIdentifierTree> inputs;
		public final List<TypedIdentifierTree> outputs;

		public InterfacePattern(Interface interface_, List<TypedIdentifierTree> inputs, List<TypedIdentifierTree> outputs) {
			this.interface_ = interface_;
			this.inputs = inputs;
			this.outputs = outputs;
		}
	}

	public static InterfacePattern compileInterface(String name, wyil.lang.Type.FunctionOrMethod type, CompilationData data) throws VHDLCompilationException {
		ArrayList<Port> ports = new ArrayList<Port>();
		ArrayList<TypedIdentifierTree> inputs = new ArrayList<>();
		ArrayList<TypedIdentifierTree> outputs = new ArrayList<>();
		int i = 0;
		for(Type t : type.params())
			inputs.add(TypedIdentifierTree.createPort(name+"_in_"+Integer.toString(i++), TypeCompiler.compileType(t, data), Mode.IN, ports, data));
		i = 0;
		for(Type t : type.returns())
			outputs.add(TypedIdentifierTree.createPort(name+"_out_"+Integer.toString(i++), TypeCompiler.compileType(t, data), Mode.OUT, ports, data));
		return new InterfacePattern(new Interface(ports.toArray(new Port[0])), inputs, outputs);
	}

}
