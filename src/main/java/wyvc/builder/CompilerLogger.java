package wyvc.builder;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CompilerLogger {
	public static final class CompilerException extends Exception {
		private static final long serialVersionUID = -1744237848876954932L;

		public final CompilerError error;

		public CompilerException(CompilerError error) {
			this.error = error;
		}
	}

	public static enum CompilerMessageType {
		Error, Warning, Notice
	}

	private static abstract class CompilerMessage {
		public final CompilerMessageType type;

		public CompilerMessage(CompilerMessageType type) {
			this.type = type;
		}

		public abstract String info();
	}

	public static abstract class CompilerError extends CompilerMessage {
		public CompilerError() {
			super(CompilerMessageType.Error);
		}
	}

	public static abstract class CompilerWarning extends CompilerMessage {
		public CompilerWarning() {
			super(CompilerMessageType.Warning);
		}
	}

	public static abstract class CompilerNotice extends CompilerMessage {
		public CompilerNotice() {
			super(CompilerMessageType.Notice);
		}
	}

	public static class UnsupportedCompilerError extends CompilerError {
		public String info() {
			return "This feature is currently unsupported";
		}
	}

	public static class UnboundLoopCompilerWarning extends CompilerWarning {
		@Override
		public String info() {
			return "This loop is unbound and prevent any time garantee";
		}
	}






	private final PrintStream err;
	private List<CompilerMessage> messages = new ArrayList<>();
	private Map<CompilerMessageType, Integer> types = new HashMap<>();
	private boolean debugOut = !true;

	public CompilerLogger() {
		err = System.err;
	}

	public CompilerLogger(PrintStream err) {
		this.err = err;
	}

	public boolean has(CompilerMessageType type) {
		return types.getOrDefault(type, 0) != 0;
	}

	public void printMessages() {
		// TODO
	}

	public <T> T addMessage(CompilerMessage message, T value) {
		addMessage(message);
		return value;
	}

	public CompilerLogger addMessage(CompilerMessage message) {
		messages.add(message);
		err.println("┌─── "+message.type.name() + " ────────────────────────────────────────────────────────────────".substring(0, 60-message.type.name().length()));
		for (String i : message.info().split("\n"))
			err.println("│"+i);
		types.put(message.type, types.getOrDefault(message.type, 0) + 1);
		return this;
	}

	public void debug(String message) {
		if (debugOut )
			for (String i : message.split("\n"))
				err.println("    > "+i);
	}



	public static class LoggedBuilder {
		public final CompilerLogger logger;

		public LoggedBuilder() {
			logger = new CompilerLogger();
		}

		public LoggedBuilder(LoggedBuilder parent) {
			logger = parent.logger;
		}

		public LoggedBuilder(CompilerLogger logger) {
			this.logger = logger;
		}

		public <T> T addMessage(CompilerMessage message, T defaultValue) {
			addMessage(message);
			return defaultValue;
		}
		public void addMessage(CompilerMessage message) {
			logger.addMessage(message);
		}

		public boolean has(CompilerMessageType type) {
			return logger.has(type);
		}

		public void debug(String message){
			logger.debug(message);
		}
	}




}
