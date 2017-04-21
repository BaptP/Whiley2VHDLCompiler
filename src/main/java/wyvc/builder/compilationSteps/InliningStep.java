package wyvc.builder.compilationSteps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;

import wyvc.builder.CompilerLogger;
import wyvc.io.GraphPrinter;
import wyvc.builder.ControlFlowGraph.FuncCallNode;
import wyvc.builder.ControlFlowGraph.WyilSection;
import wyvc.builder.compilationSteps.CompileFunctionsStep.CompiledFunctions;
import wyvc.builder.compilationSteps.CompileTypesStep.CompiledTypes;
import wyvc.builder.compilationSteps.RecursionAnalysisStep.OrderedFunction;
import wyvc.utils.Pair;

public class InliningStep extends CompilationStep<OrderedFunction, InliningStep.SplittedFunctions> {
	public static class SplittedFunctions extends CompiledTypes {
		public final List<Pair<String, List<WyilSection>>> func;

		public SplittedFunctions(CompiledTypes cmp, List<Pair<String, List<WyilSection>>> func) {
			super(cmp);
			this.func = func;
		}
		public SplittedFunctions(SplittedFunctions other) {
			super(other);
			func = other.func;
		}
	}


	@Override
	protected SplittedFunctions compile(CompilerLogger logger, OrderedFunction data) {
		/*UnaryOperator<WyilSection> inlining = (WyilSection s) -> {
			for (FuncCallNode c : s.invokes)
				c.inline(data.func.get(c.funcName));
			return s;
		};*/

		List<Pair<String, List<WyilSection>>> func = new ArrayList<>();
		for (Pair<String, WyilSection> p : data.func) {
			GraphPrinter.print(logger, p.second.inputs, p.second.outputs, p.first);
			//WyilSection se = inlining.apply(s);
			//GraphPrinter.print(logger, se.inputs, se.outputs, n);
			func.add(new Pair<String, List<WyilSection>>(p.first,  split(p.second)));
		};
		return new SplittedFunctions(data, func);
	}


	private List<WyilSection> split(WyilSection s) {
		return Collections.singletonList(s);
	}
}
