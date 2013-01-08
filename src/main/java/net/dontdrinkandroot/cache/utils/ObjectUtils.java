package net.dontdrinkandroot.cache.utils;

/**
 * Required Methods From Apache Commons Lang ObjectUtils.
 * 
 * @author Apache Commons Lang
 */
public class ObjectUtils {

	public static <T extends Comparable<? super T>> int compare(T c1, T c2) {

		return ObjectUtils.compare(c1, c2, false);
	}


	public static <T extends Comparable<? super T>> int compare(T c1, T c2, boolean nullGreater) {

		if (c1 == c2) {
			return 0;
		} else if (c1 == null) {
			return nullGreater ? 1 : -1;
		} else if (c2 == null) {
			return nullGreater ? -1 : 1;
		}
		return c1.compareTo(c2);
	}
}
