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
package net.dontdrinkandroot.cache.statistics.impl;

import net.dontdrinkandroot.cache.statistics.CacheStatistics;


/**
 * @author Philip W. Sorst <philip@sorst.net>
 */
public class SimpleCacheStatistics implements CacheStatistics
{

	private static final long serialVersionUID = 1L;

	private long cacheHits;

	private long cacheMissesNotFound;

	private long cacheMissesExpired;

	private long putCount;

	private long getCount;

	private int currentSize = 0;


	public SimpleCacheStatistics()
	{
		this.cacheHits = 0;
		this.cacheMissesNotFound = 0;
		this.cacheMissesExpired = 0;
		this.putCount = 0;
		this.getCount = 0;
	}


	@Override
	public long getCacheHits()
	{
		return this.cacheHits;
	}


	public void setCacheHits(final long cacheHits)
	{
		this.cacheHits = cacheHits;
	}


	@Override
	public long getCacheMissesNotFound()
	{
		return this.cacheMissesNotFound;
	}


	public void setCacheMissesNotFound(final long cacheMissesNotFound)
	{
		this.cacheMissesNotFound = cacheMissesNotFound;
	}


	@Override
	public long getCacheMissesExpired()
	{
		return this.cacheMissesExpired;
	}


	public void setCacheMissesExpired(final long cacheMissesExpired)
	{
		this.cacheMissesExpired = cacheMissesExpired;
	}


	@Override
	public long getPutCount()
	{
		return this.putCount;
	}


	public void setPutCount(final long putCount)
	{
		this.putCount = putCount;
	}


	@Override
	public long getGetCount()
	{
		return this.getCount;
	}


	public void setGetCount(final long getCount)
	{
		this.getCount = getCount;
	}


	@Override
	public void reset()
	{
		this.cacheHits = 0;
		this.cacheMissesNotFound = 0;
		this.cacheMissesExpired = 0;
		this.putCount = 0;
		this.getCount = 0;
	}


	@Override
	public float getHitRate()
	{
		final long cacheHits = this.getCacheHits();
		final long cacheMisses = this.getCacheMisses();

		/* Avoid division by zero */
		if (cacheMisses == 0 && cacheHits == 0) {
			return 0f;
		}

		return (float) cacheHits / (cacheHits + cacheMisses);
	}


	@Override
	public long getCacheMisses()
	{
		return this.cacheMissesExpired + this.cacheMissesNotFound;
	}


	@Override
	public int getCurrentSize()
	{
		return this.currentSize;
	}


	public void setCurrentSize(final int currentSize)
	{
		this.currentSize = currentSize;
	}


	public void increasePutCount()
	{
		this.putCount++;
	}


	public void increaseGetCount()
	{
		this.getCount++;
	}


	public void increaseCacheMissesNotFound()
	{
		this.cacheMissesNotFound++;
	}


	public void increaseCacheMissesExpired()
	{
		this.cacheMissesExpired++;
	}


	public void increaseCacheHits()
	{
		this.cacheHits++;
	}


	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("hitRate: " + this.getHitRate());
		sb.append(",size: " + this.getCurrentSize());
		sb.append(",hits: " + this.getCacheHits());
		sb.append(",missesNotFound: " + this.getCacheMissesNotFound());
		sb.append(",missesExpired: " + this.getCacheMissesExpired());
		sb.append(",getCount: " + this.getGetCount());
		sb.append(",putCount: " + this.getPutCount());

		return sb.toString();
	}

}