package wyvc.lang;


import wyvc.lang.TypedValue.Port;
import wyvc.lang.TypedValue.Port.Mode;
import wyvc.lang.TypedValue.PortError;

import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;

import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.lang.Type.Signed;
import wyvc.lang.Type.TypeError;
import wyvc.lang.Type.Unsigned;
import wyvc.lang.Type.VectorType;


public interface Expression extends LexicalElement {
	public Type getType();
	public Precedence getPrecedence();

	public final class TypesMismatchException extends TypeError {
		private final Type expected;

		public TypesMismatchException(Class<?> element, Type expected, Type given) {
			super(element, given);
			this.expected = expected;
		}

		@Override
		protected String typeExceptionDetails() {
			return expected + " expected";
		}

	}


	public static enum Precedence {
		COMPARISON,
		LOGICAL_OP,
		SHIFT_OP,
		ADDITIVE_OP,
		UNARY_SIGN,
		MULTIPL_OP,
		UNARY_NOT,
		FUNC_CALL,
		SUB_VECTOR,
		VAR_ACCESS;

	}

	public static abstract class UnaryOperation extends TypedElement implements Expression {
		private final String op;
		public final Expression arg;

		protected UnaryOperation(String op, Expression arg, Type type){
			super(type);
			this.op = op;
			this.arg = arg;
		}

		@Override
		public final void addTokens(Token t) {
			t.n(op).n("(").n(arg).n(")");
		}


		private static final Type getType(Type t) throws CompilerException{
			if (t == Type.Boolean)
				return t;
			throw new CompilerException(new TypesMismatchException(UnaryOperation.class, t, t));
		}
	}


	public static final class Not extends UnaryOperation {
		public Not(Expression arg) throws CompilerException {
			super("not", arg, UnaryOperation.getType(arg.getType()));
		}

		@Override
		public Precedence getPrecedence() {
			return Precedence.UNARY_NOT;
		}
	}

	public static abstract class BinaryOperation extends TypedElement implements Expression {
		private final String op;
		public final Expression arg1, arg2;
		public final Precedence precedence;

		protected BinaryOperation(Expression arg1, String op, Expression arg2, Precedence precedence, Type type){
			super(type);
			this.op = op;
			this.arg1 = arg1;
			this.arg2 = arg2;
			this.precedence = precedence;
		}

		@Override
		public final void addTokens(Token t) {
			int p = getPrecedence().ordinal();
			int p1 = arg1.getPrecedence().ordinal();
			int p2 = arg2.getPrecedence().ordinal();
			if (p > p1 || true)
				t.n("(").n(arg1).n(")");
			else
				t.n(arg1);
			t.n(" "+op+" ");
			if (p2 > p && false)
				t.n(arg2);
			else
				t.n("(").n(arg2).n(")");
		}

		@Override
		public Precedence getPrecedence() {
			return precedence;
		}
	}

	public static abstract class LogicalBinaryOperation extends BinaryOperation {
		public LogicalBinaryOperation(Expression arg1, String op, Expression arg2) throws CompilerException {
				super(arg1, op, arg2, Precedence.LOGICAL_OP, getType(arg1.getType(), arg2.getType()));
			}

		private static final Type getType(Type t1, Type t2) throws CompilerException{
			if (t1.equals(t2))
				return t1;
			if (t1 instanceof VectorType && t2 instanceof VectorType) {
				VectorType vt1 = (VectorType) t1;
				VectorType vt2 = (VectorType) t2;
				if (vt1.isSameVectorType(vt2) && !(vt1.isAscendant()^vt2.isAscendant()))
					return vt1.isAscendant() ? vt1.cloneType(0, Math.max(vt1.lenght(), vt2.lenght())-1)
					                         : vt1.cloneType(Math.max(vt1.lenght(), vt2.lenght())-1, 0);
				throw new CompilerException(new TypesMismatchException(LogicalBinaryOperation.class, vt1, vt2));
			}
			throw new CompilerException(new TypesMismatchException(LogicalBinaryOperation.class, t1, t2));
		}
	}

	public static final class And extends LogicalBinaryOperation {
		public And(Expression arg1, Expression arg2) throws CompilerException {
			super(arg1, "and", arg2);
		}
	}

	public static final class Nand extends LogicalBinaryOperation {
		public Nand(Expression arg1, Expression arg2) throws CompilerException {
			super(arg1, "nand", arg2);
		}
	}

	public static final class Or extends LogicalBinaryOperation {
		public Or(Expression arg1, Expression arg2) throws CompilerException {
			super(arg1, "or", arg2);
		}
	}

	public static final class Nor extends LogicalBinaryOperation {
		public Nor(Expression arg1, Expression arg2) throws CompilerException {
			super(arg1, "nor", arg2);
		}
	}

