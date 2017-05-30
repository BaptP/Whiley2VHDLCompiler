package wyvc.builder.compilationSteps;

import java.util.HashMap;
import java.util.Map;

import wyil.lang.WyilFile.FunctionOrMethod;
import wyvc.builder.CompilerLogger;
import wyvc.builder.DataFlowGraphBuilder;
import wyvc.builder.CompilerLogger.CompilerError;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.builder.DataFlowGraph;
import wyvc.builder.compilationSteps.CompileTypesStep.CompiledTypes;

public class CompileFunctionsStep extends CompilationStep<CompiledTypes, CompileFunctionsStep.CompiledFunctions> {
	public static class CompiledFunctions extends CompiledTypes {
		public final Map<String, DataFlowGraph> func;

		public CompiledFunctions(CompiledTypes cmp, Map<String, DataFlowGraph> func) {
			super(cmp);
			this.func = func;
		}
		public CompiledFunctions(CompiledFunctions other) {
			super(other);
			func = other.func;
		}
	}

	@Override
	protected CompiledFunctions compile(CompilerLogger logger, CompiledTypes data) throws CompilerException {
		throw new CompilerException(new CompilerError() {
			@Override
			public String info() {
				// TODO Auto-generated method stub
				return "fin";
			}
		});
//		DataFlowGraphBuilder builder = new DataFlowGraphBuilder(logger, data.typeCompiler);
//		Map<String, DataFlowGraph> func = new HashMap<>();
//		for (FunctionOrMethod fct : data.file.functionOrMethods())
//			func.put(fct.name(), builder.buildGraph(fct));
//		return new CompiledFunctions(data, func);
	}
}
