package wyvc.builder.compilationSteps;

import java.util.HashMap;
import java.util.Map;

import wyil.lang.WyilFile.FunctionOrMethod;
import wyvc.builder.CompilerLogger;
import wyvc.builder.ControlFlowGraphBuilder;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.builder.ControlFlowGraph.WyilSection;
import wyvc.builder.compilationSteps.CompileTypesStep.CompiledTypes;

public class CompileFunctionsStep extends CompilationStep<CompiledTypes, CompileFunctionsStep.CompiledFunctions> {
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

	@Override
	protected CompiledFunctions compile(CompilerLogger logger, CompiledTypes data) throws CompilerException {
		Map<String, WyilSection> func = new HashMap<>();
		for (FunctionOrMethod fct : data.file.functionOrMethods())
			func.put(fct.name(), ControlFlowGraphBuilder.buildGraph(logger, fct, data.types));
		return new CompiledFunctions(data, func);
	}
}
