package wyvc.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import wycc.util.Logger;
import wyil.lang.WyilFile;
import wyvc.utils.FunctionalInterfaces.BiFunction;
import wyvc.utils.FunctionalInterfaces.Consumer;
import wyvc.utils.FunctionalInterfaces.Function;
import wyvc.utils.FunctionalInterfaces.Predicate;
import wyvc.utils.Generators;
import wyvc.utils.Generators.Generator_;
import wyvc.utils.Generators.PairGenerator;
import wyvc.utils.Generators.PairGenerator_;
import wyvc.utils.Generators.CustomPairGenerator;
import wyvc.utils.Generators.CustomGenerator;
import wyvc.utils.Generators.EndOfGenerationException;
import wyvc.utils.Generators.Generator;
import wyvc.utils.Pair;
import wyvc.builder.CompilerLogger.CompilerDebug;
import wyvc.builder.CompilerLogger.CompilerError;
import wyvc.builder.CompilerLogger.CompilerException;
import wyvc.builder.CompilerLogger.CompilerNotice;
import wyvc.lang.Type;

public class TypeCompiler extends LexicalElementTree {

	/*------- Abstract Type Analysis -------*/
//
//	private static interface AbstractProducedType extends Tree {
//	}

	private static interface AbstractType extends Tree {

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
				{	Disjointed,	Disjointed,	Unknown,	Greater,	Unknown,	DisGreater,	DisGreater,	Greater		};
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
				{	Equal, 		Equal, 		Lesser, 	DisLesser,	Lesser,		DisEqual,	DisEqual,	DisLesser	},
				{	Lesser, 	Lesser, 	Lesser, 	DisLesser,	Lesser,		DisEqual,	DisEqual,	DisLesser	},
				{	Disjointed,	DisLesser,	DisLesser,	Disjointed,	Disjointed,	DisGreater,	DisEqual,	DisLesser	},
				{	Unknown, 	Lesser, 	Lesser, 	Disjointed,	Unknown,	DisGreater,	DisEqual,	DisLesser	},
				{	DisGreater,	DisEqual,	DisEqual,	DisGreater,	DisGreater,	DisGreater,	DisEqual,	DisEqual	},
				{	DisEqual,	DisEqual,	DisEqual,	DisEqual,	DisEqual,	DisEqual,	DisEqual,	DisEqual	},
				{	DisLesser,	DisLesser,	DisLesser,	DisLesser,	DisLesser,	DisEqual,	DisEqual,	DisLesser	}
			};
			public Order intersection(Order other) {
				return Intersection[ordinal()][other.ordinal()];
			}

			/** A/B && A/B => A/B **/
			private static final Order[][] Precise = {
				{	Greater, 	Equal,		Equal,		DisGreater,	Greater,	DisGreater,	DisEqual,	DisEqual	},
				{	Equal, 		Equal, 		Equal, 		DisEqual,	Equal,		DisEqual,	DisEqual,	DisEqual	},
				{	Equal, 		Equal, 		Lesser, 	DisLesser,	Lesser,		DisEqual,	DisEqual,	DisLesser	},
				{	DisGreater,	DisEqual,	DisLesser,	Disjointed,	Disjointed,	DisGreater,	DisEqual,	DisLesser	},
				{	Greater, 	Equal, 		Lesser, 	Disjointed,	Unknown,	DisGreater,	DisEqual,	DisLesser	},
				{	DisGreater,	DisEqual,	DisEqual,	DisGreater,	DisGreater,	DisGreater,	DisEqual,	DisEqual	},
				{	DisEqual,	DisEqual,	DisEqual,	DisEqual,	DisEqual,	DisEqual,	DisEqual,	DisEqual	},
				{	DisEqual,	DisEqual,	DisLesser,	DisLesser,	DisLesser,	DisEqual,	DisEqual,	DisLesser	}
			};
			public Order precise(Order other) {
				return Precise[ordinal()][other.ordinal()];
			}

			private static final int[][] Information = {
				{	 0, -1,	 0,	 0,	 1,	-1,	-2,	 0	},
				{	 1,  0,  1,  0,	 2,	 0,	-1,	 0	},
				{	 0, -1,  0,  0,	 1,	 0,	-2,	-1	},
				{	 0,	 0,	 0,	 0,	 1,	-1,	-2,	-1	},
				{	-1, -2, -1, -1,	 0,	-2,	-2,	-2	},
				{	 1,	 0,	 0,	 1,	 2,	 0,	-1,	 0	},
				{	 2,	 1,	 2,	 2,	 2,	 1,	 0,	 1	},
				{	 0,	 0,	 1,	 1,	 2,	 0,	-1,	 0	}
			};
			public int information(Order other) {
				return Information[ordinal()][other.ordinal()];
			}


			private static String formatR(String s) {
				return ("          "+s).substring(s.length());
			}
			private static String formatR(Order o) {
				return formatR(o.name());
			}
			private static String formatL(String s) {
				return (s+"          ").substring(0, 10);
			}
			private static String formatL(Order o) {
				return formatL(o.name());
			}

