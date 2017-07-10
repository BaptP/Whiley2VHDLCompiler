package wyvc.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
		private final Delay from;
		private final Delay to;

		public ImpossibleSynchronizationCompilerError(Delay from, Delay to) {
			this.from= from;
			this.to= to;
		}

		@Override
		public String info() {
			return "The synchronization from delay "+from+" to delay "+to+" is impossible";
		}

		public static CompilerException exception(Delay from, Delay to) {
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

	public static class NullDelay extends Delay {

		public NullDelay() {
			super(0);
		}

		@Override
		public String toString() {
			return "=0";
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




		private static Delay concat(Delay d1, Delay d2) {
			return null; // TODO
		}
		private static Delay merge(Delay d1, Delay d2) {
			return null; // TODO
		}


	private static int CONCURRENCY = 4;
	private class Builder extends LoggedBuilder {


		class CalculationSource {
			public NodeTimeline timeLine;
			public Delay delay;

			public CalculationSource(NodeTimeline timeLine) {
				this.delay = delay;
			}

			public Delay getDelay(CalculationSource source) {
				return source == this ? delay : timeLine.getDelay(source);
			}

			public HalfArrow getStart() {
				return null; // TODO
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

			public Delay getDelay(CalculationSource source) {
				return Generators.fromMap(sources).mapFirst(s -> getDelay(s)).
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
		}

		private class EmptyTimeLine extends NodeTimeline {

			public EmptyTimeLine() throws CompilerException {
				super(Generators.emptyPairGenerator());
			}

			@Override
			protected DataNode computeDone() throws CompilerException {
				throw EmptySourceCompilerError.exception();
			}
			
		}


		private class Synchronyzer {
			private final Set<CalculationSource> nodeSources = new HashSet<>();
			private final GPairList<CalculationSource, KnownDelay> newSources = new GPairList.GPairArrayList<>();
			private final GPairList<CalculationSource, Integer> toSync = new GPairList.GPairArrayList<>();
			private final NodeTimeline timeline;
			
			public Synchronyzer(GList<NodeTimeline> timelines) throws CompilerException {
				timelines.forEach(t -> nodeSources.addAll(t.sources.keySet()));
				
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
					if (minLatency < latency)
						toSync.add(s, latency);
					if (minLatency == -1)
						newSources.add(s, new KnownDelay(latency));
				}
				
				timeline = new NodeTimeline(newSources.generate()) {
					private DataNode buildSynchronization(GPairList<CalculationSource, Integer> opts, int s, int e) throws CompilerException {
						return e-s == 1 
								? newGraph.new Register(opts.get(s).first.getStart(), opts.get(s).second)
								: newGraph.new BinOpNode(BinaryOperation.And, Type.Boolean, 
										new HalfArrow(buildSynchronization(opts, s, (s+e)/2)), 
										new HalfArrow(buildSynchronization(opts, (s+e)/2, e)));
					}
				
					@Override
					protected DataNode computeDone() throws CompilerException {
						return buildSynchronization(toSync, 0, toSync.size());
					}};
				
			}
			
			private DataNode synchronizeNode(DataNode node) {
				return null;
			}
		}



		private DataNode synchronizeNode(DataNode node, NodeTimeline timeline) throws CompilerException {
			NodeTimeline t = getTimeLine(node);
//			if (timeline.getSources().forAll(test))
			return null;
		}


		private NodeTimeline getTimeLine(DataNode node) throws CompilerException {
			// TODO check contains
			return nodeDelays.get(node);
		}
		private NodeTimeline getTimeLine(HalfArrow node) throws CompilerException {
			return nodeDelays.get(node.node);
		}
		private NodeTimeline getTimeLine(DataArrow source) throws CompilerException {
			return getTimeLine(getConvertedNode(source.from));
		}
		
		

		private NodeTimeline mergeNodes(Generator<DataNode> nodes) throws CompilerException {
			return synchronizeTimelines(nodes.map_(this::getTimeLine).toList());
		}
		private NodeTimeline mergeSources(Generator<DataArrow> sources) throws CompilerException {
			return mergeNodes(sources.map(a -> a.from));
		}
		
		
		
		
		
		
		private final Map<DataNode, NodeTimeline> nodeDelays = new HashMap<>();
		private final Map<DataNode, DataNode> convertedNodes = new HashMap<>();
		private final DataFlowGraph newGraph = new DataFlowGraph();
//		private final DataFlowGraph graph;

		
		private CalculationSource input = new CalculationSource(new EmptyTimeLine());
		
private InputNode clck = newGraph.new InputNode("clck", Type.Boolean);
		private HalfArrow getClock() throws CompilerException {
			return new HalfArrow(clck, "clck"); // TODO
		}

		private DataNode strt = newGraph.new InputNode("strt", Type.Boolean);
		private HalfArrow getStart() throws CompilerException {
			return new HalfArrow(strt, "strt"); // TODO
		}
		private DataNode getStartNode() throws CompilerException {
			return strt; // TODO
		}
		private void setStart(DataNode strt) {
			this.strt = strt;
		}

		public Builder(CompilerLogger logger, DataFlowGraph graph) throws CompilerException {
			super(logger);
			buildPipeline(graph);
		}


		private <T extends DataNode> T setNodeDelay(DataNode previous, T node, NodeTimeline delay) {
			convertedNodes.put(previous, node);
			nodeDelays.put(node, delay);
			return node;
		}


		private DataNode buildDone(GPairList<Counter, BackRegister> sync, int from, int to) throws CompilerException {
			return to - from == 1
					? sync.get(from).first.isNonZero
					: newGraph.new BinOpNode(BinaryOperation.And, Type.Boolean,
							new HalfArrow(buildDone(sync, from, (from + to)/2)),
							new HalfArrow(buildDone(sync, (from + to)/2, to)));
		}


		private HalfArrow delayHalfArrow(HalfArrow node, int delay) throws CompilerException {
			return delay == 0 ? node : new HalfArrow(newGraph.new Register(node, delay), node.ident);

		}

		private HalfArrow synchronizeNode(HalfArrow node, NodeTimeline delay) throws CompilerException {

			throw UnsupportedDelayCompilerError.exception(delay.delay);
		}

		private HalfArrow synchronizeSource(DataArrow source, NodeTimeline delay) throws CompilerException {
			return synchronizeNode(new HalfArrow(getConvertedNode(source.from), source.getIdent()), delay);
		}





		private DataNode getConvertedNode(DataNode node) throws CompilerException {
			if (!convertedNodes.containsKey(node))
				buildPipeline(node);
			return convertedNodes.get(node);
		}

		private HalfArrow getConvertedHalfArrow(DataArrow a) throws CompilerException {
			return new HalfArrow(getConvertedNode(a.from), a.getIdent());
		}



		private HalfArrow getReadyHalfArrow(DelayFlags<?> delay) throws CompilerException {
			if (delay instanceof UnknownDelayFlags)
				return ((UnknownDelayFlags) delay).outputReady;
			return delayHalfArrow(getStart(), delay.delay.latency);
		}



		private void buildWhilePipeline(WhileNode node) throws CompilerException {
//			DataNode start;
			NodeTimeline delay = mergeSources(Generators.fromCollection(node.sources));
			BackRegister rStart = newGraph.new BackRegister(Type.Boolean);
			DataNode savStart = getStartNode();
			setStart(newGraph.new BinOpNode(BinaryOperation.Or, Type.Boolean,
					new HalfArrow(rStart),
					delay instanceof UnknownDelayFlags ? ((UnknownDelayFlags) delay).outputReady: getStart()));

			Map<DataNode, BackRegister> registers = new HashMap<>();
			//Generators.fromCollection(node.sources).biMap(DataArrow::getIdent, a -> a.from).duplicateSecond().mapThird(DataNode::getType);
			node.getSources().compute(DataNode::getType).mapSecond_(t -> newGraph.new BackRegister(t)).forEach(registers::put);
			Map<DataNode, EndIfNode> entries = new HashMap<>();
			Generators.fromMap(registers).computeSecond_((n,r) -> newGraph.new EndIfNode(getStart(),
					new HalfArrow(getConvertedNode(n), node.bInputs.get(n).nodeIdent), new HalfArrow(r, node.bInputs.get(n).nodeIdent+"_reg"))).forEach(entries::put); // TODO name
			node.bInputs.getValues().mapFirst(entries::get).forEach((e,i) -> setNodeDelay(i, e, delay));
			node.cInputs.getValues().mapFirst(entries::get).forEach((e,i) -> setNodeDelay(i, e, delay));

			setStart(node);

			DataNode condition = buildPipeline(node.conditionValue.source.from);

			DelayFlags<?> conditionDelay = getNodeDelay(condition);

			DelayFlags<?> bodyDelay = new NullDelayFlags(); //TODO
			newGraph.new BackRegisterEnd(getReadyHalfArrow(bodyDelay), rStart);


			HalfArrow conditionDone = conditionDelay instanceof UnknownDelayFlags
					? ((UnknownDelayFlags) conditionDelay).outputReady
					: delayHalfArrow(new HalfArrow(condition), conditionDelay.delay.latency);

			node.bOutputs.getValues().duplicateFirst().map_(o -> o.nodeIdent, o -> getConvertedNode(o.source.from), WhileResultNode::getPreviousValue).
			mapThird(registers::get).map21(HalfArrow::new).forEach((h,r) -> newGraph.new BackRegisterEnd(h, r));

			DelayFlags<?> finalDelay = new UnknownDelayFlags(new UnknownDelay(conditionDelay.delay.latency),
					newGraph.new BinOpNode(BinaryOperation.And, Type.Boolean, conditionDone, new HalfArrow(condition)));
			node.bOutputs.getValues().takeSecond().compute(WhileResultNode::getPreviousValue).
			mapSecond(entries::get).mapSecond_(n -> synchronizeNode(new HalfArrow(n), conditionDelay)).forEach((r,h) -> setNodeDelay(r, h.node, finalDelay));
			setStart(savStart);
		}





		private DataNode buildPipeline(WhileResultNode node) throws CompilerException {
			buildWhilePipeline(node.whileNode);
			return getConvertedNode(node);
		}


		private BinOpNode buildPipeline(BinOpNode node) throws CompilerException {
			NodeTimeline delay = mergeSources(Generators.fromValues(node.op1, node.op2));
			return setNodeDelay(node, newGraph.new BinOpNode(node.kind, node.type,
					synchronizeSource(node.op1, delay), synchronizeSource(node.op2, delay), node.location), delay);
		}
		private UnaOpNode buildPipeline(UnaOpNode node) throws CompilerException {
			NodeTimeline delay = getSourceDelay(node.op);
			return setNodeDelay(node, newGraph.new UnaOpNode(node.kind, node.type,
					synchronizeSource(node.op, delay), node.location), delay);
		}
		private EndIfNode buildPipeline(EndIfNode node) throws CompilerException {
			NodeTimeline delay = mergeSources(Generators.fromValues(node.condition, node.trueNode, node.falseNode));
			return setNodeDelay(node, newGraph.new EndIfNode(synchronizeSource(node.condition, delay),
					synchronizeSource(node.trueNode, delay), synchronizeSource(node.falseNode, delay), node.location), delay);
		}
		private Register buildPipeline(Register node) throws CompilerException {
			NodeTimeline delay = delay(getSourceDelay(node.previousValue));
			return setNodeDelay(node, newGraph.new Register(getConvertedHalfArrow(node.previousValue), node.delay), delay);
		}
		private InputNode buildPipeline(InputNode node) throws CompilerException {
			return setNodeDelay(node, newGraph.new InputNode(node.nodeIdent, node.type), new EmptyTimeLine());
		}
		private ConstNode buildPipeline(ConstNode node) throws CompilerException {
			return setNodeDelay(node, newGraph.new ConstNode(node.nodeIdent, node.type, node.location), new EmptyTimeLine());
		}

		private DataNode buildPipeline(DataNode node) throws CompilerException {
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


		private NodeTimeline buildPipeline(DataFlowGraph graph) throws CompilerException {
			NodeTimeline delay = mergeSources(graph.getOutputNodes().map(o -> o.source));
			graph.getOutputNodes().forEach_(o -> newGraph.new OutputNode(o.nodeIdent, synchronizeSource(o.source, delay)));
			return delay;
		}

	}


	public DataFlowGraph buildPipeline(DataFlowGraph graph) throws CompilerException {
		return new Builder(logger, graph).newGraph;
	}

/*
 *
		private abstract class Synchronizer implements BiFunction_<HalfArrow, HalfArrow, Pair<HalfArrow, HalfArrow>, CompilerException> {
			public final Delay delay;

			public Synchronizer(Delay delay) {
				this.delay = delay;
			}

			@Override
			public final Pair<HalfArrow, HalfArrow> apply(HalfArrow left, HalfArrow right) throws CompilerException {
				return new Pair<>(synchonizeLeftSide(left), synchonizeRightSide(right));
			}

			public abstract HalfArrow synchonizeLeftSide(HalfArrow node) throws CompilerException;
			public abstract HalfArrow synchonizeRightSide(HalfArrow node) throws CompilerException;

			public Synchronizer reverse() {
				Synchronizer This = this;
				return new Synchronizer(delay) {
					@Override
					public HalfArrow synchonizeRightSide(HalfArrow node) throws CompilerException {
						return This.synchonizeLeftSide(node);
					}
					@Override
					public HalfArrow synchonizeLeftSide(HalfArrow node) throws CompilerException {
						return This.synchonizeRightSide(node);
					}};
			}
		}



		private Synchronizer buildSynchronization(NullDelay leftDelay, NullDelay rightDelay) {
			return new Synchronizer(leftDelay){
				@Override public HalfArrow synchonizeLeftSide(HalfArrow node) throws CompilerException {
					return node;
				}
				@Override public HalfArrow synchonizeRightSide(HalfArrow node) throws CompilerException {
					return node;
				}};
		}
		private Synchronizer buildSynchronization(KnownDelay leftDelay, NullDelay rightDelay) {
			return new Synchronizer(leftDelay){
				@Override public HalfArrow synchonizeLeftSide(HalfArrow node) throws CompilerException {
					return node;
				}
				@Override public HalfArrow synchonizeRightSide(HalfArrow node) throws CompilerException {
					return delay(node, leftDelay.delay);
				}};
		}
		private Synchronizer buildSynchronization(KnownDelay leftDelay, KnownDelay rightDelay) throws CompilerException {
			int newDelay = Math.max(leftDelay.delay, rightDelay.delay);
			return new Synchronizer(new KnownDelay(leftDelay.flags, newDelay)){
				@Override public HalfArrow synchonizeLeftSide(HalfArrow node) throws CompilerException {
					return delay(node, newDelay-leftDelay.delay);
				}
				@Override public HalfArrow synchonizeRightSide(HalfArrow node) throws CompilerException {
					return delay(node, newDelay-rightDelay.delay);
				}};
		}
		private Synchronizer buildSynchronization(UnknownDelay leftDelay, NullDelay rightDelay) {
			return null;
		}
		private Synchronizer buildSynchronization(UnknownDelay leftDelay, KnownDelay rightDelay) {
			return null;
		}
		private Synchronizer buildSynchronization(UnknownDelay leftDelay, UnknownDelay rightDelay) throws CompilerException {
			if (leftDelay.latency > rightDelay.latency)
				return buildSynchronization(rightDelay, leftDelay).reverse();
//			Buffer sync = graph.new Buffer(write, read, previousValue, size);
// TODO vérifier que les entrées sont les même
			UnknownDelay newDelay = new UnknownDelay(leftDelay.flags,
				graph.new BinOpNode(BinaryOperation.And, Type.Boolean, leftDelay.inputReady, rightDelay.inputReady),
				graph.new BinOpNode(BinaryOperation.And, Type.Boolean, leftDelay.outputReady, rightDelay.outputReady),
				rightDelay.latency);
			return new Synchronizer(newDelay){
				@Override public HalfArrow synchonizeLeftSide(HalfArrow node) throws CompilerException {
					return delay(node, rightDelay.latency-leftDelay.latency);
				}
				@Override public HalfArrow synchonizeRightSide(HalfArrow node) throws CompilerException {
					return node;
				}};
		}


		private Synchronizer buildSynchronization(Delay leftDelay, Delay rightDelay) throws CompilerException {
			if (leftDelay instanceof NullDelay){
				if (rightDelay instanceof NullDelay)
					return buildSynchronization((NullDelay) leftDelay, (NullDelay)rightDelay);
				return buildSynchronization(rightDelay,leftDelay).reverse();
			}
			if (leftDelay instanceof KnownDelay){
				if (rightDelay instanceof NullDelay)
					return buildSynchronization((KnownDelay) leftDelay, (NullDelay)rightDelay);
				if (rightDelay instanceof KnownDelay)
					return buildSynchronization((KnownDelay) leftDelay, (KnownDelay)rightDelay);
				return buildSynchronization(rightDelay,leftDelay).reverse();
			}
			if (leftDelay instanceof UnknownDelay){
				if (rightDelay instanceof NullDelay)
					return buildSynchronization((UnknownDelay) leftDelay, (NullDelay)rightDelay);
				if (rightDelay instanceof KnownDelay)
					return buildSynchronization((UnknownDelay) leftDelay, (KnownDelay)rightDelay);
				if (rightDelay instanceof UnknownDelay)
					return buildSynchronization((UnknownDelay) leftDelay, (UnknownDelay)rightDelay);
				return buildSynchronization(rightDelay,leftDelay).reverse();
			}
			throw UnsupportedDelayCompilerError.exception(leftDelay);
		}





//		private Delay getBlockDelay(NodeBlock block) throws CompilerException {
//			if (blockDelays.containsKey(block))
//				return blockDelays.get(block);
//			throw UnknownBlockDelayCompilerError.exception(block);
//		}

		private <A extends DataNode> A getConvertedNode(A node) throws CompilerException {
			if (nodeDelays.containsKey(node))
				return (A) nodeDelays.get(node);
			throw UnknownNodeDelayCompilerError.exception(node);
		}
		private HalfArrow getConvertedNode(HalfArrow node) throws CompilerException {
			return new HalfArrow(getConvertedNode(node.node), node.ident);
		}
		private HalfArrow getConvertedNode(DataArrow node) throws CompilerException {
			return new HalfArrow(getConvertedNode(node.from), node.getIdent());
		}





		private HalfArrow delay(HalfArrow node, int nCycle) throws CompilerException {
			return nCycle <= 0 ? node : new HalfArrow(graph.new Register(node, nCycle), node.ident);
		}


		private HalfArrow delay(HalfArrow node, UnknownDelay delay) throws CompilerException {
			return null;
		}






		private Delay buildPipeline(IfBlock block, DelayFlags flags) throws CompilerException {
			IfBlock newBlock = newGraph.openIfBlock();
			newBlock.openCondition();
			Delay conditionDelay = buildPipeline(block.condition, flags);
			newBlock.openTrueBranch();
			Delay trueBranchDelay = buildPipeline(block.trueBranch, flags);
			newBlock.openFalseBranch();
			Delay falseBranchDelay = buildPipeline(block.falseBranch, flags);
			newBlock.setCurrent();

			Synchronizer branchSynchronizer = buildSynchronization(trueBranchDelay, falseBranchDelay);
			Synchronizer synchronizer = buildSynchronization(conditionDelay, branchSynchronizer.delay);

			HalfArrow newCondition = synchronizer.synchonizeLeftSide(getConvertedNode(block.conditionNode));
			for (EndIfNode n : block.endIfNode)
				graph.new EndIfNode(
					newCondition,
					synchronizer.synchonizeRightSide(branchSynchronizer.synchonizeLeftSide(getConvertedNode(n.trueNode))),
					synchronizer.synchonizeRightSide(branchSynchronizer.synchonizeRightSide(getConvertedNode(n.falseNode))),
					n.location);
			newBlock.close();
			return synchronizer.delay;
		}


		private Delay buildPipeline(WhileBlock block, UnknownDelayFlags flags) throws CompilerException {
			WhileBlock newBlock = newGraph.openWhileBlock();
			Map<WhileNode, EndIfNode> registers = new HashMap<>();
			block.getWhileNodes().compute(DataNode::getType).mapSecond_(t -> graph.new BackRegister(t)).duplicateFirst().mapSecond(w -> w.value).map23(
				(v,r) -> graph.new EndIfNode(
					flags.start,
					new HalfArrow(v.from, v.getIdent()),
					new HalfArrow(r, "reg_"+v.getIdent()))).forEach(registers::put);
			BinOpNode startIteration = graph.new BinOpNode(
				BinaryOperation.Or, Type.Boolean, flags.start,
				new HalfArrow(graph.new BackRegister(Type.Boolean), "reg_start"));

//			newGraph.new Buffer(write, read, previousValue, size)

			newBlock.close();
			return null;
		}



		Delay buildJoining(Delay firstDelay, Delay secondDelay) throws CompilerException { // D1 + =1 + D2
			throw UnsupportedDelayCompilerError.exception(firstDelay);
		}

		private Delay buildPipeline(ComputationBlock block, DelayFlags flags) throws CompilerException {
			return block.getNestedBlocks().fold_((d,b) -> {
				newGraph.openFollowingBlock();
				return buildJoining(d, buildPipeline(b, d.flags));
			}, null);
		}




		private Delay setNodeDelay(DataNode node, Delay delay) {
			return null;
		}

		private Delay getNodeDelay(DataNode node) {
			return null;
		}


		private Duplicator adjust(int latency) {
			return a -> new HalfArrow(blockNodeDelays.containsKey(a.from) ? null : getConvertedNode(a.from), a.getIdent());
		}


		private final HashMap<DataNode, Delay> blockNodeDelays = new HashMap<>();

		private NodeBlock getBlock(GraphBlock block, NodeBlock nodeBlock) {
			if (nodeBlock == null) return null;
			if (nodeBlock == block) return block;
			if (nodeBlock.getParent() == block) return nodeBlock;
			return getBlock(block, nodeBlock.getParent());
		}

		private Delay getNodeDelay(GraphBlock block, DataNode node, DelayFlags flags) {
			NodeBlock nodeBlock = getBlock(block, node.block);
			if (nodeBlock == null) return new NullDelay(flags); 	// Case node out of the block
			if (nodeBlock == block) return getNodeDelay(node); 		// Case node in the block
			return getBlock(block, nodeBlock)						// Case node in a nested block
		}

		private Delay buildNodeDelay(DataNode node, GraphBlock block, DelayFlags flags) throws CompilerException {
			if (node.block == block) {
				List<Pair<HalfArrow, Delay>> args = Generators.fromCollection(node.sources).
						compute_(a -> buildNodeDelay(a.from, block, flags)).mapFirst(this::getConvertedNode).
						map(p -> new Pair<HalfArrow, Delay>(p.first, p.second)).toList();
				int latencyMax = 0;
				boolean unbound = false;
				for (Pair<HalfArrow, Delay> p : args) {
					latencyMax = Math.max(latencyMax, p.second.latency);
					unbound = unbound | (p.second instanceof UnknownDelay);
				}
				if (unbound)
					throw UnsupportedDelayCompilerError.exception(args.get(0).second);
				converted.put(node, node.duplicate(newGraph, adjust(latencyMax)));
				blockNodeDelays.put(node, new KnownDelay((KnownDelayFlags)flags, latencyMax));
			}
			if (node.block == block.parent)
				return new NullDelay(flags);

			throw UnsupportedNodeBlockCompilerError.exception(block);
		}
		*/
	/*
	 * 
		public class DelayFlags<D extends Delay> {
			public final DataFlowGraph graph;
			public final D delay;

			public DelayFlags(D delay) {
				this.graph = newGraph;
				this.delay = delay;
			}

			public D getDelay() {
				return delay;
			}
		}


		public class NullDelayFlags extends DelayFlags<NullDelay> {

			public NullDelayFlags() {
				super(new NullDelay());
			}
		}

		public class KnownDelayFlags extends DelayFlags<KnownDelay> {
			public final HalfArrow clock;

			public KnownDelayFlags(KnownDelay delay) throws CompilerException {
				super(delay);
				clock = getClock();
			}
		}

		public class UnknownDelayFlags extends DelayFlags<UnknownDelay> {
			public final HalfArrow outputReady;

			public UnknownDelayFlags(UnknownDelay delay, DataNode done) throws CompilerException {
				super(delay);
//				inputReady = new HalfArrow(graph.new InputNode("inR", Type.Boolean),"inR");
				outputReady = new HalfArrow(done,"done");
			}


		}
	 */

}
