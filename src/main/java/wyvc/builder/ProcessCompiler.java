package wyvc.builder;

import wyvc.builder.DataFlowGraph;
import wyvc.lang.Statement.Process;
import wyvc.lang.Statement.SequentialStatement;
import wyvc.lang.TypedValue.Signal;
import wyvc.lang.TypedValue.Variable;;

public class ProcessCompiler {


	static Process compileProcess(String ident, DataFlowGraph section) {
		return new Process(ident, new Variable[0], new Signal[0], new SequentialStatement[0]);
	}
}
