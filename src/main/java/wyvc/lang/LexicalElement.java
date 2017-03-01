package wyvc.lang;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import wyvc.io.TextualOutputStream;
import wyvc.io.Tokenisable;
import wyvc.io.Tokenisable.Token.StartToken;


/**
 * The LexicalElement interfaces defines the common methods to every class that
 * is part of the VHDL abstract syntax tree.
 *
 * @author Baptiste Pauget
 *
 */
public interface LexicalElement extends Tokenisable {


	static String stringFromStream(LexicalElement element) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			StartToken t = new StartToken();
			element.addTokens(t);
			t.write(new TextualOutputStream(baos));
		} catch (IOException e) {
			return "Printing impossible";
		}
		return baos.toString();
	}

	public abstract static class NamedElement implements LexicalElement {
		public final String ident;

		public NamedElement(String ident) {
			this.ident = ident;
		}

		@Override
		public void addTokens(Token t) {
			t.comment().n(ident).n(" NamedElement printer unimplemented").code();
		}

		@Override
		public String toString(){
			return stringFromStream(this);
		}
	}

	public abstract static class TypedElement implements LexicalElement {
		public final Type type;


		protected TypedElement(Type type) {
			this.type = type;
		}

		public Type getType(){
			return type;
		}

		@Override
		public String toString(){
			return stringFromStream(this);
		}
	}

	public static abstract class VHDLException extends Exception {
		private static final long serialVersionUID = -9053725943728710364L;
		private final String elementName;

		public VHDLException(Class<?> element) {
			this.elementName = element.getSimpleName();
		}

		public final void info() {
			System.err.println("VHDL error : " + elementName);
			details();
		}

		protected abstract void details();
	}

	public static class UnsupportedException extends VHDLException {
		private static final long serialVersionUID = 8137641416108743385L;

		public UnsupportedException(Class<?> element) {
			super(element);
		}

		protected void details() {
			System.err.println("    Unsupported feature");
		}
	}
}
