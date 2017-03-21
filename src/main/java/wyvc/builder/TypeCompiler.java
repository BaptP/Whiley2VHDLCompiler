package wyvc.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import wyvc.utils.Pair;
import wyvc.builder.LexicalElementTree.Tree;
import wyvc.builder.LexicalElementTree.Compound;
import wyvc.builder.LexicalElementTree.Primitive;
import wyvc.builder.VHDLCompileTask.VHDLCompilationException;
import wyvc.lang.Type;

public class TypeCompiler {

	public static class UnsupportedTypeException extends VHDLCompilationException {
		private static final long serialVersionUID = -814513156259175164L;
		private final wyil.lang.Type type;

		public UnsupportedTypeException(wyil.lang.Type type) {
			this.type = type;
		}

		@Override
		protected void details() {
			System.err.println("    Representation for type \""+type.toString()+"\" unknown "+type.getClass().getName());

		}

	}

	public static class NominalTypeException extends VHDLCompilationException {
		private static final long serialVersionUID = 3770868368103490124L;
		private final String typeName;

		public NominalTypeException(String typeName) {
			this.typeName = typeName;
		}

		@Override
		protected void details() {
			System.err.println("    Type \""+typeName+"\" unknown. Recursive definition ?");
		}
	}



	///////////////////////////////////////////////////////////////////////
	//                             Types                                 //
	///////////////////////////////////////////////////////////////////////

	public static interface TypeTree extends Tree<TypeTree,Type> {

	}

	public static class PrimitiveType extends Primitive<TypeTree,Type> implements TypeTree {
		public PrimitiveType(Type value) {
			super(value);
		}
	}

	public static class CompoundType extends Compound<TypeTree,Type> implements TypeTree {
		public CompoundType(List<Pair<String, TypeTree>> components) {
			super(components);
		}
	}




	public static final PrimitiveType SIGNED 	= new PrimitiveType(new Type.Signed(31,0));
	public static final PrimitiveType UNSIGNED 	= new PrimitiveType(new Type.Unsigned(31,0));
	public static final PrimitiveType BYTE 		= new PrimitiveType(new Type.Std_logic_vector(7,0));
	public static final PrimitiveType BOOL 		= new PrimitiveType(Type.Boolean);



	public static TypeTree compileType(wyil.lang.Type type, Map<String, TypeTree> types) throws UnsupportedTypeException, NominalTypeException {
		if (type == wyil.lang.Type.T_INT)
			return SIGNED;
		if (type == wyil.lang.Type.T_BOOL)
			return BOOL;
		if (type == wyil.lang.Type.T_BYTE)
			return BYTE;
		if (type instanceof wyil.lang.Type.Record) {
			wyil.lang.Type.Record record = (wyil.lang.Type.Record) type;
			ArrayList<Pair<String, TypeTree>> fields = new ArrayList<>(record.getFieldNames().length);
			for (String f : record.getFieldNames())
				fields.add(new Pair<String, TypeTree>(f, compileType(record.getField(f), types)));
			return new CompoundType(fields);
		}
		if (type instanceof wyil.lang.Type.Nominal) {
			String t = ((wyil.lang.Type.Nominal) type).name().name();
			if (! types.containsKey(t))
				throw new NominalTypeException(t);
			return types.get(t);
		}
		throw new UnsupportedTypeException(type);
	}
}
