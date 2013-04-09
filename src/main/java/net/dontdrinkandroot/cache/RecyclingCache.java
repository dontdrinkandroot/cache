package net.dontdrinkandroot.cache;

/**
 * A Buffered Cache that uses a recycling ExpungeStrategy.
 * 
 * @author Philip W. Sorst <philip@sorst.net>
 */
public interface RecyclingCache<K, V> extends Cache<K, V> {

	int getMaxSize();


	void setMaxSize(int maxSize);


	int getRecycleSize();


	void setRecycleSize(int recycleSize);
}
