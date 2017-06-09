package wyvc.builder.compilationSteps;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import wyvc.builder.CompilerLogger;
import wyvc.builder.CompilerLogger.CompilerError;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.builder.TypeCompiler;
import wyvc.builder.compilationSteps.ParsingStep.ParsedFile;
import wyvc.utils.Generators;
import wyvc.utils.Generators.Generator;
import wyvc.utils.Generators.CustomGenerator;
import wyvc.utils.Generators.EndOfGenerationException;

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
	


	
	Generator<Integer> genere(int n) {
		List<Integer> l = new ArrayList<>(n);
		for (int i = 0; i < n; ++i)
			l.add(i);
		return Generators.fromCollection(l);
	}
	
	void print(CompilerLogger logger, Generator<Integer> g) {
		logger.debug(g.map(Object::toString).map(" "::concat).fold(String::concat, " "));
	}
	
	void testGen(CompilerLogger logger) {
		print(logger, genere(5));
		print(logger, genere(5).append(genere(3)));
		print(logger, Generators.concat(Generators.cartesianProduct(Generators.fromCollection(Arrays.asList(genere(3),genere(2))))));
		print(logger, new CustomGenerator<Integer>(){
			@Override
			protected void generate() throws EndOfGenerationException {
				for(int k = 0; k < 10; ++k)
					yield(k);
			}});
	}
	

	@Override
	protected CompiledTypes compile(CompilerLogger logger, ParsedFile data) throws CompilerException {
		testGen(logger);
		//TypeCompiler typeCompiler = new TypeCompiler(logger);
		//Generators.fromCollection(data.file.types()).forEach_(typeCompiler::addNominalType);
		throw new CompilerException(new CompilerError() {

			@Override
			public String info() {
				// TODO Auto-generated method stub
				return "fin";
			}
		});
//		return new CompiledTypes(data, typeCompiler);
	}

}
