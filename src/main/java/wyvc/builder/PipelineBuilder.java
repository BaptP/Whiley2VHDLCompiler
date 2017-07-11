package wyvc.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.crypto.Data;

import wyal.lang.WyalFile.Type.Int;
import wyvc.builder.CompilerLogger.CompilerError;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.builder.CompilerLogger.LoggedBuilder;
import wyvc.builder.DataFlowGraph.BackRegister;
import wyvc.builder.DataFlowGraph.BinOpNode;
import wyvc.builder.DataFlowGraph.BinaryOperation;
import wyvc.builder.DataFlowGraph.ConstNode;
import wyvc.builder.DataFlowGraph.Counter;
import wyvc.builder.DataFlowGraph.DataArrow;
import wyvc.builder.DataFlowGraph.DataNode;
import wyvc.builder.DataFlowGraph.EndIfNode;
import wyvc.builder.DataFlowGraph.HalfArrow;
import wyvc.builder.DataFlowGraph.InputNode;
import wyvc.builder.DataFlowGraph.Latency;
import wyvc.builder.DataFlowGraph.Register;
import wyvc.builder.DataFlowGraph.UnaOpNode;
import wyvc.builder.DataFlowGraph.WhileNode;
import wyvc.builder.DataFlowGraph.WhileResultNode;
import wyvc.lang.Type;
import wyvc.utils.GList;
import wyvc.utils.GPairList;
import wyvc.utils.Generators;
import wyvc.utils.Generators.Generator;
import wyvc.utils.Generators.Generator_;
import wyvc.utils.Generators.PairGenerator;
import wyvc.utils.Generators.PairGenerator_;
import wyvc.utils.Pair;

public class PipelineBuilder extends LoggedBuilder {

	public PipelineBuilder(CompilerLogger logger) {
		super(logger);
	}


	public static class UnsupportedDelayCompilerError extends CompilerError {
		private final Delay delay;

		public UnsupportedDelayCompilerError(Delay delay) {
			this.delay = delay;
		}

		@Override
		public String info() {
			return "The delay "+delay+" is not supported";
		}

		public static CompilerException exception(Delay delay) {
			return new CompilerException(new UnsupportedDelayCompilerError(delay));
		}
	}

	public static class UnsupportedNodeCompilerError extends CompilerError {
		private final DataNode node;

		public UnsupportedNodeCompilerError(DataNode node) {
			this.node= node;
		}

		@Override
		public String info() {
			return "The time analysis for node "+node+" is not supported";
		}

		public static CompilerException exception(DataNode node) {
			return new CompilerException(new UnsupportedNodeCompilerError(node));
		}
	}

	public static class ImpossibleSynchronizationCompilerError extends CompilerError {
		private final Builder.NodeTimeline from;
		private final Builder.NodeTimeline to;

		public ImpossibleSynchronizationCompilerError(Builder.NodeTimeline from, Builder.NodeTimeline to) {
			this.from= from;
			this.to= to;
		}

		@Override
		public String info() {
			return "The synchronization from delay "+from+" to delay "+to+" is impossible";
		}

		public static CompilerException exception(Builder.NodeTimeline from, Builder.NodeTimeline to) {
			return new CompilerException(new ImpossibleSynchronizationCompilerError(from, to));
		}
	}



	public static class EmptySourceCompilerError extends CompilerError {

		@Override
		public String info() {
			return "Done signal from timeline with no source should not be asked";
		}

		public static CompilerException exception() {
			return new CompilerException(new EmptySourceCompilerError());
		}
	}



	public static class UnknownTimelineCompilerError extends CompilerError {
		private final DataNode node;

		public UnknownTimelineCompilerError(DataNode node) {
			this.node = node;
		}

		@Override
		public String info() {
			return "The time analysis for node <"+node+"> should have been made previously";
		}

		public static CompilerException exception(DataNode node) {
			return new CompilerException(new UnknownTimelineCompilerError(node));
		}
	}




	private static <T> Set<T> union(Set<T> s1, Set<T> s2) {
		Set<T> r = new HashSet<>();
		r.addAll(s1);
		r.addAll(s2);
		return r;
	}






	public static abstract class Delay {
		public final int latency;

