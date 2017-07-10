package wyvc.utils;

import java.util.ArrayList;

import wyvc.utils.Generators.GTripleCollection;

public interface GTripleList<S,T,U> extends GList<Triple<S,T,U>>, GTripleCollection<S,T,U> {
	public static class GTripleArrayList<S,T,U> extends ArrayList<Triple<S,T,U>> implements GTripleList<S,T,U> {
		private static final long serialVersionUID = -900737152546547779L;
	}
}
