/**
 * Copyright (C) 2012 Philip W. Sorst <philip@sorst.net>
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
package net.dontdrinkandroot.cache.expungestrategy.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.dontdrinkandroot.cache.metadata.impl.TestMetaData;
import net.dontdrinkandroot.cache.statistics.impl.SimpleCacheStatistics;
import net.dontdrinkandroot.utils.lang.time.DateUtils;

import org.junit.Assert;
import org.junit.Test;


public class ExpiredOnlyExpungeStrategyTest {

	@Test
	public void testTriggers() throws InterruptedException {

		ExpiredOnlyExpungeStrategy strategy = new ExpiredOnlyExpungeStrategy(5);
		Assert.assertFalse(strategy.triggers(new SimpleCacheStatistics()));
		Thread.sleep(6);
		Assert.assertTrue(strategy.triggers(new SimpleCacheStatistics()));
	}


	@Test
	public void testGetToExpungeMetaData() {

		ExpiredOnlyExpungeStrategy strategy = new ExpiredOnlyExpungeStrategy(0);

		TestMetaData m1 = new TestMetaData().setExpiry(0);
		TestMetaData m2 = new TestMetaData().setExpiry(System.currentTimeMillis() + DateUtils.MILLIS_PER_DAY);

		Map<String, TestMetaData> map = new HashMap<String, TestMetaData>();
		map.put("m1", m1);
		map.put("m2", m2);

		Collection<Entry<String, TestMetaData>> toExpungeMetaData = strategy.getToExpungeMetaData(map.entrySet());
		Assert.assertEquals(1, toExpungeMetaData.size());
		Assert.assertEquals("m1", toExpungeMetaData.iterator().next().getKey());
	}

}
