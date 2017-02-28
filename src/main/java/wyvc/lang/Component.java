package wyvc.lang;


import wyvc.lang.LexicalElement.NamedElement;

public class Component extends NamedElement {
	public final Interface interface_;

	public Component(String ident, Interface interface_) {
		super(ident);
		this.interface_ = interface_;
	}

	@Override
	public void addTokens(Token t) {
		t.n("component ").n(ident).endLine().indent();
		t.n(interface_);
		t.dedent().n("end component ").n(ident).semiColon().endLine();
	}

}
