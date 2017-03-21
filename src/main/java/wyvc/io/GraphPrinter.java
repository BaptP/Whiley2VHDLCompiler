package wyvc.io;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import wyvc.builder.ControlFlowGraph.GraphNode;

public class GraphPrinter {

	private static class Node {
		public final GraphNode<?> data;
		public float yPos;
		public int ident;

		public Node(GraphNode<?> data, HashMap<GraphNode<?>, Node> nodes) {
			this.data = data;
			yPos = 0;
			nodes.put(data, this);
			for (GraphNode<?> d : data.sources) {
				if (!nodes.containsKey(d))
					nodes.put(d, new Node(d, nodes));
				yPos = Math.max(yPos, nodes.get(d).yPos+1);
			}
			for (GraphNode<?> d : data.getTargets())
				if (!nodes.containsKey(d))
					nodes.put(d, new Node(d, nodes));
		}
	}

	public static void print(List<? extends GraphNode<?>> roots, List<? extends GraphNode<?>> leaves, String name) {
		HashMap<GraphNode<?>, Node> nodes = new HashMap<>();
		ArrayList<Node> rootNodes = new ArrayList<>();
//		int lr = 1;
		for (GraphNode<?> data : roots) {
			if (!nodes.containsKey(data))
				nodes.put(data, new Node(data, nodes));
			Node n = nodes.get(data);
//			lr += Math.max(2*Math.max(data.getTargets().size(), data.sources.size())-1, n.data.label.length()) + 3;
			rootNodes.add(n);
		}
//		int ll = 1;
//		float yMax = 0;
		ArrayList<Node> leafNodes = new ArrayList<>();
		for (GraphNode<?> data : leaves) {
			if (!nodes.containsKey(data))
				nodes.put(data, new Node(data, nodes));
			Node n = nodes.get(data);
//			ll += Math.max(2*Math.max(data.getTargets().size(), data.sources.size())-1, n.data.label.length()) + 3;
//			yMax = Math.max(yMax, n.yPos);
			leafNodes.add(n);
		}
		try {
			FileWriter output = new FileWriter("gr.dot");
			output.write("digraph {\n");
			output.write("  inp [label=\"inputs\",color=blue,shape=box];\n");
			int i = 0;
			for (Node n : nodes.values()) {
				n.ident = ++i;
				output.write("  n"+i+" [label=\""+n.data.label+"\"");
				for (String o : n.data.getOptions())
					output.write(","+o);
				output.write("];\n");
			}
			output.write("  out [label=\"outputs\",color=red,shape=box];\n\n");

			for (Node n : rootNodes)
				output.write("  inp -> n"+n.ident+" "+"[color=blue];\n");

			for (Node n : nodes.values())
				for (GraphNode<?> t : n.data.getTargets())
					output.write("  n"+n.ident+" -> n"+nodes.get(t).ident+";\n");

			for (Node n : leafNodes)
				output.write("  n"+n.ident+" -> out [color=red];\n");
			output.write("}\n");

			output.close();
			System.out.println("inputs : "+roots.size());
			System.out.println("outputs : "+leaves.size());
			Process t = Runtime.getRuntime().exec("graph-viewer gr.dot");
			Process u = Runtime.getRuntime().exec("dot -Tps gr.dot -o "+name+".ps");
			//*
			t.waitFor();
			u.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();//*/
		} catch (IOException e) {
			e.printStackTrace();
		}

		/*
//		for (Node node : leafNodes)
//			node.yPos = yMax;
		float dr = Math.max(0, (ll-lr)/(rootNodes.size()));
		int xr = 0;
		for (Node n : rootNodes) {
			xr += 2 + dr;
			n.xPos = xr + n.data.label.length()/2;
			xr += 1 + n.data.label.length();
		}
		float dl = Math.max(0, (lr-ll)/(leafNodes.size()));
		int xl = 0;
		for (Node n : rootNodes) {
			xl += 2 + dl;
			n.xPos = xl + n.data.label.length()/2;
			xl += 1 + n.data.label.length();
		}*/
	}
}
