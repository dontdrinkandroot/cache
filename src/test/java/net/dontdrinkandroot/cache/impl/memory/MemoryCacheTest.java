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

import net.dontdrinkandroot.cache.Cache;
import net.dontdrinkandroot.cache.expungestrategy.impl.NoopExpungeStrategy;
import net.dontdrinkandroot.cache.impl.AbstractSerializableCustomTtlCacheTest;
import net.dontdrinkandroot.cache.impl.memory.MemoryCache;
import net.dontdrinkandroot.utils.lang.time.DateUtils;

import org.junit.Test;


public class MemoryCacheTest extends AbstractSerializableCustomTtlCacheTest {

	@Test
	public void runBasicTests() throws Exception {

		final MemoryCache<Serializable, Serializable> cache =
				new MemoryCache<Serializable, Serializable>(
						"testCache",
						DateUtils.MILLIS_PER_DAY,
						Cache.UNLIMITED_IDLE_TIME,
						new NoopExpungeStrategy());

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
}
