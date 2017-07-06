package wyvc.utils;

import java.util.HashMap;
import java.util.Map;

import wyvc.utils.Generators.PairGenerator;
import wyvc.utils.Generators.PairGenerator_;

public class BiMap<K,V> {
	private final Map<K, V> direct = new HashMap<>();
	private final Map<V, K> inverse = new HashMap<>();
	
	public BiMap() {}
	public BiMap(PairGenerator<K, V> values) {
		values.forEach(this::put);
	}
	public <E extends Exception> BiMap(PairGenerator_<K, V, E> values) throws E {
		values.forEach(this::put);
	}
	
	public void put(K key, V value) {
		direct.put(key, value);
		inverse.put(value, key);
	}
	
	public int size() {
		return direct.size();
	}
	
	public V get(K key) {
		return direct.get(key);
	}
	
	public K getKey(V value) {
		return inverse.get(value);
	}
	
	public boolean containsKey(K key) {
		return direct.containsKey(key);
	}

	public boolean containsValue(V value) {
		return inverse.containsKey(value);
	}
	
	public void clear() {
		direct.clear();
		inverse.clear();
	}
	
	public PairGenerator<K,V> getValues() {
		return Generators.fromMap(direct);
	}
}