			private static Generator<Order> enumerate() {
				return Generators.fromCollection(values());
			}
			private static PairGenerator<Order,Order> enumeratePair() {
				return enumerate().cartesianProduct(enumerate());
			}

			private static String orderInfo(Pair<Order,Order> p) {
				switch (p.first.information(p.second)) {
				case -2:
					return " >> ";
				case -1:
					return " >- ";
				case 0:
					return " >< ";
				case 1:
					return " -< ";
				case 2:
					return " << ";
				default:
					return " ?? ";
				}
			}

			private static boolean check(CompilerLogger logger, String s, Function<Order,Order> f1, Function<Order,Order> f2) {
				List<Pair<Order,Pair<Order,Order>>> failed = new ArrayList<>();
				enumerate().forEach((Order o) -> {
						Order a = f1.apply(o);
						Order b = f2.apply(o);
						if (!a.equals(b))
							failed.add(new Pair<>(o, new Pair<>(a,b)));
					});
				if (!failed.isEmpty())
					return logger.addMessage(new CompilerDebug() {
						@Override
						public String info() {
							return "The following test failed :\n  "+s+"\nFailing case(s) :\n  "+formatL("Order")+"   "+formatR("f1")+"   "+formatL("f2")+"\n"
								+Generators.fromCollection(failed).fold(
								(String c, Pair<Order,Pair<Order,Order>> p) ->
								c+"  "+formatL(p.first)+" : "+formatR(p.second.first)+orderInfo(p.second)+formatL(p.second.second)+"\n", "");
						}}, false);
				return true;
			}
			private static boolean check(CompilerLogger logger, String s, BiFunction<Order,Order,Order> f1, BiFunction<Order,Order,Order> f2) {
				List<Pair<Pair<Order,Order>,Pair<Order,Order>>> failed = new ArrayList<>();
				enumeratePair().forEach((Order o, Order p) -> {
						Order a = f1.apply(o,p);
						Order b = f2.apply(o,p);
						if (!a.equals(b))
							failed.add(new Pair<>(new Pair<>(o,p), new Pair<>(a,b)));
					});
				if (!failed.isEmpty())
					return logger.addMessage(new CompilerDebug() {
						@Override
						public String info() {
							return "The following test failed :\n  "+s+"\nFailing case(s) :\n  "+formatR("Order1")+"   "+formatL("Order2")+"   "+formatR("f1")+"   "+formatL("f2")+"\n"
						+Generators.fromCollection(failed).fold(
								(String c, Pair<Pair<Order,Order>,Pair<Order,Order>> p) ->
								c+"  "+formatR(p.first.first)+" - "+formatL(p.first.second)+" : "+formatR(p.second.first)+orderInfo(p.second)+formatL(p.second.second)+"\n", "");
						}}, false);
				return true;
			}

			private static void print(CompilerLogger logger, Generators.Generator<Integer> g) {
				String s = "";
				for (Integer i : g.toList())
					s += i + " ";
				logger.debug("Gen "+s);
			}

			private static void print(CompilerLogger logger, Generators.Generator_<Integer, ?> g) {
				String s = "";
				try {
					for (Integer i : g.toList())
						s += i + " ";
					logger.debug("Gen "+s);
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					logger.debug("Gen exception "+ e);
				}
			}

			private static Generators.Generator<Integer> getGenerator() {
				return Generators.fromCollection(Arrays.asList(0,0,0,2));
			}

