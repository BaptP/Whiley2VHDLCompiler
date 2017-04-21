package wyvc.lang;

import static wyvc.lang.LexicalElement.stringFromStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.lang.TypedValue.Port;
import wyvc.lang.TypedValue.Port.Mode;
import wyvc.lang.TypedValue.Signal;
import wyvc.lang.TypedValue.Variable;
import wyvc.lang.TypedValue.PortError;
import wyvc.lang.Expression.TypesMismatchException;
import wyvc.utils.Pair;

public interface Statement extends LexicalElement {

	public static interface ConcurrentStatement extends Statement {


	}

	public static interface SequentialStatement extends Statement {

	}
	public static class StatementGroup implements ConcurrentStatement {
		public final ConcurrentStatement[] statements;

		public StatementGroup(ConcurrentStatement[] statements) {
			this.statements = statements;
		}

		@Override
		public void addTokens(Token t) {
			t.n(statements);
		}

		@Override
		public String toString(){
			return stringFromStream(this);
		}

		public StatementGroup concat(StatementGroup other) {
			ArrayList<ConcurrentStatement> st = new ArrayList<>(statements.length + other.statements.length);
			st.addAll(Arrays.asList(statements));
			st.addAll(Arrays.asList(other.statements));
			return new StatementGroup(st.toArray(new ConcurrentStatement[0]));
		}

	}


	public static class SignalAssignment implements ConcurrentStatement, SequentialStatement {
		public final Signal dest;
		public final Expression expr;

		@Override
		public void addTokens(Token t) {
			t.n(dest.ident).align().n(" <= ").n(expr).semiColon();
		}

		@Override
		public String toString(){
			return stringFromStream(this);
		}

		public SignalAssignment(Signal dest, Expression expr) throws CompilerException {
			if (!dest.type.equals(expr.getType()))
				throw new CompilerException(new TypesMismatchException(SignalAssignment.class, dest.type, expr.getType()));
			if (dest instanceof Port && ((Port) dest).mode == Mode.IN)
				throw new CompilerException(new PortError(SignalAssignment.class, (Port)dest));
			this.dest = dest;
			this.expr = expr;
		}
	}

	public static class VariableAssignment implements SequentialStatement {
		public final Variable dest;
		public final Expression expr;

		@Override
		public void addTokens(Token t) {
			t.n(dest.ident).n(" := ").n(expr).semiColon();
		}

		@Override
		public String toString(){
			return stringFromStream(this);
		}

		public VariableAssignment(Variable dest, Expression expr) throws CompilerException {
			if (!dest.type.equals(expr.getType()))
				throw new CompilerException(new TypesMismatchException(SignalAssignment.class, dest.type, expr.getType()));
			this.dest = dest;
			this.expr = expr;
		}
	}

	public static class ComponentInstance implements ConcurrentStatement {
		public final Component component;
		public final Signal[] ports;
		public final String ident;

		public ComponentInstance(String ident, Component component, Signal[] ports) {
			this.component = component;
			this.ports = ports;
			this.ident = ident;
			// TODO vérifications : même nb de ports + bons types + in/out.
		}

		@Override
		public void addTokens(Token t) {
			t.merge().n(ident).n(": ").n(component.ident).n(" port map (").indent().endLine();
			ArrayList<Pair<Port, Signal>> connexions = new ArrayList<>();
			for (int k = 0; k < ports.length; ++k)
				connexions.add(new Pair<>(component.interface_.ports[k], ports[k]));
			t.n(connexions, (Pair<Port, Signal> p, Token to)
				-> to.n(p.first.ident).align().n(" => ").n(p.second.ident), ",\n");
			t.dedent().endLine().n(")").semiColon();
		}

		@Override
		public String toString(){
			return stringFromStream(this);
		}
	}

	public static class Process implements ConcurrentStatement {
		public final String ident;
		public final Variable[] variables;
		public final Signal[] signals;
		public final SequentialStatement[] statements;

		public Process(String ident, Variable[] variables, Signal[] signals, SequentialStatement[] statements) {
			this.ident = ident;
			this.variables = variables;
			this.signals = signals;
			this.statements = statements;
		}

		@Override
		public void addTokens(Token t) {
			t.n(ident+": process(").n(signals, (Signal s, Token to) -> to.n(s.ident), ", ").n(")").indent().endLine();
			t.n(variables).dedent().n("begin").indent().endLine();
			t.n(statements).dedent().n("end process "+ident).semiColon();
		}
	}


	public static class ConditionalSignalAssignment implements ConcurrentStatement {
		public final Signal signal;
		public final List<Pair<Expression, Expression>> cond;
		public final Expression defaultExpr;

		public ConditionalSignalAssignment(Signal signal, List<Pair<Expression, Expression>> cond, Expression defaultExpr) {
			this.signal = signal;
			this.cond = cond;
			this.defaultExpr = defaultExpr;
		}

		@Override
		public void addTokens(Token t) {
			t.n(signal.ident).align().n(" <= ");
			if (cond.size()!=1)
				t.endLine();
			/*
			String esp = cond.size() == 1 ? "" : "    ";
			t.n(cond, (Pair<Expression, Expression> p, Token to) -> to.n(esp).align().n(p.first).align().n(" when ").n(p.second).align().n(" else "), "\n");
			t.n("    ").n(defaultExpr).endLine();
			/*/
			t.endLine().n(cond, (Pair<Expression, Expression> p, Token to) -> to.n("    ").align().n(p.first).align().n(" when ").n(p.second).align().n(" else "), "\n");
			t.endLine().n("    ").n(defaultExpr).endLine();
			//*/
		}

		@Override
		public String toString(){
			return stringFromStream(this);
		}
	}

	public static class NotAStatement implements ConcurrentStatement, SequentialStatement {
		@Override
		public void addTokens(Token t) {
		}

	}
}
