package wyvc.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class GraphNode<T extends GraphNode<T>> {
	public final List<T> sources = new ArrayList<>();
	public final List<T> targets = new ArrayList<>();
	public final String label;

	@SuppressWarnings("unchecked")
	public GraphNode(String label, List<T> sources) {
//		for(T t: sources) System.out.println("Source "+t+(t == null ? "" : " : "+t.label));
		this.label = label;
		this.sources.addAll(sources);
		for (GraphNode<T> n : sources)
			n.addTarget((T)this);
	}

	private void addTarget(T target) {
		targets.add(target);
	}

	public List<T> getTargets() {
		return targets;
	}

	public List<String> getNodeOptions() {
		return Arrays.asList("shape=\"box\"","color=black");
	}


	public List<String> getArrowOptions(T other) {
		return Collections.emptyList();
	}
}
