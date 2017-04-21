package wyvc.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import wyvc.utils.Pair;
import wyvc.utils.Utils;
import wyvc.builder.LexicalElementTree.Tree;
import wyvc.builder.CompilerLogger.CompilerError;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.builder.LexicalElementTree.Compound;
import wyvc.builder.LexicalElementTree.Primitive;
import wyvc.lang.Type;

public class TypeCompiler {

	public static class UnsupportedTypeCompilerError extends CompilerError {
		private final wyil.lang.Type type;

		public UnsupportedTypeCompilerError(wyil.lang.Type type) {
			this.type = type;
		}

		@Override
		public String info() {
			return "Representation for type \""+type.toString()+"\" unknown "+type.getClass().getName();
		}

	}

	public static class NominalTypeCompilerError extends CompilerError {
		private final String typeName;

		public NominalTypeCompilerError(String typeName) {
			this.typeName = typeName;
		}

		@Override
		public String info() {
			return "Type \""+typeName+"\" unknown. Recursive definition ?";
		}
	}



	///////////////////////////////////////////////////////////////////////
	//                             Types                                 //
	///////////////////////////////////////////////////////////////////////

	public static interface TypeTree extends Tree<TypeTree,Type> {
		@Override
		public String toString();
	}

	public static class PrimitiveType extends Primitive<TypeTree,Type> implements TypeTree {
		public PrimitiveType(Type value) {
			super(value);
		}

		@Override
		public String toString() {
			return value.toString();
		}
	}

	public static class CompoundType extends Compound<TypeTree,Type> implements TypeTree {
		public CompoundType(List<Pair<String, TypeTree>> components) {
			super(components);
		}

		@Override
		public String toString() {
			return "{"+String.join(",", Utils.convert(components, (Pair<String,TypeTree> c) -> c.first+":"+c.second.toString()))+"}";
		}
	}




	public static final PrimitiveType SIGNED 	= new PrimitiveType(new Type.Signed(31,0));
	public static final PrimitiveType UNSIGNED 	= new PrimitiveType(new Type.Unsigned(31,0));
	public static final PrimitiveType BYTE 		= new PrimitiveType(new Type.Std_logic_vector(7,0));
	public static final PrimitiveType BOOL 		= new PrimitiveType(Type.Boolean);



	public static TypeTree compileType(CompilerLogger logger, wyil.lang.Type type, Map<String, TypeTree> types) throws CompilerException {
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
				fields.add(new Pair<String, TypeTree>(f, compileType(logger, record.getField(f), types)));
			return new CompoundType(fields);
		}
		if (type instanceof wyil.lang.Type.Nominal) {
			String t = ((wyil.lang.Type.Nominal) type).name().name();
			if (!types.containsKey(t))
				throw new CompilerException(new NominalTypeCompilerError(t));
			return types.get(t);
		}
		throw new CompilerException(new UnsupportedTypeCompilerError(type));
	}
}
