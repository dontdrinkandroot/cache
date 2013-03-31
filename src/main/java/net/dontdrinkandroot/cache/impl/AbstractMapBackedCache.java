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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.dontdrinkandroot.cache.Cache;
import net.dontdrinkandroot.cache.CacheException;
import net.dontdrinkandroot.cache.expungestrategy.ExpungeStrategy;
import net.dontdrinkandroot.cache.metadata.MetaData;
import net.dontdrinkandroot.cache.statistics.impl.SimpleCacheStatistics;


/**
 * @author Philip W. Sorst <philip@sorst.net>
 */
public abstract class AbstractMapBackedCache<K, V, M extends MetaData> extends AbstractCache<K, V>
		implements Cache<K, V> {

	/** Statistics for this cache (e.g hit rate) */
	private final SimpleCacheStatistics statistics;

	/** The expunge strategy of this cache */
	private ExpungeStrategy expungeStrategy;

	private Map<K, M> entriesMetaDataMap;


	/**
	 * Construct a new AbstractCache.
	 * 
	 * @param name
	 *            The name of the cache.
	 * @param defaultTimeToLive
	 *            The default time to live for Cache entries.
	 * @param expungeStrategy
	 *            The {@link ExpungeStrategy} to use.
	 */
	public AbstractMapBackedCache(final String name, final long defaultTimeToLive, final ExpungeStrategy expungeStrategy) {

		super(name, defaultTimeToLive);

		this.expungeStrategy = expungeStrategy;
		this.statistics = new SimpleCacheStatistics();
		this.entriesMetaDataMap = new HashMap<K, M>();
	}


	/**
	 * Construct a new AbstractCache.
	 * 
	 * @param name
	 *            The name of the cache.
	 * @param defaultTimeToLive
	 *            The default time to live for Cache entries.
	 * @param defaultMaxIdleTime
	 *            The default max idle time for cache entries.
	 * @param expungeStrategy
	 *            The {@link ExpungeStrategy} to use.
	 */
	public AbstractMapBackedCache(
			final String name,
			final long defaultTimeToLive,
			long defaultMaxIdleTime,
			final ExpungeStrategy expungeStrategy) {

		super(name, defaultTimeToLive, defaultMaxIdleTime);

		this.expungeStrategy = expungeStrategy;
		this.statistics = new SimpleCacheStatistics();
		this.entriesMetaDataMap = new HashMap<K, M>();
	}


	@Override
	public synchronized <T extends V> T put(K key, T data) {

		try {
			return this.putWithErrors(key, data);
		} catch (CacheException e) {
			this.getLogger().warn("Putting " + key + " to cache failed", e);
			return data;
		}
	}


	@Override
	public final synchronized <T extends V> T putWithErrors(final K key, final T data) throws CacheException {

		if (key == null) {
			throw new CacheException("Key must not be null");
		}

		this.getLogger().trace("Putting '{}' to cache", key);

		/*
		 * If key is already known, delete old entry before inserting new one (instead of
		 * overwriting, e.g. for disk based implementations)
		 */
		final M metaData = this.getEntriesMetaDataMap().get(key);
		if (metaData != null) {
			this.delete(key, metaData);
		}

		if (this.expungeStrategy.triggers(this.statistics)) {
			this.cleanUp();
		}

		final T result = this.doPut(key, data);

		this.statistics.increasePutCount();
		this.statistics.setCurrentSize(this.entriesMetaDataMap.size());

		return result;
	};


	@Override
	public final synchronized void delete(final K key) throws CacheException {

		final M metaData = this.entriesMetaDataMap.get(key);

		if (metaData != null) {
			this.delete(key, metaData);
		}
	}


	@Override
	public final synchronized void cleanUp() throws CacheException {

		final Collection<Entry<K, M>> expungeEntriesMetaData =
				this.expungeStrategy.getToExpungeMetaData(this.entriesMetaDataMap.entrySet());
		this.expunge(expungeEntriesMetaData);
	}


	@Override
	public synchronized MetaData getMetaData(K key) throws CacheException {

		final M metaData = this.entriesMetaDataMap.get(key);

		/* Not found */
		if (metaData == null) {

			return null;
		}

		/* Expired */
		if (metaData.isExpired()) {

			this.delete(key, metaData);

			return null;
		}

		return metaData;
	}


	@Override
	public synchronized <T extends V> T get(K key) {

		try {
			return this.getWithErrors(key);
		} catch (CacheException e) {
			this.getLogger().error("Getting " + key + " from cache failed", e);
			return null;
		}
	}


	@Override
	public final synchronized <T extends V> T getWithErrors(final K key) throws CacheException {

		final M metaData = this.entriesMetaDataMap.get(key);

		if (metaData == null) {

			/* Entry not found: cache miss */
			this.statistics.increaseCacheMissesNotFound();
			this.statistics.increaseGetCount();
			this.getLogger().trace("Cache Miss for '{}'", key);

			return null;
		}

		if (metaData != null && metaData.isExpired()) {

			/* Entry expired: cache miss expired */
			this.statistics.increaseCacheMissesExpired();
			this.statistics.increaseGetCount();
			this.getLogger().trace("Cache Miss expired '{}'", key);

			this.delete(key, metaData);

			return null;
		}

		/* Cache hit: return */
		try {

			final T result = this.doGet(key, metaData);

			this.statistics.increaseCacheHits();
			this.statistics.increaseGetCount();
			this.getLogger().trace("Cache Hit for '{}'", key);

			metaData.update();

			return result;

		} catch (final CacheException e) {

			/* Delete entry on fail */
			this.delete(key, metaData);
			throw e;

		}

	}


	@Override
	public SimpleCacheStatistics getStatistics() {

		return this.statistics;
	}


	public ExpungeStrategy getExpungeStrategy() {

		return this.expungeStrategy;
	}


	public void setExpungeStrategy(final ExpungeStrategy expungeStrategy) {

		this.expungeStrategy = expungeStrategy;
	}


	protected void setEntriesMetaDataMap(final Map<K, M> map) {

		this.entriesMetaDataMap = map;
	}


	protected Map<K, M> getEntriesMetaDataMap() {

		return this.entriesMetaDataMap;
	}


	/**
	 * Performs the actual expunging of entries with the given metadata. Can be subclasses as they
	 * might want to chain or do some other magic, by default the entries will simply be deleted.
	 * 
	 * @param expungeEntriesMetaData
	 *            A List of the {@link MetaData} of the entries to expunge.
	 * @throws CacheException
	 *             Thrown on any errors encountered.
	 */
	protected void expunge(final Collection<Entry<K, M>> expungeEntriesMetaData) throws CacheException {

		for (final Entry<K, M> metaData : expungeEntriesMetaData) {
			this.delete(metaData.getKey(), metaData.getValue());
		}

		this.getLogger().info("{}: Expunged {} entries", this.getName(), expungeEntriesMetaData.size());
		this.getCleanUpLogger().info("{}: Expunged {} entries", this.getName(), expungeEntriesMetaData.size());
	}


	public synchronized void delete(K key, M metaData) throws CacheException {

		this.doDelete(key, metaData);

		this.entriesMetaDataMap.remove(key);
		this.statistics.setCurrentSize(this.entriesMetaDataMap.size());
	}


	/**
	 * Returns a copy of the List of all metadata entries.
	 * 
	 * @return a copy of the List of all metadata entries.
	 */
	public synchronized List<M> getEntriesMetaData() {

		return new ArrayList<M>(this.entriesMetaDataMap.values());
	}


	/**
	 * Performs deletion of the data belonging to the metadata. Removal from the map is done by the
	 * superclass.
	 */
	protected abstract void doDelete(K key, M metaData) throws CacheException;


	/**
	 * Performs retrieval of the data belonging to the metadata.
	 */
	protected abstract <T extends V> T doGet(K key, M metaData) throws CacheException;


	/**
	 * Performs storage of the given data and adds new metadata to the map.
	 */
	protected abstract <T extends V> T doPut(K key, T data) throws CacheException;

}
