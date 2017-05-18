package wyvc.io;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.function.Function;

import wyvc.lang.LexicalElement;

import java.io.OutputStream;

/**
 * The TextualOutputStream offers a smart text stream to write VHDL source code
 * using some modifiers.
 *
 * This class handles the management of indentation,  with the <code> indent </code>,
 * <code> dedent </code> and  <code> setIndentationLevel </code> methods.  The insertion of
 * is also made easy thanks to the <code>code</code> and <code>comment</code> output modes.
 *
 * To avoid repetitive accesses to the stream and to improve the readability of
 * the resulting code,  each edition method returns a reference to it,  so that
 * outputs commands can be chained.
 * To strengthen stream API, {@link LexicalElement} can be send directly to the
 * stream that will then call their <code> write </code> method.
 *
 * To make the produced VHDL code more readable, the <code>fill</code> edition method
 * enables to align the major parts of the lines.
 *
 * @author Baptiste Pauget
 *
 */
public class TextualOutputStream {
	protected final OutputStreamWriter output;
	private int indentationLevel = 0;
	private int indentationSize = 2;
	private int lineLenght = 0;
	private boolean isComment = false;
	private boolean newLine = true;

	public TextualOutputStream(OutputStream output){
		this.output = new OutputStreamWriter(output);
	}

	public int getIndentationLevel() {
		return indentationLevel;
	}

	public void setIndentationLevel(int indentationLevel) {
		this.indentationLevel = indentationLevel;
	}

	public int getIndentationSize() {
		return indentationSize;
	}

	public void setIndentationSize(int indentationSize) {
		this.indentationSize = indentationSize;
	}

	public TextualOutputStream(OutputStreamWriter output){
		this.output = output;
	}

	public int getLineStart() {
		return indentationLevel*indentationSize;
	}

	public void write(String content) throws IOException {
		String[] lines = content.split("\n",-2);
		int l = lines.length;
		for(int k = 0; k < l - 1; ++k){
			writeLine(lines[k]);
			endLine();
		}
		if (l > 0)
			writeLine(lines[l-1]);
		output.flush();
	}

	private void writeLine(String string) throws IOException {
		if (string.length() == 0)
			return;
		if (newLine){
			if (isComment){
				int a = Math.max(indentationLevel*indentationSize-2,0);
				lineLenght += 2+a;
				output.write("--");
				output.write(new String(new char[a]).replace('\0', ' '));
			}
			else{
				output.write(new String(new char[indentationLevel*indentationSize]).replace('\0', ' '));
				lineLenght += indentationLevel*indentationSize;
			}
			newLine = false;
		}
		output.write(string);
		lineLenght += string.length();
	}

	public TextualOutputStream indent(){
		indentationLevel++;
		return this;
	}

	public TextualOutputStream dedent(){
		indentationLevel--;
		return this;
	}

	public TextualOutputStream comment(){
		isComment = true;
		return this;
	}

	public TextualOutputStream code() throws IOException {
		if (isComment)
			endLine();
		isComment = false;
		return this;
	}


	public TextualOutputStream endLine() throws IOException {
		newLine = true;
		lineLenght = 0;
		output.write("\n");
		output.flush();
		return this;
	}

	public TextualOutputStream flush() throws IOException {
		output.flush();
		return this;
	}

	public TextualOutputStream fill(int n, char c) throws IOException {
		if (newLine && isComment)
			n -= 2;
		if (n > lineLenght){
			int indent = indentationLevel;
			indentationLevel = 0;
			write(new String(new char[n-lineLenght]).replace('\0', c));
			indentationLevel = indent;
		}
		return this;
	}

	public TextualOutputStream fill(int n) throws IOException {
		return fill(n, ' ');
	}


	public TextualOutputStream w(String s) throws IOException {
		write(s);
		return this;
	}

	public TextualOutputStream w(int i) throws IOException {
		write(Integer.toString(i));
		return this;
	}

	public TextualOutputStream semiColon() throws IOException {
		w(";").endLine();
		return this;
	}
/*
	public TextualOutputStream w(LexicalElement element) throws IOException {
		element.write(this);
		return this;
	}

	public <T> TextualOutputStream wp(T[] param, Function<T, String> f, String end) throws IOException {
		int l = param.length;
		for (int k = 0; k < l - 1; ++k)
			w(f.apply(param[k])).w(end);
		if (l != 0)
			w(f.apply(param[l-1]));
		return this;
	}

	public <T> TextualOutputStream wp(T[] param, Function<T, String> f) throws IOException {
		return wp(param, f, ", ");
	}

	public <T extends LexicalElement> TextualOutputStream wl(T[] list, String end) throws IOException {
		for (T t : list)
			w(t).w(end);
		return this;
	}

	public <T extends LexicalElement> TextualOutputStream wl(T[] list) throws IOException {
		return wl(list, ";\n");
	}*/


	public static <T> int alignment(T[] l, Function<T, String> f){
		int i = 0;
		for (T k : l)
			i = Math.max(i, f.apply(k).length());
		return i;
	}
}
