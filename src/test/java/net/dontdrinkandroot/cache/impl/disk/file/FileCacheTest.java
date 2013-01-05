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
package net.dontdrinkandroot.cache.impl.disk.file;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import net.dontdrinkandroot.cache.AbstractCacheTest;
import net.dontdrinkandroot.cache.Cache;
import net.dontdrinkandroot.cache.expungestrategy.impl.NoopExpungeStrategy;
import net.dontdrinkandroot.utils.lang.time.DateUtils;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class FileCacheTest extends AbstractCacheTest<Md5, File> {

	private final static File baseDir = new File(FileUtils.getTempDirectory(), "filefilebackedcachetest");


	@AfterClass
	public static void afterClass() throws IOException {

		FileUtils.deleteDirectory(FileCacheTest.baseDir);
	}


	@Before
	public void before() throws IOException {

		FileUtils.deleteDirectory(FileCacheTest.baseDir);
	}


	@Test
	public void testDefaultGetPutDelete() throws Exception {

		FileCache cache =
				new FileCache(
						"testCache",
						DateUtils.MILLIS_PER_MINUTE,
						Cache.UNLIMITED_IDLE_TIME,
						new NoopExpungeStrategy(),
						FileCacheTest.baseDir,
						2);

		super.testDefaultPutGetDelete(cache);
	}


	@Test
	public void testReReadIndex() throws Exception {

		FileCache cache =
				new FileCache(
						"testCache",
						DateUtils.MILLIS_PER_MINUTE,
						Cache.UNLIMITED_IDLE_TIME,
						new NoopExpungeStrategy(),
						FileCacheTest.baseDir,
						2);

		cache.put(this.translateKey(0), this.createInputObject(0));
		this.doAssertGet(0, cache);
		cache.put(this.translateKey(1), this.createInputObject(1));
		this.doAssertGet(1, cache);
		cache.put(this.translateKey(2), this.createInputObject(2));
		this.doAssertGet(2, cache);

		cache =
				new FileCache(
						"testCache",
						DateUtils.MILLIS_PER_MINUTE,
						Cache.UNLIMITED_IDLE_TIME,
						new NoopExpungeStrategy(),
						FileCacheTest.baseDir,
						2);

		this.doAssertGet(0, cache);
		this.doAssertGet(1, cache);
		this.doAssertGet(2, cache);
	}


	@Test
	public void testDefaultExpiry() throws Exception {

		final FileCache cache =
				new FileCache(
						"testCache",
						0L,
						Cache.UNLIMITED_IDLE_TIME,
						new NoopExpungeStrategy(),
						FileCacheTest.baseDir,
						2);

		super.testDefaultExpiry(cache);
	}


	@Override
	protected void doAssertGet(int key, Cache<Md5, File> cache) throws Exception {

		File file = cache.get(this.translateKey(key));
		Assert.assertNotNull(file);

		final List<String> lines = FileUtils.readLines(file);
		Assert.assertEquals(1, lines.size());
		Assert.assertEquals(this.translateKey(key).getHex(), lines.iterator().next());
	}


	@Override
	protected File createInputObject(int key) throws Exception {

		File file = File.createTempFile("filecachetest", ".tmp");
		file.deleteOnExit();
		FileUtils.writeLines(file, Collections.singleton(this.translateKey(key).getHex()));

		return file;
	}


	@Override
	protected Md5 translateKey(int key) {

		final StringBuffer sb = new StringBuffer();
		for (int i = -1; i < key % 100; i++) {
			sb.append(Long.toString(key));
		}

		return new Md5(sb.toString());
	}

}
