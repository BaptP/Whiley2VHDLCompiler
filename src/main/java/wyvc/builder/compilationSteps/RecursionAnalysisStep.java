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
import wyvc.utils.Pair;
import wyvc.utils.Utils;

public class RecursionAnalysisStep extends CompilationStep<CompiledFunctions, RecursionAnalysisStep.OrderedFunction> {
	public static class OrderedFunction extends CompiledTypes {
		public final List<Pair<String, DataFlowGraph>> func;

		public OrderedFunction(CompiledTypes cmp, List<Pair<String, DataFlowGraph>> func) {
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
				int r = 0;
				for (FuncCallNode f : data.invokes) {
					FuncNode fc = func.get(f.funcName);
					if (fc.rank == -2)
						throw new CompilerException(new RecursiveCallError(f));
					r = Math.max(r, fc.findRank(func)+1);
				}
				rank = r;
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
		List<Pair<String, FuncNode>> fcts = new ArrayList<>();
		func.forEach((String s, FuncNode n) -> fcts.add(new Pair<String, FuncNode>(s, n)));
		func.forEach((String s, FuncNode n) -> logger.debug(s+" rang "+n.rank));
		fcts.sort((Pair<String, FuncNode> e1, Pair<String, FuncNode> e2) -> e1.second.rank - e2.second.rank);
		return new OrderedFunction(data, Utils.convert(fcts, (Pair<String, FuncNode> p) -> new Pair<String, DataFlowGraph>(p.first, p.second.data)));
	}
}
