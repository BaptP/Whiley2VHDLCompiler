package wyvc.lang;

import static wyvc.lang.LexicalElement.stringFromStream;

import wyvc.lang.Type;
import wyvc.lang.TypedValue.Port;
import wyvc.lang.TypedValue.Port.Mode;

public class Interface implements LexicalElement {
	public final Port[] ports;

	public Interface(){
		this.ports = new Port[] {
			new Port("a", Type.Std_logic, Mode.IN),
			new Port("abon", Type.Std_logic, Mode.OUT)
		};
	}

	public Interface(Port[] ports){
		this.ports = ports;
	}


	@Override
	public void addTokens(Token t) {
		t.n("port (").indent().endLine();
		t.n(ports, ";\n");
		t.endLine().dedent().n(");").endLine();
	}

	@Override
	public String toString(){
		return stringFromStream(this);
	}
}
