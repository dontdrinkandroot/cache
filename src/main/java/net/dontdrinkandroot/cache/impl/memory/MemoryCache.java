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

import java.io.Serializable;
import java.util.HashMap;

import net.dontdrinkandroot.cache.CacheException;
import net.dontdrinkandroot.cache.impl.AbstractMapBackedCustomTtlCache;
import net.dontdrinkandroot.cache.metadata.impl.SimpleMetaData;
import net.dontdrinkandroot.cache.utils.Serializer;


/**
 * @author Philip W. Sorst <philip@sorst.net>
 */
public class MemoryCache<K, V> extends AbstractMapBackedCustomTtlCache<K, V, SimpleMetaData> {

	protected final HashMap<K, V> dataMap;

	private boolean copyOnRead = true;

	private boolean copyOnWrite = true;


	public MemoryCache(final String name, final long defaultTimeToLive, int maxSize, int recycleSize) {

		super(name, defaultTimeToLive, maxSize, recycleSize);
		this.dataMap = new HashMap<K, V>();
	}


	public MemoryCache(
			final String name,
			final long defaultTimeToLive,
			final long defaultMaxIdleTime,
			int maxSize,
			int recycleSize) {

		super(name, defaultTimeToLive, maxSize, recycleSize);
		this.dataMap = new HashMap<K, V>();
	}


	@Override
	protected <T extends V> T doPut(final K key, final T data, final long timeToLive, final long maxIdleTime)
			throws CacheException {

		final SimpleMetaData metaData = new SimpleMetaData(timeToLive);
		this.putEntry(key, metaData);
		this.dataMap.put(key, data);

		/* Copy data if desired so changes after put are not reflected in cache */
		if (this.copyOnWrite) {
			return this.copyData(data);
		} else {
			return data;
		}
	}


	@Override
	protected void doDelete(K key, final SimpleMetaData metaData) throws CacheException {

		this.dataMap.remove(key);
	}


	@Override
	protected <T extends V> T doGet(K key, final SimpleMetaData metaData) throws CacheException {

		@SuppressWarnings("unchecked")
		T data = (T) this.dataMap.get(key);

		/* Copy data if desired so changes after get are not reflected in cache */
		if (this.copyOnRead) {
			return this.copyData(data);
		} else {
			return data;
		}
	}


	@SuppressWarnings("unchecked")
	protected <T extends V> T copyData(T data) {

		if (data == null) {
			return null;
		}

		if (data instanceof Serializable) {
			Serializable serializable = (Serializable) data;
			return (T) Serializer.clone(serializable);
		}

		this.getLogger().error(
				"Don't know how to copy data of type {}, override copyData() to specify. Data is returned uncopied.",
				data.getClass().toString());
		return data;
	}


	public boolean isCopyOnRead() {

		return this.copyOnRead;
	}


	public boolean isCopyOnWrite() {

		return this.copyOnWrite;
	}


	public MemoryCache<K, V> setCopyOnRead(boolean copyOnRead) {

		this.copyOnRead = copyOnRead;
		return this;
	}


	public MemoryCache<K, V> setCopyOnWrite(boolean copyOnWrite) {

		this.copyOnWrite = copyOnWrite;
		return this;
	}

}
