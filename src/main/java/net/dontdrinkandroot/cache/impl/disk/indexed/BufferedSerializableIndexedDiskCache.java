/**
 * Copyright (C) 2012, 2013 Philip W. Sorst <philip@sorst.net>
 * and individual contributors as indicated
 * by the @authors tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.dontdrinkandroot.cache.impl.disk.indexed;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import net.dontdrinkandroot.cache.BufferedRecyclingCache;
import net.dontdrinkandroot.cache.Cache;
import net.dontdrinkandroot.cache.CacheException;
import net.dontdrinkandroot.cache.metadata.impl.BlockMetaData;
import net.dontdrinkandroot.cache.statistics.impl.SimpleCacheStatistics;
import net.dontdrinkandroot.cache.utils.Serializer;


/**
 * A {@link SerializableIndexedDiskCache} that buffers entries in memory on successful disk put and
 * get operations. The size and contents of the buffer are determined by a buffer
 * {@link ExpungeStrategy} that works the same way as a normal {@link ExpungeStrategy} but only on
 * the buffer entries.
 * 
 * @author Philip W. Sorst <philip@sorst.net>
 */
public class BufferedSerializableIndexedDiskCache extends SerializableIndexedDiskCache
		implements BufferedRecyclingCache<Serializable, Serializable> {

	private final Map<Serializable, Serializable> buffer;

	private final SimpleCacheStatistics bufferStatistics;

	private boolean copyOnRead = true;

	private boolean copyOnWrite = true;

	private int bufferSize;


	public BufferedSerializableIndexedDiskCache(
			final String name,
			final long defaultTimeToLive,
			final int maxSize,
			final int recycleSize,
			final File baseDir,
			final int bufferSize) throws IOException {

		this(name, defaultTimeToLive, Cache.UNLIMITED_IDLE_TIME, maxSize, recycleSize, baseDir, bufferSize);
	}


	public BufferedSerializableIndexedDiskCache(
			final String name,
			final long defaultTimeToLive,
			final long defaultMaxIdleTime,
			final int maxSize,
			final int recycleSize,
			final File baseDir,
			final int bufferSize) throws IOException {

		super(name, defaultTimeToLive, defaultMaxIdleTime, maxSize, recycleSize, baseDir);

		this.bufferSize = bufferSize;
		this.buffer = new HashMap<Serializable, Serializable>();
		this.bufferStatistics = new SimpleCacheStatistics();
	}


	/**
	 * Get the cache statistics of the buffer. The buffer statistics indicate the following: a cache
	 * hit means that the entry exists in the cache and was found in the buffer, a cache miss means
	 * that the entry exists in the cache and was not found in the buffer, the get and put count
	 * reflects gets and puts to the buffer, not the cache itself, the size is the current size of
	 * the buffer.
	 */
	public synchronized SimpleCacheStatistics getBufferStatistics() {

		this.bufferStatistics.setCurrentSize(this.buffer.size());
		return this.bufferStatistics;
	}


	@Override
	protected <T extends Serializable> T doGet(Serializable key, final BlockMetaData metaData) throws CacheException {

		this.bufferStatistics.increaseGetCount();

		@SuppressWarnings("unchecked")
		T bufferedValue = (T) this.buffer.get(key);
		if (bufferedValue != null) {

			this.bufferStatistics.increaseCacheHits();

			/*
			 * Return a copy if desired so changes on the data after the cache get are not reflected
			 * in the buffer
			 */
			if (this.copyOnRead) {
				return this.copyData(bufferedValue);
			} else {
				return bufferedValue;
			}

		} else {

			this.bufferStatistics.increaseCacheMissesNotFound();
		}

		/* Get data from disk and store it in the buffer */
		final T data = super.doGet(key, metaData);
		this.addToBuffer(key, data);

		return data;
	}


	@Override
	protected <T extends Serializable> T doPut(
			Serializable key,
			final T data,
			final long timeToLive,
			final long maxIdleTime) throws CacheException {

		/* Put data to disk and store it in the buffer */
		T putData = super.doPut(key, data, timeToLive, maxIdleTime);
		this.addToBuffer(key, putData);

		/*
		 * Return a copy if desired so changes on the data after the cache put are not reflected in
		 * the buffer
		 */
		if (this.copyOnWrite) {
			return this.copyData(putData);
		} else {
			return putData;
		}
	}


	@Override
	protected void doDelete(Serializable key, final BlockMetaData metaData) throws CacheException {

		/* Remove entry from buffer and from disk */
		this.buffer.remove(key);
		super.doDelete(key, metaData);
	}


	@Override
	public int getBufferSize() {

		return this.bufferSize;
	}


	@Override
	public void setBufferSize(int bufferSize) {

		this.bufferSize = bufferSize;
	}


	/**
	 * Creates a copy of the given data.
	 */
	@SuppressWarnings("unchecked")
	protected <T extends Serializable> T copyData(T data) {

		Serializable serializable = data;
		return (T) Serializer.clone(serializable);
	}


	public boolean isCopyOnRead() {

		return this.copyOnRead;
	}


	public boolean isCopyOnWrite() {

		return this.copyOnWrite;
	}


	/**
	 * Sets if a successful get should return a copy of the cache entry, so when manipulating the
	 * object no changes are persisted in the buffer.
	 */
	public BufferedSerializableIndexedDiskCache setCopyOnRead(boolean copyOnRead) {

		this.copyOnRead = copyOnRead;
		return this;
	}


	/**
	 * Sets if a successful put should return a copy of the cache entry, so when manipulating the
	 * object no changes are persisted in the buffer.
	 */
	public BufferedSerializableIndexedDiskCache setCopyOnWrite(boolean copyOnWrite) {

		this.copyOnWrite = copyOnWrite;
		return this;
	}


	/**
	 * Adds an entry to the buffer.
	 */
	private void addToBuffer(Serializable key, final Serializable data) {

		if (this.buffer.size() >= this.bufferSize) {

			int toDelete = this.buffer.size() - this.bufferSize + 1;
			TreeSet<Entry<Serializable, BlockMetaData>> orderedSet =
					new TreeSet<Entry<Serializable, BlockMetaData>>(this.getComparator());
			orderedSet.addAll(this.buildBufferMetaDataMap().entrySet());

			Iterator<Entry<Serializable, BlockMetaData>> iterator = orderedSet.iterator();
			int numDeleted = 0;
			while (iterator.hasNext() && numDeleted < toDelete) {
				this.buffer.remove(iterator.next().getKey());
			}
		}

		this.bufferStatistics.increasePutCount();
		this.buffer.put(key, data);
	}


	/**
	 * Builds a map that holds the key metadata mapping of the buffer. Needed to run an expunge
	 * strategy on the buffer.
	 */
	private Map<Serializable, BlockMetaData> buildBufferMetaDataMap() {

		Map<Serializable, BlockMetaData> bufferEntries = new HashMap<Serializable, BlockMetaData>();
		Iterator<Serializable> keyIterator = this.buffer.keySet().iterator();
		while (keyIterator.hasNext()) {

			Serializable key = keyIterator.next();
			BlockMetaData metaData = this.getEntry(key);

			if (metaData == null) {
				/*
				 * Strange, metadata was not found anymore, so entry does not exist on disk. Warn
				 * and also delete in buffer
				 */
				this.getLogger().warn("{}: Metadata for {} was null", this.getName(), key.toString());
				keyIterator.remove();
			} else {
				bufferEntries.put(key, metaData);
			}
		}

		return bufferEntries;
	}

}
