package wyvc.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import wyvc.utils.Generator;
import wyvc.utils.Pair;
import wyvc.utils.Triple;
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
		public boolean equals(TypeTree other);
	}

	public static class PrimitiveType extends Primitive<TypeTree,Type> implements TypeTree {
		public PrimitiveType(Type value) {
			super(value);
		}

		@Override
		public String toString() {
			return value.toString();
		}

		@Override
		public boolean equals(TypeTree other) {
			return other instanceof PrimitiveType && value.equals(other.getValue());
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

		@Override
		public boolean equals(TypeTree other) {
			return other instanceof UnionType && isStructuredAs(other) && Utils.isAll(
				Utils.gather(components, other.getComponents()),
				(Pair<Pair<String, TypeTree>, Pair<String, TypeTree>> p) -> p.first.second.equals(p.second.second));
		}
	}


	public static class UnionType extends CompoundType {
		public final static String FLAG_PREFIX = "is_t";
		public final static String TYPE_PREFIX = "vl_t";

		public UnionType(List<TypeTree> types) {
			super(new Generator<Pair<String, TypeTree>>() {
				@Override
				protected void generate() throws InterruptedException {
					int k=0;
					for (TypeTree t : types){
						yield(new Pair<String, TypeTree>(FLAG_PREFIX+k, new PrimitiveType(Type.Boolean)));
						yield(new Pair<String, TypeTree>(TYPE_PREFIX+k++, t));
					}}}.toList());
		}

		public List<TypeTree> getOptions() {
			return new Generator<TypeTree>() {
				@Override
				protected void generate() throws InterruptedException {
					boolean k = false;
					for (Pair<String, TypeTree> p : getComponents()){
						if (k)
							yield(p.second);
						k = !k;
					}

				}
			}.toList();
		}

		public List<Triple<String,String,TypeTree>> getNamedOptions() {
			return new Generator<Triple<String,String,TypeTree>>() {
				@Override
				protected void generate() throws InterruptedException {
					boolean k = false;
					String n = "";
					for (Pair<String, TypeTree> p : getComponents()){
						if (k)
							yield(new Triple<>(n, p.first,p.second));
						else
							n = p.first;
						k = !k;
					}

				}
			}.toList();
		}

		@Override
		public boolean equals(TypeTree other) {
			return other instanceof UnionType && super.equals(other);
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
		if (type == wyil.lang.Type.T_NULL)
			return new CompoundType(Collections.emptyList());
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
		if (type instanceof wyil.lang.Type.Union) {
			wyil.lang.Type[] opts = ((wyil.lang.Type.Union) type).bounds();
			return new UnionType(Utils.checkedConvert(
					Arrays.asList(opts),
					(wyil.lang.Type t) -> compileType(logger, t, types)));
		}
		throw new CompilerException(new UnsupportedTypeCompilerError(type));
	}
}
