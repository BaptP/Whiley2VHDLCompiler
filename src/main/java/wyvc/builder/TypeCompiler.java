package wyvc.builder;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import wyil.lang.WyilFile;
import wyvc.utils.Generator;
import wyvc.utils.Generator.CheckedGenerator;
import wyvc.utils.Generator.StandardGenerator;
import wyvc.utils.Generator.StandardPairGenerator;
import wyvc.utils.Pair;
import wyvc.builder.CompilerLogger.CompilerDebug;
import wyvc.builder.CompilerLogger.CompilerError;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.builder.CompilerLogger.CompilerNotice;
import wyvc.lang.Type;

public class TypeCompiler extends LexicalElementTree {

	/*------- Abstract Type Analysis -------*/

	private static interface AbstractType {

		/**
		 * Represents some relations that can exist between two sets.
		 *
		 * @author Baptiste Pauget
		 *
		 */
		public static enum Order {
			Greater,
			Equal,
			Lesser,
			Disjointed,
			Unknown,
			DisGreater,
			DisEqual,
			DisLesser;

			/** A/B => B/A **/
			private static final Order[] Opposite =
				{	Lesser, 	Equal, 		Greater,	Disjointed,	Unknown,	DisLesser,	DisEqual,	DisGreater	};
			public Order opposite() {
				return Opposite[ordinal()];
			}

			/** A/B => (!A)/(!B) **/
			private static final Order[] Negation =
				{	Lesser, 	Equal, 		Greater, 	Unknown, 	Unknown,	Lesser, 	Equal, 		Greater		};
			public Order negation() {
				return Negation[ordinal()];
			}

			/** A/B => (!A)/B **/
			private static final Order[] SemiNegation =
				{	Disjointed,	Disjointed,	Unknown,	Greater,	Unknown,	DisGreater,	Greater,	Greater		};
			public Order semiNegation() {
				return SemiNegation[ordinal()];
			}

			/** A/C && B/D => (A,B)/(C,D) **/
			private static final Order[][] Conjunction = {
				{	Greater,	Greater, 	Unknown,	Disjointed,	Unknown,	DisGreater,	DisEqual,	DisLesser	},
				{	Greater,	Equal, 		Lesser, 	Disjointed,	Unknown,	DisGreater,	DisEqual,	DisLesser	},
				{	Unknown,	Lesser,		Lesser,		Disjointed,	Unknown,	DisGreater,	DisEqual,	DisLesser	},
				{	Disjointed,	Disjointed,	Disjointed, Disjointed,	Disjointed,	DisGreater,	DisEqual,	DisLesser	},
				{	Unknown, 	Unknown,	Unknown, 	Disjointed,	Unknown,	DisGreater,	DisEqual,	DisLesser	},
				{	DisGreater,	DisGreater,	DisGreater,	DisGreater,	DisGreater,	DisGreater,	DisEqual,	DisEqual	},
				{	DisEqual, 	DisEqual,	DisEqual, 	DisEqual,	DisEqual,	DisEqual,	DisEqual,	DisEqual	},
				{	DisLesser, 	DisLesser,	DisLesser, 	DisLesser,	DisLesser,	DisEqual,	DisEqual,	DisLesser	}
			};
			public Order conjunction(Order other) {
				return Conjunction[ordinal()][other.ordinal()];
			}

			/** A/C && B/C => (A|B)/C **/
			private static final Order[][] Union = {
				{	Greater, 	Greater, 	Greater, 	Greater,	Greater,	DisGreater,	DisGreater,	Greater		},
				{	Greater, 	Equal, 		Equal, 		Greater,	Greater,	DisGreater,	DisEqual,	Equal		},
				{	Greater, 	Equal, 		Lesser, 	Unknown,	Unknown,	DisGreater,	DisEqual,	Lesser		},
				{	Greater, 	Greater, 	Unknown,	Disjointed,	Unknown,	DisGreater,	DisGreater,	Disjointed	},
				{	Greater, 	Greater, 	Unknown, 	Unknown,	Unknown,	DisGreater,	DisGreater,	Unknown		},
				{	DisGreater,	DisGreater,	DisGreater,	DisGreater,	DisGreater,	DisGreater,	DisGreater,	DisGreater	},
				{	DisGreater,	DisEqual,	DisEqual,	DisGreater,	DisGreater,	DisGreater,	DisEqual,	DisEqual	},
				{	Greater, 	Equal, 		Lesser, 	Disjointed,	Unknown,	DisGreater,	DisEqual,	DisLesser	}
			};
			public Order union(Order other) {
				return Union[ordinal()][other.ordinal()];
			}

