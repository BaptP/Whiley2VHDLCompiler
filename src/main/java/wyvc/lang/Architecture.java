package wyvc.lang;

import wyvc.lang.TypedValue.Signal;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.lang.LexicalElement.NamedElement;
import wyvc.lang.Statement.ConcurrentStatement;
import wyvc.lang.TypedValue.Constant;

public class Architecture extends NamedElement {
	public final Entity entity;
	public final Signal[] signals;
	public final Constant[] constants;
	public final Component[] components;
	public /*final*/ ConcurrentStatement[] statements;

	public Architecture(Entity entity, String ident) throws CompilerException {
		super(ident);
		this.entity = entity;
/*
		// TODO Temporaire
		signals = new Signal[] {
			new Signal("oui", new Type.Signed(7,0)),
			new Signal("non", Type.Std_logic)
		};
		constants = new Constant[] {
			new Constant("FIXE", new Type.Signed(7,0)),
			new Constant("INVARIABLE", Type.Std_logic)
		};

		components = new Component[] {
			new Component("Rtn"),
			new Component("Hummmm")
		};

		statements = new ConcurrentStatement[] {
			new SignalAssignment(
				entity.interface_.ports[1],
				new And(
					new Access(constants[1]),
					new Access(entity.interface_.ports[0])))
		};
*/
		signals = new Signal[0];
		constants = new Constant[0];
		components = new Component[0];
	}

	public Architecture(Entity entity, String ident, Signal[] signals, Constant[] constants,
			Component[] components, ConcurrentStatement[] statements) {
		super(ident);
		this.entity = entity;
		this.signals = signals;
		this.constants = constants;
		this.components = components;
		this.statements = statements;
	}

	@Override
	public void addTokens(Token t) {
		t.endLine().endLine();
		t.n("architecture ").n(ident).n(" of ");
		t.n(entity.ident).n(" is").indent().endLine();
		t.n(components, "\n").endLine().n(signals).endLine().n(constants);
		t.dedent().n("begin").indent().endLine().n(statements, "\n");
		t.dedent().n("end architecture ").n(ident).semiColon();
	}

}
