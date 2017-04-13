package wyvc.lang;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import wyvc.builder.CompilerLogger.CompilerError;
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

	public static abstract class VHDLError extends CompilerError {
		private final String elementName;

		public VHDLError(Class<?> element) {
			this.elementName = element.getSimpleName();
		}

		public final String info() {
			return "VHDL error : " + elementName + "\n" + details();
		}

		protected abstract String details();
	}

	public static class UnsupportedException extends VHDLError {

		public UnsupportedException(Class<?> element) {
			super(element);
		}

		protected String details() {
			return "    Unsupported feature";
		}
	}
}
