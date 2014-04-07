/**
 * Copyright (C) 2012-2014 Philip W. Sorst <philip@sorst.net>
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
package net.dontdrinkandroot.cache.impl;

import net.dontdrinkandroot.cache.CacheException;
import net.dontdrinkandroot.cache.CustomTtlCache;
import net.dontdrinkandroot.cache.metadata.MetaData;


/**
 * @author Philip W. Sorst <philip@sorst.net>
 */
public abstract class AbstractMapBackedCustomTtlCache<K, V, M extends MetaData> extends AbstractMapBackedCache<K, V, M>
		implements CustomTtlCache<K, V> {

	/**
	 * Construct a new AbstractCustomTtlCache.
	 * 
	 * @param name
	 *            The name of the cache.
	 * @param defaultTimeToLive
	 *            The default Time to Live for Cache entries.
	 */
	public AbstractMapBackedCustomTtlCache(final String name, final long defaultTimeToLive, int maxSize, int recycleSize) {

		super(name, defaultTimeToLive, maxSize, recycleSize);
	}


	/**
	 * Construct a new AbstractCustomTtlCache.
	 * 
	 * @param name
	 *            The name of the cache.
	 * @param defaultTimeToLive
	 *            The default Time to Live for Cache entries.
	 */
	public AbstractMapBackedCustomTtlCache(
			final String name,
			final long defaultTimeToLive,
			final long defaultMaxIdleTime,
			int maxSize,
			int recycleSize) {

		super(name, defaultTimeToLive, defaultMaxIdleTime, maxSize, recycleSize);
	}


	@Override
	public <T extends V> T put(K key, T data, long timeToLive) {

		try {
			return this.putWithErrors(key, data, timeToLive, this.getDefaultMaxIdleTime());
		} catch (CacheException e) {
			this.getLogger().warn("Putting " + key + " to cache failed", e);
			return data;
		}
	}


	@Override
	public synchronized <T extends V> T put(K key, T data, long timeToLive, long maxIdleTime) {

		try {
			return this.putWithErrors(key, data, timeToLive, maxIdleTime);
		} catch (CacheException e) {
			this.getLogger().warn("Putting " + key + " to cache failed", e);
			return data;
		}
	}


	@Override
	public final synchronized <T extends V> T putWithErrors(
			final K key,
			final T data,
			final long timeToLive,
			final long maxIdleTime) throws CacheException {

		if (key == null) {
			throw new CacheException("Key must not be null");
		}

		this.getLogger().trace("Putting '{}' to cache", key);

		final M metaData = this.getEntry(key);

		/* If the key is already known, delete entry first */
		if (metaData != null) {
			this.delete(key, metaData);
		}

		if (this.triggerExpunge()) {
			this.expunge();
		}

		final T result = this.doPut(key, data, timeToLive, maxIdleTime);

		this.getStatistics().increasePutCount();

		return result;
	}


	@Override
	public final synchronized <T extends V> T putWithErrors(final K key, final T data, final long timeToLive)
			throws CacheException {

		return this.putWithErrors(key, data, timeToLive, this.getDefaultMaxIdleTime());
	}


	@Override
	protected <T extends V> T doPut(final K key, final T data) throws CacheException {

		return this.doPut(key, data, this.getDefaultTtl(), this.getDefaultMaxIdleTime());
	};


	/**
	 * Performs storage of the given data and adds new metadata to the map.
	 */
	protected abstract <T extends V> T doPut(K key, T data, long timeToLive, long defaultMaxIdleTime)
			throws CacheException;

}
