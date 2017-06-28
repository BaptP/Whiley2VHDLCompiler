package wyvc.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import wyvc.utils.Generators.Generator;

public abstract class OrientedGraph<N extends OrientedGraph.Node<N,A>, A extends OrientedGraph.Arrow<A,N>> {
	public abstract static class Node<N extends Node<N,A>, A extends Arrow<A,N>> {
		public final List<A> sources = new ArrayList<>();
		public final List<A> targets = new ArrayList<>();
		public final OrientedGraph<N,A> graph;

		@SuppressWarnings("unchecked")
		public Node(OrientedGraph<N,A> graph) {
			this.graph = graph;
			graph.addNode((N)this);
		}

		protected void addSource(A source) {
			sources.add(source);
		}

		protected void addTarget(A target) {
			targets.add(target);
		}

		public Generator<N> getSources() {
			return Generators.fromCollection(sources).map((A a) -> a.from);
		}

		public Generator<N> getTargets() {
			return Generators.fromCollection(targets).map((A a) -> a.to);
		}

		protected void removed() {}
	}

	public abstract static class Arrow<A extends Arrow<A,N>, N extends Node<N,A>> {
		public final N from;
		public final N to;
		public OrientedGraph<N,A> graph = null;

		@SuppressWarnings("unchecked")
		public Arrow(OrientedGraph<N,A> graph, N from, N to) {
			this.from = from;
			this.to = to;
			this.graph = graph;
			graph.addArrow((A)this);
			from.addTarget((A) this);
			to.addSource((A) this);
		}

		protected void removed() {}
	}



	public final Set<N> nodes = new HashSet<>();
	public final Set<A> arrows = new HashSet<>();


	public final void addNode(N node) {
		nodes.add(node);
		nodeAdded(node);
	}

	public final void addArrow(A arrow) {
		arrows.add(arrow);
		arrowAdded(arrow);
	}

	public void nodeAdded(N node){}
	public void arrowAdded(A arrow){}

	public void removeNode(N node) {
		if (nodes.contains(node)) {
			nodes.remove(node);
			node.removed();
			for (A a : new ArrayList<>(node.targets))
				removeArrow(a);
			for (A a : new ArrayList<>(node.sources))
				removeArrow(a);
		}
	}

	public void removeArrow(A arrow) {
		if (arrows.contains(arrow)) {
			arrows.remove(arrow);
			arrow.removed();
			arrow.from.targets.remove(arrow);
			arrow.to.sources.remove(arrow);
		}
	}

}
