package wyvc.utils;

import java.util.ArrayList;
import java.util.List;

import wyvc.utils.Generators.GCollection;

public interface GList<T> extends List<T>, GCollection<T> {
	public static class GArrayList<T> extends ArrayList<T> implements GList<T> {
		private static final long serialVersionUID = 7960431203659261571L;
	}
}
