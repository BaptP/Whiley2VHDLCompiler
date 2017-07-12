package wyvc.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;


public interface Tokenisable {
	void addTokens(Token t);


	public static abstract class Token {
		private class SharedData {
			protected final TextualOutputStream output;
			protected ArrayList<Integer> align = new ArrayList<>();
			protected int index = 0;

			public SharedData(TextualOutputStream output) {
				this.output = output;
			}
		}
		private Token next = null;
		protected SharedData data = null;

		private Token last(){
			return next == null ? this : next.last();
		}

		public Token alignement(int pos, ArrayList<Integer> lengths, int index) {
			if (next != null)
				next.alignement(pos, lengths, index);
			return this;
		}

		protected abstract void writeToken() throws IOException;

		public void write(TextualOutputStream output) throws IOException {
			//System.out.print("Token " + this+"... ");
			write(new SharedData(output));
			//System.out.println(" ok");
		}

		protected void write(SharedData data) throws IOException {
			this.data = data;
			writeToken();
			if (next != null)
				next.write(this.data);
			this.data = null;
		}


		private final Token n(Token n) {
			last().next = n;
			return n.last();
		}

		public final Token n(Tokenisable t) {
			t.addTokens(this);
			return last();
		}

		public final Token n(String s) {
			return n(new StringToken(s));
		}

		public final Token n(int i) {
			return n(new StringToken(Integer.toString(i)));
		}

		public final <T> Token n(List<T> list, BiConsumer<T,Token> tokens, String end) {
			return n(new ListToken<T>(list, (T t) -> {
				Token s = new StartToken();
				tokens.accept(t,s);
				return s;
			}, end));
		}

		public final <T> Token n(T[] list, BiConsumer<T,Token> tokens, String end) {
			return n(Arrays.asList(list), tokens, end);
		}

		public final <T extends Tokenisable> Token n(List<T> list, String end) {
			/*System.out.println("LIST "+list.size());
			for (T t : list)
				System.out.print(t==null ? "NULL" : t.getClass()+" - ");
			System.out.println("");*/
			return n(list, (T e,Token t) -> e.addTokens(t), end);
		}

		public final <T extends Tokenisable> Token n(T[] list, String end) {
			return n(Arrays.asList(list), end);
		}

		public final <T extends Tokenisable> Token n(List<T> list) {
			return n(list, "");
		}

		public final <T extends Tokenisable> Token n(T[] list) {
			return n(Arrays.asList(list));
		}

		public final Token endLine() {
			return n(new EndLineToken());
		}

		public final Token semiColon() {
			return n(new SemiColonToken());
		}

		public final Token indent() {
			return n(new IndentToken());
		}

		public final Token dedent() {
			return n(new DedentToken());
		}

		public final Token comment() {
			return n(new CommentToken());
		}

		public final Token code() {
			return n(new CodeToken());
		}

		public final Token fill(int n, char c) {
			return n(new FillToken(n,c));
		}

		public final Token fill(int n) {
			return fill(n, (' '));
		}

		public final Token align() {
			return n(new AlignToken());
		}

		public final Token merge() {
			return n(new MergeToken());
		}



		////////////////////////////////////////////////////////
		//                       Tokens                       //
		////////////////////////////////////////////////////////


		public static class StartToken extends Token {
			@Override
			protected void writeToken() throws IOException {};
		}

		private static class StringToken extends Token {
			public final String string;

			public StringToken(String s) {
				System.out.println("+"+s+"+");
				int i = s.indexOf('\n');
				if (i != -1){
					string = s.substring(0, i);
					endLine().n(s.substring(i+1));
				}
				else
					string = s;
			}

			@Override
			public Token alignement(int pos, ArrayList<Integer> lengths, int index) {
				int l = string.length();
				lengths.set(index, Math.max(lengths.get(index), pos + l));
				return super.alignement(pos + l, lengths, index);
			}

			@Override
			protected void writeToken() throws IOException {
				data.output.w(string);
			}
		}

		private static class EndLineToken extends Token {
			@Override
			protected void writeToken() throws IOException {
				data.output.endLine();
			}

			@Override
			public Token alignement(int pos, ArrayList<Integer> lengths, int index) {
				return super.alignement(0, lengths, index);
			}
		}

		private static class SemiColonToken extends Token {
			@Override
			protected void writeToken() throws IOException{
				data.output.semiColon();
			}
		}

		private static class IndentToken extends Token {
			@Override
			protected void writeToken() throws IOException {
				data.output.indent();
			}
		}

		private static class DedentToken extends Token {
			@Override
			protected void writeToken() throws IOException {
				data.output.dedent();
			}
		}

		private static class CommentToken extends Token {
			@Override
			protected void writeToken() throws IOException {
				data.output.comment();
			}
		}

		private static class CodeToken extends Token {
			@Override
			protected void writeToken() throws IOException {
				data.output.code();
			}
		}

		private static class FillToken extends Token {
			public final int n;
			public final char c;

			public FillToken(int n, char c) {
				this.n = n;
				this.c = c;
			}

			@Override
			protected void writeToken() throws IOException {
				data.output.fill(n, c);
			}
		}

		private static class AlignToken extends Token {
			@Override
			protected void writeToken() throws IOException {
				if (data.index < data.align.size())
					data.output.fill(data.output.getLineStart() + data.align.get(data.index));
				data.index++;
			}

			@Override
			public Token alignement(int pos, ArrayList<Integer> lengths, int index) {
				if (index == lengths.size() -1)
					lengths.add(0);
				return super.alignement(0, lengths, index+1);
			}
		}

		private static class MergeToken extends Token {
			@Override
			protected void writeToken() throws IOException {
				data.index--;
				if (data.index >= 0 && data.index < data.align.size())
					data.output.fill(data.output.getLineStart() + data.align.get(data.index));
				data.index++;
			}

			@Override
			public Token alignement(int pos, ArrayList<Integer> lengths, int index) {
				/*if (index == lengths.size() -2)
					lengths.add(0);*/
				if (index == lengths.size() -1)
					lengths.add(0);
				return super.alignement(0, lengths, Math.min(index+1, lengths.size()));
			}
		}

		private static class ListToken<T> extends Token {
			public final List<T> list;
			public final Function<T,Token> getTokens;
			public final String end;

			public ListToken(List<T> list, Function<T,Token> getTokens, String end) {
				this.list = list;
				this.getTokens = getTokens;
				this.end = end;
			}

			@Override
			protected void writeToken() throws IOException {
				ArrayList<Token> tokens = new ArrayList<>();
				SharedData data = new SharedData(this.data.output);
				data.align.add(0);
				for(T t : list)
					tokens.add(getTokens.apply(t).alignement(0, data.align, 0));
				if (!tokens.isEmpty()){
					Token last = tokens.get(tokens.size() - 1);
					for(Token t : tokens){
						t.write(data);
						data.index = 0;
						if (t != last)
							this.data.output.w(end);
					}
				}
			}
		}
	}
}
