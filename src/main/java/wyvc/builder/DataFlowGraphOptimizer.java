package wyvc.builder;

import wyvc.builder.CompilerLogger.LoggedBuilder;

public class DataFlowGraphOptimizer extends LoggedBuilder {

	public DataFlowGraphOptimizer(CompilerLogger logger) {
		super(logger);
	}

	public static enum Optimization {
		UndefinedMuxBranch,
		BooleanMux,
		StaticallyKnowValueResolution;
	}




	public DataFlowGraph optimize(DataFlowGraph graph) {
		return graph;
	}
}
