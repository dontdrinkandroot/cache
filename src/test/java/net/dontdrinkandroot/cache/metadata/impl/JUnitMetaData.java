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
package net.dontdrinkandroot.cache.metadata.impl;

import net.dontdrinkandroot.cache.Cache;
import net.dontdrinkandroot.cache.metadata.MetaData;


public class JUnitMetaData implements MetaData {

	private long expiry;

	private long lastAccess;

	private long maxIdleTime;

	private long created;

	private int hits;


	public JUnitMetaData() {

	}


	@Override
	public boolean isExpired() {

		return this.expiry < System.currentTimeMillis();
	}


	@Override
	public long getExpiry() {

		return this.expiry;
	}


	@Override
	public void update() {

		this.hits++;
		this.lastAccess = System.currentTimeMillis();
	}


	@Override
	public long getHitCount() {

		return this.hits;
	}


	@Override
	public long getLastAccess() {

		return this.lastAccess;
	}


	@Override
	public long getCreated() {

		return this.created;
	}


	public JUnitMetaData setCreated(long created) {

		this.created = created;
		return this;
	}


	public JUnitMetaData setExpiry(long expiry) {

		this.expiry = expiry;
		return this;
	}


	public JUnitMetaData setHits(int hits) {

		this.hits = hits;
		return this;
	}


	public JUnitMetaData setLastAccess(long lastAccess) {

		this.lastAccess = lastAccess;
		return this;
	}


	@Override
	public boolean isIdledAway() {

		if (this.maxIdleTime == Cache.UNLIMITED_IDLE_TIME) {
			return false;
		}

		return this.lastAccess + this.maxIdleTime < System.currentTimeMillis();
	}


	@Override
	public long getMaxIdleTime() {

		return this.maxIdleTime;
	}

}