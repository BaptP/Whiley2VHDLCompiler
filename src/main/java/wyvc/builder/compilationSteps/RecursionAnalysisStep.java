package wyvc.builder.compilationSteps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import wyvc.builder.CompilerLogger;
import wyvc.builder.CompilerLogger.CompilerError;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.builder.DataFlowGraph;
import wyvc.builder.DataFlowGraph.FuncCallNode;
import wyvc.builder.compilationSteps.CompileFunctionsStep.CompiledFunctions;
import wyvc.builder.compilationSteps.CompileTypesStep.CompiledTypes;
import wyvc.utils.GPairList;
import wyvc.utils.Generators;
import wyvc.utils.Pair;
import wyvc.utils.Utils;

public class RecursionAnalysisStep extends CompilationStep<CompiledFunctions, RecursionAnalysisStep.OrderedFunction> {
	public static class OrderedFunction extends CompiledTypes {
		public final GPairList<String, DataFlowGraph> func;

		public OrderedFunction(CompiledTypes cmp, GPairList<String, DataFlowGraph> func) {
			super(cmp);
			this.func = func;
		}
		public OrderedFunction(OrderedFunction other) {
			super(other);
			func = other.func;
		}
	}

	public static class RecursiveCallError extends CompilerError {
		private final FuncCallNode call;

		public RecursiveCallError(FuncCallNode call) {
			this.call = call;
		}
		@Override
		public String info() {
			return "Compilation of recursive calls unsupported\n(Detected call loop start in function "+call.funcName+")";
		}

	}

	private static class FuncNode {
		public final DataFlowGraph data;
		public int rank = -1;

		public FuncNode(DataFlowGraph s) {
			this.data = s;
		}

		public int findRank(Map<String, FuncNode> func) throws CompilerException {
			if (rank < 0) {
				rank = -2;
				rank = data.getInvokesNodes().fold_((r,f) -> {
					FuncNode fc = func.get(f.funcName);
					if (fc.rank == -2)
						throw new CompilerException(new RecursiveCallError(f));
					return Math.max(r, fc.findRank(func)+1);
				}, 0);
			}
			return rank;
		}
	}

	@Override
	protected OrderedFunction compile(CompilerLogger logger, CompiledFunctions data) throws CompilerException {
		Map<String, FuncNode> func = new HashMap<>();
		data.func.forEach((String n, DataFlowGraph s) -> func.put(n, new FuncNode(s)));
		for (FuncNode c : func.values())
			c.findRank(func);
		GPairList<String, FuncNode> fcts = Generators.fromMap(func).toList();
		func.forEach((s, n) -> logger.debug(s+" rang "+n.rank));
		fcts.sort((e1, e2) -> e1.second.rank - e2.second.rank);
		Generators.fromPairCollection(fcts).takeSecond().forEach_(n -> n.data.updateInvokeLatency(s -> func.containsKey(s) ? func.get(s).data : null));
		return new OrderedFunction(data, fcts.generate().mapSecond(f -> f.data).toList());
	}
}
