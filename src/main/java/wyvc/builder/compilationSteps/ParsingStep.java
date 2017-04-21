package wyvc.builder.compilationSteps;

import java.io.IOException;

import wyil.lang.WyilFile;
import wyvc.builder.CompilerLogger;
import wyvc.builder.CompilerLogger.CompilerError;
import wyvc.builder.VHDLCompileTask.CompilationUnit;

public class ParsingStep extends CompilationStep<CompilationUnit, ParsingStep.ParsedFile> {
	public static class ParsedFile extends CompilationUnit {
		public final WyilFile file;

		public ParsedFile(CompilationUnit cmp, WyilFile file) {
			super(cmp);
			this.file = file;
		}
		public ParsedFile(ParsedFile other) {
			super(other);
			file = other.file;
		}
	}

	@Override
	protected ParsedFile compile(CompilerLogger logger, CompilationUnit data) {
		try {
			return new ParsedFile(data, data.source.read());
		} catch (IOException e) {
			logger.addMessage(new CompilerError() {
				@Override
				public String info() {
					return "Error reading file "+data.source+"\n"+e.getStackTrace();
				}
			});
		}
		return null;
	}
}