			/** A/C && B/C => (A&B)/C **/
			private static final Order[][] Intersection = {
				{	Greater, 	Equal, 		Lesser,		Disjointed,	Unknown,	DisGreater,	DisEqual,	DisLesser	},
				{	Equal, 		Equal, 		Lesser, 	Lesser,		Lesser,		DisEqual,	DisEqual,	DisLesser	},
				{	Lesser, 	Lesser, 	Lesser, 	Lesser,		Lesser,		DisEqual,	DisEqual,	DisLesser	},
				{	Disjointed,	Lesser,		Lesser,		Disjointed,	Disjointed,	DisGreater,	DisEqual,	DisLesser	},
				{	Unknown, 	Lesser, 	Lesser, 	Disjointed,	Unknown,	DisGreater,	DisEqual,	DisLesser	},
				{	DisGreater,	DisEqual,	DisEqual,	DisGreater,	DisGreater,	DisGreater,	DisEqual,	DisEqual	},
				{	DisEqual,	DisEqual,	DisEqual,	DisEqual,	DisEqual,	DisEqual,	DisEqual,	DisEqual	},
				{	DisLesser,	DisLesser,	DisLesser,	DisLesser,	DisLesser,	DisEqual,	DisEqual,	DisLesser	}
			};
			public Order intersection(Order other) {
				return Intersection[ordinal()][other.ordinal()];
			}

			/** A/B && B/A => A/B **/
			private static final Order[][] Precise = {
				{	Equal, 		Equal,		Greater,	DisGreater,	Greater,	DisEqual,	DisEqual,	DisGreater	},
				{	Equal, 		Equal, 		Equal, 		DisEqual,	Equal,		DisEqual,	DisEqual,	DisEqual	},
				{	Lesser, 	Equal, 		Equal, 		Disjointed,	Lesser,		DisLesser,	DisEqual,	DisEqual	},
				{	DisLesser,	DisEqual,	Disjointed,	Disjointed,	Disjointed,	DisLesser,	DisEqual,	DisGreater	},
				{	Lesser, 	Equal, 		Greater, 	Disjointed,	Unknown,	DisLesser,	DisEqual,	DisGreater	},
				{	DisEqual,	DisEqual,	DisGreater,	DisGreater,	DisGreater,	DisEqual,	DisEqual,	DisGreater	},
				{	DisEqual,	DisEqual,	DisEqual,	DisEqual,	DisEqual,	DisEqual,	DisEqual,	DisEqual	},
				{	DisLesser,	DisEqual,	DisEqual,	DisLesser,	DisLesser,	DisLesser,	DisEqual,	DisEqual	}
			};
			public Order precise(Order other) {
				return Precise[ordinal()][other.ordinal()];
			}




		}



		public boolean isFinite();
		public String toString(String prefix1, String prefix2);
		public CanonicalUnion toCanonicalForm();
		public Order compareWith(AbstractType other);
		public default Order dualCompareWith(AbstractType other) {
			return compareWith(other).precise(other.compareWith(this));
		}

		public default String toString(String prefix) {
			return toString(prefix, prefix);
		}
	}

	private static interface CanonicalType extends AbstractType {
		public AbstractType simplify();
	}

	private static interface CanonicalTypeOrNegation extends CanonicalType {

	}

	private static interface CanonicalTypeOrRecord extends CanonicalTypeOrNegation {

	}

	private abstract class SimpleTypes implements AbstractType,  CanonicalTypeOrRecord {
		public final String name;