		public Delay(int latency) {
			this.latency = latency;
		}
	}

	public static class InvalidDelay extends Delay {
		public InvalidDelay() {
			super(-1);
		}

		@Override
		public String toString() {
			return "><";
		}

	}

	public static class KnownDelay extends Delay {
		public KnownDelay(int delay) throws CompilerException {
			super(delay);
		}

		@Override
		public String toString() {
			return "="+latency;
		}
	}


	public static class UnknownDelay extends Delay {
		public UnknownDelay(int latencyMin) {
			super(latencyMin);
		}

		@Override
		public String toString() {
			return "?>"+latency;
		}
	}




		private static Delay concat(Delay d1, Delay d2) throws CompilerException {
			if (d1 instanceof InvalidDelay || d2 instanceof InvalidDelay)
				return new InvalidDelay();
			if (d1 instanceof KnownDelay && d2 instanceof KnownDelay)
				return new KnownDelay(d1.latency + d2.latency);
			if (d1 instanceof UnknownDelay || d2 instanceof UnknownDelay)
				return new UnknownDelay(d1.latency + d2.latency);
			throw UnsupportedDelayCompilerError.exception(d1);
		}
		private static Delay merge(Delay d1, Delay d2) throws CompilerException {
			if (d1 instanceof InvalidDelay)
				return d2;
			if (d2 instanceof InvalidDelay)
				return d1;
			if (d1 instanceof KnownDelay && d2 instanceof KnownDelay)
				return new KnownDelay(Math.max(d1.latency, d2.latency));
			if (d1 instanceof UnknownDelay || d2 instanceof UnknownDelay)
				return new UnknownDelay(Math.max(d1.latency, d2.latency));
			throw UnsupportedDelayCompilerError.exception(d1);
		}


	private static int CONCURRENCY = 4;











	private class Builder extends LoggedBuilder {


		abstract class CalculationSource {
			public final NodeTimeline timeline;
			public final Delay delay;

			public CalculationSource(NodeTimeline timeline, Delay delay) {
				this.timeline = timeline;
				this.delay = delay;
			}

			public Delay getDelay(CalculationSource source) throws CompilerException {
				return source == this ? new KnownDelay(0) : concat(timeline.getDelay(source), delay);
			}

			public abstract HalfArrow getStart() throws CompilerException;
		}

		class InnerEmptySource extends CalculationSource {
			private final HalfArrow start;

			public InnerEmptySource(DataNode start) throws CompilerException {
				super(new EmptyTimeline(), new KnownDelay(0));
				this.start = new HalfArrow(start, "start");
			}

			@Override
			public HalfArrow getStart() throws CompilerException {
				return start;
			}
		}

		class InnerSource extends CalculationSource {
			private final HalfArrow start;

			public InnerSource(NodeTimeline sources, DataNode done, UnknownDelay delay) throws CompilerException {
				super(sources, delay);
				this.start = new HalfArrow(done);
			}

			@Override
			public HalfArrow getStart() throws CompilerException {
				return start;
			}
		}

		class InputSource extends CalculationSource {
			public InputSource() throws CompilerException {
				super(new EmptyTimeline(), new KnownDelay(0));
			}

			@Override
			public HalfArrow getStart() throws CompilerException {
				return Builder.this.getStart();
			}
		}




		abstract class NodeTimeline {
			protected final Map<CalculationSource, KnownDelay> sources = new HashMap<>();
			private HalfArrow done = null;


			public NodeTimeline(PairGenerator<CalculationSource, KnownDelay> sources) throws CompilerException {
				sources.forEach(this.sources::put);
			}
			public NodeTimeline(PairGenerator_<CalculationSource, KnownDelay, CompilerException> sources) throws CompilerException {
				sources.forEach(this.sources::put);
			}

			public Delay getDelay(CalculationSource source) throws CompilerException {
				return Generators.fromMap(sources).mapFirst_(s -> s.getDelay(source)).
						map(PipelineBuilder::concat).fold(PipelineBuilder::merge, new InvalidDelay());
			}

			public PairGenerator<CalculationSource, KnownDelay> getSources() {
				return Generators.fromMap(sources);
			}