			public static void check(CompilerLogger logger) {
				print(logger, getGenerator());
				print(logger, getGenerator().map((Integer x) -> x+1));
				print(logger, getGenerator().map((Integer x) -> x+1).map((Integer x) -> x+1));
				print(logger, getGenerator().map((Integer x) -> x).map((Integer x) -> {if (x%5 == 1) throw new RuntimeException();return x;}).
						map_((Integer x) -> {if (x%5 == 2) throw new Exception();return x;}).
						map((Integer x) -> {if (x%5 == 3) throw new RuntimeException();return x;}));
				/*logger.debug("Debut");
				boolean test = true;
				test &= check(logger, "reflexivity of Opposite",
					(Order o) -> o,
					(Order o) -> o.opposite().opposite());
				test &= check(logger, "Correlation between Negation, SemiNegation and Opposite",
					(Order o) -> o.negation(),
					(Order o) -> o.opposite().semiNegation().opposite().semiNegation().precise(
						o.semiNegation().opposite().semiNegation().opposite()));
				test &= check(logger, "Symmetry of Conjunction",
					(Order o, Order p) -> o.conjunction(p),
					(Order o, Order p) -> p.conjunction(o));
				test &= check(logger, "Symmetry of Union",
					(Order o, Order p) -> o.union(p),
					(Order o, Order p) -> p.union(o));
				test &= check(logger, "Symmetry of Intersection",
					(Order o, Order p) -> o.intersection(p),
					(Order o, Order p) -> p.intersection(o));
				test &= check(logger, "Symmetry of Precise",
					(Order o, Order p) -> o.precise(p),
					(Order o, Order p) -> p.precise(o));
				test &= check(logger, "Idempotence of Precise",
					(Order o, Order p) -> o.precise(p),
					(Order o, Order p) -> o.precise(p).precise(p));
				test &= check(logger, "Correlation between SemiNegation, Union and Intersection",
					(Order o, Order p) -> o.intersection(p),
					//(Order o, Order p) -> o.negation().union(p.negation()).negation().precise(p.negation().union(o.negation()).negation()));
					(Order o, Order p) -> o.semiNegation().union(p.semiNegation()).semiNegation().precise(p.semiNegation().union(o.semiNegation()).semiNegation()));
					//(Order o, Order p) -> o.semiNegation().union(p.semiNegation()).semiNegation());
					//(Order o, Order p) -> p.semiNegation().union(o.semiNegation()).semiNegation());
				if (test)
					logger.addMessage(new CompilerDebug() {
						@Override
						public String info() {
							return "Order comparison tests successful";
						}});*/
			}

		}



		boolean isFinite();
		CanonicalUnion toCanonicalForm();
		Order compareWith(AbstractType other);
		default Order dualCompareWith(AbstractType other) {
			return compareWith(other).precise(other.compareWith(this).opposite());
		}
	}

	private static interface CanonicalType extends AbstractType {
		CanonicalType simplify();
	}

	private static interface CanonicalTypeOrNegation<T extends CanonicalTypeOrRecord<?>> extends CanonicalType {
		boolean isNegation();
		T getUnderlyingType();
		@Override
		public CanonicalTypeOrNegation<?> simplify();
	}

	private static interface CanonicalTypeOrRecord<T extends CanonicalTypeOrRecord<?>> extends CanonicalTypeOrNegation<T> {
		@Override
		default boolean isNegation() {
			return false;
		}
		@Override
		public CanonicalTypeOrRecord<?> simplify();

	}

	private abstract class SimpleType extends Leaf<String> implements AbstractType,  CanonicalTypeOrRecord<SimpleType> {
		public SimpleType(String name) {
			super(name);
		}

		@Override
		public String toString(String prefix1, String prefix2) {
			return prefix1 + getValue() +"\n";
		}

		@Override
		public SimpleType getUnderlyingType() {
			return this;
		}

		@Override
		public CanonicalUnion toCanonicalForm() {
			return new CanonicalUnion(new CanonicalIntersection(this));
		}

		@Override
		public SimpleType simplify() {
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

	private final SimpleType Any = new SimpleType("Any"){
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
	private final SimpleType Null = new SimpleType("Null"){
		@Override
		public boolean isFinite() {
			return true;
		}
	};

	private final SimpleType Bool = new SimpleType("Bool"){
		@Override
		public boolean isFinite() {
			return true;
		}
	};

	private final SimpleType Byte = new SimpleType("Byte"){
		@Override
		public boolean isFinite() {
			return true;
		}
	};

	private final SimpleType Int = new SimpleType("Int"){
		@Override
		public boolean isFinite() {
			return true;
		}
	};

	private final SimpleType Void = new SimpleType("Void"){
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

//	private abstract class ConstructType implements AbstractType {
//		protected abstract int getNumberOfComponents();
//		protected abstract Generator<Pair<String, AbstractType>> getComponents();
//
//		@Override
//		public String toString(String prefix1, String prefix2) {
//			final int l = getNumberOfComponents();
//			return prefix1+this.getClass().getSimpleName()+"\n"+
//			getComponents().enumerate().fold((String a, Pair<Integer, Pair<String, AbstractType>> c) ->
//			a+c.second.second.toString(
//				prefix2 + (c.first+1 == l ? " └─ " : " ├─ ") + (c.second.first.length() == 0 ? "" : c.second.first+" : "),
//				prefix2 + (c.first+1 == l ? "   " : " │ ")), "");
//		}
//	}



	private class Record<T extends AbstractType> extends NamedNode<T> implements AbstractType {
//		public final List<Pair<String,T>> fields;

		public Record(PairGenerator<String,T> fields) {
			super(fields);
//			this.fields = fields.toList();
		}

		public <E extends Exception> Record(PairGenerator_<String,T, E> fields) throws E {
			super(fields);
//			this.fields = fields.toList();
		}

		boolean hasSameFields(Record<?> other) {
			return getNumberOfComponents() == other.getNumberOfComponents() && getFields().takeFirst().gather(other.getFields().takeFirst()).forAll(String::equals);
		}

		@Override
		public boolean isFinite() {
			return getFields().takeSecond().forAll(AbstractType::isFinite);
		}


		@Override
		public CanonicalUnion toCanonicalForm() {/*
			return new CanonicalUnion( Generators.cartesianProduct(getFields().mapSecond(AbstractType::toCanonicalForm).
				map(Generators::constant, CanonicalUnion::getOptions).map(Generator<String>::<CanonicalIntersection>gather)).
				map(Generators::toPairGenerator).map((PairGenerator<String, CanonicalIntersection> i) -> Generators.cartesianProduct(i.
					map(Generators::constant,CanonicalIntersection::getOptions).map(Generator<String>::<CanonicalTypeOrNegation>gather)).
					map(Generators::toPairGenerator)).map((Generator<PairGenerator<String,CanonicalTypeOrNegation>> i) -> i.map(
						(PairGenerator<String,CanonicalTypeOrNegation> r) -> {
							final List<Pair<String, CanonicalTypeOrNegation>> chs = r.toList();
							return Generators.fromPairCollection(chs).mapFirst((String a) -> Generators.fromPairCollection(chs).
								mapSecond(CanonicalTypeOrNegation::getUnderlyingType).map(
									(String g, CanonicalTypeOrRecord h) -> new Pair<>(g,g.equals(a) ? h : Any))).mapFirst(CanonicalRecord::new).
									map((CanonicalRecord s, CanonicalTypeOrNegation t) ->  t.isNegation() ? new CanonicalNegation(s) : s);
							})).map(Generators::concat).map(CanonicalIntersection::new));
			/*/
			// { | & ! }
			Generator<PairGenerator<String, CanonicalIntersection>> fi = Generators.cartesianProduct(getFields().mapSecond(AbstractType::toCanonicalForm).
					map(Generators::constant, CanonicalUnion::getOptions).map(Generator<String>::<CanonicalIntersection>gather)).map(Generators::toPairGenerator);
			// | { & ! }
			Generator<Generator<PairGenerator<String,CanonicalTypeOrNegation<?>>>> fie =
					fi.map((PairGenerator<String, CanonicalIntersection> i) -> Generators.cartesianProduct(i.map(Generators::constant,CanonicalIntersection::getOptions).
				map(Generator<String>::<CanonicalTypeOrNegation<?>>gather)).map(Generators::toPairGenerator));
			// | & { ! }
			Generator<Generator<Generator<CanonicalTypeOrNegation<?>>>> fiel =
					fie.map((Generator<PairGenerator<String,CanonicalTypeOrNegation<?>>> i) -> i.map((PairGenerator<String,CanonicalTypeOrNegation<?>> r) -> {
						final List<Pair<String, CanonicalTypeOrNegation<?>>> chs = r.toList();
						return Generators.fromPairCollection(chs).mapFirst((String a) -> Generators.fromPairCollection(chs).
							mapSecond(CanonicalTypeOrNegation::getUnderlyingType).map((String g, CanonicalTypeOrRecord<?> h) -> new Pair<>(g,g.equals(a) ? h : Any))).mapFirst(Generators::toPairGenerator)
								.mapFirst(CanonicalRecord::new).
								map((CanonicalRecord s, CanonicalTypeOrNegation<?> t) ->  t.isNegation() ? new CanonicalNegation(s) : s);
					}));
			// | & ! { }
			return new CanonicalUnion(fiel.map(Generators::concat).map(CanonicalIntersection::new));//*/
		}
//		@Override
//		public int getNumberOfComponents() {
//			return fields.size();
//		}
//		@Override
//		protected Generator<Pair<String, AbstractType>> getComponents() {
//			return getFields().map(Pair<String, AbstractType>::new);
//		}

		public <U extends AbstractType> Order compareWithHelper(Record<U> other) {
			return other.getNumberOfFields() != getNumberOfFields() || !getFields().takeFirst().gather(
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

	private class CanonicalRecord extends Record<CanonicalTypeOrRecord<?>> implements CanonicalTypeOrRecord<CanonicalTypeOrRecord<?>> {
		public CanonicalRecord(PairGenerator<String,CanonicalTypeOrRecord<?>> fields) {
			super(fields);
		}


		@Override
		public CanonicalRecord getUnderlyingType() {
			return this;
		}

		@Override
		public CanonicalTypeOrRecord<?> simplify() {
			if (getFields().takeSecond().find(Void) != null)
				return Void.simplify();
			return this;
		}
	}


	private class Union<T extends AbstractType> extends UnnamedNode<T> implements AbstractType {

		public Union(T option) {
			super(option);
		}

		public Union(Generator<T> options) {
			super(options);
		}

		public <E extends Exception> Union(Generator_<T, E> options) throws E {
			super(options);
		}

		@Override
		public CanonicalUnion toCanonicalForm() {
			return new CanonicalUnion(Generators.concat(getOptions().map(AbstractType::toCanonicalForm).map(Union::getOptions)));
		}

		@Override
		public Order compareWith(AbstractType other) {
			return getOptions().map(other::dualCompareWith).map(Order::opposite).fold(Order::union, Order.DisLesser);
		}


		@Override
		protected String getLabel(int k) {
			return "";
		}

		@Override
		public boolean isFinite() {
			return getOptions().forAll(AbstractType::isFinite);
		}

	}

	private class CanonicalUnion extends Union<CanonicalIntersection> implements CanonicalType {

		public CanonicalUnion(CanonicalIntersection option) {
			super(option);
		}

		public CanonicalUnion(Generator<CanonicalIntersection> options) {
			super(options);
		}


		@SuppressWarnings("unchecked")
		@Override
		public CanonicalUnion simplify() {
			List<CanonicalIntersection> options = getOptions().toList();
			if (options.isEmpty())
				return this;
			if (options.size() == 1)
				return new CanonicalUnion(options.get(0).simplify());
			List<CanonicalIntersection> utils = new ArrayList<>(options.size());
			utils.addAll(options);
			final int noo = options.size();
			for (int k = 0; k < noo; ++k)	{
				AbstractType t = options.get(k);
				for (int i = k+1; i < noo; ++i) {
					AbstractType u = options.get(i);
					if (utils.get(i) != null){
						switch (t.dualCompareWith(u)) {
						case Lesser:
						case DisLesser:
							utils.set(k, null);
							break;
						case Equal:
						case Greater:
						case DisEqual:
						case DisGreater:
							utils.set(i, null);
							break;
						default:
							break;
						}}
					}
			}
			// TODO better
			List<CanonicalIntersection> utils2 = new ArrayList<>();
			for (CanonicalIntersection t : utils)
				if (t != null)
					utils2.add(t.simplify());
			if (utils2.isEmpty())
				return new CanonicalUnion(Generators.emptyGenerator());
			if (utils2.size() == 1)
				return new CanonicalUnion(utils2.iterator().next());
			return new CanonicalUnion(Generators.fromCollection(utils2));
		}

		@Override
		public CanonicalUnion toCanonicalForm() {
			return this;
		}
	}

	private class Intersection<T extends AbstractType> extends UnnamedNode<T> implements AbstractType {

		public Intersection(T option) {
			super(Generators.fromSingleton(option));
		}

		public Intersection(Generator<T> options) {
			super(options);
		}
		public <E extends Exception> Intersection(Generator_<T,E> options) throws E {
			super(options);
		}

		@Override
		protected String getLabel(int k) {
			return "";
		}

		@Override
		public CanonicalUnion toCanonicalForm() {
			return new CanonicalUnion(Generators.cartesianProduct(getOptions().map(AbstractType::toCanonicalForm).map(Union::getOptions)).map(
				(Generator<CanonicalIntersection> g) -> g.map(CanonicalIntersection::getOptions)).map(Generators::concat).map(CanonicalIntersection::new));
		}

		@Override
		public boolean isFinite() {
			return getOptions().forAll(AbstractType::isFinite);
		}

		@Override
		public Order compareWith(AbstractType other) {
			return getOptions().map(other::dualCompareWith).map(Order::opposite).fold(Order::intersection, Order.Greater);
		}
	}

	private class CanonicalIntersection extends Intersection<CanonicalTypeOrNegation<?>> implements CanonicalType {
		public CanonicalIntersection(CanonicalTypeOrNegation<?> option) {
			super(option);
		}

		public CanonicalIntersection(Generator<CanonicalTypeOrNegation<?>> options) {
			super(options);
		}

		public <E extends Exception> CanonicalIntersection(Generator_<CanonicalTypeOrNegation<?>,E> options) throws E {
			super(options);
		}


		@Override
		public CanonicalIntersection simplify() {
			List<CanonicalTypeOrNegation<?>> options = getOptions().toList();
			if (options.isEmpty())
				return this;
			if (options.size() == 1)
				return new CanonicalIntersection(options.get(0).simplify());
			List<CanonicalTypeOrNegation<?>> utils = new ArrayList<>(options.size());
			utils.addAll(options);
			final int noo = options.size();
			for (int k = 0; k < noo; ++k)	{
				AbstractType t = options.get(k);
				for (int i = k+1; i < noo; ++i) {
					AbstractType u = options.get(i);
					if (utils.get(i) != null)
						switch (t.dualCompareWith(u)) {
						case Lesser:
						case DisLesser:
							utils.set(i, null);
							break;
						case Equal:
						case Greater:
						case DisEqual:
						case DisGreater:
							utils.set(k, null);
							break;
						case Disjointed:
							return new CanonicalIntersection(Generators.emptyGenerator());
						case Unknown:
						default:
							break;
						}
				}
			}
//			if (utils.isEmpty())
//				return Void;
//			if (utils.size() == 1)
//				return utils.iterator().next();

			Map<String, List<CanonicalRecord>> records = new HashMap<>();
			List<CanonicalTypeOrNegation<?>> nonRecords = new ArrayList<>();
			for (CanonicalTypeOrNegation<?> t : utils) {
				if (t==null);
				else
				if (t instanceof CanonicalRecord) {
					String f = ((CanonicalRecord) t).getFields().takeFirst().<String>fold((String s, String u) -> s+"#"+u, "").substring(1);
					if (!records.containsKey(f))
						records.put(f, new ArrayList<>());
					records.get(f).add((CanonicalRecord)t);
				}
				else
					nonRecords.add(t);
			}


			for (Entry<String, List<CanonicalRecord>> e : records.entrySet()){
				String[] f = e.getKey().split("#");
				List<Generator<CanonicalTypeOrRecord<?>>> l = Generators.fromCollection(e.getValue()).
						map(CanonicalRecord::getFields).map(PairGenerator::takeSecond).toList();

				Generators.cartesianProduct(Generators.fromCollection(f).gather(new CustomGenerator<Generator<CanonicalTypeOrRecord<?>>>() {
					@Override
					protected void generate() throws EndOfGenerationException {
						while (true)
							yield(new CanonicalIntersection(Generators.fromCollection(l).map_(Generator::next)).simplify().
								getOptions().map(CanonicalTypeOrNegation::getUnderlyingType));
					}}).mapFirst(Generators::constant).map(Generator<String>::<CanonicalTypeOrRecord<?>>gather)).
				map(Generators::toPairGenerator).map(CanonicalRecord::new).forEach(nonRecords::add);;
			}

			return new CanonicalIntersection(Generators.fromCollection(nonRecords));
		}

		@Override
		public CanonicalUnion toCanonicalForm() {
			return new CanonicalUnion(this);
		}

	}



	private class Negation<T extends AbstractType> extends UnaryNode<AbstractType, T> implements AbstractType {
		public Negation(T type) {
			super(type);
		}

		public T getType() {
			return getOperand();
		}

		@Override
		public boolean isFinite() {
			return false;
		}

		@Override
		public CanonicalUnion toCanonicalForm() {
			return new CanonicalUnion(Generators.cartesianProduct(getType().toCanonicalForm().getOptions().map(CanonicalIntersection::getOptions)).map(
				(Generator<CanonicalTypeOrNegation<?>> g) -> new CanonicalIntersection(g.map(this::newNegation))));
		}

		public CanonicalTypeOrNegation<?> newNegation(CanonicalTypeOrNegation<?> type) {
			return type instanceof CanonicalNegation ? ((CanonicalNegation)type).getType()
			                                         : new CanonicalNegation((CanonicalTypeOrRecord<?>)type);
		}

		@Override
		public Order compareWith(AbstractType other) {
			return other instanceof Negation ? getType().dualCompareWith(((Negation<?>)other).getType()).negation()
			                                 : getType().compareWith(other).semiNegation();
		}
	}

	private class CanonicalNegation extends Negation<CanonicalTypeOrRecord<?>> implements CanonicalTypeOrNegation<CanonicalTypeOrRecord<?>> {
		public CanonicalNegation(CanonicalTypeOrRecord<?> type) {
			super(type);
		}

		@Override
		public boolean isNegation() {
			return true;
		}

		@Override
		public CanonicalTypeOrNegation<?> simplify() {
			AbstractType type = getType().simplify();
			if (type.equals(Any))
				return Void;
			if (type.equals(Void))
				return Any;
			return this;
		}

		@Override
		public CanonicalTypeOrRecord<?> getUnderlyingType() {
			return getType();
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
			return new Record<>((Generators.fromCollection(((wyil.lang.Type.Record) type).getFieldNames()).biMap_((String f) -> f,
				(String f) -> constructRepresentation(((wyil.lang.Type.Record) type).getField(f)))));
		if (type instanceof wyil.lang.Type.Union)
			return new Union<>(Generators.fromCollection(((wyil.lang.Type.Union) type).bounds()).map_(this::constructRepresentation));
		if (type instanceof wyil.lang.Type.Intersection)
			return new Intersection<>(Generators.fromCollection(((wyil.lang.Type.Intersection) type).bounds()).map_(this::constructRepresentation));
		if (type instanceof wyil.lang.Type.Negation)
			return new Negation<>(constructRepresentation(((wyil.lang.Type.Negation) type).element()));
		throw UnsupportedTypeCompilerError.exception(type);
	}

/*
	public class RecordUnion extends BinaryNode<AbstractProducedType, ProducedRecord<AbstractProducedType>, ProducedRecord<AbstractProducedType>> implements AbstractProducedType {
		private final CoexistingFields coexisting;

		public RecordUnion(ProducedRecord<AbstractProducedType> firstOperand, ProducedRecord<AbstractProducedType> secondOperand, CoexistingFields coexisting) {
			super(firstOperand, secondOperand);
			this.coexisting = coexisting;
		}

		public boolean coexist(String field1, String field2) {
			return coexisting.coexist(field1, field2);
		}

		@Override
		protected String getFirstLabel() {
			return "shared";
		}
		@Override
		protected String getSecondLabel() {
			return "specific";
		}

		@Override
		public boolean isFinite() {
			return getFirstOperand().isFinite() && getSecondOperand().isFinite();
		}


		public PairGenerator<String,AbstractProducedType> getSharedFields() {
			return getFirstOperand().getFields();
		}
		public PairGenerator<String,AbstractProducedType> getSpecificFields() {
			return getSecondOperand().getFields();
		}
	}

	public static class CoexistingFields {
		private final Set<String> coexisting = new HashSet<>();

		public void makeCoexisting(List<String> fields) {
			for (int k = 0; k < fields.size()-1; ++k)
				for (int l = k+1; l < fields.size(); ++l)
					coexisting.add(fields.get(k).compareTo(fields.get(l)) < 0
						? fields.get(k) + "#" + fields.get(l)
						: fields.get(l) + "#" + fields.get(k));
		}

		public boolean coexist(String field1, String field2) {
			return coexisting.contains(field1 + "#" + field2) || coexisting.contains(field2 + "#" + field1);
		}
	}

	private AbstractProducedType createRecordUnion(Generator<Record<AbstractType>> options) {
		Map<String, List<AbstractType>> sCps1 = new HashMap<>();
		Map<String, List<AbstractType>> sCps2 = new HashMap<>();
		Map<String, List<AbstractType>> spCps = new HashMap<>();
		CoexistingFields ceCps = new CoexistingFields();
		try {
			Record<AbstractType> opt = options.next();
			ceCps.makeCoexisting(opt.getFieldNames().toList());
			opt.getFields().mapSecond(Collections::singletonList).mapSecond(ArrayList<AbstractType>::new).forEach(sCps1::put);;
			while (true) {
				opt = options.next();
				ceCps.makeCoexisting(opt.getFieldNames().toList());
				opt.getFields().forEach((String n, AbstractType t) -> {
					if(sCps1.containsKey(n)) {
						List<AbstractType> l = sCps1.remove(n);
						l.add(t);
						sCps2.put(n, l);
					}
					else
						sCps1.put(n, new ArrayList<>(Collections.singletonList(t)));
				});
				for (Entry<String, List<AbstractType>> s : sCps1.entrySet()) {
					if(spCps.containsKey(s.getKey()))
						spCps.get(s.getKey()).addAll(s.getValue());
					else
						spCps.put(s.getKey(), s.getValue());
				}
				sCps1.clear();
				sCps1.putAll(sCps2);
				sCps2.clear();
			}
		}
		catch (EndOfGenerationException e) {}
		if (spCps.isEmpty())
			return new ProducedRecord<>(Generators.fromMap(sCps1).mapSecond(Generators::fromCollection).mapSecond(Union<AbstractType>::new).
					mapSecond(AbstractType::toCanonicalForm).mapSecond(CanonicalType::simplify));
		return new RecordUnion(
			new ProducedRecord<>(Generators.fromMap(sCps1).mapSecond(Generators::fromCollection).mapSecond(Union<AbstractType>::new).
					mapSecond(AbstractType::toCanonicalForm).mapSecond(CanonicalType::simplify)),
			new ProducedRecord<>(Generators.fromMap(spCps).mapSecond(Generators::fromCollection).mapSecond(Union<AbstractType>::new).
					mapSecond(AbstractType::toCanonicalForm).mapSecond(CanonicalType::simplify)),
			ceCps);
	}*/


	/*------- Abstract Type Compilation -------*/



	public static interface TypeTree extends Tree {

	}
	public static interface AccessibleTypeTree extends TypeTree {

	}

	public class TypeLeaf extends Leaf<Type> implements AccessibleTypeTree {
		public TypeLeaf(Type value) {
			super(value);
		}
	}

	public class BooleanTypeLeaf extends TypeLeaf {
		public BooleanTypeLeaf() {
			super(Type.Boolean);
		}
	}

	public class TypeRecord<T extends TypeTree> extends NamedNode<T> implements TypeTree {
		public TypeRecord(Generator<Pair<String, T>> fields) {
			super(fields);
		}
		public <E extends Exception> TypeRecord(Generator_<Pair<String, T>, E> fields) throws E {
			super(fields);
		}
	}

	public class TypeSimpleRecord extends TypeRecord<AccessibleTypeTree> implements AccessibleTypeTree {
		public TypeSimpleRecord(Generator<Pair<String, AccessibleTypeTree>> fields) {
			super(fields);
		}
		public <E extends Exception> TypeSimpleRecord(Generator_<Pair<String, AccessibleTypeTree>, E> fields) throws E {
			super(fields);
		}
	}

	public class TypeOption<T extends AccessibleTypeTree> extends BinaryNode<TypeTree, BooleanTypeLeaf, T> implements TypeTree {
		public TypeOption(T type) {
			super(new BooleanTypeLeaf(), type);
		}

		@Override
		public String getFirstLabel() {
			return "has";
		}
		@Override
		public String getSecondLabel() {
			return "val";
		}
	}

	public class TypeSimpleUnion extends UnnamedNode<TypeOption<TypeLeaf>> implements AccessibleTypeTree {

		public TypeSimpleUnion(Generator<TypeOption<TypeLeaf>> options) {
			super(options);
		}
		public <E extends Exception> TypeSimpleUnion(Generator_<TypeOption<TypeLeaf>, E> options) throws E {
			super(options);
		}
		public TypeSimpleUnion(TypeOption<TypeLeaf> option) {
			super(option);
		}
	}


	public class TypeRecordUnion extends BinaryNode<TypeTree, TypeSimpleRecord, TypeRecord<TypeOption<AccessibleTypeTree>>> implements AccessibleTypeTree {
		public TypeRecordUnion(TypeSimpleRecord shared, TypeRecord<TypeOption<AccessibleTypeTree>> specific) {
			super(shared, specific);
		}

		public PairGenerator<String,AccessibleTypeTree> getSharedFields() {
			return getFirstOperand().getFields();
		}
		public PairGenerator<String,TypeOption<AccessibleTypeTree>> getSpecificFields() {
			return getSecondOperand().getFields();
		}
	}


	public class TypeUnion extends BinaryNode<TypeTree, TypeSimpleUnion, TypeRecordUnion> implements AccessibleTypeTree {

		public TypeUnion(TypeSimpleUnion simpleOptions, TypeRecordUnion recordOptions) {
			super(simpleOptions, recordOptions);
		}

		@Override
		public String getFirstLabel() {
			return "sha";
		}
		@Override
		public String getSecondLabel() {
			return "spe";
		}

	}



	//Map<String, TypeTree> nominalTypes = new HashMap<>();
	Map<String, AbstractType> nominalAbstractTypes = new HashMap<>();

	public TypeCompiler(CompilerLogger logger) {
		super(logger);
		AbstractType.Order.check(logger);
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






	public static class CoexistingFields {
		private final Set<String> coexisting = new HashSet<>();

		public void makeCoexisting(List<String> fields) {
			for (int k = 0; k < fields.size()-1; ++k)
				for (int l = k+1; l < fields.size(); ++l)
					coexisting.add(fields.get(k).compareTo(fields.get(l)) < 0
						? fields.get(k) + "#" + fields.get(l)
						: fields.get(l) + "#" + fields.get(k));
		}

		public boolean coexist(String field1, String field2) {
			return coexisting.contains(field1 + "#" + field2) || coexisting.contains(field2 + "#" + field1);
		}
	}

	private TypeTree createRecordUnion(Generator<Record<AbstractType>> options) throws CompilerException {
		Map<String, List<AbstractType>> sCps1 = new HashMap<>();
		Map<String, List<AbstractType>> sCps2 = new HashMap<>();
		Map<String, List<AbstractType>> spCps = new HashMap<>();
		CoexistingFields ceCps = new CoexistingFields();
		try {
			Record<AbstractType> opt = options.next();
			ceCps.makeCoexisting(opt.getFieldNames().toList());
			opt.getFields().mapSecond(Collections::singletonList).mapSecond(ArrayList<AbstractType>::new).forEach(sCps1::put);;
			while (true) {
				opt = options.next();
				ceCps.makeCoexisting(opt.getFieldNames().toList());
				opt.getFields().forEach((String n, AbstractType t) -> {
					if(sCps1.containsKey(n)) {
						List<AbstractType> l = sCps1.remove(n);
						l.add(t);
						sCps2.put(n, l);
					}
					else
						sCps1.put(n, new ArrayList<>(Collections.singletonList(t)));
				});
				for (Entry<String, List<AbstractType>> s : sCps1.entrySet()) {
					if(spCps.containsKey(s.getKey()))
						spCps.get(s.getKey()).addAll(s.getValue());
					else
						spCps.put(s.getKey(), s.getValue());
				}
				sCps1.clear();
				sCps1.putAll(sCps2);
				sCps2.clear();
			}
		}
		catch (EndOfGenerationException e) {}
		if (spCps.isEmpty())
			return new TypeRecord<>(Generators.fromMap(sCps1).mapSecond(Generators::fromCollection).mapSecond(Union<AbstractType>::new).
					mapSecond_(this::compileType));
		return new TypeRecordUnion(
			new TypeRecord<TypeTree>(Generators.fromMap(sCps1).mapSecond(Generators::fromCollection).mapSecond(Union<AbstractType>::new).
					mapSecond_(this::compileType)),
			new TypeRecord<TypeOption>(Generators.fromMap(spCps).mapSecond(Generators::fromCollection).mapSecond(Union<AbstractType>::new).
					mapSecond_(this::compileType).mapSecond(TypeOption::new)));
	}



	public void addNominalType(WyilFile.Type type) throws CompilerException {
		AbstractType t = constructRepresentation(type.type());
		debug(t.toString(type.name() +" : "));
		debug(t.toCanonicalForm().toString(type.name() +" ? "));
		CanonicalType t2 = t.toCanonicalForm().simplify();
		debug(t2.toString(type.name() +" # "));
//		if (!t2.isFinite())
//			logger.addMessage(new UnresolvedTypeCompileNotice(type.name(), t2));
		nominalAbstractTypes.put(type.name(), t2);
		//nominalTypes.put(type.name(), compileType(type.type()));
	}

	public TypeLeaf getByte() {
		return new TypeLeaf(new Type.Std_logic_vector(7,0));
	}

	private AccessibleTypeTree compileType(AbstractType type) throws CompilerException {
		if (type == Int)
			return new TypeLeaf(new Type.Signed(31,0));
		if (type == Bool)
			return new BooleanTypeLeaf();
		if (type == Byte)
			return getByte();
		if (type == Null)
			return new TypeSimpleRecord(Generators.emptyPairGenerator());
		if (type instanceof Record)
			return new TypeSimpleRecord(((Record<?>)type).getFields().mapSecond_(this::compileType));
		if (type instanceof Union){
			if (((Union<?>)type).getOptions().forAll((AbstractType t) -> t instanceof Record))
				return createRecordUnion(((Union<?>)type).getOptions().map((AbstractType t) -> (Record) t));
			return new TypeSimpleUnion(((Union<?>)type).getOptions().map_(this::compileType).map(TypeOption::new)); // TODO plus ! (rec union)
		}
		throw new CompilerException(new UnsupportedAbstractTypeCompilerError(type));

	}

	public AccessibleTypeTree compileType(wyil.lang.Type type) throws CompilerException {
		CanonicalType sType = constructRepresentation(type).toCanonicalForm().simplify();
		if (!sType.isFinite())
			throw UnresolvedTypeCompilerError.exception(type, sType);
		return compileType(sType);
	}
}
