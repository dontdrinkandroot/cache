package net.dontdrinkandroot.cache;

/**
 * A Buffered Cache that has a recycling ExpungeStrategy for both persistent storage and buffer.
 * 
 * @author Philip W. Sorst <philip@sorst.net>
 */
public interface BufferedRecyclingCache<K, V> extends RecyclingCache<K, V> {

	int getBufferMaxSize();


	void setBufferMaxSize(int maxSize);


	int getBufferRecycleSize();


	void setBufferRecycleSize(int bufferRecycleSize);

}