			public HalfArrow getDone() throws CompilerException {
				if (done == null)
					done = new HalfArrow(computeDone());
				return done;
			}

			protected abstract DataNode computeDone() throws CompilerException;

			@Override
			public String toString() {
				return toString("");
//				return sources.isEmpty() ? "{}" : "{"+Generators.fromMap(sources).mapFirst(s -> s.timeline).map(Object::toString, Object::toString).
//						map(", "::concat, ":"::concat).map(String::concat).fold(String::concat, "").substring(2)+"}";
			}
			public String toString(String sep) {
				return getClass().getSimpleName() + (sources.isEmpty() ? "{}" : "{"+Generators.fromMap(sources).map(s -> s.getClass().getSimpleName()+":"+s.timeline.toString(sep+"  "), Object::toString).
						map(("\n  "+sep)::concat, ":"::concat).map(String::concat).fold(String::concat, "")+"\n"+sep+"}");
			}
			public NodeTimeline delay(int delay) throws CompilerException {
				NodeTimeline This = this;
				return new NodeTimeline(Generators.fromMap(sources).mapSecond_(d -> new KnownDelay(d.latency + delay))) {

					@Override
					protected DataNode computeDone() throws CompilerException {
						return newGraph.new Register(This.getDone(), delay);
					}
				};
			}
		}

		private class EmptyTimeline extends NodeTimeline {
			public EmptyTimeline() throws CompilerException {
				super(Generators.emptyPairGenerator());
			}

			@Override
			protected DataNode computeDone() throws CompilerException {
				throw EmptySourceCompilerError.exception();
			}

		}
		private class InputTimeline extends NodeTimeline {
			private final CalculationSource source;

			public InputTimeline(CalculationSource source) throws CompilerException {
				super(Generators.fromPairSingleton(source, new KnownDelay(0)));
				this.source = source;
			}

			@Override
			protected DataNode computeDone() throws CompilerException {
				return source.getStart().node;
			}

		}







		private final Map<DataNode, NodeTimeline> nodeDelays = new HashMap<>();
		private final Map<DataNode, DataNode> convertedNodes = new HashMap<>();
		private final DataFlowGraph newGraph = new DataFlowGraph();
		private final InputSource input = new InputSource();
		private HalfArrow start = null;
		private HalfArrow clock = null;


		public Builder(CompilerLogger logger, DataFlowGraph graph) throws CompilerException {
			super(logger);
			buildPipeline(graph);
		}


		private DataNode getConvertedNode(DataNode node) throws CompilerException {
			if (!convertedNodes.containsKey(node))
				buildPipeline(node);
			return convertedNodes.get(node);
		}

		private <T extends DataNode> T setNodeDelay(DataNode previous, T node, NodeTimeline delay) {
			debugLevel("SET : " +node.getClass().getSimpleName() +" -> "+ node.nodeIdent + " " + delay.toString(""));
			convertedNodes.put(previous, node);
			nodeDelays.put(node, delay);
			return node;
		}

		private HalfArrow getConvertedHalfArrow(DataArrow a) throws CompilerException {
			return new HalfArrow(getConvertedNode(a.from), a.getIdent());
		}


		private NodeTimeline getTimeline(DataNode node) throws CompilerException {
			if (! nodeDelays.containsKey(node) || node.graph != newGraph)
				throw UnknownTimelineCompilerError.exception(node);
			return nodeDelays.get(node);
		}
		private NodeTimeline getTimeline(DataArrow source) throws CompilerException {
			return getTimeline(getConvertedNode(source.from));
		}


		private HalfArrow getClock() throws CompilerException {
			if (clock == null)
				clock = new HalfArrow(newGraph.new InputNode("clock", Type.Boolean), "clock");
			return clock;
		}

		private HalfArrow getStart() throws CompilerException {
			if (start == null)
				start = new HalfArrow(newGraph.new InputNode("start", Type.Boolean), "start");
			return start;
		}





		private HalfArrow delayNode(HalfArrow node, int delay) throws CompilerException {
			return delay == 0 ? node : new HalfArrow(newGraph.new Register(node, delay));
		}

