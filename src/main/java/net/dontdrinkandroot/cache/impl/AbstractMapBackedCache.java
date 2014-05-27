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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import net.dontdrinkandroot.cache.Cache;
import net.dontdrinkandroot.cache.CacheException;
import net.dontdrinkandroot.cache.metadata.MetaData;
import net.dontdrinkandroot.cache.metadata.comparator.MetaDataComparator;
import net.dontdrinkandroot.cache.metadata.comparator.impl.LfuComparator;
import net.dontdrinkandroot.cache.statistics.impl.SimpleCacheStatistics;
import net.dontdrinkandroot.cache.utils.Duration;


/**
 * @author Philip W. Sorst <philip@sorst.net>
 */
public abstract class AbstractMapBackedCache<K, V, M extends MetaData> extends AbstractCache<K, V>
		implements Cache<K, V>
{

	/** Statistics for this cache (e.g hit rate) */
	private final SimpleCacheStatistics statistics;

	private final Map<K, M> entriesMetaDataMap;

	private long lastCleanUp = System.currentTimeMillis();

	private long cleanUpInterval = Duration.hours(1);

	private final MetaDataComparator<K, M> comparator = new LfuComparator<K, M>();

	private int maxSize;

	private int recycleSize;


	/**
	 * Construct a new {@link AbstractMapBackedCache}.
	 * 
	 * @param name
	 *            The name of the cache.
	 * @param defaultTimeToLive
	 *            The default time to live for Cache entries.
	 * @param expungeStrategy
	 *            The {@link ExpungeStrategy} to use.
	 */
	public AbstractMapBackedCache(
			final String name,
			final long defaultTimeToLive,
			final int maxSize,
			final int recycleSize)
	{
		this(name, defaultTimeToLive, Cache.UNLIMITED_IDLE_TIME, maxSize, recycleSize);
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
			final int maxSize,
			final int recycleSize)
	{
		super(name, defaultTimeToLive, defaultMaxIdleTime);

		this.entriesMetaDataMap = new HashMap<K, M>();
		this.statistics = new SimpleCacheStatistics();

		this.maxSize = maxSize;
		this.recycleSize = recycleSize;
	}


	@Override
	public synchronized <T extends V> T put(K key, T data)
	{
		try {

			return this.putWithErrors(key, data);

		} catch (CacheException e) {
			this.getLogger().warn(this.getName() + ": Putting " + key + " to cache failed", e);
			return data;
		}
	}


	@Override
	public final synchronized <T extends V> T putWithErrors(final K key, final T data) throws CacheException
	{
		if (key == null) {
			throw new CacheException("Key must not be null");
		}

		this.getLogger().trace(this.getName() + ": Putting '{}' to cache", key);

		/*
		 * If key is already known, delete old entry before inserting new one (instead of
		 * overwriting, e.g. for disk based implementations)
		 */
		final M metaData = this.getEntry(key);
		if (metaData != null) {
			this.delete(key, metaData);
		}

		if (this.triggerExpunge()) {
			this.expunge();
		}

		final T result = this.doPut(key, data);

		this.statistics.increasePutCount();

		return result;
	};


	@Override
	public final synchronized void delete(final K key) throws CacheException
	{
		final M metaData = this.entriesMetaDataMap.get(key);

		if (metaData != null) {
			this.delete(key, metaData);
		}
	}


	@Override
	public final synchronized void expunge() throws CacheException
	{
		Set<Entry<K, M>> entrySet = this.entriesMetaDataMap.entrySet();

		List<Entry<K, M>> toExpunge = new ArrayList<Entry<K, M>>();
		TreeSet<Entry<K, M>> orderedSet = new TreeSet<Entry<K, M>>(this.comparator);

		/* Select expired and idled away */
		for (final Entry<K, M> entry : entrySet) {

			MetaData metaData = entry.getValue();

			if (metaData.isExpired() || metaData.isStale()) {
				toExpunge.add(entry);
			} else {
				orderedSet.add(entry);
			}
		}

		/* Select from remaining */
		final int numToDelete = entrySet.size() - toExpunge.size() + 1 - this.maxSize;
		final Iterator<Entry<K, M>> iterator = orderedSet.iterator();

		int numDeleted = 0;
		while (iterator.hasNext() && numDeleted < numToDelete) {
			Entry<K, M> entry = iterator.next();
			toExpunge.add(entry);
			numDeleted++;
		}

		this.expunge(toExpunge);

		for (M metaData : this.entriesMetaDataMap.values()) {
			metaData.decay();
		}
	}


	@Override
	public synchronized void cleanUp() throws CacheException
	{
		Iterator<Entry<K, M>> entriesIterator = this.entriesMetaDataMap.entrySet().iterator();
		long numExpired = 0;
		long numStale = 0;
		while (entriesIterator.hasNext()) {

			Entry<K, M> entry = entriesIterator.next();
			M metaData = entry.getValue();

			if (metaData.isExpired()) {
				numExpired++;
				this.doDelete(entry.getKey(), metaData);
				entriesIterator.remove();
			}

			if (metaData.isStale()) {
				numStale++;
				this.doDelete(entry.getKey(), metaData);
				entriesIterator.remove();
			}
		}

		this.getLogger().info(this.getName() + ": Cleaned up {} expired and {} stale entries", numExpired, numStale);
		this.getCleanUpLogger().info(
				this.getName() + ": Cleaned up {} expired and {} stale entries",
				numExpired,
				numStale);

		this.lastCleanUp = System.currentTimeMillis();
	}


	@Override
	public synchronized MetaData getMetaData(K key) throws CacheException
	{
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
	public synchronized <T extends V> T get(K key)
	{
		try {

			return this.getWithErrors(key);

		} catch (CacheException e) {
			this.getLogger().error(this.getName() + ": Getting " + key + " from cache failed", e);
			return null;
		}
	}


	@Override
	public final synchronized <T extends V> T getWithErrors(final K key) throws CacheException
	{
		if (this.lastCleanUp + this.cleanUpInterval < System.currentTimeMillis()) {
			this.cleanUp();
		}

		final M metaData = this.entriesMetaDataMap.get(key);

		if (metaData == null) {

			/* Entry not found: cache miss */
			this.statistics.increaseCacheMissesNotFound();
			this.statistics.increaseGetCount();
			this.getLogger().trace(this.getName() + ": Cache Miss for '{}'", key);

			return null;
		}

		if (metaData != null && metaData.isExpired()) {

			/* Entry expired: cache miss expired */
			this.statistics.increaseCacheMissesExpired();
			this.statistics.increaseGetCount();
			this.getLogger().trace(this.getName() + ": Cache Miss expired '{}'", key);

			this.delete(key, metaData);

			return null;
		}

		/* Cache hit: return */
		try {

			final T result = this.doGet(key, metaData);

			this.statistics.increaseCacheHits();
			this.statistics.increaseGetCount();
			this.getLogger().trace(this.getName() + ": Cache Hit for '{}'", key);

			metaData.update();

			return result;

		} catch (final CacheException e) {

			/* Delete entry on fail */
			this.delete(key, metaData);
			throw e;
		}
	}


	@Override
	public synchronized SimpleCacheStatistics getStatistics()
	{
		this.statistics.setCurrentSize(this.entriesMetaDataMap.size());
		return this.statistics;
	}


	public MetaDataComparator<K, M> getComparator()
	{
		return this.comparator;
	}


	public int getMaxSize()
	{
		return this.maxSize;
	}


	public void setMaxSize(int maxSize)
	{
		this.maxSize = maxSize;
	}


	public int getRecycleSize()
	{
		return this.recycleSize;
	}


	public void setRecycleSize(int recycleSize)
	{
		this.recycleSize = recycleSize;
	}


	public long getCleanUpInterval()
	{
		return this.cleanUpInterval;
	}


	public void setCleanUpInterval(long cleanUpInterval)
	{
		this.cleanUpInterval = cleanUpInterval;
	}


	protected void putEntry(K key, M metaData)
	{
		this.entriesMetaDataMap.put(key, metaData);
	}


	protected M getEntry(K key)
	{
		return this.entriesMetaDataMap.get(key);
	}


	/**
	 * Performs the actual expunging of entries with the given metadata. Can be subclassed as they
	 * might want to chain or do some other magic, by default the entries will simply be deleted.
	 * 
	 * @param expungeEntriesMetaData
	 *            A List of the {@link MetaData} of the entries to expunge.
	 * @throws CacheException
	 *             Thrown on any errors encountered.
	 */
	protected void expunge(final Collection<Entry<K, M>> expungeEntriesMetaData) throws CacheException
	{
		for (final Entry<K, M> metaData : expungeEntriesMetaData) {
			this.delete(metaData.getKey(), metaData.getValue());
		}

		this.getLogger().info(this.getName() + ": Expunged {} entries", expungeEntriesMetaData.size());
		this.getCleanUpLogger().info(this.getName() + ": Expunged {} entries", expungeEntriesMetaData.size());
	}


	public synchronized void delete(K key, M metaData) throws CacheException
	{
		this.doDelete(key, metaData);

		this.entriesMetaDataMap.remove(key);
		this.statistics.setCurrentSize(this.entriesMetaDataMap.size());
	}


	/**
	 * Returns a copy of the List of all metadata entries.
	 * 
	 * @return a copy of the List of all metadata entries.
	 */
	public synchronized List<M> getEntriesMetaData()
	{
		return new ArrayList<M>(this.entriesMetaDataMap.values());
	}


	protected boolean triggerExpunge()
	{
		return this.entriesMetaDataMap.size() >= this.maxSize + this.recycleSize;
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
