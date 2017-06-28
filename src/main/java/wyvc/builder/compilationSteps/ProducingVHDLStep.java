package wyvc.builder.compilationSteps;

import java.util.HashMap;
import java.util.Map;

import wyvc.builder.CompilerLogger;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.builder.EntityCompiler;
import wyvc.builder.VHDLCompileTask.CompilationUnit;
import wyvc.builder.compilationSteps.InliningStep.SplittedFunctions;
import wyvc.lang.Component;
import wyvc.lang.Entity;
import wyvc.lang.VHDLFile;
import wyvc.utils.Generators;

public class ProducingVHDLStep extends CompilationStep<SplittedFunctions, ProducingVHDLStep.CompiledFile> {
	public static class CompiledFile extends CompilationUnit {
		public final VHDLFile file;

		public CompiledFile(CompilationUnit cmp, VHDLFile file) {
			super(cmp);
			this.file = file;
		}

		public CompiledFile(CompiledFile other) {
			super(other);
			file = other.file;
		}
	}

	@Override
	protected CompiledFile compile(CompilerLogger logger, SplittedFunctions data) throws CompilerException {
		Map<String,Component> fcts = new HashMap<>();
		return new CompiledFile(data, new VHDLFile(Generators.fromPairCollection(data.func).
			map_((n,g) -> EntityCompiler.compile(logger, n, g, fcts)).toList().toArray(new Entity[data.func.size()])));
	}
}