		private class Synchronizer {
			private final Set<CalculationSource> nodeSources = new HashSet<>();
			private final GPairList<CalculationSource, KnownDelay> newSources = new GPairList.GPairArrayList<>();
			private final GPairList<CalculationSource, Integer> toSync = new GPairList.GPairArrayList<>();
			private final NodeTimeline timeline;

			public Synchronizer(GList<NodeTimeline> timelines) throws CompilerException {
				openLevel("Args");
				timelines.generate().map(NodeTimeline::toString).forEach(Builder.this::debugLevel);
				closeLevel();

				timelines.forEach(t -> nodeSources.addAll(t.sources.keySet()));

				openLevel("Sources");
				Generators.fromCollection(nodeSources).map(s -> s.timeline.toString()).forEach(Builder.this::debugLevel);
				closeLevel();

				for (CalculationSource s : nodeSources) {
					int latency = 0;
					int minLatency = -1;
					for (NodeTimeline t : timelines) {
						Delay sDelay = t.getDelay(s);
						if (sDelay instanceof KnownDelay)
							latency = Math.max(latency, sDelay.latency);
						else if (sDelay instanceof UnknownDelay)
							minLatency = Math.max(minLatency, sDelay.latency);
					}
					if (minLatency < latency) {
						newSources.add(s, new KnownDelay(latency));
/**/						debugLevel("Needed "+s.getClass().getSimpleName()+":"+s.timeline+", "+latency+", "+minLatency);
						if (minLatency != -1)
							toSync.add(s, latency);
					}
				}

				debugLevel(toSync.size() +" to sync");
				timeline = new NodeTimeline(newSources.generate()) {
					private DataNode buildSynchronization(GPairList<CalculationSource, Integer> opts, int s, int e) throws CompilerException {
						return e-s == 1
								? delayNode(opts.get(s).first.getStart(), opts.get(s).second).node
								: newGraph.new BinOpNode(BinaryOperation.And, Type.Boolean,
										new HalfArrow(buildSynchronization(opts, s, (s+e)/2)),
										new HalfArrow(buildSynchronization(opts, (s+e)/2, e)));
					}

					@Override
					protected DataNode computeDone() throws CompilerException {
						if (sources.size() == 1) {
							Entry<CalculationSource, KnownDelay> e = sources.entrySet().iterator().next();
							return delayNode(e.getKey().getStart(), e.getValue().latency).node;
						}
						if (toSync.isEmpty())
							throw EmptySourceCompilerError.exception();
						return buildSynchronization(toSync, 0, toSync.size());
					}};

			}

			private HalfArrow backDone = null;
			private HalfArrow getDoneRegister() throws CompilerException {
				if (backDone == null) {
					BackRegister reg = newGraph.new BackRegister(Type.Boolean);
					newGraph.new BackRegisterEnd(timeline.getDone(), reg);
					backDone = new HalfArrow(reg);
				}
				return backDone;
			}

			private DataNode synchronizeNode(DataNode node) throws CompilerException {
				openLevel("Sync node "+node.nodeIdent);
				NodeTimeline nt = getTimeline(node);
				if (nt.sources.size() == 0)
					return end(node);
				int diff = -1;
				boolean sync = false;
				for (CalculationSource s : timeline.sources.keySet()) {
					if (nt.sources.containsKey(s)) {
						int d = timeline.sources.get(s).latency - nt.sources.get(s).latency;
						if (d == diff || diff == -1)
							diff = d;
						else
							throw ImpossibleSynchronizationCompilerError.exception(nt, timeline);
					}
					else
						sync = true;
				}
				return end(sync ? newGraph.new Buffer(nt.getDone(), getDoneRegister(), new HalfArrow(node), CONCURRENCY)
						: diff == 0 ? node : newGraph.new Register(new HalfArrow(node), diff));
			}


			private HalfArrow synchronizeSource(DataArrow source) throws CompilerException {
				return new HalfArrow(synchronizeNode(getConvertedNode(source.from)), source.getIdent());
			}
		}






		private Synchronizer mergeSources(Generator<DataArrow> sources) throws CompilerException {
			return new Synchronizer(sources.map_(this::getTimeline).toList());
		}