	public static final class Xor extends LogicalBinaryOperation {
		public Xor(Expression arg1, Expression arg2) throws CompilerException {
			super(arg1, "xor", arg2);
		}
	}

	public static final class Xnor extends LogicalBinaryOperation {
		public Xnor(Expression arg1, Expression arg2) throws CompilerException {
			super(arg1, "xnor", arg2);
		}
	}

	public static abstract class ComparisonOperation extends BinaryOperation {
		public ComparisonOperation(Expression arg1, String op, Expression arg2) throws CompilerException {
				super(arg1, op, arg2, Precedence.COMPARISON, getType(arg1.getType(), arg2.getType()));
			}

		private static final Type getType(Type t1, Type t2) throws CompilerException{
			if (t1.equals(t2))
				return Type.Boolean;
			throw new CompilerException(new TypesMismatchException(ComparisonOperation.class, t1, t2));
		}
	}

	public static final class Eq extends ComparisonOperation {
		public Eq(Expression arg1, Expression arg2) throws CompilerException {
			super(arg1, "=", arg2);
		}
	}

	public static final class Ne extends ComparisonOperation {
		public Ne(Expression arg1, Expression arg2) throws CompilerException {
			super(arg1, "/=", arg2);
		}
	}

	public static final class Gt extends ComparisonOperation {
		public Gt(Expression arg1, Expression arg2) throws CompilerException {
			super(arg1, ">", arg2);
		}
	}

	public static final class Ge extends ComparisonOperation {
		public Ge(Expression arg1, Expression arg2) throws CompilerException {
			super(arg1, ">=", arg2);
		}
	}

	public static final class Lt extends ComparisonOperation {
		public Lt(Expression arg1, Expression arg2) throws CompilerException {
			super(arg1, "<", arg2);
		}
	}

	public static final class Le extends ComparisonOperation {
		public Le(Expression arg1, Expression arg2) throws CompilerException {
			super(arg1, "<=", arg2);
		}
	}




	public static abstract class AdditiveBinaryOperation extends BinaryOperation {
		public AdditiveBinaryOperation(Expression arg1, String op, Expression arg2) throws CompilerException {
				super(arg1, op, arg2, Precedence.ADDITIVE_OP, getType(arg1.getType(), arg2.getType()));
			}

		private static final Type getType(Type t1, Type t2) throws CompilerException{
			if (t1 instanceof Unsigned && t2 instanceof Unsigned &&
					!(((Unsigned) t1).isAscendant() ^ ((Unsigned) t2).isAscendant())) {
				Unsigned vt1 = (Unsigned) t1;
				Unsigned vt2 = (Unsigned) t2;
				return vt1.isAscendant() ? new Unsigned(0, Math.max(vt1.lenght(), vt2.lenght())-1)
				                         : new Unsigned(Math.max(vt1.lenght(), vt2.lenght())-1, 0);
			}
			if (t1 instanceof Signed && t2 instanceof Signed &&
					!(((Signed) t1).isAscendant() ^ ((Signed) t2).isAscendant())) {
				Signed vt1 = (Signed) t1;
				Signed vt2 = (Signed) t2;
				return vt1.isAscendant() ? new Signed(0, Math.max(vt1.lenght(), vt2.lenght())-1)
				                         : new Signed(Math.max(vt1.lenght(), vt2.lenght())-1, 0);
			}
			throw new CompilerException(new TypesMismatchException(LogicalBinaryOperation.class, t1, t2));
		}
	}

	public static final class Add extends AdditiveBinaryOperation {
		public Add(Expression arg1, Expression arg2) throws CompilerException {
			super(arg1, "+", arg2);
		}
	}

	public static final class Sub extends AdditiveBinaryOperation {
		public Sub(Expression arg1, Expression arg2) throws CompilerException {
			super(arg1, "-", arg2);
		}
	}


	public static final class Mul extends BinaryOperation {

		public Mul(Expression arg1, Expression arg2) throws CompilerException {
			super(arg1, "*", arg2, Precedence.MULTIPL_OP, getType(arg1.getType(), arg2.getType()));
		}


