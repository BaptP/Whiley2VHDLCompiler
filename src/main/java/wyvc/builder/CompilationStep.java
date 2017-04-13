package wyvc.builder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import wyil.lang.WyilFile;
import wyil.lang.WyilFile.FunctionOrMethod;
import wyvc.builder.CompilerLogger.CompilerError;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.builder.CompilerLogger.CompilerMessageType;
import wyvc.builder.ControlFlowGraph.FuncCallNode;
import wyvc.builder.ControlFlowGraph.WyilSection;
import wyvc.builder.TypeCompiler.TypeTree;
import wyvc.builder.VHDLCompileTask.CompilationUnit;
import wyvc.io.GraphPrinter;
import wyvc.lang.VHDLFile;
import wyvc.lang.Statement.ConcurrentStatement;

public abstract class CompilationStep<T,U> {
	private CompilationStep<U,?> next = null;

	protected abstract U compile(CompilerLogger logger, T data) throws CompilerException;

	public final void compileStep(CompilerLogger logger, T data) {
		try {
			U u = compile(logger, data);
			if (next != null && !logger.has(CompilerMessageType.Error)){
				next.compileStep(logger, u);
				return;
			}
		}
		catch (CompilerException e){
			logger.addMessage(e.error);
		}
		logger.printMessages();
	}

	public <V> CompilationStep<U,V> setNextStep(CompilationStep<U,V> step) {
		next = step;
		return step;
	}


	public static class CompilationFront<T> {
		private CompilationStep<T,?> next = null;

		public <U> CompilationStep<T,U> setNextStep(CompilationStep<T,U> step) {
			next = step;
			return step;
		}

		public void compile(CompilerLogger logger, T data) {
			if(next != null)
				next.compileStep(logger, data);
		}
	}






	public static class ParsedFile extends CompilationUnit {
		public final WyilFile file;

		public ParsedFile(CompilationUnit cmp, WyilFile file) {
			super(cmp);
			this.file = file;
		}
		public ParsedFile(ParsedFile other) {
			super(other);
			file = other.file;
		}
	}

	public static class ParsingStep extends CompilationStep<CompilationUnit, ParsedFile> {
		@Override
		protected ParsedFile compile(CompilerLogger logger, CompilationUnit data) {
			try {
				return new ParsedFile(data, data.source.read());
			} catch (IOException e) {
				logger.addMessage(new CompilerError() {
					@Override
					public String info() {
						return "Error reading file "+data.source+"\n"+e.getStackTrace();
					}
				});
			}
			return null;
		}

	}

	public static class CompiledTypes extends ParsedFile {
		public final Map<String, TypeTree> types;

		public CompiledTypes(ParsedFile cmp, Map<String, TypeTree> types) {
			super(cmp);
			this.types = types;
		}
		public CompiledTypes(CompiledTypes other) {
			super(other);
			types = other.types;
		}
	}

	public static class CompileTypesStep extends CompilationStep<ParsedFile, CompiledTypes> {
		@Override
		protected CompiledTypes compile(CompilerLogger logger, ParsedFile data) {
			Map<String, TypeTree> types = new HashMap<>();
			for (WyilFile.Type t  : data.file.types())
				types.put(t.name(), TypeCompiler.compileType(logger, t.type(),types));
			return new CompiledTypes(data, types);
		}

	}

	public static class CompiledFunctions extends CompiledTypes {
		public final Map<String, WyilSection> func;

		public CompiledFunctions(CompiledTypes cmp, Map<String, WyilSection> func) {
			super(cmp);
			this.func = func;
		}
		public CompiledFunctions(CompiledFunctions other) {
			super(other);
			func = other.func;
		}
	}

	public static class CompileFunctionsStep extends CompilationStep<CompiledTypes, CompiledFunctions> {
		@Override
		protected CompiledFunctions compile(CompilerLogger logger, CompiledTypes data) throws CompilerException {
			Map<String, WyilSection> func = new HashMap<>();
			for (FunctionOrMethod fct : data.file.functionOrMethods())
				func.put(fct.name(), ControlFlowGraphBuilder.buildGraph(logger, fct, data.types));
			return new CompiledFunctions(data, func);
		}

	}

	public static class InliningStep extends CompilationStep<CompiledFunctions, CompiledFunctions> {
		@Override
		protected CompiledFunctions compile(CompilerLogger logger, CompiledFunctions data) {
			UnaryOperator<WyilSection> inlining = (WyilSection s) -> {
				for (FuncCallNode c : s.invokes)
					c.inline(data.func.get(c.funcName));
				return s;
			};

			data.func.forEach((String n, WyilSection s) -> {
				//GraphPrinter.print(s.inputs, s.outputs, n);
				WyilSection se = inlining.apply(s);
				GraphPrinter.print(logger, se.inputs, se.outputs, n);
			});
			return data;
		}

	}

	public static class ProducingVHDLStep extends CompilationStep<CompiledFunctions, Void> {

		@Override
		protected Void compile(CompilerLogger logger, wyvc.builder.CompilationStep.CompiledFunctions data)
				throws CompilerException {
			for (String f : data.func.keySet())
				for (ConcurrentStatement s : SectionCompiler.compile(logger, data.func.get(f), true, true))
					System.out.println(s);
			return null;
		}

	}



	public static class ExportFileStep extends CompilationStep<VHDLFile,Void> {

		@Override
		protected Void compile(CompilerLogger logger, VHDLFile data) {
			/*try {
				Path.Entry<VHDLFile> target = dst.create(source.id(), Activator.ContentType);
				graph.registerDerivation(source, target);
				generatedFiles.add(target);
				target.write(data);
			} catch (IOException e) {
				e.printStackTrace();
			}*/
			return null;
		}
	}

}