		public SimpleTypes(String name) {
			this.name = name;
		}

		@Override
		public String toString(String prefix1, String prefix2) {
			return prefix1 + name +"\n";
		}

		@Override
		public CanonicalUnion toCanonicalForm() {
			return new CanonicalUnion(new CanonicalIntersection(this));
		}

		@Override
		public AbstractType simplify() {
			return this;
		}

		@Override
		public Order compareWith(AbstractType other) {
			if (other == this)
				return Order.Equal;
			if (other == Any)
				return Order.Lesser;
			if (other == Void)
				return Order.DisGreater;
			if (other instanceof Union || other instanceof Intersection || other instanceof Negation)
				return other.compareWith(this).opposite();
			return Order.Disjointed;
		}
	}

	private final SimpleTypes Any = new SimpleTypes("Any"){
		@Override
		public boolean isFinite() {
			return false;
		}

		@Override
		public Order compareWith(AbstractType other) {
			if (other == this)
				return Order.Equal;
			return Order.Greater;
		}
	};
	private final SimpleTypes Null = new SimpleTypes("Null"){
		@Override
		public boolean isFinite() {
			return true;
		}
	};

	private final SimpleTypes Bool = new SimpleTypes("Bool"){
		@Override
		public boolean isFinite() {
			return true;
		}
	};

	private final SimpleTypes Byte = new SimpleTypes("Byte"){
		@Override
		public boolean isFinite() {
			return true;
		}
	};

	private final SimpleTypes Int = new SimpleTypes("Int"){
		@Override
		public boolean isFinite() {
			return true;
		}
	};

	private final SimpleTypes Void = new SimpleTypes("Void"){
		@Override
		public boolean isFinite() {
			return true;
		}

		@Override
		public Order compareWith(AbstractType other) {
			if (other == this)
				return Order.DisEqual;
			return Order.DisLesser;
		}
	};

	private abstract class ConstructType implements AbstractType {
		protected abstract int getNumberOfComponents();
		protected abstract StandardGenerator<Pair<String, AbstractType>> getComponents();

		@Override
		public String toString(String prefix1, String prefix2) {
			final int l = getNumberOfComponents();
			return prefix1+this.getClass().getSimpleName()+"\n"+
			getComponents().enumerate().fold((String a, Pair<Integer, Pair<String, AbstractType>> c) ->
			a+c.second.second.toString(
				prefix2 + (c.first+1 == l ? " └─ " : " ├─ ") + (c.second.first.length() == 0 ? "" : c.second.first+" : "),
				prefix2 + (c.first+1 == l ? "   " : " │ ")), "");
		}
	}

	private class Record<T extends AbstractType> extends ConstructType {
		public final List<Pair<String,T>> fields;

		public Record(StandardGenerator<Pair<String,T>> fields) {
			this.fields = fields.toList();
		}

		public <E extends Exception> Record(CheckedGenerator<Pair<String,T>, E> fields) throws E {
			this.fields = fields.toList();
		}

		StandardPairGenerator<String, T> getFields() {
			return Generator.fromPairCollection(this.fields);
		}

		@Override
		public boolean isFinite() {
			return getFields().takeSecond().ForAll(AbstractType::isFinite);
		}

		@Override
		public CanonicalUnion toCanonicalForm() {
			return new CanonicalUnion(new CanonicalIntersection(new CanonicalRecord(getFields().mapSecond(AbstractType::toCanonicalForm))));
		}

		@Override
		protected int getNumberOfComponents() {
			return fields.size();
		}
		@Override
		protected StandardGenerator<Pair<String, AbstractType>> getComponents() {
			return getFields().map(Pair<String, AbstractType>::new);
		}

		public <U extends AbstractType> Order compareWithHelper(Record<U> other) {
			return other.fields.size() != fields.size() || !getFields().takeFirst().gather(
				other.getFields().takeFirst()).forAll(String::equals)
					? Order.Disjointed
					: getFields().takeSecond().gather(other.getFields().takeSecond()).map(
						AbstractType::dualCompareWith).fold(Order::conjunction, Order.Equal);
		}

