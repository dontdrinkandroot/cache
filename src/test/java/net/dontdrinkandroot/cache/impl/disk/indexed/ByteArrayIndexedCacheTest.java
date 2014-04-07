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
package net.dontdrinkandroot.cache.impl.disk.indexed;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import net.dontdrinkandroot.cache.AbstractCustomTtlCacheTest;
import net.dontdrinkandroot.cache.Cache;
import net.dontdrinkandroot.cache.utils.Duration;
import net.dontdrinkandroot.cache.utils.FileUtils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class ByteArrayIndexedCacheTest extends AbstractCustomTtlCacheTest<Serializable, byte[]> {

	private File baseDir;


	@Before
	public void before() throws IOException {

		this.baseDir = File.createTempFile("cachetest", null);
		this.baseDir.delete();
	}


	@After
	public void after() throws IOException {

		FileUtils.deleteDirectory(this.baseDir);
	}


	@Test
	public void testPutGetDelete() throws Exception {

		ByteArrayIndexedDiskCache cache =
				new ByteArrayIndexedDiskCache(
						"testCache",
						Duration.minutes(1),
						Cache.UNLIMITED_IDLE_TIME,
						Integer.MAX_VALUE,
						Integer.MAX_VALUE,
						this.baseDir);

		Assert.assertEquals(0, cache.getDataFileNumAllocatedBlocks());
		Assert.assertEquals(0, cache.getMetaFileNumAllocatedBlocks());
		cache.assertAllocatedConsistency();

		this.testCustomGetPutDelete(cache);

		cache.close();
	}


	@Override
	protected void doAssertGet(int key, Cache<Serializable, byte[]> cache) throws Exception {

		byte[] bytes = cache.getWithErrors(this.translateKey(key));
		Assert.assertNotNull(bytes);
		Assert.assertArrayEquals(this.createInputObject(key), bytes);
	}


	@Override
	protected byte[] createInputObject(int key) throws Exception {

		final StringBuffer s = new StringBuffer();
		for (int i = -1; i < key % 100; i++) {
			s.append(Long.toString(key));
		}

		return s.toString().getBytes("UTF-8");
	}


	@Override
	protected String translateKey(int key) {

		final StringBuffer sb = new StringBuffer();
		for (int i = -1; i < key % 100; i++) {
			sb.append(Long.toString(key));
		}

		return sb.toString();
	}

}
