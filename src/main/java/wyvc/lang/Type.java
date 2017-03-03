package wyvc.lang;

import static wyvc.lang.LexicalElement.stringFromStream;


public abstract class Type implements LexicalElement {

	public abstract boolean equals(Type other);


	@Override
	public String toString(){
		return stringFromStream(this);
	}


	public static abstract class TypeException extends VHDLException {
		private static final long serialVersionUID = 3877750530770083046L;
		private final Type type;

		public TypeException(Class<?> element, Type type) {
			super(element);
			this.type = type;
		}

		@Override
		protected final void details() {
			System.err.print("    Given type "+type.toString() + " unexpected : ");
			typeExceptionDetails();
		}

		protected abstract void typeExceptionDetails();
	}


	public static enum Primitive {
		P_STD_LOGIC,
		P_BOOLEAN
	}

	private static final class PrimitiveType extends Type {
		public final Primitive type;
		public PrimitiveType(Primitive type){
			this.type = type;
		}

		@Override
		public void addTokens(Token t) {
			switch (type) {
			case P_STD_LOGIC:
				t.n("std_logic");
				break;
			case P_BOOLEAN:
				t.n("boolean");
				break;
			default:
				t.n("unknown");
				break;
			}
		}

		@Override
		public boolean equals(Type other) {
			return other instanceof PrimitiveType && ((PrimitiveType)other).type == type;
		}
	}

	public static final PrimitiveType Std_logic = new PrimitiveType(Primitive.P_STD_LOGIC);
	public static final PrimitiveType Boolean = new PrimitiveType(Primitive.P_BOOLEAN);


	public static abstract class VectorType extends Type {
		public final int start;
		public final int end;

		protected VectorType(int start, int end){
			// TODO empêcher vecteur taille 1
			this.start = start;
			this.end = end;
		}

		public final int lenght(){
			return Math.abs(end - start)+1;
		}

		public final boolean isAscendant() {
			return start < end;
		}

		protected abstract Token addSubTypeTokens(Token t);

		@Override
		public void addTokens(Token t) {
			t = addSubTypeTokens(t);
			t.n("(").n(start).n(end <= start ? " downto " : " to ").n(end).n(")");
		}

		public abstract boolean isSameVectorType(VectorType other);
		public abstract VectorType cloneType(int start, int end);

		@Override
		public final boolean equals(Type other){
			if (other instanceof VectorType){
				VectorType vectorType = (VectorType) other;
				return vectorType.lenght() == lenght() && vectorType.isAscendant() == isAscendant()
						&& isSameVectorType(vectorType);
			}
			return  false;
		}
	}

	public static final class Std_logic_vector extends VectorType {

		public Std_logic_vector(int start, int end) {
			super(start, end);
		}

		@Override
		protected Token addSubTypeTokens(Token t) {
			return t.n("std_logic_vector");
		}

		@Override
		public boolean isSameVectorType(VectorType other) {
			return other instanceof Std_logic_vector;
		}

		@Override
		public VectorType cloneType(int start, int end){
			return new Std_logic_vector(start, end);
		}
	}

	public static final class Unsigned extends VectorType {

		public Unsigned(int start, int end) {
			super(start, end);
		}

		@Override
		protected Token addSubTypeTokens(Token t) {
			return t.n("unsigned");
		}

		@Override
		public boolean isSameVectorType(VectorType other) {
			return other instanceof Unsigned;
		}

		@Override
		public VectorType cloneType(int start, int end){
			return new Unsigned(start, end);
		}
	}

	public static final class Signed extends VectorType {

		public Signed(int start, int end) {
			super(start, end);
		}

		@Override
		protected Token addSubTypeTokens(Token t) {
			return t.n("signed");
		}

		@Override
		public boolean isSameVectorType(VectorType other) {
			return other instanceof Signed;
		}

		@Override
		public VectorType cloneType(int start, int end){
			return new Signed(start, end);
		}
	}

}
