package wyvc.lang;

import static wyvc.lang.LexicalElement.stringFromStream;


public abstract class TypedValue implements LexicalElement {
	public final Type type;
	public final String ident;

	private TypedValue(String ident, Type type) {
		this.ident = ident;
		this.type = type;
	}

	@Override
	public String toString(){
		return stringFromStream(this);
	}

	public abstract boolean isWritable();


	public static class Constant extends TypedValue {
		public Constant(String ident, Type type) {
			super(ident, type);
		}

		@Override
		public void addTokens(Token t) {
			t.n("constant ").n(ident).align().n(" : ").n(type).semiColon();
		}

		@Override
		public boolean isWritable() {
			return false;
		}
	}

	public static class Signal extends TypedValue {
		public Signal(String ident, Type type) {
			super(ident, type);
		}

		@Override
		public void addTokens(Token t) {
			t.n("signal ").n(ident).align().n(" : ").n(type).semiColon();
		}

		@Override
		public boolean isWritable() {
			return true;
		}
	}



	public static class Port extends Signal {
		public final Mode mode;

		public static enum Mode {
			IN,
			OUT
		}

		public Port(String ident, Type type, Mode mode) {
			super(ident, type);
			this.mode = mode;
		}

		@Override
		public void addTokens(Token t) {
			t.n(ident).align().n(" : ").n(mode == Mode.IN ? "in  " : "out ").align().n(type);
		}

		@Override
		public boolean isWritable() {
			return mode == Mode.OUT;
		}

	}



	public static class PortError extends VHDLError {
		private final Port port;

		@Override
		protected String details() {
			return "Bad port use : " + (port.mode == Port.Mode.IN ? "Input port \""+port.ident+"\" cannot be written"
			                                                      : "Output port \""+port.ident+"\" cannot be read");
		}

		public PortError(Class<?> element, Port port) {
			super(element);
			this.port = port;
		}
	}

	public static class Variable extends TypedValue {
		public Variable(String ident, Type type) {
			super(ident, type);
		}

		@Override
		public void addTokens(Token t) {
			t.n("variable ").n(ident).align().n(" : ").n(type).semiColon();
		}

		@Override
		public boolean isWritable() {
			return true;
		}
	}

}
