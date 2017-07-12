package wyvc.builder;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import wybs.lang.Build;
import wybs.lang.Build.Graph;
import wybs.lang.Build.Project;
import wycc.util.Logger;
import wycc.util.Pair;
import wyfs.lang.Path;
import wyfs.lang.Path.Entry;
import wyfs.lang.Path.Root;
import wyil.lang.WyilFile;
import wyvc.builder.compilationSteps.CompilationStep.CompilationFront;
import wyvc.builder.compilationSteps.CompileFunctionsStep;
import wyvc.builder.compilationSteps.CompileTypesStep;
import wyvc.builder.compilationSteps.ExportFileStep;
import wyvc.builder.compilationSteps.GraphOptimizationStep;
import wyvc.builder.compilationSteps.TimingStep;
import wyvc.builder.compilationSteps.ParsingStep;
import wyvc.builder.compilationSteps.ProducingVHDLStep;
import wyvc.builder.compilationSteps.RecursionAnalysisStep;

public class VHDLCompileTask implements Build.Task {

	public static class CompilationUnit {
		public final Entry<WyilFile> source;
		public final Graph graph;
		public final Root dst;
		public final Set<Entry<?>> generatedFiles;

		@SuppressWarnings("unchecked")
		public CompilationUnit(Pair<Entry<?>, Root> delta, Graph graph, Set<Entry<?>> generatedFiles) {
			this.source = (Entry<WyilFile>) delta.first();
			this.graph = graph;
			this.dst = delta.second();
			this.generatedFiles = generatedFiles;
		}

		public CompilationUnit(CompilationUnit other) {
			source = other.source;
			graph = other.graph;
			dst = other.dst;
			generatedFiles = other.generatedFiles;
		}
	}

	private Logger logger = Logger.NULL;
	private Build.Project project;

	private final CompilationFront<CompilationUnit> compilation = new CompilationFront<>();

	public VHDLCompileTask(Build.Project project) {
		this.project = project;
		compilation.
			setNextStep(new ParsingStep()).
			setNextStep(new CompileTypesStep()).
			setNextStep(new CompileFunctionsStep()).
//			setNextStep(new GraphOptimizationStep()).
			setNextStep(new RecursionAnalysisStep()).
			setNextStep(new TimingStep()).
			setNextStep(new ProducingVHDLStep()).
			setNextStep(new ExportFileStep());
	}

	public Project project() {
		return project;
	}

	public Set<Entry<?>> build(Collection<Pair<Entry<?>, Root>> delta, Graph graph) throws IOException {
		Runtime runtime = Runtime.getRuntime();
		long start = System.currentTimeMillis();
		long memory = runtime.freeMemory();

		Set<Path.Entry<?>> generatedFiles = new HashSet<Path.Entry<?>>();
		CompilerLogger logger = new CompilerLogger();

		for (Pair<Path.Entry<?>, Path.Root> p : delta)
			compilation.compile(logger, new CompilationUnit(p,graph,generatedFiles));

		long endTime = System.currentTimeMillis();
		this.logger.logTimedMessage("Wyil => VHDL: compiled " + delta.size() + " file(s)", endTime - start,
				memory - runtime.freeMemory());
		return generatedFiles;
	}
}
