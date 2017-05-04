package wyvc.builder.compilationSteps;


import wyvc.builder.CompilerLogger;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.builder.CompilerLogger.CompilerMessageType;


public abstract class CompilationStep<T,U> {
	private CompilationStep<U,?> next = null;

	protected abstract U compile(CompilerLogger logger, T data) throws CompilerException;

	public final void compileStep(CompilerLogger logger, T data) {
		try {
			U u = compile(logger, data);
			if (next != null && !logger.has(CompilerMessageType.Error)){
				next.compileStep(logger, u);
				return;
			}
		}
		catch (CompilerException e){
			e.printStackTrace();
			logger.addMessage(e.error);
		}
		logger.printMessages();
	}

	public <V> CompilationStep<U,V> setNextStep(CompilationStep<U,V> step) {
		next = step;
		return step;
	}


	public static class CompilationFront<T> {
		private CompilationStep<T,?> next = null;

		public <U> CompilationStep<T,U> setNextStep(CompilationStep<T,U> step) {
			next = step;
			return step;
		}

		public void compile(CompilerLogger logger, T data) {
			if(next != null)
				next.compileStep(logger, data);
		}
	}
}
