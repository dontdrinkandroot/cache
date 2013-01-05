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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.dontdrinkandroot.cache.CacheException;
import net.dontdrinkandroot.cache.expungestrategy.ExpungeStrategy;
import net.dontdrinkandroot.cache.metadata.impl.BlockMetaData;
import net.dontdrinkandroot.cache.statistics.impl.SimpleCacheStatistics;
import net.dontdrinkandroot.utils.lang.SerializationUtils;


/**
 * A {@link SerializableIndexedDiskCache} that buffers entries in memory on successful disk put and
 * get operations. The size and contents of the buffer are determined by a buffer
 * {@link ExpungeStrategy} that works the same way as a normal {@link ExpungeStrategy} but only on
 * the buffer entries.
 * 
 * @author Philip W. Sorst <philip@sorst.net>
 */
public class BufferedSerializableIndexedDiskCache extends SerializableIndexedDiskCache {

	private final Map<Serializable, Serializable> buffer;

	private final ExpungeStrategy bufferExpungeStrategy;

	private final SimpleCacheStatistics bufferStatistics;

	private boolean copyOnRead = true;

	private boolean copyOnWrite = true;


	public BufferedSerializableIndexedDiskCache(
			final String name,
			final long defaultTimeToLive,
			final ExpungeStrategy expungeStrategy,
			final File baseDir,
			final ExpungeStrategy bufferExpungeStrategy) throws IOException {

		super(name, defaultTimeToLive, expungeStrategy, baseDir);

		this.bufferExpungeStrategy = bufferExpungeStrategy;
		this.buffer = new HashMap<Serializable, Serializable>();
		this.bufferStatistics = new SimpleCacheStatistics() {

			private static final long serialVersionUID = 1L;


			@Override
			public int getCurrentSize() {

				return BufferedSerializableIndexedDiskCache.this.buffer.size();
			};
		};
	}


	public BufferedSerializableIndexedDiskCache(
			final String name,
			final long defaultTimeToLive,
			final long defaultMaxIdleTime,
			final ExpungeStrategy expungeStrategy,
			final File baseDir,
			final ExpungeStrategy bufferExpungeStrategy) throws IOException {

		super(name, defaultTimeToLive, defaultMaxIdleTime, expungeStrategy, baseDir);

		this.bufferExpungeStrategy = bufferExpungeStrategy;
		this.buffer = new HashMap<Serializable, Serializable>();
		this.bufferStatistics = new SimpleCacheStatistics() {

			private static final long serialVersionUID = 1L;


			@Override
			public int getCurrentSize() {

				return BufferedSerializableIndexedDiskCache.this.buffer.size();
			};
		};
	}


	/**
	 * Get the cache statistics of the buffer. The buffer statistics indicate the following: a cache
	 * hit means that the entry exists in the cache and was found in the buffer, a cache miss means
	 * that the entry exists in the cache and was not found in the buffer, the get and put count
	 * reflects gets and puts to the buffer, not the cache itself, the size is the current size of
	 * the buffer.
	 */
	public SimpleCacheStatistics getBufferStatistics() {

		return this.bufferStatistics;
	}


	@Override
	protected Serializable doGet(Serializable key, final BlockMetaData metaData) throws CacheException {

		this.bufferStatistics.increaseGetCount();

		Serializable bufferedValue = this.buffer.get(key);
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
		final Serializable data = super.doGet(key, metaData);
		this.addToBuffer(key, data);

		return data;
	}


	@Override
	protected Serializable doPut(
			Serializable key,
			final Serializable data,
			final long timeToLive,
			final long maxIdleTime) throws CacheException {

		/* Put data to disk and store it in the buffer */
		Serializable putData = super.doPut(key, data, timeToLive, maxIdleTime);
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
		this.buffer.remove(metaData);
		super.doDelete(key, metaData);
	}


	/**
	 * Creates a copy of the given data.
	 */
	protected Serializable copyData(Serializable data) {

		Serializable serializable = data;
		return SerializationUtils.fastClone(serializable);
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

		if (this.bufferExpungeStrategy.triggers(this.bufferStatistics)) {

			/* Expunge buffer entries */
			Map<Serializable, BlockMetaData> bufferEntries = new HashMap<Serializable, BlockMetaData>();
			for (Serializable bufferKey : this.buffer.keySet()) {
				BlockMetaData metaData = this.getEntriesMetaDataMap().get(bufferKey);
				if (metaData == null) {
					this.getLogger().error("Metadata for %s was null", bufferKey.toString());
				} else {
					bufferEntries.put(bufferKey, metaData);
				}
			}

			final Collection<Entry<Serializable, BlockMetaData>> toExpunge =
					this.bufferExpungeStrategy.getToExpungeMetaData(bufferEntries.entrySet());
			for (final Entry<Serializable, BlockMetaData> expunge : toExpunge) {
				this.buffer.remove(expunge.getKey());
			}
		}

		this.bufferStatistics.increasePutCount();
		this.buffer.put(key, data);
	}

}