		private HalfArrow synchronizeOuputput(DataNode node, NodeTimeline delay, InnerEmptySource source) throws CompilerException {
			Delay d = delay.getDelay(source);
			if (d instanceof KnownDelay)
				return delayNode(new HalfArrow(node), d.latency);
			return new HalfArrow(newGraph.new Buffer(source.getStart(), delay.getDone(), new HalfArrow(node), 1));
		}

		private void buildWhilePipeline(WhileNode node) throws CompilerException {

//			openLevel("WHILE");
			Synchronizer startDelay = mergeSources(Generators.fromCollection(node.sources));
//			debugLevel(" Début "+startDelay.timeline.toString(""));
			BackRegister rStart = newGraph.new BackRegister(Type.Boolean);
			InnerEmptySource source = new InnerEmptySource(newGraph.new BinOpNode(BinaryOperation.Or, Type.Boolean,
					new HalfArrow(rStart, "next_start"),
					startDelay.timeline.getDone()));
			InputTimeline whileSourceTimeLine = new InputTimeline(source);

			Map<DataNode, BackRegister> registers = new HashMap<>();
			node.getSources().compute(DataNode::getType).mapSecond_(t -> newGraph.new BackRegister(t)).forEach(registers::put);
			Map<DataNode, EndIfNode> entries = new HashMap<>();
			Generators.fromCollection(node.sources).biMap_(a -> a.from, a -> newGraph.new EndIfNode(getStart(),
					startDelay.synchronizeSource(a), new HalfArrow(registers.get(a.from), a.getIdent()+"_reg"))).forEach(entries::put); // TODO name
			node.bInputs.getValues().mapFirst(entries::get).forEach((e,i) -> setNodeDelay(i, e, whileSourceTimeLine));
			node.cInputs.getValues().mapFirst(entries::get).forEach((e,i) -> setNodeDelay(i, e, whileSourceTimeLine));


			DataNode condition = buildPipeline(node.conditionValue.source.from);

			NodeTimeline conditionDelay = getTimeline(condition);

			HalfArrow conditionReady = conditionDelay.getDone(); // TODO use proper ready signal
//
			Synchronizer bodyDelay = mergeSources(node.bOutputs.getValues().takeFirst().map(o -> o.source));


			Delay condD = conditionDelay.getDelay(source);

			boolean bodyToSync = false;
			HalfArrow nextStepReady;
			if (condD instanceof KnownDelay)
				nextStepReady = bodyDelay.timeline.getDone();
			else if (condD instanceof UnknownDelay) {
				bodyToSync = true;
				nextStepReady = null; // TODO
			}
			else
				throw UnsupportedDelayCompilerError.exception(condD);

			newGraph.new BackRegisterEnd(nextStepReady , rStart);
//
//
//			HalfArrow conditionDone = conditionDelay instanceof UnknownDelayFlags
//					? ((UnknownDelayFlags) conditionDelay).outputReady
//					: delayHalfArrow(new HalfArrow(condition), conditionDelay.delay.latency);
//
//			node.bOutputs.getValues().duplicateFirst().map_(o -> o.nodeIdent, o -> getConvertedNode(o.source.from), WhileResultNode::getPreviousValue).
//			mapThird(registers::get).map21(HalfArrow::new).forEach((h,r) -> newGraph.new BackRegisterEnd(h, r));
//


			NodeTimeline finalDelay = new InputTimeline(
					new InnerSource(startDelay.timeline,
							newGraph.new BinOpNode(BinaryOperation.And, Type.Boolean, conditionDelay.getDone(), new HalfArrow(condition)),
							new UnknownDelay(conditionDelay.getDelay(source).latency)));
			if (bodyToSync)
				node.getSources().<DataNode, CompilerException>compute_(n -> node.modification.containsKey(n)
						? getConvertedHalfArrow(node.modification.get(n).source).node
						: entries.get(n)).map(registers::get, bodyDelay::synchronizeNode);
			else
				node.getSources().<DataNode, CompilerException>compute_(n -> node.modification.containsKey(n)
						? getConvertedHalfArrow(node.modification.get(n).source).node
						: entries.get(n)).map(registers::get, bodyDelay::synchronizeNode).forEach((r,h) -> newGraph.new BackRegisterEnd(new HalfArrow(h), r));;

//			DelayFlags<?> finalDelay = new UnknownDelayFlags(new UnknownDelay(conditionDelay.delay.latency),
//					newGraph.new BinOpNode(BinaryOperation.And, Type.Boolean, conditionDone, new HalfArrow(condition)));
			node.bOutputs.getValues().takeSecond().compute(WhileResultNode::getPreviousValue).
			mapSecond(entries::get).mapSecond_(n -> synchronizeOuputput(n, conditionDelay, source)).
			forEach((r,h) -> setNodeDelay(r, h.node, finalDelay));
//			setStart(savStart);

//			closeLevel();
		}





