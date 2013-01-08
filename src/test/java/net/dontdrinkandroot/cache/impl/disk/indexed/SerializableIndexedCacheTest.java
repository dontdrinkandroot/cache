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
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import net.dontdrinkandroot.cache.Cache;
import net.dontdrinkandroot.cache.CacheException;
import net.dontdrinkandroot.cache.ExampleObject;
import net.dontdrinkandroot.cache.SimulationRunner;
import net.dontdrinkandroot.cache.TestUtils;
import net.dontdrinkandroot.cache.expungestrategy.impl.LruRecyclingExpungeStrategy;
import net.dontdrinkandroot.cache.expungestrategy.impl.NoopExpungeStrategy;
import net.dontdrinkandroot.cache.impl.AbstractSerializableCustomTtlCacheTest;
import net.dontdrinkandroot.cache.utils.Duration;
import net.dontdrinkandroot.cache.utils.FileUtils;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;


public class SerializableIndexedCacheTest extends AbstractSerializableCustomTtlCacheTest {

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
				new SerializableIndexedDiskCache(
						"testCache",
						Duration.minutes(1),
						Cache.UNLIMITED_IDLE_TIME,
						new NoopExpungeStrategy(),
						this.baseDir);

		this.testCustomGetPutDelete(cache);
	}


	/**
	 * Tests if on putting the same key/value the filesize doesn't change as the entries get
	 * overridden.
	 */
	@Test
	public void testSameFileSizeOnPut() throws IOException, CacheException {

		final AbstractIndexedDiskCache<Serializable, Serializable> cache =
				new SerializableIndexedDiskCache(
						"testCache",
						Duration.minutes(1),
						Cache.UNLIMITED_IDLE_TIME,
						new NoopExpungeStrategy(),
						this.baseDir);

		cache.put("12345", "12345");
		final long dataFileSize = cache.dataFile.length();
		final long metafileLength = cache.indexFile.length();
		cache.put("12345", "12345");
		Assert.assertEquals(dataFileSize, cache.dataFile.length());
		Assert.assertEquals(metafileLength, cache.indexFile.length());
	}


	@Test
	public void testInvalidGet() throws Exception {

		final SerializableIndexedDiskCache cache =
				new SerializableIndexedDiskCache(
						"testCache",
						TestUtils.getFutureExpiry(),
						Cache.UNLIMITED_IDLE_TIME,
						new NoopExpungeStrategy(),
						this.baseDir);

		cache.put("1", new ExampleObject(3));

		final RandomAccessFile data = new RandomAccessFile(cache.dataFile.getFileName(), "rw");
		data.seek(1);
		for (int i = 0; i < 30; i++) {
			data.writeLong(0);
		}
		data.close();

		try {
			cache.get("1");
			throw new Exception("Exception expected");
		} catch (final CacheException e) {
			/* Expected */
		}

		Assert.assertNull(cache.get("1"));

		cache.put("1", new ExampleObject(3));
		Assert.assertEquals(new ExampleObject(3), cache.get("1"));
		cache.delete("1");

		Assert.assertEquals(0, cache.getStatistics().getCurrentSize());
		Assert.assertEquals(0, cache.dataFile.length());
		// Assert.assertEquals(0, SerializableIndexedCacheTest.metaDataFile.length());
	}


	@Test
	public void testDuplicatePut() throws IOException, CacheException {

		final SerializableIndexedDiskCache cache =
				new SerializableIndexedDiskCache(
						"testCache",
						Duration.minutes(1),
						Cache.UNLIMITED_IDLE_TIME,
						new NoopExpungeStrategy(),
						this.baseDir);

		cache.put("1", new ExampleObject(3));

		final long metaFileSize = cache.dataFile.length();
		final long dataFileSize = cache.indexFile.length();

		cache.put("1", new ExampleObject(3));

		Assert.assertEquals(new ExampleObject(3), cache.get("1"));
		Assert.assertEquals(metaFileSize, cache.dataFile.length());
		Assert.assertEquals(dataFileSize, cache.indexFile.length());
	}


	@Test
	public void testLockFile() throws IOException {

		new SerializableIndexedDiskCache(
				"testCache",
				Duration.minutes(1),
				Cache.UNLIMITED_IDLE_TIME,
				new NoopExpungeStrategy(),
				this.baseDir);

		try {
			new SerializableIndexedDiskCache(
					"testCache",
					Duration.minutes(1),
					Cache.UNLIMITED_IDLE_TIME,
					new NoopExpungeStrategy(),
					this.baseDir);
			Assert.fail("IOException expected");
		} catch (IOException e) {
			/* Expected */
			Assert.assertTrue(e.getMessage().startsWith("Lock file found"));
		}
	}


	@Test
	public void testReReadIndex() throws IOException, CacheException {

		AbstractIndexedDiskCache<Serializable, Serializable> cache =
				new SerializableIndexedDiskCache(
						"testCache",
						Duration.minutes(1),
						Cache.UNLIMITED_IDLE_TIME,
						new NoopExpungeStrategy(),
						this.baseDir);

		for (int i = 0; i < 10; i++) {
			cache.put(Integer.toString(i), new ExampleObject(i));
		}
		/* Invalidate one entry */
		cache.delete(Integer.toString(0));
		cache.close();

		cache =
				new SerializableIndexedDiskCache(
						"testCache",
						Duration.minutes(1),
						Cache.UNLIMITED_IDLE_TIME,
						new NoopExpungeStrategy(),
						this.baseDir);

		/* Test invalidated not there */
		Assert.assertNull(cache.get(Integer.toString(0)));

		for (int i = 1; i < 10; i++) {
			Assert.assertEquals(new ExampleObject(i), cache.get(Integer.toString(i)));
		}
	}


	@Test
	public void runLoadTest() throws Throwable {

		Assume.assumeNotNull(System.getProperty("cache.test.runloadtest"));
		final File ramDiskDir = new File(System.getProperty("cache.test.ramdisk"));
		Assume.assumeTrue(ramDiskDir.exists());

		Logger.getRootLogger().setLevel(Level.DEBUG);

		final File dataFile = new File(ramDiskDir, "dataFile");
		final File metaFile = new File(ramDiskDir, "metaFile");

		try {

			final SerializableIndexedDiskCache loadTestCache =
					new SerializableIndexedDiskCache(
							"serializableMetaFileCacheTest",
							Duration.seconds(1),
							Cache.UNLIMITED_IDLE_TIME,
							new LruRecyclingExpungeStrategy(100000, .1f),
							this.baseDir);
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
			runner.runLoadTest(loadTestCache, 1, 100000, TestUtils.PARETO_EIGHTY_PERCENT_UNDER_HUNDREDTHOUSAND);

			final int size = loadTestCache.getStatistics().getCurrentSize();
			final float hitRate = loadTestCache.getStatistics().getHitRate();

			loadTestCache.close();

			Logger.getRootLogger().setLevel(Level.INFO);

			final SerializableIndexedDiskCache loadTestCacheNew =
					new SerializableIndexedDiskCache(
							"serializableMetaFileCacheTest",
							Duration.seconds(1),
							Cache.UNLIMITED_IDLE_TIME,
							new LruRecyclingExpungeStrategy(100000, .1f),
							this.baseDir);

			Logger.getRootLogger().setLevel(Level.DEBUG);

			this.getLogger().info("Hitrate: " + hitRate);

			Assert.assertEquals(size, loadTestCacheNew.getStatistics().getCurrentSize());

		} finally {
			metaFile.delete();
			dataFile.delete();
		}
	}


	@Test
	public void runKnownTest() throws Throwable {

		// Logger.getRootLogger().setLevel(Level.INFO);

		Assume.assumeNotNull(System.getProperty("cache.test.runloadtest"));
		final File ramDiskDir = new File(System.getProperty("cache.test.ramdisk"));
		Assume.assumeTrue(ramDiskDir.exists());
		final File dataFile = new File(ramDiskDir, "dataFile");
		final File metaFile = new File(ramDiskDir, "metaFile");

		try {

			final SerializableIndexedDiskCache loadTestCache =
					new SerializableIndexedDiskCache(
							"serializableMetaFileCacheTest",
							Duration.days(1),
							Cache.UNLIMITED_IDLE_TIME,
							new NoopExpungeStrategy(),
							this.baseDir);

			Set<Long> known = new HashSet<Long>();
			final SimulationRunner runner = new SimulationRunner() {

				@Override
				protected void knownTestpostIterationHook(final Cache<Serializable, Serializable> cache) {

					Assert.assertEquals(
							loadTestCache.getEntriesMetaData().size(),
							loadTestCache.getIndexFileNumAllocatedBlocks());
					Assert.assertEquals(
							loadTestCache.getEntriesMetaData().size() * 2,
							loadTestCache.getDataFileNumAllocatedBlocks());
				}
			};
			known =
					runner.runKnownTest(loadTestCache, 1, 100000, TestUtils.PARETO_EIGHTY_PERCENT_UNDER_THOUSAND, known);

			loadTestCache.close();

			final SerializableIndexedDiskCache loadTestCacheNew =
					new SerializableIndexedDiskCache(
							"serializableMetaFileCacheTest",
							Duration.days(1),
							Cache.UNLIMITED_IDLE_TIME,
							new NoopExpungeStrategy(),
							this.baseDir);

			known =
					runner.runKnownTest(
							loadTestCacheNew,
							1,
							100000,
							TestUtils.PARETO_EIGHTY_PERCENT_UNDER_THOUSAND,
							known);

			this.getLogger().info(loadTestCacheNew.getStatistics().toString());

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
