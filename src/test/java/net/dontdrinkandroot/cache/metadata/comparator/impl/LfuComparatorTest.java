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
package net.dontdrinkandroot.cache.metadata.comparator.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import net.dontdrinkandroot.cache.metadata.impl.TestMetaData;

import org.junit.Assert;
import org.junit.Test;


public class LfuComparatorTest {

	@Test
	public void test() {

		Map<String, TestMetaData> map = new HashMap<String, TestMetaData>();

		TestMetaData m1 = new TestMetaData();
		m1.setExpiry(2);
		m1.setHits(1);
		m1.setLastAccess(1);
		m1.setCreated(0);
		map.put("one", m1);

		TestMetaData m2 = new TestMetaData();
		m2.setExpiry(2);
		m2.setHits(2);
		m2.setLastAccess(2);
		m2.setCreated(1);
		map.put("two", m2);

		TestMetaData m3 = new TestMetaData();
		m3.setExpiry(2);
		m3.setHits(2);
		m3.setLastAccess(1);
		m3.setCreated(2);
		map.put("three", m3);

		TestMetaData m4 = new TestMetaData();
		m4.setExpiry(2);
		m4.setLastAccess(2);
		m4.setHits(2);
		m4.setCreated(4);
		map.put("four", m4);

		LfuComparator<String, TestMetaData> comparator = new LfuComparator<String, TestMetaData>();
		TreeSet<Entry<String, TestMetaData>> set = new TreeSet<Entry<String, TestMetaData>>(comparator);
		set.addAll(map.entrySet());
		Assert.assertEquals(4, set.size());

		Iterator<Entry<String, TestMetaData>> iterator = set.iterator();
		Assert.assertEquals("one", iterator.next().getKey());
		Assert.assertEquals("three", iterator.next().getKey());
		Assert.assertEquals("two", iterator.next().getKey());
		Assert.assertEquals("four", iterator.next().getKey());
	}
}
