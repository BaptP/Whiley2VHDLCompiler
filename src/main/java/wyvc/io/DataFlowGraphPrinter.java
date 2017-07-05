package wyvc.io;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import wyvc.builder.DataFlowGraph;
import wyvc.builder.DataFlowGraph.DataArrow;
import wyvc.builder.DataFlowGraph.DataNode;
import wyvc.builder.DataFlowGraph.GraphBlock;
import wyvc.builder.DataFlowGraph.NodeBlock;
import wyvc.utils.Generators;
import wyvc.utils.Utils;

public class DataFlowGraphPrinter {

	private static class Printer {
		private final FileWriter output;
		public final Map<DataNode, Integer> nodes = new HashMap<>();
		private int nCluster = 0;
		private int i = 0;


		public void print(NodeBlock block, double h, double s, double v) throws IOException {
			output.write("subgraph clouster_"+nCluster+++"{\n");
			output.write("label = "+block.getClass().getSimpleName()+";\n");
			output.write("color=blue;\n");
			int k = 0;
			for (NodeBlock b : block.getNestedBlocks().toList())
//				print(b, h,s,v);
				print(b, h+k++/10.,s+0.05,v);
			//output.write("ranksep=0.02;\n");
			block.getNodes().forEach_(n -> {
				nodes.put(n,  ++i);
				output.write("  n"+i+" [label=\""+n.getSafeIdent()+"\"");
				for (String o : n.getOptions())
					if (o.startsWith("shape"))
						output.write(","+o);
				output.write(", style=filled, fillcolor=\""+h+" "+s+" "+v+"\"");
				output.write("];\n");
			});
			for (DataNode n : block.getNodes().toList())
				for (DataArrow a : n.sources)
					if (a.from.block == n.block)
						output.write("  n"+nodes.get(a.from)+" -> n"+nodes.get(a.to)+
		/**/				"["+String.join(",",Utils.concat(a.getOptions(),Collections.singletonList("label=\""+a.getSafeIdent()+"\"")))+"]"+
						";\n");

			output.write("}\n");
		}

		public Printer(FileWriter output) {
			this.output = output;
		}
	}


	public static void print(DataFlowGraph graph, String name) {
		try {
			FileWriter output = new FileWriter("gr.dot");
			output.write("digraph {\n");
			Printer p = new Printer(output);
			p.print(graph.topLevelBlock,0.,0.1,0.999);




			output.write("  inp [label=\"inputs\",color=blue,shape=box];\n");

			output.write("  out [label=\"outputs\",color=red,shape=box];\n\n");

			for (DataNode n : graph.getInputs())
				output.write("  inp -> n"+p.nodes.get(n)+" "+"[color=blue];\n");

			for (DataArrow a : graph.arrows)
				if (a.from.block != a.to.block)
					output.write("  n"+p.nodes.get(a.from)+" -> n"+p.nodes.get(a.to)+
	/**/				"["+String.join(",",Utils.concat(a.getOptions(),Collections.singletonList("label=\""+a.getSafeIdent()+"\"")))+"]"+
					";\n");

			for (DataNode n : graph.getOutputs())
				output.write("  n"+p.nodes.get(n)+" -> out [color=red];\n");
			output.write("}\n");

			output.close();
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
