package wyvc.lang;

import wyvc.builder.CompilerLogger.CompilerException;

/**
 * <p>
 * The VHDLFile class represents the root of the abstract syntax tree of a VHDL
 * file.
 *
 * </p>
 *
 * @author Baptiste Pauget
 *
 */
public class VHDLFile implements LexicalElement {
	public final Entity[] entities;

	public VHDLFile() throws CompilerException {
		Entity entity = new Entity("Bonjour");
		entities = new Entity[] {entity};
		entity.addArchitectures(new Architecture(entity, "Behavioural"));
	}

	public VHDLFile(Entity[] entities) {
		this.entities = entities;
	}

	@Override
	public final void addTokens(Token t) {
		t.comment().fill(60, '-').endLine();
		t.fill(15).n("VHDL file generated by").fill(58).n("--").endLine();
		t.fill(16).n("Whiley2VHDLCompiler").fill(58).n("--").endLine();
		t.fill(60, '-').endLine();
		t.code().endLine();
		t.n(entities);
	}
}
