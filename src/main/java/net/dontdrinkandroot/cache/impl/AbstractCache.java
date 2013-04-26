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

import net.dontdrinkandroot.cache.Cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Philip W. Sorst <philip@sorst.net>
 */
public abstract class AbstractCache<K, V> implements Cache<K, V> {

	private final String name;

	/** Normal logger */
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	/** Aggregated logger for cache entries */
	private final Logger cleanUpLogger = LoggerFactory.getLogger("CleanUp");

	/** Default time to live for cache entries */
	private long defaultTimeToLive;

	/** Default max idle time for cache entries */
	private long defaultMaxIdleTime;


	public AbstractCache(final String name, long defaultTimeToLive) {

		this(name, defaultTimeToLive, Cache.UNLIMITED_IDLE_TIME);
	}


	public AbstractCache(final String name, long defaultTimeToLive, long defaultMaxIdleTime) {

		this.name = name;
		this.defaultTimeToLive = defaultTimeToLive;
		this.defaultMaxIdleTime = defaultMaxIdleTime;
	}


	@Override
	public final String getName() {

		return this.name;
	}


	@Override
	public final long getDefaultTtl() {

		return this.defaultTimeToLive;
	}


	@Override
	public final long getDefaultMaxIdleTime() {

		return this.defaultMaxIdleTime;
	}


	public final Logger getLogger() {

		return this.logger;
	}


	public final Logger getCleanUpLogger() {

		return this.cleanUpLogger;
	}


	@Override
	public final void setDefaultTtl(final long defaultTTL) {

		this.defaultTimeToLive = defaultTTL;
	}


	public final void setDefaultMaxIdleTime(long defaultMaxIdleTime) {

		this.defaultMaxIdleTime = defaultMaxIdleTime;
	}

}
