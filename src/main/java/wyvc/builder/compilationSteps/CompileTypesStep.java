package wyvc.builder.compilationSteps;


import wyvc.builder.CompilerLogger;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.builder.TypeCompiler;
import wyvc.builder.compilationSteps.ParsingStep.ParsedFile;
import wyvc.utils.Generators;

public class CompileTypesStep extends CompilationStep<ParsedFile, CompileTypesStep.CompiledTypes> {
	public static class CompiledTypes extends ParsedFile {
		public final TypeCompiler typeCompiler;

		public CompiledTypes(ParsedFile cmp, TypeCompiler typeCompiler) {
			super(cmp);
			this.typeCompiler = typeCompiler;
		}
		public CompiledTypes(CompiledTypes other) {
			super(other);
			typeCompiler = other.typeCompiler;
		}
	}

	@Override
	protected CompiledTypes compile(CompilerLogger logger, ParsedFile data) throws CompilerException {
		TypeCompiler typeCompiler = new TypeCompiler(logger);
		Generators.fromCollection(data.file.types()).forEach_(typeCompiler::addNominalType);
		return new CompiledTypes(data, typeCompiler);
	}

}
