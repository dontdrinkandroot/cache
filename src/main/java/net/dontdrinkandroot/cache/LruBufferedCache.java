package net.dontdrinkandroot.cache;

/**
 * A Buffered Cache that has a LruRecyclingExpungeStrategy for both persistent storage and buffer.
 * 
 * @author Philip W. Sorst <philip@sorst.net>
 */
public interface LruBufferedCache extends LruCache {

	int getBufferMaxSize();


	void setBufferMaxSize(int maxSize);


	int getBuferRecycleSize();


	void setBufferRecycleSize(int bufferRecycleSize);

}
