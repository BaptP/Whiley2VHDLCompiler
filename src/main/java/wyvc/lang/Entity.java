package wyvc.lang;


import java.util.ArrayList;


import wyvc.lang.LexicalElement.NamedElement;


public class Entity extends NamedElement {

	public final Interface interface_;

	private ArrayList<Architecture> architectures = new ArrayList<Architecture>();

	public final ArrayList<Architecture> getArchitectures() {
		return architectures;
	}

	public void addArchitectures(Architecture architecture) {
		architectures.add(architecture);
	}

	public Entity(String ident){
		super(ident);
		interface_ = new Interface();
	}


	public Entity(String ident, Interface interface_){
		super(ident);
		this.interface_ = interface_;
	}


	public Entity(String ident, Interface interface_, Architecture architecture){
		super(ident);
		this.interface_ = interface_;
		addArchitectures(architecture);
	}

	@Override
	public void addTokens(Token t) {
		t.comment().fill(60, '-').endLine();
		t.n(" Entity ").n(ident).code();
		t.n("library ieee").semiColon();
		t.n("use ieee.std_logic_1164.all").semiColon();
		t.n("use ieee.numeric_std.all").semiColon().endLine();
		t.n("entity ").n(ident).n(" is").endLine().indent();
		t.n(interface_);
		t.dedent().n("end entity ").n(ident).semiColon();
		t.n(architectures, "\n\n");
		t.comment().n(" Entity ").n(ident).endLine();
		t.fill(60, '-').endLine().code().endLine().endLine();
	}
}
