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

/**
 * A {@link Cache} that permits storing entries with an individual time to live and an individual
 * idle time.
 * 
 * @author Philip W. Sorst <philip@sorst.net>
 */
public interface CustomTtlCache<K, V> extends Cache<K, V> {

	/**
	 * Store an entry in the cache with a specific time to live. Any errors are swallowed, use
	 * {@link CustomTtlCache#put(Object, Object, long)} if you want to handle them.
	 * 
	 * 
	 * @param id
	 *            A unique identifier.
	 * @param data
	 *            The data to store, make sure that you don't alter the data after it has been put
	 *            to cache as (depending on the implementation) this might lead to altering the
	 *            entry in the cache. Use the returned entry instead.
	 * @param timeToLive
	 *            The time (in milliseconds) after which the entry expires.
	 * @return The entry that has been stored in the cache. It is save to alter this as
	 *         implementations as implmentations make sure that this is always a copy.
	 */
	V put(K key, V data, long timeToLive);


	/**
	 * Store an entry in the cache with a specific time to live.
	 * 
	 * 
	 * @param id
	 *            A unique identifier.
	 * @param data
	 *            The data to store, make sure that you don't alter the data after it has been put
	 *            to cache as (depending on the implementation) this might lead to altering the
	 *            entry in the cache. Use the returned entry instead.
	 * @param timeToLive
	 *            The time (in milliseconds) after which the entry expires.
	 * @return The entry that has been stored in the cache. It is save to alter this as
	 *         implementations as implmentations make sure that this is always a copy.
	 * @throws CacheException
	 *             Thrown if the storage fails.
	 */
	V putWithErrors(K key, V data, long timeToLive) throws CacheException;


	/**
	 * Store an entry in the cache with a specific time to live and max idle time. Any errors are
	 * swallowed, use {@link CustomTtlCache#putWithErrors(Object, Object, long)} if you want to
	 * handle them.
	 * 
	 * 
	 * @param id
	 *            A unique identifier.
	 * @param data
	 *            The data to store, make sure that you don't alter the data after it has been put
	 *            to cache as (depending on the implementation) this might lead to altering the
	 *            entry in the cache. Use the returned entry instead.
	 * @param timeToLive
	 *            The time (in milliseconds) after which the entry expires.
	 * @param maxIdleTime
	 *            The time (in milliseconds) that an entry may idle (not being accessed) before
	 *            being expunged.
	 * @return The entry that has been stored in the cache. It is save to alter this as
	 *         implementations as implmentations make sure that this is always a copy.
	 * @throws CacheException
	 *             Thrown if the storage fails.
	 */
	V put(K key, V data, long timeToLive, long maxIdleTime);


	/**
	 * Store an entry in the cache with a specific time to live and max idle time.
	 * 
	 * 
	 * @param id
	 *            A unique identifier.
	 * @param data
	 *            The data to store, make sure that you don't alter the data after it has been put
	 *            to cache as (depending on the implementation) this might lead to altering the
	 *            entry in the cache. Use the returned entry instead.
	 * @param timeToLive
	 *            The time (in milliseconds) after which the entry expires.
	 * @param maxIdleTime
	 *            The time (in milliseconds) that an entry may idle (not being accessed) before
	 *            being expunged.
	 * @return The entry that has been stored in the cache. It is save to alter this as
	 *         implementations as implmentations make sure that this is always a copy.
	 * @throws CacheException
	 *             Thrown if the storage fails.
	 */
	V putWithErrors(K key, V data, long timeToLive, long maxIdleTime) throws CacheException;

}
