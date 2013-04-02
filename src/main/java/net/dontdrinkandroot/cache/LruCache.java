package net.dontdrinkandroot.cache;

/**
 * A Buffered Cache that uses a LruRecyclingExpungeStrategy.
 * 
 * @author Philip W. Sorst <philip@sorst.net>
 */
public interface LruCache {

	int getMaxSize();


	void setMaxSize(int maxSize);


	int getRecycleSize();


	void setRecycleSize(int recycleSize);
}
