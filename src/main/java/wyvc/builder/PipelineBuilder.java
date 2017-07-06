package wyvc.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tools.ant.taskdefs.Sync;

import wyvc.builder.CompilerLogger.CompilerError;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.builder.CompilerLogger.LoggedBuilder;
import wyvc.builder.DataFlowGraph.BinOpNode;
import wyvc.builder.DataFlowGraph.BinaryOperation;
import wyvc.builder.DataFlowGraph.ConstNode;
import wyvc.builder.DataFlowGraph.DataArrow;
import wyvc.builder.DataFlowGraph.DataNode;
import wyvc.builder.DataFlowGraph.Duplicator;
import wyvc.builder.DataFlowGraph.EndIfNode;
import wyvc.builder.DataFlowGraph.HalfArrow;
import wyvc.builder.DataFlowGraph.InputNode;
import wyvc.builder.DataFlowGraph.Latency;
import wyvc.builder.DataFlowGraph.Register;
import wyvc.builder.DataFlowGraph.UnaOpNode;
import wyvc.builder.DataFlowGraph.WhileNode;
import wyvc.lang.Type;
import wyvc.utils.FunctionalInterfaces.BiFunction_;
import wyvc.utils.Generators;
import wyvc.utils.Generators.Generator;
import wyvc.utils.Generators.Generator_;
import wyvc.utils.Pair;

public class PipelineBuilder extends LoggedBuilder {
	public final int NoDelay = 0;
	public final int UnboundDelay = -1;
	private final Map<DataNode, DataNode> converted = new HashMap<>();

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











	public static class DelayFlags {
		public final DataFlowGraph graph;

		public DelayFlags(DataFlowGraph graph) {
			this.graph = graph;
		}
	}


	public static class NullDelayFlags extends DelayFlags {

		public NullDelayFlags(DataFlowGraph graph) {
			super(graph);
		}

		public NullDelayFlags(NullDelayFlags flag) {
			super(flag.graph);
		}
	}

	public static class KnownDelayFlags extends NullDelayFlags {
		public final HalfArrow clock;

		public KnownDelayFlags(DataFlowGraph graph) throws CompilerException {
			super(graph);
			clock = new HalfArrow(graph.new InputNode("clock", Type.Boolean),"clock");
		}

		public KnownDelayFlags(KnownDelayFlags flags) throws CompilerException {
			super(flags);
			clock = flags.clock;
		}
	}

	public static class UnknownDelayFlags extends KnownDelayFlags {
		public final HalfArrow start;

		public UnknownDelayFlags(DataFlowGraph graph) throws CompilerException {
			super(graph);
			start = new HalfArrow(graph.new InputNode("start", Type.Boolean),"start");
		}

		public UnknownDelayFlags(KnownDelayFlags flags, HalfArrow start) throws CompilerException {
			super(flags);
			this.start = start;
		}

	}




	public static abstract class Delay {
		public final DataFlowGraph graph;
		public final int latency;

		public Delay(DataFlowGraph graph, int latency) {
			this.graph = graph;
			this.latency = latency;
		}
	}

	public static class NullDelay extends Delay {

		public NullDelay(DataFlowGraph graph) {
			super(graph, 0);
		}

		@Override
		public String toString() {
			return "=0";
		}

	}

	public static class KnownDelay extends Delay {
		public final DataNode clock;

		public KnownDelay(DataFlowGraph graph, DataNode clock, int delay) throws CompilerException {
			super(graph, delay);
			this.clock = clock;
		}

		@Override
		public String toString() {
			return "="+latency;
		}
	}



//	public static class UnknownDelay extends Delay {
//		public final UnknownDelayFlags flags;
//		public final HalfArrow inputReady;
//		public final HalfArrow outputReady;
//		public final int delayMin;
//
//		public UnknownDelay(UnknownDelayFlags flags, HalfArrow inputReady, HalfArrow outputReady, int delayMin) throws CompilerException {
//			super(flags, delayMin);
//			this.flags = flags;
//			this.inputReady = inputReady;
//			this.outputReady = outputReady;
//			this.delayMin = delayMin;
//		}
//		public UnknownDelay(UnknownDelayFlags flags, DataNode inputReady, DataNode outputReady, int delayMin) throws CompilerException {
//			this(flags, new HalfArrow(inputReady, "inputReady"), new HalfArrow(outputReady, "outputReady"), delayMin);
//		}
//
//		@Override
//		public int getMinimumDelay() throws CompilerException {
//			return delayMin;
//		}
//
//		@Override
//		public String toString() {
//			return ">="+delayMin;
//		}
//
//
//
//		@Override
//		public UnknownDelayFlags getDelayFlags() {
//			return flags;
//		}
//	}





