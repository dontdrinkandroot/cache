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
package net.dontdrinkandroot.cache.impl.disk.indexed.storage;

import java.io.Serializable;

import net.dontdrinkandroot.cache.metadata.MetaData;
import net.dontdrinkandroot.cache.metadata.impl.SimpleMetaData;


public class KeyedMetaData<K extends Serializable> implements Serializable {

	private final K key;

	private final long created;

	private final long maxIdleTime;

	private final long timeToLive;


	public KeyedMetaData(K key, MetaData metaData) {

		this.key = key;
		this.created = metaData.getCreated();
		this.timeToLive = metaData.getTimeToLive();
		this.maxIdleTime = metaData.getMaxIdleTime();
	}


	public K getKey() {

		return this.key;
	}


	public long getCreated() {

		return this.created;
	}


	public long getMaxIdleTime() {

		return this.maxIdleTime;
	}


	public SimpleMetaData getMetaData() {

		return new SimpleMetaData(this.created, this.timeToLive, this.maxIdleTime);
	}

}
