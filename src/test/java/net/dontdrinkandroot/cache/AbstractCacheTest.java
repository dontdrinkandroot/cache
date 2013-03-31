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

import net.dontdrinkandroot.cache.statistics.CacheStatistics;

import org.junit.Assert;


public abstract class AbstractCacheTest<K, V> {

	private int currentId = 0;

	protected int putCount = 0;

	protected int getCount = 0;

	private int notFoundCount = 0;

	protected int expiredCount = 0;

	private int hitCount = 0;

	protected int size = 0;


	protected void testDefaultPutGetDelete(Cache<K, V> cache) throws Exception {

		/* Simple put get delete */
		this.put(this.increaseAndGetCurrentId(), cache);
		this.assertGet(this.getCurrentId(), cache);
		this.delete(this.getCurrentId(), cache);
		this.assertNotFound(this.getCurrentId(), cache);

		/* Put get multiple times */
		for (int i = 0; i < 10; i++) {
			this.put(this.increaseAndGetCurrentId(), cache);
			this.assertGet(this.getCurrentId(), cache);
		}

		/* Accessing more than one time */
		for (int i = 0; i < 10; i++) {
			this.put(this.increaseAndGetCurrentId(), cache);
			this.assertGet(this.getCurrentId(), cache);
			this.assertGet(this.getCurrentId(), cache);
		}
	}


	protected void testDefaultExpiry(Cache<K, V> cache) throws Exception {

		Assert.assertEquals(0L, cache.getDefaultTtl());
		cache.putWithErrors(this.translateKey(0), this.createInputObject(0));
		Assert.assertEquals(1L, cache.getStatistics().getPutCount());
		Thread.sleep(1);
		Assert.assertNull(cache.getWithErrors(this.translateKey(0)));
		Assert.assertEquals(1L, cache.getStatistics().getGetCount());
		Assert.assertEquals(1L, cache.getStatistics().getCacheMisses());
		Assert.assertEquals(0L, cache.getStatistics().getCacheMissesNotFound());
		Assert.assertEquals(1L, cache.getStatistics().getCacheMissesExpired());
		Assert.assertEquals(0L, cache.getStatistics().getCurrentSize());
	}


	private void assertNotFound(int key, Cache<K, V> cache) throws Exception {

		Assert.assertNull(cache.getWithErrors(this.translateKey(key)));
		this.getCount++;
		this.notFoundCount++;
		this.assertStatistics(cache);
	}


	protected void delete(int key, Cache<K, V> cache) throws Exception {

		cache.delete(this.translateKey(key));
		this.size--;
		this.assertStatistics(cache);
	}


	protected abstract K translateKey(int key);


	protected int getCurrentId() {

		return this.currentId;
	}


	protected int increaseAndGetCurrentId() {

		return ++this.currentId;
	}


	protected V put(int key, Cache<K, V> cache) throws Exception {

		V object = cache.putWithErrors(this.translateKey(key), this.createInputObject(key));
		this.putCount++;
		this.size++;
		this.assertStatistics(cache);

		return object;
	}


	protected void assertStatistics(Cache<K, V> cache) {

		CacheStatistics stats = cache.getStatistics();
		Assert.assertEquals(this.getCount, stats.getGetCount());
		Assert.assertEquals(this.putCount, stats.getPutCount());
		Assert.assertEquals(this.hitCount, stats.getCacheHits());
		Assert.assertEquals(this.expiredCount, stats.getCacheMissesExpired());
		Assert.assertEquals(this.notFoundCount, stats.getCacheMissesNotFound());
		Assert.assertEquals(this.expiredCount + this.notFoundCount, stats.getCacheMisses());
		Assert.assertEquals(this.size, stats.getCurrentSize());
	}


	protected void assertGet(int key, Cache<K, V> cache) throws Exception {

		this.doAssertGet(key, cache);
		this.getCount++;
		this.hitCount++;
		this.assertStatistics(cache);
	}


	protected abstract void doAssertGet(int key, Cache<K, V> cache) throws Exception;


	protected abstract V createInputObject(int key) throws Exception;

	// protected abstract O createOutputObject(String key);
}
