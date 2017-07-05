package wyvc.builder.compilationSteps;

import wyvc.builder.CompilerLogger;
import wyvc.builder.DataFlowGraphOptimizer;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.builder.compilationSteps.CompileFunctionsStep.CompiledFunctions;

public class GraphOptimizationStep extends CompilationStep<CompiledFunctions, CompiledFunctions> {

	@Override
	protected CompiledFunctions compile(CompilerLogger logger, CompiledFunctions data) throws CompilerException {
		DataFlowGraphOptimizer opt = new DataFlowGraphOptimizer(logger);
		data.func.forEach((s,t) -> opt.optimize(t));
		return data;
	}
}
