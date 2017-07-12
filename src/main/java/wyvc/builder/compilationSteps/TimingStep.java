package wyvc.builder.compilationSteps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import wyvc.builder.CompilerLogger;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.builder.DataFlowGraph;
import wyvc.builder.DataFlowGraph.WhileNode;
import wyvc.builder.DataFlowGraphBuilder;
import wyvc.builder.PipelineBuilder;
import wyvc.builder.PipelineBuilder.Delay;
import wyvc.io.GraphPrinter;
import wyvc.builder.compilationSteps.CompileTypesStep.CompiledTypes;
import wyvc.builder.compilationSteps.RecursionAnalysisStep.OrderedFunction;
import wyvc.utils.GPairList;
import wyvc.utils.GTripleList;
import wyvc.utils.Pair;
import wyvc.utils.Triple;

public class TimingStep extends CompilationStep<OrderedFunction, TimingStep.TimedFunctions> {
	public static class TimedFunctions extends CompiledTypes {
		public final GPairList<String, DataFlowGraph> func;

		public TimedFunctions(CompiledTypes cmp, GPairList<String, DataFlowGraph> func) {
			super(cmp);
			this.func = func;
		}
		public TimedFunctions(TimedFunctions other) {
			super(other);
			func = other.func;
		}
	}





	private static class Builder {
		private final CompilerLogger logger;
		private final PipelineBuilder pipelining;
		private final Map<String, Delay> funcDelays = new HashMap<>();

		public Builder(CompilerLogger logger) {
			this.logger = logger;
			pipelining = new PipelineBuilder(logger);
		}

		private void print(DataFlowGraph graph, String name) {
			GraphPrinter.print(logger, graph, name);
			int k = 0;
			for (WhileNode n : graph.whileNodes) {
				print(n.body, name+"_w"+k+"_b");
				print(n.condition, name+"_w"+k+"_c");
				k++;
			}
		}

		private DataFlowGraph analyse(String funcName, DataFlowGraph func) throws CompilerException {
			print(func, funcName);
			Pair<DataFlowGraph,Delay> r = pipelining.buildPipeline(func, funcDelays);
			print(r.first, funcName);
			funcDelays.put(funcName, r.second);
			return r.first;
		}

		public GPairList<String, DataFlowGraph> build(GPairList<String, DataFlowGraph> functions) throws CompilerException {
			return functions.generate().computeSecond_(this::analyse).toList();
		}
	}




	@Override
	protected TimedFunctions compile(CompilerLogger logger, OrderedFunction data) throws CompilerException {
		return new TimedFunctions(data, new Builder(logger).build(data.func));
	}


}