		private DataNode buildPipeline(WhileResultNode node) throws CompilerException {
			buildWhilePipeline(node.whileNode);
			return getConvertedNode(node);
		}


		private BinOpNode buildPipeline(BinOpNode node) throws CompilerException {
			Synchronizer delay = mergeSources(Generators.fromValues(node.op1, node.op2));
			return setNodeDelay(node, newGraph.new BinOpNode(node.kind, node.type,
					delay.synchronizeSource(node.op1), delay.synchronizeSource(node.op2), node.location), delay.timeline);
		}
		private UnaOpNode buildPipeline(UnaOpNode node) throws CompilerException {
			NodeTimeline delay = getTimeline(node.op);
			return setNodeDelay(node, newGraph.new UnaOpNode(node.kind, node.type,
					getConvertedHalfArrow(node.op), node.location), delay);
		}
		private EndIfNode buildPipeline(EndIfNode node) throws CompilerException {
			Synchronizer delay = mergeSources(Generators.fromValues(node.condition, node.trueNode, node.falseNode));
			return setNodeDelay(node, newGraph.new EndIfNode(delay.synchronizeSource(node.condition),
					delay.synchronizeSource(node.trueNode), delay.synchronizeSource(node.falseNode), node.location), delay.timeline);
		}
		private Register buildPipeline(Register node) throws CompilerException {
			NodeTimeline delay = getTimeline(node.previousValue).delay(node.delay);
			return setNodeDelay(node, newGraph.new Register(getConvertedHalfArrow(node.previousValue), node.delay), delay);
		}
		private InputNode buildPipeline(InputNode node) throws CompilerException {
			return setNodeDelay(node, newGraph.new InputNode(node.nodeIdent, node.type), new InputTimeline(input));
		}
		private ConstNode buildPipeline(ConstNode node) throws CompilerException {
			return setNodeDelay(node, newGraph.new ConstNode(node.nodeIdent, node.type, node.location), new EmptyTimeline());
		}

		private DataNode buildPipeline2(DataNode node) throws CompilerException {
			if (node instanceof WhileResultNode)
				return buildPipeline((WhileResultNode) node);
			if (node instanceof BinOpNode)
				return buildPipeline((BinOpNode) node);
			if (node instanceof UnaOpNode)
				return buildPipeline((UnaOpNode) node);
			if (node instanceof EndIfNode)
				return buildPipeline((EndIfNode) node);
			if (node instanceof Register)
				return buildPipeline((Register) node);
			if (node instanceof InputNode)
				return buildPipeline((InputNode) node);
			if (node instanceof ConstNode)
				return buildPipeline((ConstNode) node);

			throw UnsupportedNodeCompilerError.exception(node);
		}
		private DataNode buildPipeline(DataNode node) throws CompilerException {
			openLevel("Build Pipeline "+node.nodeIdent);
			return end(buildPipeline2(node));
		}


		private NodeTimeline buildPipeline(DataFlowGraph graph) throws CompilerException {
			Synchronizer delay = mergeSources(graph.getOutputNodes().map(o -> o.source));
			graph.getOutputNodes().forEach_(o -> newGraph.new OutputNode(o.nodeIdent, delay.synchronizeSource(o.source)));
			newGraph.new OutputNode("done", delay.timeline.getDone());
			return delay.timeline;
		}

	}


	public DataFlowGraph buildPipeline(DataFlowGraph graph) throws CompilerException {
		return new Builder(logger, graph).newGraph;
	}

}