		@Override
		public Order compareWith(AbstractType other) {
			if (other instanceof Record)
				return compareWithHelper((Record<?>) other);
			return other.compareWith(this).opposite();
		}
	}

	private class CanonicalRecord extends Record<CanonicalUnion> implements CanonicalTypeOrRecord {
		public CanonicalRecord(StandardGenerator<Pair<String,CanonicalUnion>> fields) {
			super(fields);
		}

		@Override
		public AbstractType simplify() {
			List<Pair<String, AbstractType>> fields = getFields().mapSecond(CanonicalType::simplify).toList();
			if (Generator.fromPairCollection(fields).takeSecond().find(Void) != null)
				return Void;
			return new Record<>(Generator.fromCollection(fields));
		}
	}

	private class Union<T extends AbstractType> extends ConstructType {
		protected final List<T> options;

		public Union(T option) {
			this.options = Collections.singletonList(option);
		}

		public Union(StandardGenerator<T> options) {
			this.options = options.toList();
		}

		public <E extends Exception> Union(CheckedGenerator<T, E> options) throws E {
			this.options = options.toList();
		}

		StandardGenerator<T> getOptions() {
			return Generator.fromCollection(this.options);
		}

		@Override
		public boolean isFinite() {
			return getOptions().ForAll(AbstractType::isFinite);
		}

		@Override
		public CanonicalUnion toCanonicalForm() {
			return new CanonicalUnion(Generator.concat(getOptions().map(AbstractType::toCanonicalForm).map(Union::getOptions)));
		}

		@Override
		protected int getNumberOfComponents() {
			return options.size();
		}

		@Override
		protected StandardGenerator<Pair<String, AbstractType>> getComponents() {
			return getOptions().map((AbstractType t) -> new Pair<>("",t));
		}

		@Override
		public Order compareWith(AbstractType other) {
			return getOptions().map(other::dualCompareWith).map(Order::opposite).fold(Order::union, Order.DisLesser);
		}

	}

	private class CanonicalUnion extends Union<CanonicalIntersection> implements CanonicalType {

		public CanonicalUnion(CanonicalIntersection option) {
			super(option);
		}

		public CanonicalUnion(StandardGenerator<CanonicalIntersection> options) {
			super(options);
		}


		@Override
		public AbstractType simplify() {
			List<AbstractType> options = getOptions().map(CanonicalType::simplify).toList();
			Set<AbstractType> utils = new HashSet<>();
			utils.addAll(options);
			if (options.isEmpty())
				return Void;
			if (options.size() == 1)
				return options.get(0);
			final int noo = options.size();
			for (int k = 0; k < noo; ++k)	{
				AbstractType t = options.get(k);
				if (t.equals(Void))
					return Void;
				for (int i = k+1; i < noo; ++i) {
					AbstractType u = options.get(i);
					if (utils.contains(u)){
						switch (t.dualCompareWith(u)) {
						case Lesser:
						case DisLesser:
							utils.remove(t);
							break;
						case Equal:
						case Greater:
						case DisEqual:
						case DisGreater:
							utils.remove(u);
							break;
						default:
							break;
						}}
				}
			}
			if (utils.isEmpty())
				return Void;
			if (utils.size() == 1)
				return utils.iterator().next();
			return new Union<>(Generator.fromCollection(utils));
		}
	}

	private class Intersection<T extends AbstractType> extends ConstructType {
		protected final List<T> options;

		public Intersection(T option) {
			this.options = Collections.singletonList(option);
		}

		public Intersection(StandardGenerator<T> options) {
			this.options = options.toList();
		}
		public <E extends Exception> Intersection(CheckedGenerator<T,E> options) throws E {
			this.options = options.toList();
		}

		StandardGenerator<T> getOptions() {
			return Generator.fromCollection(this.options);
		}


		@Override
		public CanonicalUnion toCanonicalForm() {
			return new CanonicalUnion(Generator.cartesianProduct(getOptions().map(AbstractType::toCanonicalForm).map(Union::getOptions)).map(
				(StandardGenerator<CanonicalIntersection> g) -> g.map(CanonicalIntersection::getOptions)).map(Generator::concat).map(CanonicalIntersection::new));
		}

