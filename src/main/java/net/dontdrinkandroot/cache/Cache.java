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
package net.dontdrinkandroot.cache;

import net.dontdrinkandroot.cache.metadata.MetaData;
import net.dontdrinkandroot.cache.statistics.CacheStatistics;


/**
 * A cache is a component that transparently stores data so that future requests for that data can
 * be served faster. Usually caches are used to "remember" the results of computationally complex
 * operations or to speed up the access to a slow medium. The general approach is to ask the cache
 * if it has the result available, if yes then return the stored result, if not, compute the result
 * and store it in the cache. Caches are usually chosen to have a (much) smaller size than the
 * amount of the overall available data, so strategies come into play how entries are evicted when
 * the cache size becomes to large. Therefore caches are not "reliable" as one might expect from a
 * persistent storage: when an entries is stored in the cache, there is no guarantee that it is
 * still available on the next get. Cache entries can also have a time to live which means that even
 * if the entry is available after its expiration it is discarded in favor of new data.
 * 
 * @author Philip W. Sorst <philip@sorst.net>
 * 
 * @param <K>
 *            Type of the key that is used to store and lookup entries. Keys should be kept simple
 *            as they might be stored in memory. A Key relies on correct implementations of HashCode
 *            and Equals.
 * @param <V>
 *            Type of entries that can be stored and retrieved from the cache.
 */
public interface Cache<K, V> {

	static long UNLIMITED_IDLE_TIME = 0;


	/**
	 * Store an entry in the cache with the default time to live. Any errors are swallowed, use
	 * {@link Cache#putWithErrors(Object, Object)} if you want to handle them.
	 * 
	 * @param key
	 *            A unique identifier.
	 * @param data
	 *            The data to store. Make sure that you don't alter the data after it has been put
	 *            to cache as (depending on the implementation) this might lead to altering the
	 *            entry in the cache. Use the returned data instead.
	 * @return The entry that has been stored in the cache. It is save to alter this as
	 *         implementations as implementations make sure that this is always (at least) a copy.
	 */
	<T extends V> T put(K key, T data);


	/**
	 * Store an entry in the cache with the default time to live.
	 * 
	 * @param key
	 *            A unique identifier.
	 * @param data
	 *            The data to store. Make sure that you don't alter the data after it has been put
	 *            to cache as (depending on the implementation) this might lead to altering the
	 *            entry in the cache. Use the returned data instead.
	 * @return The entry that has been stored in the cache. It is save to alter this as
	 *         implementations as implementations make sure that this is always (at least) a copy.
	 * @throws CacheException
	 *             Thrown on any errors encountered, supposed to include the stacktrace (if any).
	 */
	<T extends V> T putWithErrors(K key, T data) throws CacheException;


	/**
	 * Retrieve an entry from the cache if it is available. Any errors are swallowed, use
	 * {@link Cache#getWithErrors(Object)} if you want to handle them.
	 * 
	 * @param key
	 *            The unique key under which the entry was stored.
	 * @return The cache entry if it is valid and not expired, null otherwise.
	 */
	<T extends V> T get(K key);


	/**
	 * Retrieve an entry from the cache if it is available.
	 * 
	 * @param key
	 *            The unique key under which the entry was stored.
	 * @return The cache entry if it is valid and not expired, null otherwise.
	 * @throws CacheException
	 *             Thrown on any errors encountered, supposed to include the stacktrace (if any).
	 */
	<T extends V> T getWithErrors(K key) throws CacheException;


	/**
	 * Retrieve the {@link MetaData} of a cached entry if it is available.
	 * 
	 * @param key
	 *            The key under which the entry was stored.
	 * @return The {@link MetaData} of the entry if found, null otherwise.
	 * @throws CacheException
	 *             Thrown on any errors encountered, supposed to include the stacktrace (if any).
	 */
	MetaData getMetaData(K key) throws CacheException;


	/**
	 * Retrieve the default time to live for cache entries.
	 * 
	 * @return The default time to live for a cache entry in milliseconds.
	 */
	long getDefaultTtl();


	/**
	 * Get the default max idle time for cache entries.
	 * 
	 * @return The default max idle time for cache entries in milliseconds.
	 */
	long getDefaultMaxIdleTime();


	/**
	 * Manually remove an entry from the cache.
	 * 
	 * @param key
	 *            The key under which the entry was stored.
	 * @throws CacheException
	 *             Thrown on any errors encountered, supposed to include the stacktrace (if any).
	 */
	void delete(K key) throws CacheException;


	/**
	 * Cleanup the cache. Usually this means that expired entries are deleted but this can also
	 * incorporate optimization functions etc. depending on the implementation.
	 * 
	 * @throws CacheException
	 *             Thrown on any errors encountered, supposed to include the stacktrace (if any).
	 */
	void cleanUp() throws CacheException;


	/**
	 * Get the name of this cache.
	 * 
	 * @return The name of this cache.
	 */
	String getName();


	/**
	 * Get statistics of the cache like hitrate, or size.
	 * 
	 * @return Statistics of the cache.
	 */
	CacheStatistics getStatistics();
}
