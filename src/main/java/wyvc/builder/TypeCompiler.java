package wyvc.builder;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import wyil.lang.WyilFile;
import wyvc.utils.Generator;
import wyvc.utils.Generator.CheckedGenerator;
import wyvc.utils.Generator.StandardGenerator;
import wyvc.utils.Generator.StandardPairGenerator;
import wyvc.utils.Pair;
import wyvc.utils.Utils;
import wyvc.builder.CompilerLogger.CompilerDebug;
import wyvc.builder.CompilerLogger.CompilerError;
import wyvc.builder.CompilerLogger.CompilerException;
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
			Unordered,
			Disjointed,
			Unknown;

			/** A/B => B/A **/
			private static final Order[] Opposite =
				{	Lesser, 	Equal, 		Greater,	Unordered,	Disjointed,	Unknown		};
			public Order opposite() {
				return Opposite[ordinal()];
			}

			/** A/B => (!A)/(!B) **/
			private static final Order[] Negation =
				{	Lesser, 	Equal, 		Greater, 	Unordered, 	Unordered, 	Unknown		};
			public Order negation() {
				return Negation[ordinal()];
			}

			/** A/B => (!A)/B **/
			private static final Order[] SemiNegation =
				{	Disjointed,	Disjointed,	Unordered,	Unordered,	Greater,	Unknown		};
			public Order semiNegation() {
				return SemiNegation[ordinal()];
			}

			/** A/C && B/D => (A,B)/(C,D) **/
			private static final Order[][] Conjunction = {
				{	Greater,	Greater, 	Disjointed,	Unordered, 	Disjointed,	Unknown		},
				{	Greater,	Equal, 		Lesser, 	Unordered, 	Disjointed,	Unknown		},
				{	Disjointed,	Lesser,		Lesser,		Unordered, 	Disjointed,	Unknown		},
				{	Unordered,	Unordered,	Unordered, 	Unordered, 	Disjointed,	Unknown		},
				{	Disjointed,	Disjointed,	Disjointed, Disjointed, Disjointed,	Disjointed	},
				{	Unknown, 	Unknown,	Unknown, 	Unknown, 	Disjointed,	Unknown		}
			};
			public Order conjunction(Order other) {
				return Conjunction[ordinal()][other.ordinal()];
			}

			/** A/C && B/C => (A|B)/C **/
			private static final Order[][] Union = {
				{	Greater, 	Greater, 	Greater, 	Greater,	Greater,	Greater		},
				{	Greater, 	Equal, 		Equal, 		Greater,	Greater,	Greater		},
				{	Greater, 	Equal, 		Lesser, 	Unknown,	Unordered,	Unknown		},
				{	Greater, 	Greater, 	Unknown, 	Unknown,	Unordered,	Unknown		},
				{	Greater, 	Greater, 	Unordered,	Unordered,	Disjointed,	Unknown		},
				{	Greater, 	Greater, 	Unknown, 	Unknown,	Unknown,	Unknown		}
			};
			public Order union(Order other) {
				return Union[ordinal()][other.ordinal()];
			}

			/** A/C && B/C => (A&B)/C **/
			private static final Order[][] Intersection = {
				{	Greater, 	Equal, 		Lesser,		Unordered,	Disjointed,	Unknown		},
				{	Equal, 		Equal, 		Lesser, 	Lesser,		Lesser,		Lesser		},
				{	Lesser, 	Lesser, 	Lesser, 	Lesser,		Lesser,		Lesser		},
				{	Unordered, 	Lesser, 	Lesser, 	Unknown,	Disjointed,	Unknown		},
				{	Disjointed,	Lesser,		Lesser,		Disjointed,	Disjointed,	Disjointed	},
				{	Unknown, 	Lesser, 	Lesser, 	Unknown,	Disjointed,	Unknown		}
			};
			public Order intersection(Order other) {
				return Intersection[ordinal()][other.ordinal()];
			}

			/** A/B && B/A => A/B **/
			private static final Order[][] Precise = {
				{	Greater, 	Equal,		Equal,		Greater,	Unknown,	Greater		},
				{	Equal, 		Equal, 		Equal, 		Equal,		Unknown,	Equal		},
				{	Equal, 		Equal, 		Lesser, 	Lesser,		Disjointed,	Lesser		},
				{	Greater, 	Equal, 		Lesser, 	Unordered,	Disjointed,	Unordered	},
				{	Unknown,	Unknown,	Disjointed,	Disjointed,	Disjointed,	Disjointed	},
				{	Greater, 	Equal, 		Lesser, 	Unordered,	Disjointed,	Unknown		}
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
				return Order.Greater;
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
				return Order.Equal;
			return Order.Lesser;
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
			return getOptions().map(other::dualCompareWith).map(Order::opposite).fold(Order::union, Order.Lesser);
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
					if (utils.contains(u))
						switch (t.dualCompareWith(u)) {
						case Lesser:
							utils.remove(t);
							break;
						case Equal:
						case Greater:
							utils.remove(u);
							break;
						default:
							break;
						}
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
			return getOptions().map(other::dualCompareWith).map(Order::opposite).fold(Order::intersection, Order.Equal);
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
							utils.remove(u);
							break;
						case Equal:
						case Greater:
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
			return other instanceof Negation ? ((Negation<?>)other).type.dualCompareWith(type).negation()
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

	Map<String, TypeTree> nominalTypes = new HashMap<>();
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



	///////////////////////////////////////////////////////////////////////
	//                             Types                                 //
	///////////////////////////////////////////////////////////////////////

	public static interface TypeTree extends Tree<TypeTree,Type> {
		@Override
		public String toString();
		public boolean equals(TypeTree other);
	}

	public class PrimitiveType extends Primitive<TypeTree,Type> implements TypeTree {
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

	public class CompoundType<S extends Structure<TypeTree, Type>> extends Compound<TypeTree,Type, S> implements TypeTree {
		public CompoundType(S structure) {
			super(structure);
		}

		@Override
		public String toString() {
			return "{"+String.join(", ", structure.getComponents().map((String s, TypeTree t) -> s+":"+t.toString()).toList())+"}";
		}

		@Override
		public boolean equals(TypeTree other) {
			return isStructuredAs(other) && structure.getComponents().takeSecond().gather(other.getStructure().getComponents().takeSecond()).fold(
				(Boolean b, Pair<TypeTree,TypeTree> p) -> b && p.first.equals(p.second), true);
		}
	}

	public static class RecordStructure<T extends Tree<T,V>,V> implements EffectiveRecordStructure<T,V> {
		private final List<Pair<String, T>> components;

		public RecordStructure(StandardGenerator<Pair<String, T>> components) {
			this.components = components.toList();
		}

		public <E extends Exception> RecordStructure(CheckedGenerator<Pair<String, T>, E> components) throws E {
			this.components = components.toList();
		}

//		public RecordStructure(List<Pair<String, T>> components) {
//			this.components = components;
//		}

		public StandardPairGenerator<String, T> getFields() {
			return Generator.fromPairCollection(components);
		}
		public int getNumberOfFields() {
			return components.size();
		}

		@Override
		public String toString(String prefix) {
			final int l = getNumberOfFields();
			return getFields().enumerate().fold(
				(String s, Pair<Integer, Pair<String, T>> c)
					-> s+prefix + (c.first+1 == l ? " └─ " : " ├─ ") + c.second.first+"\n"+c.second.second.toString(prefix + " │ "),
				"");
		}

		@Override
		public boolean isStructureAs(Structure<?, ?> other) {
			if (other instanceof RecordStructure)
				return getNumberOfFields() == ((RecordStructure<?,?>)other).getNumberOfFields() && getFields().takeFirst().gather(
					((RecordStructure<?,?>)other).getFields().takeFirst()).forAll((String s1, String s2) -> s1.equals(s2));
			return false;
		}

		@Override
		public StandardPairGenerator<String, T> getComponents() {
			return getFields();
		}

		@Override
		public int getNumberOfComponents() {
			return components.size();
		}

	}

	public static class UnionStructure<T extends Tree<T,V>,V> implements EffectiveUnionStructure<T,V> {
		public final List<Pair<T,T>> options;

		public UnionStructure(StandardGenerator<Pair<T,T>> options) {
			this.options = options.toList();
		}

		public <E extends Exception> UnionStructure(CheckedGenerator<Pair<T,T>, E> options) throws E {
			this.options = options.toList();
		}

		@Override
		public StandardPairGenerator<String, T> getComponents() {
			return new StandardPairGenerator<String,T>(){
				@Override
				protected void generate() throws InterruptedException, wyvc.utils.Generator.EndOfGenerationException {
					int k = 0;
					for (Pair<T,T> p : options) {
						yield(new Pair<>(FLG_PREFIX + k, p.first));
						yield(new Pair<>(VAL_PREFIX + k++, p.second));
					}
				}};
		}

		public StandardPairGenerator<T,T> getOptions() {
			return Generator.fromPairCollection(options);
		}

		public int getNumberOfOptions() {
			return options.size();
		}

		@Override
		public String toString(String prefix) {
			final int l = getNumberOfOptions();
			return getOptions().enumerate().fold(
				(String s, Pair<Integer, Pair<T, T>> c) -> s+
				prefix + (c.first+1 == l ? " └┰ " : " ├┰ ") + FLG_PREFIX + c.first +"\n"+c.second.first.toString(prefix + " │  ") +
				prefix + (c.first+1 == l ? "  ┖ " : " │┖ ") + VAL_PREFIX + c.first+"\n"+c.second.second.toString(prefix + (c.first+1 == l ? "    " : " │  ")),
				"");
		}

		public <U extends Tree<U,W>,W> boolean isStructureAsHelper(Structure<U, W> other) {
			if (other instanceof UnionStructure)
				return getNumberOfOptions() == ((UnionStructure<U,W>)other).getNumberOfOptions() && getOptions().takeSecond().gather(
					((UnionStructure<U,W>)other).getOptions().takeSecond()).forAll((T t, U u) -> t.isStructuredAs(u));
			return false;
		}
		@Override
		public boolean isStructureAs(Structure<?, ?> other) {
			return isStructureAsHelper(other);
		}

		@Override
		public int getNumberOfComponents() {
			return 2*options.size();
		}

	}


	public static class RecordsUnionStructure<T extends Tree<T,V>,V> implements EffectiveUnionStructure<T,V>, EffectiveRecordStructure<T,V> {
		private final List<Pair<String,T>> components;
		private final List<Pair<T,T>> options;

		public RecordsUnionStructure(StandardGenerator<Pair<String, T>> components, StandardGenerator<Pair<T,T>> options) {
			this.components = components.toList();
			this.options = options.toList();
		}

		@Override
		public int getNumberOfComponents() {
			return 2*options.size();
		}

		@Override
		public StandardPairGenerator<String, T> getComponents() {
			return new StandardPairGenerator<String,T>(){
				@Override
				protected void generate() throws InterruptedException, wyvc.utils.Generator.EndOfGenerationException {
					int k = 0;
					for (Pair<T,T> p : options) {
						yield(new Pair<>(FLG_PREFIX + k, p.first));
						yield(new Pair<>(VAL_PREFIX + k++, p.second));
					}
				}};
		}

		@Override
		public String toString(String prefix) {
			final int l = getNumberOfFields();
			final int m = getNumberOfOptions();
			return getFields().enumerate().fold(
				(String s, Pair<Integer, Pair<String, T>> c)
					-> s+prefix + (c.first+1 == l+m ? " └─ " : " ├─ ") + c.second.first+"\n"+c.second.second.toString(prefix + " │ "),
				"") + getOptions().enumerate().fold(
				(String s, Pair<Integer, Pair<T, T>> c) -> s+
				prefix + (c.first+1 == m ? " └┰ " : " ├┰ ") + FLG_PREFIX + c.first +"\n"+c.second.first.toString(prefix + " │  ") +
				prefix + (c.first+1 == m ? "  ┖ " : " │┖ ") + VAL_PREFIX + c.first+"\n"+c.second.second.toString(prefix + (c.first+1 == l ? "    " : " │  ")),
				"");
		}

		public <U extends Tree<U,W>,W> boolean isStructureAsHelper(Structure<U, W> other) {
			if (other instanceof RecordsUnionStructure)
				return getNumberOfOptions() == ((RecordsUnionStructure<U,W>)other).getNumberOfOptions() && getOptions().takeSecond().gather(
					((RecordsUnionStructure<U,W>)other).getOptions().takeSecond()).forAll((T t, U u) -> t.isStructuredAs(u));
			return false;
		}

		@Override
		public boolean isStructureAs(Structure<?, ?> other) {
			return isStructureAsHelper(other);
		}

		@Override
		public int getNumberOfFields() {
			return components.size();
		}

		@Override
		public StandardPairGenerator<String, T> getFields() {
			return Generator.fromPairCollection(components);
		}

		@Override
		public int getNumberOfOptions() {
			return options.size();
		}

		@Override
		public StandardPairGenerator<T, T> getOptions() {
			return Generator.fromPairCollection(options);
		}
	}


	private TypeTree compileUnion(TypeTree vl_t1, TypeTree vl_t2) throws CompilerException {
		return compileUnion(Generator.fromCollection(Arrays.asList(vl_t1, vl_t2)).toCheckedGenerator());
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
		if (!Generator.fromCollection(types).forAll((TypeTree t) -> t instanceof CompoundType && t.getStructure() instanceof RecordStructure))
			return new CompoundType<>(new UnionStructure<>(Generator.fromCollection(types).map((TypeTree t) -> new Pair<>(getBoolean(), t))));

		List<CompoundType<RecordStructure<TypeTree,Type>>> recordTypes = Utils.convert(types);
		final Map<String, TypeTree> cps0 = new HashMap<>();
		final Map<String, TypeTree> cps1 = new HashMap<>();
		recordTypes.get(0).getStructure().getFields().forEach((String n, TypeTree t) -> cps0.put(n, t));
		for (int k = 1; k<recordTypes.size() && !cps0.isEmpty(); ++k) {
			recordTypes.get(k).getStructure().getFields().ForEach((String n, TypeTree t) -> {
				if(cps0.containsKey(n))
					cps1.put(n, compileUnion(cps0.get(n), t));
			});
			cps0.clear();
			cps0.putAll(cps1);
			cps1.clear();
		}
		return new CompoundType<>(new RecordsUnionStructure<>(
				Generator.fromCollection(cps0.entrySet()).map((Entry<String, TypeTree> e) -> new Pair<>(e.getKey(), e.getValue())),
				Generator.fromCollection(types).map((TypeTree t) -> new Pair<>(getBoolean(), t))));
	}

	private TypeTree compileIntersection(CheckedGenerator<TypeTree, CompilerException> options) throws CompilerException {
		throw UnsupportedTypeCompilerError.exception(null);
	}


	public void addNominalType(WyilFile.Type type) throws CompilerException {
		AbstractType t = constructRepresentation(type.type());
		debug(t.toString(type.name() +" : "));
		debug(t.toCanonicalForm().toString(type.name() +" ? "));
		debug(t.toCanonicalForm().simplify().toString(type.name() +" # "));
		nominalAbstractTypes.put(type.name(), t);
		//nominalTypes.put(type.name(), compileType(type.type()));
	}

	public PrimitiveType getBoolean() {
		return new PrimitiveType(Type.Boolean);
	}
	public PrimitiveType getByte() {
		return new PrimitiveType(new Type.Std_logic_vector(7,0));
	}

	public TypeTree compileType(wyil.lang.Type type) throws CompilerException {
		debug(constructRepresentation(type).toString("", ""));
		//debug("Compiling "+type.toString());
		if (type instanceof wyil.lang.Type.Nominal) {
			final String t = ((wyil.lang.Type.Nominal) type).name().name();
			if (!nominalTypes.containsKey(t))
				throw new CompilerException(new NominalTypeCompilerError(t));
			return nominalTypes.get(t);
		}
		if (type == wyil.lang.Type.T_INT)
			return new PrimitiveType(new Type.Signed(31,0));
		if (type == wyil.lang.Type.T_BOOL)
			return getBoolean();
		if (type == wyil.lang.Type.T_BYTE)
			return getByte();
		if (type == wyil.lang.Type.T_NULL)
			return new CompoundType<>(new RecordStructure<>(Generator.emptyGenerator()));
		if (type instanceof wyil.lang.Type.Record)
			return new CompoundType<>(new RecordStructure<>(Generator.fromCollection(((wyil.lang.Type.Record) type).getFieldNames()).Map(
				(String f) -> new Pair<>(f, compileType(((wyil.lang.Type.Record) type).getField(f))))));
		if (type instanceof wyil.lang.Type.Union)
			return compileUnion(Generator.fromCollection(((wyil.lang.Type.Union) type).bounds()).Map(this::compileType));
		if (type instanceof wyil.lang.Type.Intersection)
			return compileIntersection(Generator.fromCollection(((wyil.lang.Type.Union) type).bounds()).Map(this::compileType));
		throw new CompilerException(new UnsupportedTypeCompilerError(type));
	}



}
