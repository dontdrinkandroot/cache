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
package net.dontdrinkandroot.cache.impl;

import net.dontdrinkandroot.cache.CacheException;
import net.dontdrinkandroot.cache.CustomTtlCache;
import net.dontdrinkandroot.cache.expungestrategy.ExpungeStrategy;
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
	public AbstractMapBackedCustomTtlCache(
			final String name,
			final long defaultTimeToLive,
			final ExpungeStrategy expungeStrategy) {

		super(name, defaultTimeToLive, expungeStrategy);
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
			final ExpungeStrategy expungeStrategy) {

		super(name, defaultTimeToLive, defaultMaxIdleTime, expungeStrategy);
	}


	@Override
	public final synchronized V put(final K key, final V data, final long timeToLive, final long maxIdleTime)
			throws CacheException {

		if (key == null) {
			throw new CacheException("Key must not be null");
		}

		this.getLogger().trace("Putting '{}' to cache", key);

		final M metaData = this.getEntriesMetaDataMap().get(key);

		/* If the key is already known, delete entry first */
		if (metaData != null) {
			this.delete(key, metaData);
		}

		if (this.getExpungeStrategy().triggers(this.getStatistics())) {
			this.cleanUp();
		}

		final V result = this.doPut(key, data, timeToLive, maxIdleTime);

		this.getStatistics().increasePutCount();
		this.getStatistics().setCurrentSize(this.getEntriesMetaDataMap().size());

		return result;
	}


	@Override
	public final synchronized V put(final K key, final V data, final long timeToLive) throws CacheException {

		return this.put(key, data, timeToLive, this.getDefaultMaxIdleTime());
	}


	@Override
	protected V doPut(final K key, final V data) throws CacheException {

		return this.doPut(key, data, this.getDefaultTtl(), this.getDefaultMaxIdleTime());
	};


	/**
	 * Performs storage of the given data and adds new metadata to the map.
	 */
	protected abstract V doPut(K key, V data, long timeToLive, long defaultMaxIdleTime) throws CacheException;

}
