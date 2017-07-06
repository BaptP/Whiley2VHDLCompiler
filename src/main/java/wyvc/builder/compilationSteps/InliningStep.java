package wyvc.builder.compilationSteps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import wyvc.builder.CompilerLogger;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.builder.DataFlowGraph;
import wyvc.builder.DataFlowGraph.WhileNode;
import wyvc.builder.DataFlowGraphBuilder;
import wyvc.builder.PipelineBuilder;
import wyvc.io.DataFlowGraphPrinter;
import wyvc.io.GraphPrinter;
import wyvc.builder.compilationSteps.CompileTypesStep.CompiledTypes;
import wyvc.builder.compilationSteps.RecursionAnalysisStep.OrderedFunction;
import wyvc.utils.Pair;

public class InliningStep extends CompilationStep<OrderedFunction, InliningStep.SplittedFunctions> {
	public static class SplittedFunctions extends CompiledTypes {
		public final List<Pair<String, List<DataFlowGraph>>> func;

		public SplittedFunctions(CompiledTypes cmp, List<Pair<String, List<DataFlowGraph>>> func) {
			super(cmp);
			this.func = func;
		}
		public SplittedFunctions(SplittedFunctions other) {
			super(other);
			func = other.func;
		}
	}







	@Override
	protected SplittedFunctions compile(CompilerLogger logger, OrderedFunction data) throws CompilerException {
		/*UnaryOperator<WyilSection> inlining = (WyilSection s) -> {
			for (FuncCallNode c : s.invokes)
				c.inline(data.func.get(c.funcName));
			return s;
		};*/
		PipelineBuilder pipelining = new PipelineBuilder(logger);

		List<Pair<String, List<DataFlowGraph>>> func = new ArrayList<>();
		for (Pair<String, DataFlowGraph> p : data.func) {
			GraphPrinter.print(logger, p.second, p.first);
			for (WhileNode d : p.second.whileNodes) {
				GraphPrinter.print(logger, d.body, p.first+"wb");
				GraphPrinter.print(logger, d.condition, p.first+"wc");
			}

			GraphPrinter.print(logger, pipelining.buildPipeline(p.second), p.first);
			//WyilSection se = inlining.apply(s);
			//GraphPrinter.print(logger, se.inputs, se.outputs, n);
			func.add(new Pair<String, List<DataFlowGraph>>(p.first,  split(p.second)));
		};
		return new SplittedFunctions(data, func);
	}


	private List<DataFlowGraph> split(DataFlowGraph s) {
		return Collections.singletonList(s);
	}
}
