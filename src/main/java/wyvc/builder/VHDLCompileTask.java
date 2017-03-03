package wyvc.builder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import wybs.lang.Build;
import wybs.lang.Build.Graph;
import wybs.lang.Build.Project;
import wyil.lang.WyilFile.FunctionOrMethod;
import wyvc.Activator;
import wycc.util.Logger;
import wycc.util.Pair;
import wyfs.lang.Path;
import wyfs.lang.Path.Entry;
import wyfs.lang.Path.Root;
import wyil.lang.WyilFile;
import wyvc.lang.Entity;
import wyvc.lang.LexicalElement.VHDLException;
import wyvc.utils.Utils;
import wyvc.lang.VHDLFile;
import wyvc.builder.ElementCompiler;
import wyvc.builder.ElementCompiler.CompilationData;
import wyvc.builder.TypeCompiler.TypeTree;

public class VHDLCompileTask implements Build.Task {
	private Logger logger = Logger.NULL;

	private Build.Project project;

	public VHDLCompileTask(Build.Project project) {
		this.project = project;
	}

	public Project project() {
		return project;
	}

	public Set<Entry<?>> build(Collection<Pair<Entry<?>, Root>> delta, Graph graph) throws IOException {
		Runtime runtime = Runtime.getRuntime();
		long start = System.currentTimeMillis();
		long memory = runtime.freeMemory();

		HashSet<Path.Entry<?>> generatedFiles = new HashSet<Path.Entry<?>>();


		for (Pair<Path.Entry<?>, Path.Root> p : delta) {
			Path.Root dst = p.second();
			System.out.println(p.toString());
			@SuppressWarnings("unchecked")
			Path.Entry<WyilFile> source = (Path.Entry<WyilFile>) p.first();
			WyilFile f = source.read();
			System.out.println(f.toString());

			ArrayList<Entity> entities = new ArrayList<Entity>();
			try {
				CompilationData data = new CompilationData();
				for (WyilFile.Type t  : f.types())
					data.types.put(t.name(), TypeCompiler.compileType(t.type(),data));
				for (FunctionOrMethod fct : f.functionOrMethods()){
					//Utils.printLocation(fct.getBody(), "");
					entities.add(ElementCompiler.compileEntity(fct, data));
				}
			} catch (VHDLException e) {
				e.printStackTrace();
				e.info();
			} catch (VHDLCompilationException e) {
				e.printStackTrace();
				e.info();
			}

			Path.Entry<VHDLFile> target = dst.create(source.id(), Activator.ContentType);
			graph.registerDerivation(source, target);
			generatedFiles.add(target);
			//*
			VHDLFile contents = new VHDLFile(entities.toArray(new Entity[0]));
			/*/
			VHDLFile contents = new VHDLFile();
			//*/
			target.write(contents);

		}


		long endTime = System.currentTimeMillis();
		logger.logTimedMessage("Wyil => VHDL: compiled " + delta.size() + " file(s)", endTime - start,
				memory - runtime.freeMemory());
		return generatedFiles;
	}


	public abstract static class VHDLCompilationException extends Exception {
		private static final long serialVersionUID = 1062123869833614980L;

		protected abstract void details();

		public void info() {
			System.err.println("Unsupported VHDL compilation feature");
			details();
		}

	}


/*
	private Architecture compileFunctionBody(Location<Block> body, int i) throws VHDLException, VHDLCompilationException {
		printLocation(body, "");
		Architecture arch = new Architecture(entity, "Functional");
		ArrayList<Statement> st = new ArrayList<>();
		Location<Return> a = (Location<Return>)body.getOperand(0);
		for (Location<?> l : a.getOperands())
			st.add(new SignalAssignment(interface_.ports[i++], compileExpression(l)));
		/*
		System.out.println("Il y a " + Integer.toString(body.numberOfBlocks()) + " blocks");
		System.out.println("Il y a " + Integer.toString(body.numberOfOperands()) + " operands");
		System.out.println("Il y a " + Integer.toString(body.numberOfTypes()) + " types");
		System.out.println(body.getBytecode().toString());
		for(int i = 0; i<body.numberOfOperands(); ++i)
			System.out.println(body.getOperand(i));* /
		arch.statements = st.toArray(new ConcurrentStatement[0]);
		return arch;
	}


	@SuppressWarnings("unchecked")
	Expression compileExpression(Location<?> e) throws VHDLException, VHDLCompilationException {
		Bytecode b = e.getBytecode();
		if (b instanceof Operator)
			return compileOperator((Location<Operator>) e);
		if (b instanceof VariableAccess)
			return compileVariableAccess((Location<VariableAccess>) e);
		System.out.println("Oups "+e.toString());
		return null;
	}

	Expression compileOperator(Location<Operator> acc) throws VHDLException, VHDLCompilationException {
		Expression op0 = compileExpression(acc.getOperand(0));
		Expression op1 = compileExpression(acc.getOperand(1));
		switch (acc.getBytecode().kind()) {
		case BITWISEAND: 	return new Expression.And(op0, op1);
		case BITWISEOR: 	return new Expression.Or(op0, op1);
		case BITWISEXOR: 	return new Expression.Xor(op0, op1);
		case ADD: 			return new Expression.Add(op0, op1);
		case SUB: 			return new Expression.Sub(op0, op1);

		default: 			throw new VHDLCompilationException();
		}
	} // TODO Not + binop -> nand ...

	Expression compileVariableAccess(Location<VariableAccess> acc) throws VHDLException {
		return new Access(interface_.ports[acc.getBytecode().getOperand(0)]);
	}*/

}