		@Override
		public boolean isFinite() {
			return getOptions().ForAll(AbstractType::isFinite);
		}

		@Override
		protected int getNumberOfComponents() {
			return options.size();
		}

		@Override
		protected StandardGenerator<Pair<String, AbstractType>> getComponents() {
			return getOptions().map((AbstractType t) -> new Pair<>("",t));
		}

		@Override
		public Order compareWith(AbstractType other) {
			return getOptions().map(other::dualCompareWith).map(Order::opposite).fold(Order::intersection, Order.Greater);
		}
	}

	private class CanonicalIntersection extends Intersection<CanonicalTypeOrNegation> implements CanonicalType {
		public CanonicalIntersection(CanonicalTypeOrNegation option) {
			super(option);
		}

		public CanonicalIntersection(StandardGenerator<CanonicalTypeOrNegation> options) {
			super(options);
		}


		@Override
		public AbstractType simplify() {
			List<AbstractType> options = getOptions().map(CanonicalType::simplify).toList();
			Set<AbstractType> utils = new HashSet<>();
			utils.addAll(options);
			if (options.isEmpty())
				return Void;
			if (options.size() == 1)
				return options.get(0);
			final int noo = options.size();
			for (int k = 0; k < noo; ++k)	{
				AbstractType t = options.get(k);
				if (t.equals(Void))
					return Void;
				for (int i = k+1; i < noo; ++i) {
					AbstractType u = options.get(i);
					if (utils.contains(u))
						switch (t.dualCompareWith(u)) {
						case Lesser:
						case DisLesser:
							utils.remove(u);
							break;
						case Equal:
						case Greater:
						case DisEqual:
						case DisGreater:
							utils.remove(t);
							break;
						case Disjointed:
							return Void;
						default:
							break;
						}
				}
			}
			if (utils.isEmpty())
				return Void;
			if (utils.size() == 1)
				return utils.iterator().next();
			return new Intersection<>(Generator.fromCollection(utils));
		}
	}



	private class Negation<T extends AbstractType> implements AbstractType {
		public final T type;

		public Negation(T type) {
			this.type = type;
		}

		@Override
		public boolean isFinite() {
			return false;
		}

		@Override
		public CanonicalUnion toCanonicalForm() {
			return new CanonicalUnion(Generator.cartesianProduct(type.toCanonicalForm().getOptions().map(Intersection::getOptions)).map(
				(StandardGenerator<CanonicalTypeOrNegation> g) -> g.map(this::newNegation)).map(CanonicalIntersection::new));
		}

		@Override
		public String toString(String prefix1, String prefix2) {
			return type.toString(prefix1+"!", prefix2+" ");
		}

		public CanonicalTypeOrNegation newNegation(CanonicalTypeOrNegation type) {
			return type instanceof CanonicalNegation ? ((CanonicalNegation)type).type
			                                         : new CanonicalNegation((CanonicalTypeOrRecord)type);
		}

		@Override
		public Order compareWith(AbstractType other) {
			return other instanceof Negation ? type.dualCompareWith(((Negation<?>)other).type).negation()
			                                 : type.compareWith(other).semiNegation();
		}
	}

	private class CanonicalNegation extends Negation<CanonicalTypeOrRecord> implements CanonicalTypeOrNegation {
		public CanonicalNegation(CanonicalTypeOrRecord type) {
			super(type);
		}

		@Override
		public AbstractType simplify() {
			AbstractType type = this.type.simplify();
			if (type.equals(Any))
				return Void;
			if (type.equals(Void))
				return Any;
			return new Negation<>(type);
		}
	}