	private class Builder extends LoggedBuilder {
		private final Map<DataNode, Delay> nodeDelays = new HashMap<>();
		private final Map<DataNode, DataNode> convertedNodes = new HashMap<>();
		private final DataFlowGraph newGraph = new DataFlowGraph();
		private final DataFlowGraph graph;

private InputNode clck = newGraph.new InputNode("clck", Type.Boolean);
		private InputNode getClock() throws CompilerException {
			return clck; // TODO
		}


		public Builder(CompilerLogger logger, DataFlowGraph graph) throws CompilerException {
			super(logger);
			this.graph = graph;
			buildPipeline(graph, graph.getLatency());
		}


		private <T extends DataNode> T setNodeDelay(DataNode previous, T node, Delay delay) {
			convertedNodes.put(previous, node);
			nodeDelays.put(node, delay);
			return node;
		}

		private Delay getNodeDelay(DataNode node) throws CompilerException {
			// TODO check contains
			return nodeDelays.get(node);
		}
		private Delay getNodeDelay(HalfArrow node) throws CompilerException {
			return nodeDelays.get(node.node);
		}
		private Delay getSourceDelay(DataArrow source) throws CompilerException {
			return getNodeDelay(getConvertedNode(source.from));
		}


		private Delay merge(Delay d1, Delay d2) throws CompilerException {
			if (d1 instanceof NullDelay)
				return d2;
			if (d2 instanceof NullDelay)
				return d1;
			if (d1 instanceof KnownDelay && d2 instanceof KnownDelay)
				return new KnownDelay(d1.graph, getClock(), Math.max(d1.latency, d2.latency));
			throw UnsupportedDelayCompilerError.exception(d1);
		}

		private Delay merge(Generator_<Delay, CompilerException> delays) throws CompilerException {
			return delays.fold(this::merge, (Delay) new NullDelay(null));
		}
		private Delay mergeNodes(Generator<DataNode> nodes) throws CompilerException {
			return merge(nodes.map_(this::getConvertedNode).map(this::getNodeDelay));
		}
		private Delay mergeSources(Generator<DataArrow> sources) throws CompilerException {
			return mergeNodes(sources.map(a -> a.from));
		}

		private Delay concat(Delay first, Delay second) throws CompilerException {
			if (first instanceof NullDelay)
				return second;
			if (second instanceof NullDelay)
				return first;
			if (first instanceof KnownDelay && second instanceof KnownDelay)
				return new KnownDelay(first.graph, getClock(), first.latency + second.latency);
			throw UnsupportedDelayCompilerError.exception(first);
		}


		private HalfArrow synchronizeNode(HalfArrow node, Delay delay) throws CompilerException {
			if (node.node instanceof ConstNode)
				return node;
			Delay nodeDelay = getNodeDelay(node);
			if (nodeDelay instanceof NullDelay || nodeDelay instanceof KnownDelay) {
				if (delay instanceof NullDelay || delay instanceof KnownDelay) {
					if (delay.latency < nodeDelay.latency)
						throw ImpossibleSynchronizationCompilerError.exception(nodeDelay, delay);
					return nodeDelay.latency == delay.latency
							? node
							: new HalfArrow(newGraph.new Register(node, delay.latency - nodeDelay.latency), node.ident);
				}
			}
			throw UnsupportedDelayCompilerError.exception(delay);
		}

