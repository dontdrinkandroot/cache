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
package net.dontdrinkandroot.cache.impl.memory;

import java.io.Serializable;

import net.dontdrinkandroot.cache.Cache;
import net.dontdrinkandroot.cache.CacheException;
import net.dontdrinkandroot.cache.impl.AbstractSerializableCustomTtlCacheTest;
import net.dontdrinkandroot.cache.metadata.MetaData;
import net.dontdrinkandroot.cache.utils.Duration;

import org.junit.Assert;
import org.junit.Test;


public class MemoryCacheTest extends AbstractSerializableCustomTtlCacheTest {

	@Test
	public void runBasicTests() throws Exception {

		final MemoryCache<Serializable, Serializable> cache =
				new MemoryCache<Serializable, Serializable>(
						"testCache",
						Duration.days(1),
						Cache.UNLIMITED_IDLE_TIME,
						Integer.MAX_VALUE,
						Integer.MAX_VALUE);

		this.testCustomGetPutDelete(cache);
	}


	@Override
	protected Serializable translateKey(int key) {

		final StringBuffer sb = new StringBuffer();
		for (int i = -1; i < key % 100; i++) {
			sb.append(Long.toString(key));
		}

		return sb.toString();
	}


	@Test
	public void testLfuDecay() throws InterruptedException, CacheException {

		final MemoryCache<Serializable, Serializable> cache =
				new MemoryCache<Serializable, Serializable>(
						"testCache",
						Duration.days(1),
						Cache.UNLIMITED_IDLE_TIME,
						3,
						2);

		cache.put("1", "1");
		Assert.assertEquals(1, cache.getStatistics().getCurrentSize());
		Thread.sleep(1);

		Assert.assertEquals("1", cache.get("1"));
		Assert.assertEquals(1, cache.getStatistics().getCurrentSize());
		Assert.assertEquals(1, cache.getStatistics().getCacheHits());
		Assert.assertEquals(2, cache.getMetaData("1").getHitCount());
		Thread.sleep(1);

		cache.put("2", "2");
		Assert.assertEquals(2, cache.getStatistics().getCurrentSize());
		Thread.sleep(1);

		cache.put("3", "3");
		Assert.assertEquals(3, cache.getStatistics().getCurrentSize());
		Thread.sleep(1);

		cache.put("4", "4");
		Assert.assertEquals(4, cache.getStatistics().getCurrentSize());
		Thread.sleep(1);

		cache.put("5", "5");
		Assert.assertEquals(5, cache.getStatistics().getCurrentSize());
		Thread.sleep(1);

		cache.put("6", "6");
		Assert.assertEquals(3, cache.getStatistics().getCurrentSize());
		Thread.sleep(1);

		MetaData metaData = cache.getMetaData("1");
		Assert.assertEquals(1, metaData.getHitCount());

		Assert.assertNull(cache.get("2"));
		Assert.assertNull(cache.get("3"));
		Assert.assertNull(cache.get("4"));

		cache.put("7", "6");
		Assert.assertEquals(4, cache.getStatistics().getCurrentSize());
		Thread.sleep(1);

		cache.put("8", "6");
		Assert.assertEquals(5, cache.getStatistics().getCurrentSize());
		Thread.sleep(1);

		cache.put("9", "6");
		Assert.assertEquals(3, cache.getStatistics().getCurrentSize());
		Thread.sleep(1);

		/* "1" should now be expunged as it is the oldest entry with hit count 1 */
		Assert.assertNull(cache.get("1"));
		Assert.assertNull(cache.get("5"));
		Assert.assertNull(cache.get("6"));

	}
}
