package wyvc.io;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import wyvc.builder.CompilerLogger;
import wyvc.utils.OrientedGraph;
import wyvc.utils.Utils;

public class GraphPrinter {

	public static interface PrintableElement {
		public String getIdent();
		public List<String> getOptions();

		public default String getSafeIdent() {
			return getIdent().replaceAll("\"", "\\\\\"").replaceAll("\n", "\\\\n");
		}
	}

	public abstract static class PrintableGraph<N extends PrintableGraph.PrintableNode<N,A>, A extends PrintableGraph.PrintableArrow<A,N>> extends OrientedGraph<N,A> {
		public abstract static class PrintableNode<N extends PrintableNode<N,A>, A extends PrintableArrow<A,N>> extends Node<N,A> implements PrintableElement {

			public PrintableNode(OrientedGraph<N, A> graph) {
				super(graph);
			}

		}

		public abstract static class PrintableArrow<A extends PrintableArrow<A,N>, N extends PrintableNode<N,A>> extends Arrow<A,N> implements PrintableElement {
			public PrintableArrow(OrientedGraph<N,A> graph, N from, N to) {
				super(graph, from, to);
			}
		}

		public abstract List<N> getInputs();
		public abstract List<N> getOutputs();
	}




	public static <N extends PrintableGraph.PrintableNode<N,A>, A extends PrintableGraph.PrintableArrow<A,N>>  void print(CompilerLogger logger, PrintableGraph<N,A> graph, String name) {
		try {
			FileWriter output = new FileWriter("gr.dot");
			output.write("digraph {\n");
			output.write("  inp [label=\"inputs\",color=blue,shape=box];\n");
			Map<N, Integer> nodes = new HashMap<>();
			int i = 0;
			for (N n : graph.nodes) {
				nodes.put(n,  ++i);
				output.write("  n"+i+" [label=\""+n.getSafeIdent()+"\"");
				for (String o : n.getOptions())
					output.write(","+o);
				output.write("];\n");
			}
			output.write("  out [label=\"outputs\",color=red,shape=box];\n\n");

			for (N n : graph.getInputs())
				output.write("  inp -> n"+nodes.get(n)+" "+"[color=blue];\n");

			for (A a : graph.arrows)
				output.write("  n"+nodes.get(a.from)+" -> n"+nodes.get(a.to)+"["+String.join(",",Utils.concat(a.getOptions(),
					Collections.singletonList("label=\""+a.getSafeIdent()+"\"")))+"];\n");

			for (N n : graph.getOutputs())
				output.write("  n"+nodes.get(n)+" -> out [color=red];\n");
			output.write("}\n");

			output.close();
			logger.debug("inputs : "+graph.getInputs().size());
			logger.debug("outputs : "+graph.getOutputs().size());
			Process t = Runtime.getRuntime().exec("graph-viewer gr.dot");
			Process u = Runtime.getRuntime().exec("dot -Goverlap=scale -Tps gr.dot -o "+name+".ps");
			t.waitFor();
			u.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