		private HalfArrow synchronizeSource(DataArrow source, Delay delay) throws CompilerException {
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

		private BinOpNode buildPipeline(BinOpNode node) throws CompilerException {
			Delay delay = mergeSources(Generators.fromValues(node.op1, node.op2));
			return setNodeDelay(node, newGraph.new BinOpNode(node.kind, node.type,
					synchronizeSource(node.op1, delay), synchronizeSource(node.op2, delay), node.location), delay);
		}
		private UnaOpNode buildPipeline(UnaOpNode node) throws CompilerException {
			Delay delay = getSourceDelay(node.op);
			return setNodeDelay(node, newGraph.new UnaOpNode(node.kind, node.type,
					synchronizeSource(node.op, delay), node.location), delay);
		}
		private EndIfNode buildPipeline(EndIfNode node) throws CompilerException {
			Delay delay = mergeSources(Generators.fromValues(node.condition, node.trueNode, node.falseNode));
			return setNodeDelay(node, newGraph.new EndIfNode(synchronizeSource(node.condition, delay),
					synchronizeSource(node.trueNode, delay), synchronizeSource(node.falseNode, delay), node.location), delay);
		}
		private Register buildPipeline(Register node) throws CompilerException {
			Delay delay = concat(getSourceDelay(node.previousValue), new KnownDelay(newGraph, getClock(), node.delay));
			return setNodeDelay(node, newGraph.new Register(getConvertedHalfArrow(node.previousValue), node.delay), delay);
		}
		private InputNode buildPipeline(InputNode node) throws CompilerException {
			return setNodeDelay(node, newGraph.new InputNode(node.nodeIdent, node.type), new NullDelay(newGraph));
		}
		private ConstNode buildPipeline(ConstNode node) throws CompilerException {
			return setNodeDelay(node, newGraph.new ConstNode(node.nodeIdent, node.type, node.location), new NullDelay(newGraph));
		}

		private DataNode buildPipeline(DataNode node) throws CompilerException {
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


		private DelayFlags getFlags(Latency latency) throws CompilerException {
			switch (latency) {
			case NullDelay:
				return new NullDelayFlags(newGraph);
			case KnownDelay:
				return new KnownDelayFlags(newGraph);
			case UnknownDelay:
				return new UnknownDelayFlags(newGraph);
			default:
				throw UnsupportedDelayCompilerError.exception(null);
			}
		}

		private Delay buildPipeline(DataFlowGraph graph, Latency latency) throws CompilerException {
			DelayFlags flags = getFlags(latency);
			Delay delay = mergeSources(graph.getOutputNodes().map(o -> o.source));
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


//
//
//
//		private Delay buildDelay(WhileBlock block) throws CompilerException {
//			return setBlockDelay(block,new UnknownDelay(buildDelay(block.condition).merge(buildDelay(block.body)).getMinimumDelay()));
//		}
//		private Delay buildDelay(IfBlock block) throws CompilerException {
//			return setBlockDelay(block,buildDelay(block.condition).merge(buildDelay(block.trueBranch).merge(buildDelay(block.falseBranch))));
//		}
//		private Delay buildStep(Delay d1, Delay d2) throws CompilerException {
//			if (d2 == null)
//				throw UnsupportedDelayCompilerError.exception(d2);
//			if (d1 == null)
//				return d2;
//			return d1.compose(new KnownDelay(1)).compose(d2);
//		}
//		private Delay buildDelay(ComputationBlock block) throws CompilerException {
//			openLevel("ComputationBlock");
//			debugLevel("->"+block.getNumberOfNestedBlocks()+" "+block.getNestedBlocks().toList());
//			return end(setBlockDelay(block, block.getNumberOfNestedBlocks() == 0
//					? new NullDelay()
//					: block.getNestedBlocks().map_(this::buildDelay).fold(this::buildStep, null)));
//		}
//		private Delay buildNodeDelay(DataNode node, GraphBlock block) throws CompilerException {
//			if (nodeDelays.containsKey(node))
//				return nodeDelays.get(node);
//			if (node.block != block)
//				return setNodeDelay(node, blockDelays.containsKey(node.block) ? blockDelays.get(node.block) : new NullDelay());
//			return setNodeDelay(node, node.getSources().gather(Generators.constant(block)).map_(this::buildNodeDelay).fold(Delay::merge, new NullDelay()));
//		}
//		private Delay buildDelay(GraphBlock block) throws CompilerException {
//			block.getNestedBlocks().forEach_(this::buildDelay);
//			return block.getNodes().gather(Generators.constant(block)).map_(this::buildNodeDelay).fold(Delay::merge, new NullDelay());
//		}
//		private Delay buildDelay(NodeBlock block) throws CompilerException {
//			if (block instanceof WhileBlock)
//				return buildDelay((WhileBlock) block);
//			if (block instanceof IfBlock)
//				return buildDelay((IfBlock) block);
//			if (block instanceof ComputationBlock)
//				return buildDelay((ComputationBlock) block);
//			if (block instanceof GraphBlock)
//				return buildDelay((GraphBlock) block);
//			throw UnsupportedNodeBlockCompilerError.exception(block);
//		}

}