		private static Type getType(Type t1, Type t2) throws CompilerException {
			if (t1 instanceof Unsigned && t2 instanceof Unsigned)
				return new Unsigned(((Unsigned)t1).lenght() + ((Unsigned)t2).lenght() - 1, 0);
			if (t1 instanceof Signed && t2 instanceof Signed)
				return new Signed(((Signed)t1).lenght() + ((Signed)t2).lenght() - 1, 0);
			throw new CompilerException(new TypesMismatchException(Mul.class, t1, t2));
		}
	}
/*
	public static abstract class MultiplicativeBinaryOperation extends BinaryOperation {
		public MultiplicativeBinaryOperation(Expression arg1, String op, Expression arg2, Type type) throws TypesMismatchException {
			super(arg1, op, arg2, Precedence.MULTIPL_OP, type);
		}

		public static final VectorType mulType(VectorType t1, VectorType t2) throws TypesMismatchException {
			if (t1.isAscendant() ^ t2.isAscendant())
				throw new TypesMismatchException(MultiplicativeBinaryOperation.class, t2.cloneType(t2.end, t2.start), t2);
			int l = t1.lenght()+t2.lenght()-1;
			if (t1 instanceof Unsigned && t2 instanceof Unsigned)
				return t1.isAscendant() ? new Unsigned(0, l) : new Unsigned(l, 0);
			if (t1 instanceof Signed && t2 instanceof Signed)
				return t1.isAscendant() ? new Signed(0, l) : new Signed(l, 0);
			throw new TypesMismatchException(MultiplicativeBinaryOperation.class, t1.cloneType(t2.start, t2.end), t2);
		}
	}

	public static final class Mul extends MultiplicativeBinaryOperation {
		public Mul(Expression arg1, Expression arg2) throws TypesMismatchException {
			super(arg1, "*", arg2, mulType(arg1.getType(), arg2.getType()));
		}
	}

	public static final class Div extends MultiplicativeBinaryOperation {

	}

	public static final class Mod extends MultiplicativeBinaryOperation {

	}

	public static final class Rem extends MultiplicativeBinaryOperation {

	}
*/
	public static class SubVectorTypeCompilerError extends TypeError {
		private final int start, end;

		public SubVectorTypeCompilerError(Type type, int start, int end) {
			super(SubVector.class, type);
		this.start = start;
		this.end = end;
		}

		@Override
		protected String typeExceptionDetails() {
			return "Taking a "+start+" to "+end+" subvector impossible";
		}

	}

	public static class SubVector extends TypedElement implements Expression {
		private final Expression vector;
		private final int start, end;

		public SubVector(Expression vector, int start, int end) throws CompilerException {
			super(getType(vector.getType(), start, end));
			this.vector = vector;
			this.start = start;
			this.end = end;
		}

		private static Type getType(Type type, int start, int end) throws CompilerException {
			if (!(type instanceof VectorType))
				throw new CompilerException(new SubVectorTypeCompilerError(type, start, end));
			VectorType vector = (VectorType) type;
			BiPredicate<VectorType, Integer> in = (VectorType t, Integer i) -> Math.min(t.start, t.end) <= i && Math.max(t.start, t.end) >= end;
			if (!in.test(vector, start) || !in.test(vector, end))
				throw new CompilerException(new SubVectorTypeCompilerError(type, start, end));
			return vector.cloneType(start, end);
		}

		@Override
		public void addTokens(Token t) {
			if (getPrecedence().ordinal() > vector.getPrecedence().ordinal())
				t.n("(").n(vector).n(")");
			else
				t.n(vector);
			t.n("(").n(start).n(end <= start ? " downto " : " to ").n(end).n(")");
		}

		@Override
		public Precedence getPrecedence() {
			return Precedence.SUB_VECTOR;
		}
	}

	public static class Access extends TypedElement implements Expression {
		public final TypedValue value;

		public Access(TypedValue value) throws CompilerException {
			super(value.type);
			this.value = value;
			if (value instanceof Port && ((Port) value).mode == Mode.OUT)
				throw new CompilerException(new PortError(Access.class, (Port) value));
		}

		@Override
		public Precedence getPrecedence() {
			return Precedence.VAR_ACCESS;
		}

		@Override
		public void addTokens(Token t) {
			t.n(value.ident);
		}

	}

	public static class Value extends TypedElement implements Expression {
		public final String value;

		public Value(Type type, String value) {
			super(type);
			if (type instanceof Type.Signed)
				this.value = "to_signed("+value+", "+((Type.Signed) type).lenght()+")";
			else /*if (type instanceof Type.Std_logic_vector)
				this.value = "\""+value.substring(0, value.length()-1)+"\"";
			else*/
				this.value = value;
		}

		@Override
		public void addTokens(Token t) {
			t.n(value);
		}

		@Override
		public Precedence getPrecedence() {
			return Precedence.VAR_ACCESS;
		}


	}

	public static class FunctionCall implements Expression {
		private final Type type;
		private final String funcName;
		private final List<Expression> args;

		public FunctionCall(Type type, String funcName, List<Expression> args) {
			this.type = type;
			this.funcName = funcName;
			this.args = args;
		}
		public FunctionCall(Type type, String funcName, Expression arg) {
			this(type, funcName, Collections.singletonList(arg));
		}

		@Override
		public void addTokens(Token t) {
			t.n(funcName).n("(").n(args.get(0)).n(args.subList(1, args.size()), (e,to) -> to.n(",").n(e), "").n(")");

		}

		@Override
		public Type getType() {
			return type;
		}

		@Override
		public Precedence getPrecedence() {
			return Precedence.FUNC_CALL;
		}

	}

}
