package wyvc.builder.compilationSteps;

import java.util.HashMap;
import java.util.Map;

import wyil.lang.WyilFile;
import wyvc.builder.CompilerLogger;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.builder.TypeCompiler;
import wyvc.builder.TypeCompiler.TypeTree;
import wyvc.builder.compilationSteps.ParsingStep.ParsedFile;

public class CompileTypesStep extends CompilationStep<ParsedFile, CompileTypesStep.CompiledTypes> {
	public static class CompiledTypes extends ParsedFile {
		public final Map<String, TypeTree> types;

		public CompiledTypes(ParsedFile cmp, Map<String, TypeTree> types) {
			super(cmp);
			this.types = types;
		}
		public CompiledTypes(CompiledTypes other) {
			super(other);
			types = other.types;
		}
	}

	@Override
	protected CompiledTypes compile(CompilerLogger logger, ParsedFile data) throws CompilerException {
		Map<String, TypeTree> types = new HashMap<>();
		for (WyilFile.Type t  : data.file.types())
			types.put(t.name(), TypeCompiler.compileType(logger, t.type(),types));
		return new CompiledTypes(data, types);
	}

}
