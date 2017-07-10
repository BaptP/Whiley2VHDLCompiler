package wyvc.utils;

import java.util.ArrayList;

import wyvc.utils.Generators.GPairCollection;

public interface GPairList<S,T> extends GList<Pair<S,T>>, GPairCollection<S, T> {
		default void add(S s, T t) {
			add(new Pair<>(s,t));
		}

	public static class GPairArrayList<S,T> extends ArrayList<Pair<S,T>> implements GPairList<S,T> {
		private static final long serialVersionUID = 7370660043861313049L;
	}
}
