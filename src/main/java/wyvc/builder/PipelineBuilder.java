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
import wyvc.builder.DataFlowGraph.ComputationBlock;
import wyvc.builder.DataFlowGraph.DataArrow;
import wyvc.builder.DataFlowGraph.DataNode;
import wyvc.builder.DataFlowGraph.Duplicator;
import wyvc.builder.DataFlowGraph.EndIfNode;
import wyvc.builder.DataFlowGraph.FunctionBlock;
import wyvc.builder.DataFlowGraph.GraphBlock;
import wyvc.builder.DataFlowGraph.HalfArrow;
import wyvc.builder.DataFlowGraph.IfBlock;
import wyvc.builder.DataFlowGraph.Latency;
import wyvc.builder.DataFlowGraph.NodeBlock;
import wyvc.builder.DataFlowGraph.WhileBlock;
import wyvc.builder.DataFlowGraph.WhileNode;
import wyvc.lang.Type;
import wyvc.utils.FunctionalInterfaces.BiFunction_;
import wyvc.utils.Generators;
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

	public static class UnsupportedNodeBlockCompilerError extends CompilerError {
		private final NodeBlock nodeBlock;

		public UnsupportedNodeBlockCompilerError(NodeBlock nodeBlock) {
			this.nodeBlock = nodeBlock;
		}

		@Override
		public String info() {
			return "The block "+nodeBlock+" is not supported";
		}

		public static CompilerException exception(NodeBlock nodeBlock) {
			return new CompilerException(new UnsupportedNodeBlockCompilerError(nodeBlock));
		}
	}



	public static class UnknownBlockDelayCompilerError extends CompilerError {
		private final NodeBlock nodeBlock;

		public UnknownBlockDelayCompilerError(NodeBlock nodeBlock) {
			this.nodeBlock = nodeBlock;
		}

		@Override
		public String info() {
			return "The delay of block "+nodeBlock+" is unknown";
		}

		public static CompilerException exception(NodeBlock nodeBlock) {
			return new CompilerException(new UnknownBlockDelayCompilerError(nodeBlock));
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
		public final DelayFlags flags;
		public final int latency;

		public Delay(DelayFlags flags, int latency) {
			this.flags = flags;
			this.latency = latency;
		}

		public abstract int getMinimumDelay() throws CompilerException;

		public DelayFlags getDelayFlags() {
			return flags;
		}

		public DataFlowGraph getGraph() {
			return flags.graph;
		}




		public HalfArrow delay(HalfArrow node, int nCycle) throws CompilerException {
			return nCycle <= 0 ? node : new HalfArrow(flags.graph.new Register(node, nCycle), node.ident);
		}



	}

	public static class NullDelay extends Delay {
		public final DelayFlags flags;

		public NullDelay(DelayFlags flags) {
			super(flags, 0);
			this.flags = flags;
		}

		@Override
		public int getMinimumDelay() throws CompilerException {
			return 0;
		}
		@Override
		public String toString() {
			return "=0";
		}

		@Override
		public DelayFlags getDelayFlags() {
			return flags;
		}


	}

	public static class KnownDelay extends Delay {
		public final KnownDelayFlags flags;
		public final int delay;

		public KnownDelay(KnownDelayFlags flags, int delay) throws CompilerException {
			super(flags, delay);
			this.flags = flags;
			this.delay = delay;
		}

		@Override
		public int getMinimumDelay() throws CompilerException {
			return delay;
		}


		@Override
		public String toString() {
			return "="+delay;
		}


		@Override
		public KnownDelayFlags getDelayFlags() {
			return flags;
		}

	}



	public static class UnknownDelay extends Delay {
		public final UnknownDelayFlags flags;
		public final HalfArrow inputReady;
		public final HalfArrow outputReady;
		public final int delayMin;

		public UnknownDelay(UnknownDelayFlags flags, HalfArrow inputReady, HalfArrow outputReady, int delayMin) throws CompilerException {
			super(flags, delayMin);
			this.flags = flags;
			this.inputReady = inputReady;
			this.outputReady = outputReady;
			this.delayMin = delayMin;
		}
		public UnknownDelay(UnknownDelayFlags flags, DataNode inputReady, DataNode outputReady, int delayMin) throws CompilerException {
			this(flags, new HalfArrow(inputReady, "inputReady"), new HalfArrow(outputReady, "outputReady"), delayMin);
		}

		@Override
		public int getMinimumDelay() throws CompilerException {
			return delayMin;
		}

		@Override
		public String toString() {
			return ">="+delayMin;
		}



		@Override
		public UnknownDelayFlags getDelayFlags() {
			return flags;
		}
	}





	private class Builder extends LoggedBuilder {
		private final Map<NodeBlock, Delay> blockDelays = new HashMap<>();
		private final Map<DataNode, Delay> nodeDelays = new HashMap<>();
		private final DataFlowGraph newGraph = new DataFlowGraph();
		private final DataFlowGraph graph;




		public Builder(CompilerLogger logger, DataFlowGraph graph) throws CompilerException {
			super(logger);
			this.graph = graph;
			buildPipeline(graph.topLevelBlock, graph.getLatency());
		}


		private Delay setBlockDelay(NodeBlock block, Delay delay) {
			blockDelays.put(block, delay);
//			debugLevel("Delay block "+block+" : "+delay.toString());
			return delay;
		}
		private Delay setNodeDelay(DataNode node, Delay delay) {
			nodeDelays.put(node, delay);
			//debugLevel("Delay "+node+" : "+delay.toString());
			return delay;
		}

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



		private Delay buildPipeline(GraphBlock block, DelayFlags flags) throws CompilerException {
			blockNodeDelays.clear();
			block.getNodes().forEach_(n -> buildNodeDelay(n, block, flags));


			throw UnsupportedNodeBlockCompilerError.exception(block);

		}


		private Delay buildPipeline(NodeBlock block, DelayFlags flags) throws CompilerException {
			if (block instanceof WhileBlock)
				return buildPipeline((WhileBlock) block, flags);
			if (block instanceof IfBlock)
				return buildPipeline((IfBlock) block, flags);
			if (block instanceof ComputationBlock)
				return buildPipeline((ComputationBlock) block, flags);
			if (block instanceof GraphBlock)
				return buildPipeline((GraphBlock) block, flags);
			else
				throw UnsupportedNodeBlockCompilerError.exception(block);
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

		private void buildPipeline(FunctionBlock block, Latency latency) throws CompilerException {
			newGraph.topLevelBlock.openInputs();
			DelayFlags flags = getFlags(latency);
			graph.getInputNodes().forEach_(n -> newGraph.new InputNode(n.nodeIdent, n.type));
			newGraph.topLevelBlock.openBody();
			//buildPipeline(block.body, flags);
			newGraph.topLevelBlock.openOutputs();
			// TODO
			newGraph.topLevelBlock.setCurrent();
		}


	}


	public DataFlowGraph buildPipeline(DataFlowGraph graph) throws CompilerException {
		return new Builder(logger, graph).newGraph;
	}



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
