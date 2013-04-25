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
package net.dontdrinkandroot.cache.impl.memory;

import net.dontdrinkandroot.cache.CacheException;
import net.dontdrinkandroot.cache.CustomTtlCache;
import net.dontdrinkandroot.cache.metadata.MetaData;
import net.dontdrinkandroot.cache.statistics.CacheStatistics;
import net.dontdrinkandroot.cache.statistics.impl.SimpleCacheStatistics;


/**
 * A serializable cache that does not actually perform any caching operation.
 * 
 * @author Philip W. Sorst <philip@sorst.net>
 */
public class NoopCache<K, V> implements CustomTtlCache<K, V> {

	private String name;


	public NoopCache(final String name) {

		this.name = name;
	}


	@Override
	public <T extends V> T putWithErrors(final K key, final T data) throws CacheException {

		return data;
	}


	@Override
	public <T extends V> T getWithErrors(final K key) throws CacheException {

		return null;
	}


	@Override
	public MetaData getMetaData(K key) throws CacheException {

		return null;
	}


	public void setDefaultTtl(final long defaultTTL) {

	}


	@Override
	public long getDefaultTtl() {

		return 0;
	}


	@Override
	public void delete(final K key) throws CacheException {

	}


	@Override
	public void expunge() {

	}


	public void setName(final String name) {

		this.name = name;
	}


	@Override
	public String getName() {

		return this.name;
	}


	@Override
	public <T extends V> T putWithErrors(final K key, final T data, final long timeToLive) throws CacheException {

		return data;
	}


	@Override
	public CacheStatistics getStatistics() {

		return new SimpleCacheStatistics();
	}


	@Override
	public long getDefaultMaxIdleTime() {

		return 0;
	}


	@Override
	public <T extends V> T putWithErrors(K key, T data, long timeToLive, long maxIdleTime) throws CacheException {

		return data;
	}


	@Override
	public <T extends V> T put(K key, T data) {

		return data;
	}


	@Override
	public <T extends V> T get(K key) {

		return null;
	}


	@Override
	public <T extends V> T put(K key, T data, long timeToLive) {

		return data;
	}


	@Override
	public <T extends V> T put(K key, T data, long timeToLive, long maxIdleTime) {

		return data;
	}
}
