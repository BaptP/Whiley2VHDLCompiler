package wyvc.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import wyvc.utils.Generators.GCollection;
import wyvc.utils.Generators.Generator;
import wyvc.utils.Generators.Generator_;

public interface GList<T> extends List<T>, GCollection<T> {
	public static class GArrayList<T> extends ArrayList<T> implements GList<T> {
		private static final long serialVersionUID = 7960431203659261571L;

		public GArrayList() {}
		public GArrayList(Collection<T> values) {
			super(values);
		}
		public GArrayList(Generator<T> entries) {
			entries.forEach(this::add);
		}
		public <E extends Exception> GArrayList(Generator_<T, E> entries) throws E {
			entries.forEach(this::add);
		}
	}
}