	public AbstractType constructRepresentation(wyil.lang.Type type) throws CompilerException {
		if (type instanceof wyil.lang.Type.Nominal) {
			final String t = ((wyil.lang.Type.Nominal) type).name().name();
			if (!nominalAbstractTypes.containsKey(t))
				throw new CompilerException(new NominalTypeCompilerError(t));
			return nominalAbstractTypes.get(t);
		}
		if (type == wyil.lang.Type.T_INT)
			return Int;
		if (type == wyil.lang.Type.T_BOOL)
			return Bool;
		if (type == wyil.lang.Type.T_BYTE)
			return Byte;
		if (type == wyil.lang.Type.T_NULL)
			return Null;
		if (type == wyil.lang.Type.T_ANY)
			return Any;
		if (type == wyil.lang.Type.T_VOID)
			return Void;
		if (type instanceof wyil.lang.Type.Record)
			return new Record<>((Generator.fromCollection(((wyil.lang.Type.Record) type).getFieldNames()).Map(
				(String f) -> new Pair<>(f, constructRepresentation(((wyil.lang.Type.Record) type).getField(f))))));
		if (type instanceof wyil.lang.Type.Union)
			return new Union<>(Generator.fromCollection(((wyil.lang.Type.Union) type).bounds()).Map(this::constructRepresentation));
		if (type instanceof wyil.lang.Type.Intersection)
			return new Intersection<>(Generator.fromCollection(((wyil.lang.Type.Intersection) type).bounds()).Map(this::constructRepresentation));
		if (type instanceof wyil.lang.Type.Negation)
			return new Negation<>(constructRepresentation(((wyil.lang.Type.Negation) type).element()));
		throw UnsupportedTypeCompilerError.exception(type);
	}



	/*------- Abstract Type Compilation -------*/


	public static interface TypeTree extends Tree {

	}

	public class TypeLeaf extends Leaf<Type> implements TypeTree {
		public TypeLeaf(Type value) {
			super(value);
		}
	}

	public class TypeRecord extends RecordNode<TypeTree> implements TypeTree {
		public TypeRecord(StandardGenerator<Pair<String, TypeTree>> fields) {
			super(fields);
		}
		public <E extends Exception> TypeRecord(CheckedGenerator<Pair<String, TypeTree>, E> fields) throws E {
			super(fields);
		}
	}

	public class SimpleTypeUnion extends UnionNode<TypeTree, TypeLeaf, TypeTree> implements TypeTree {
		public SimpleTypeUnion(StandardGenerator<Pair<TypeLeaf, TypeTree>> options) {
			super(options);
		}

		@Override
		protected StandardPairGenerator<TypeTree, TypeTree> getTypedOptions() {
			return getOptions().map((TypeLeaf a) -> a, (TypeTree b) -> b);
		}
	}

	public class TypeRecordUnion extends RecordUnionNode<TypeTree, TypeLeaf, TypeRecord> implements TypeTree {
		public TypeRecordUnion(StandardGenerator<Pair<TypeLeaf, TypeRecord>> options) {
			super(options);
		}

		@Override
		protected StandardPairGenerator<TypeTree, TypeTree> getTypedOptions() {
			return getOptions().map((TypeLeaf a) -> a, (TypeRecord b) -> b);
		}
	}






	//Map<String, TypeTree> nominalTypes = new HashMap<>();
	Map<String, AbstractType> nominalAbstractTypes = new HashMap<>();

	public TypeCompiler(CompilerLogger logger) {
		super(logger);
	}



	public static class UnsupportedTypeCompilerError extends CompilerError {
		private final wyil.lang.Type type;

		public UnsupportedTypeCompilerError(wyil.lang.Type type) {
			this.type = type;
		}

		@Override
		public String info() {
			return "Representation for type \""+type.toString()+"\" unknown "+type.getClass().getName();
		}

		public static CompilerException exception(wyil.lang.Type type) {
			return new CompilerException(new UnsupportedTypeCompilerError(type));
		}
	}

	public static class UnsupportedAbstractTypeCompilerError extends CompilerError {
		private final AbstractType type;

		public UnsupportedAbstractTypeCompilerError(AbstractType type) {
			this.type = type;
		}

		@Override
		public String info() {
			return "Unsupported compilation of the abstract type\n"+type.toString("");
		}

