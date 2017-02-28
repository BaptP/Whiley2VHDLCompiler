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


	public static class Constant extends TypedValue {
		public Constant(String ident, Type type) {
			super(ident, type);
		}

		@Override
		public void addTokens(Token t) {
			t.n("constant ").n(ident).align().n(" : ").n(type).semiColon();
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

	}

	public static class PortException extends VHDLException {
		private static final long serialVersionUID = -3024890817632444811L;

		private final Port port;

		public void details(){
			System.err.print("    Bad port use : ");
			if (port.mode == Port.Mode.IN)
				System.err.println("Input port \""+port.ident+"\" cannot be written");
			else
				System.err.println("Output port \""+port.ident+"\" cannot be read");
		}

		public PortException(Class<?> element, Port port) {
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
	}

}
