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
package net.dontdrinkandroot.cache.impl.disk.indexed;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import net.dontdrinkandroot.cache.Cache;
import net.dontdrinkandroot.cache.CacheException;
import net.dontdrinkandroot.cache.ExampleObject;
import net.dontdrinkandroot.cache.SimulationRunner;
import net.dontdrinkandroot.cache.expungestrategy.impl.LruRecyclingExpungeStrategy;
import net.dontdrinkandroot.cache.expungestrategy.impl.NoopExpungeStrategy;
import net.dontdrinkandroot.cache.impl.AbstractSerializableCustomTtlCacheTest;
import net.dontdrinkandroot.utils.lang.math.RandomUtils;
import net.dontdrinkandroot.utils.lang.time.DateUtils;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;


public class BufferedSerializableIndexedCacheTest extends AbstractSerializableCustomTtlCacheTest {

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
	public void testGetPutDelete() throws Exception {

		final AbstractIndexedDiskCache<Serializable, Serializable> cache =
				new BufferedSerializableIndexedDiskCache(
						"testCache",
						DateUtils.MILLIS_PER_MINUTE,
						Cache.UNLIMITED_IDLE_TIME,
						new NoopExpungeStrategy(),
						this.baseDir,
						new NoopExpungeStrategy());

		this.testCustomGetPutDelete(cache);
	}


	/**
	 * Tests if on putting the same key/value the filesize doesn't change as the entries get
	 * overridden.
	 */
	@Test
	public void testSameFileSizeOnPut() throws IOException, CacheException {

		final AbstractIndexedDiskCache<Serializable, Serializable> cache =
				new BufferedSerializableIndexedDiskCache(
						"testCache",
						DateUtils.MILLIS_PER_MINUTE,
						Cache.UNLIMITED_IDLE_TIME,
						new NoopExpungeStrategy(),
						this.baseDir,
						new NoopExpungeStrategy());

		cache.put("12345", "12345");
		final long dataFileSize = cache.getDataFile().length();
		final long metafileLength = cache.getIndexFile().length();
		cache.put("12345", "12345");
		Assert.assertEquals(dataFileSize, cache.getDataFile().length());
		Assert.assertEquals(metafileLength, cache.getIndexFile().length());
	}


	/**
	 * Tests if the index is reread correctly after cache restart.
	 */
	@Test
	public void testReReadIndex() throws IOException, CacheException {

		AbstractIndexedDiskCache<Serializable, Serializable> cache =
				new BufferedSerializableIndexedDiskCache(
						"testCache",
						DateUtils.MILLIS_PER_MINUTE,
						Cache.UNLIMITED_IDLE_TIME,
						new NoopExpungeStrategy(),
						this.baseDir,
						new NoopExpungeStrategy());

		/* Put 10 entries to cache */
		for (int i = 0; i < 10; i++) {
			cache.put(Integer.toString(i), new ExampleObject(i));
		}
		/* Invalidate one entry */
		cache.delete(Integer.toString(0));
		cache.close();

		cache =
				new BufferedSerializableIndexedDiskCache(
						"testCache",
						DateUtils.MILLIS_PER_MINUTE,
						Cache.UNLIMITED_IDLE_TIME,
						new NoopExpungeStrategy(),
						this.baseDir,
						new NoopExpungeStrategy());

		/* Test invalidated not there */
		Assert.assertNull(cache.get(Integer.toString(0)));

		/* Test remaining still exist */
		for (int i = 1; i < 10; i++) {
			Assert.assertEquals(new ExampleObject(i), cache.get(Integer.toString(i)));
		}
	}


	@Test
	public void runLoadTest() throws Throwable {

		Assume.assumeNotNull(System.getProperty("cache.test.runloadtest"));
		final File ramDiskDir = new File(System.getProperty("cache.test.ramdisk"));
		Assume.assumeTrue(ramDiskDir.exists());
		final File dataFile = new File(ramDiskDir, "dataFile");
		final File metaFile = new File(ramDiskDir, "metaFile");

		try {

			Logger.getRootLogger().setLevel(Level.DEBUG);

			final BufferedSerializableIndexedDiskCache loadTestCache =
					new BufferedSerializableIndexedDiskCache(
							"serializableMetaFileCacheTest",
							DateUtils.MILLIS_PER_SECOND,
							Cache.UNLIMITED_IDLE_TIME,
							new LruRecyclingExpungeStrategy(100000, .1f),
							this.baseDir,
							new LruRecyclingExpungeStrategy(10000, .1f));
			final SimulationRunner runner = new SimulationRunner() {

				@Override
				protected void loadTestPostIterationHook(final Cache<Serializable, Serializable> cache) {

					Assert.assertEquals(
							loadTestCache.getEntriesMetaData().size(),
							loadTestCache.getIndexFileNumAllocatedBlocks());
					Assert.assertEquals(
							loadTestCache.getEntriesMetaData().size() * 2,
							loadTestCache.getDataFileNumAllocatedBlocks());
				}
			};
			runner.runLoadTest(loadTestCache, 10, 1000000, RandomUtils.PARETO_EIGHTY_PERCENT_UNDER_HUNDREDTHOUSAND);

			final int size = loadTestCache.getStatistics().getCurrentSize();
			final float hitRate = loadTestCache.getStatistics().getHitRate();

			loadTestCache.close();

			Logger.getRootLogger().setLevel(Level.INFO);

			final BufferedSerializableIndexedDiskCache loadTestCacheNew =
					new BufferedSerializableIndexedDiskCache(
							"serializableMetaFileCacheTest",
							DateUtils.MILLIS_PER_SECOND,
							Cache.UNLIMITED_IDLE_TIME,
							new LruRecyclingExpungeStrategy(100000, .1f),
							this.baseDir,
							new LruRecyclingExpungeStrategy(10000, .1f));

			Logger.getRootLogger().setLevel(Level.DEBUG);

			this.getLogger().info("Hitrate: " + hitRate);

			Assert.assertEquals(size, loadTestCacheNew.getStatistics().getCurrentSize());

		} finally {
			metaFile.delete();
			dataFile.delete();
		}

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