		public static CompilerException exception(AbstractType type) {
			return new CompilerException(new UnsupportedAbstractTypeCompilerError(type));
		}
	}

	public static class UnresolvedTypeCompilerError extends CompilerError {
		private final wyil.lang.Type type;
		private final AbstractType sType;

		public UnresolvedTypeCompilerError(wyil.lang.Type type, AbstractType sType) {
			this.type = type;
			this.sType = sType;
		}

		@Override
		public String info() {
			return "The type \""+type.toString()+"\"\nwas resolved to the non-synthetizable type\n"+sType.toString("  ");
		}

		public static CompilerException exception(wyil.lang.Type type, AbstractType sType) {
			return new CompilerException(new UnresolvedTypeCompilerError(type, sType));
		}
	}

	public static class UnresolvedTypeCompileNotice extends CompilerNotice {
		private final String type;
		private final AbstractType sType;

		public UnresolvedTypeCompileNotice(String type, AbstractType sType) {
			this.type = type;
			this.sType = sType;
		}

		@Override
		public String info() {
			return "The Nominal type \""+type+"\" was resolved to the non-synthetizable type\n"+sType.toString("  ");
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

		public static CompilerException exception(String typeName) {
			return new CompilerException(new NominalTypeCompilerError(typeName));
		}
	}

	public static class EmptyUnionTypeCompilerError extends CompilerError {
		@Override
		public String info() {
			return "Compiling empty union unsupported";
		}

		public static CompilerException exception() {
			return new CompilerException(new EmptyUnionTypeCompilerError());
		}
	}





	private TypeTree compileUnion(CheckedGenerator<TypeTree, CompilerException> options) throws CompilerException {
		final List<TypeTree> types = options.toList();
		if (types.isEmpty())
			throw EmptyUnionTypeCompilerError.exception();
		if (types.size() == 1) {
			logger.addMessage(new CompilerDebug(){
				@Override
				public String info() {
					return "Union type of one option optimized to a singleton type";
				}});
			return types.get(0);
		}
		if (!Generator.fromCollection(types).forAll((TypeTree t) -> t instanceof RecordNode))
			return new SimpleTypeUnion(Generator.fromCollection(types).map((TypeTree t) -> new Pair<>(getBoolean(), t)));
		return new TypeRecordUnion(Generator.fromCollection(types).map((TypeTree t) -> new Pair<>(getBoolean(), (TypeRecord)t)));
	}


	public void addNominalType(WyilFile.Type type) throws CompilerException {
		AbstractType t = constructRepresentation(type.type());
//		debug(t.toString(type.name() +" : "));
		t = t.toCanonicalForm().simplify();
//		debug(t.toString(type.name() +" # "));
		if (!t.isFinite())
			logger.addMessage(new UnresolvedTypeCompileNotice(type.name(), t));
		nominalAbstractTypes.put(type.name(), t);
		//nominalTypes.put(type.name(), compileType(type.type()));
	}

	public TypeLeaf getBoolean() {
		return new TypeLeaf(Type.Boolean);
	}
	public TypeLeaf getByte() {
		return new TypeLeaf(new Type.Std_logic_vector(7,0));
	}

	private TypeTree compileType(AbstractType type) throws CompilerException {
		if (type == Int)
			return new TypeLeaf(new Type.Signed(31,0));
		if (type == Bool)
			return getBoolean();
		if (type == Byte)
			return getByte();
		if (type == Null)
			return new TypeRecord(Generator.emptyGenerator());
		if (type instanceof Record)
			return new TypeRecord(((Record<?>)type).getFields().MapSecond(this::compileType));
		if (type instanceof Union)
			return compileUnion(((Union<?>)type).getOptions().Map(this::compileType));
		throw new CompilerException(new UnsupportedAbstractTypeCompilerError(type));

	}

	public TypeTree compileType(wyil.lang.Type type) throws CompilerException {
		AbstractType sType = constructRepresentation(type).toCanonicalForm().simplify();
		if (!sType.isFinite())
			throw UnresolvedTypeCompilerError.exception(type, sType);
		return compileType(sType);
	}
}
