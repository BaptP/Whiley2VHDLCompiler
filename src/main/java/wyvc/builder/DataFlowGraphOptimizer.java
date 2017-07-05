package wyvc.builder;

import java.util.ArrayList;
import java.util.List;

import wyvc.builder.CompilerLogger.LoggedBuilder;
import wyvc.builder.DataFlowGraph.DataArrow;
import wyvc.builder.DataFlowGraph.DataNode;
import wyvc.builder.DataFlowGraph.EndWhileNode;
import wyvc.builder.DataFlowGraph.OutputNode;
import wyvc.builder.DataFlowGraph.Register;

public class DataFlowGraphOptimizer extends LoggedBuilder {

	public DataFlowGraphOptimizer(CompilerLogger logger) {
		super(logger);
	}

	public static enum Optimization {
		UselessNodeRemoval,
		UndefinedMuxBranch,
		BooleanMux,
		StaticallyKnowValueResolution;
	}



	private void checkVertexUsefull(DataFlowGraph graph, DataNode vertex) {
		if (!(vertex instanceof EndWhileNode) && !(vertex instanceof OutputNode) && vertex.targets.size() == 0) {
			List<DataArrow> sources = new ArrayList<>(vertex.sources);
			graph.removeNode(vertex);
			for (DataArrow a : sources)
				checkVertexUsefull(graph, a.from);
		}
	}

	private void removeUselessVertexes(DataFlowGraph graph) {
		List<DataNode> vertexes = new ArrayList<>();
		vertexes.addAll(graph.nodes);
		for (DataNode n : vertexes)
			checkVertexUsefull(graph, n);
	}


	public DataFlowGraph optimize(DataFlowGraph graph) {
		removeUselessVertexes(graph);
		return graph;
	}
}
