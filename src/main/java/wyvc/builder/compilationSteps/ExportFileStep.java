package wyvc.builder.compilationSteps;

import java.io.IOException;

import wyfs.lang.Path;
import wyvc.Activator;
import wyvc.builder.CompilerLogger;
import wyvc.builder.compilationSteps.ProducingVHDLStep.CompiledFile;
import wyvc.lang.VHDLFile;

public class ExportFileStep extends CompilationStep<CompiledFile,Void> {

	@Override
	protected Void compile(CompilerLogger logger, CompiledFile data) {
		try {
			Path.Entry<VHDLFile> target = data.dst.create(data.source.id(), Activator.ContentType);
			data.graph.registerDerivation(data.source, target);
			data.generatedFiles.add(target);
			target.write(data.file);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
