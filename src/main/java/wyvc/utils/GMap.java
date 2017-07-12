package wyvc.utils;

import java.util.HashMap;
import java.util.Map;

import wyvc.utils.Generators.PairGenerator;
import wyvc.utils.Generators.PairGenerator_;

public interface GMap<K, V> extends Map<K, V> {
	public static class GHashMap<K,V> extends HashMap<K, V> implements GMap<K, V> {
		private static final long serialVersionUID = 3980558434689129229L;

		public GHashMap() {}
		public GHashMap(Map<K,V> other) {
			super(other);
		}
		public GHashMap(PairGenerator<K, V> entries) {
			entries.forEach(this::put);
		}
		public <E extends Exception> GHashMap(PairGenerator_<K, V, E> entries) throws E {
			entries.forEach(this::put);
		}
	}

	default PairGenerator<K, V> generate() {
		return Generators.fromMap(this);
	}
}
