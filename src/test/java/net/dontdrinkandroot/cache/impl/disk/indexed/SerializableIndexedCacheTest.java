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
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import net.dontdrinkandroot.cache.Cache;
import net.dontdrinkandroot.cache.CacheException;
import net.dontdrinkandroot.cache.ExampleObject;
import net.dontdrinkandroot.cache.JUnitUtils;
import net.dontdrinkandroot.cache.SimulationRunner;
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


public class SerializableIndexedCacheTest extends AbstractSerializableCustomTtlCacheTest
{

	private File baseDir;


	@Before
	public void before() throws IOException
	{
		this.baseDir = File.createTempFile("cachetest", null);
		this.baseDir.delete();
	}


	@After
	public void after() throws IOException
	{
		FileUtils.deleteDirectory(this.baseDir);
	}


	@Test
	public void testGetPutDelete() throws Exception
	{
		final AbstractIndexedDiskCache<Serializable, Serializable> cache =
				new SerializableIndexedDiskCache(
						"testCache",
						Duration.minutes(1),
						Cache.UNLIMITED_IDLE_TIME,
						1000,
						1000,
						this.baseDir);

		this.testCustomGetPutDelete(cache);

		// cache.close();
	}


	/**
	 * Tests if on putting the same key/value the filesize doesn't change as the entries get overridden.
	 */
	@Test
	public void testSameFileSizeOnPut() throws IOException, CacheException
	{
		final AbstractIndexedDiskCache<Serializable, Serializable> cache =
				new SerializableIndexedDiskCache(
						"testCache",
						Duration.minutes(1),
						Cache.UNLIMITED_IDLE_TIME,
						Integer.MAX_VALUE,
						Integer.MAX_VALUE,
						this.baseDir);

		cache.putWithErrors("12345", "12345");
		cache.flush();

		final long dataFileSize = cache.dataFile.length();
		final long metafileLength = cache.indexFile.length();

		cache.putWithErrors("12345", "12345");
		cache.flush();

		Assert.assertEquals(dataFileSize, cache.dataFile.length());
		Assert.assertEquals(metafileLength, cache.indexFile.length());

		cache.close();
	}


	@Test
	public void testInvalidGet() throws Exception
	{
		final SerializableIndexedDiskCache cache =
				new SerializableIndexedDiskCache(
						"testCache",
						JUnitUtils.getFutureExpiry(),
						Cache.UNLIMITED_IDLE_TIME,
						1000,
						1000,
						this.baseDir);

		cache.putWithErrors("1", new ExampleObject(3));

		cache.flush();

		final RandomAccessFile data = new RandomAccessFile(cache.dataFile.getFileName(), "rw");
		data.seek(1);
		for (int i = 0; i < 30; i++) {
			data.writeLong(0);
		}
		data.close();

		try {
			cache.getWithErrors("1");
			throw new Exception("Exception expected");
		} catch (final CacheException e) {
			/* Expected */
		}

		Assert.assertNull(cache.getWithErrors("1"));

		cache.putWithErrors("1", new ExampleObject(3));
		Assert.assertEquals(new ExampleObject(3), cache.getWithErrors("1"));
		cache.delete("1");

		Assert.assertEquals(0, cache.getStatistics().getCurrentSize());
		Assert.assertEquals(0, cache.dataFile.length());
		// Assert.assertEquals(0, SerializableIndexedCacheTest.metaDataFile.length());

		// cache.close();
	}


	@Test
	public void testDuplicatePut() throws IOException, CacheException
	{
		final SerializableIndexedDiskCache cache =
				new SerializableIndexedDiskCache(
						"testCache",
						Duration.minutes(1),
						Cache.UNLIMITED_IDLE_TIME,
						Integer.MAX_VALUE,
						Integer.MAX_VALUE,
						this.baseDir);

		cache.putWithErrors("1", new ExampleObject(3));

		cache.flush();

		final long metaFileSize = cache.dataFile.length();
		final long dataFileSize = cache.indexFile.length();

		cache.putWithErrors("1", new ExampleObject(3));
		Assert.assertEquals(new ExampleObject(3), cache.getWithErrors("1"));

		cache.flush();

		Assert.assertEquals(metaFileSize, cache.dataFile.length());
		Assert.assertEquals(dataFileSize, cache.indexFile.length());

		cache.close();

	}


	@Test
	public void testLockFile() throws IOException
	{
		new SerializableIndexedDiskCache(
				"testCache",
				Duration.minutes(1),
				Cache.UNLIMITED_IDLE_TIME,
				Integer.MAX_VALUE,
				Integer.MAX_VALUE,
				this.baseDir);

		try {

			new SerializableIndexedDiskCache(
					"testCache",
					Duration.minutes(1),
					Cache.UNLIMITED_IDLE_TIME,
					Integer.MAX_VALUE,
					Integer.MAX_VALUE,
					this.baseDir);
			Assert.fail("IOException expected");

		} catch (IOException e) {
			/* Expected */
			Assert.assertTrue(e.getMessage().startsWith("Lock file found"));
		}
	}


	@Test
	public void testReReadIndex() throws IOException, CacheException
	{
		AbstractIndexedDiskCache<Serializable, Serializable> cache =
				new SerializableIndexedDiskCache(
						"testCache",
						Duration.minutes(1),
						Cache.UNLIMITED_IDLE_TIME,
						1000,
						1000,
						this.baseDir);

		for (int i = 0; i < 10; i++) {
			cache.putWithErrors(Integer.toString(i), new ExampleObject(i));
		}
		/* Invalidate one entry */
		cache.delete(Integer.toString(0));
		cache.close();

		cache =
				new SerializableIndexedDiskCache(
						"testCache",
						Duration.minutes(1),
						Cache.UNLIMITED_IDLE_TIME,
						1000,
						1000,
						this.baseDir);

		/* Test invalidated not there */
		Assert.assertNull(cache.getWithErrors(Integer.toString(0)));

		for (int i = 1; i < 10; i++) {
			Assert.assertEquals(new ExampleObject(i), cache.getWithErrors(Integer.toString(i)));
		}
		cache.close();
	}


	@Test
	public void runLoadTest() throws Throwable
	{
		Assume.assumeNotNull(System.getProperty("cache.test.runloadtest"));
		final File ramDiskDir = new File(System.getProperty("cache.test.ramdisk"));
		Assume.assumeTrue(ramDiskDir.exists());

		// Logger.getRootLogger().setLevel(Level.DEBUG);

		File dataFile = new File(ramDiskDir, "serializableMetaFileCacheTest.data");
		File metaFile = new File(ramDiskDir, "serializableMetaFileCacheTest.index");

		try {

			final SerializableIndexedDiskCache loadTestCache =
					new SerializableIndexedDiskCache(
							"serializableMetaFileCacheTest",
							Duration.seconds(1),
							1000,
							100,
							ramDiskDir);
			final SimulationRunner runner = new SimulationRunner() {

				@Override
				protected void loadTestPostIterationHook(final Cache<Serializable, Serializable> cache)
				{

				}
			};
			runner.runLoadTest(loadTestCache, 10, 10000, JUnitUtils.PARETO_EIGHTY_PERCENT_UNDER_HUNDREDTHOUSAND);

			final int size = loadTestCache.getStatistics().getCurrentSize();
			final float hitRate = loadTestCache.getStatistics().getHitRate();

			loadTestCache.flush();
			Assert.assertEquals(
					loadTestCache.getEntriesMetaData().size(),
					loadTestCache.getIndexFileNumAllocatedBlocks());
			Assert.assertEquals(
					loadTestCache.getEntriesMetaData().size() * 2,
					loadTestCache.getDataFileNumAllocatedBlocks());

			loadTestCache.close();

			// Logger.getRootLogger().setLevel(Level.INFO);

			final SerializableIndexedDiskCache loadTestCacheNew =
					new SerializableIndexedDiskCache(
							"serializableMetaFileCacheTest",
							Duration.seconds(1),
							1000,
							100,
							ramDiskDir);

			// Logger.getRootLogger().setLevel(Level.DEBUG);

			this.getLogger().info("Hitrate: " + hitRate);

			Assert.assertEquals(size, loadTestCacheNew.getStatistics().getCurrentSize());

			loadTestCache.close();

		} finally {
			dataFile.delete();
			metaFile.delete();
		}
	}


	@Test
	public void runKnownLoadTest() throws Throwable
	{
		Logger.getRootLogger().setLevel(Level.INFO);

		Assume.assumeNotNull(System.getProperty("cache.test.runloadtest"));
		final File ramDiskDir = new File(System.getProperty("cache.test.ramdisk"));
		Assume.assumeTrue(ramDiskDir.exists());

		File dataFile = new File(ramDiskDir, "serializableMetaFileCacheTest.data");
		File metaFile = new File(ramDiskDir, "serializableMetaFileCacheTest.index");

		try {

			final SerializableIndexedDiskCache loadTestCache =
					new SerializableIndexedDiskCache(
							"serializableMetaFileCacheTest",
							Duration.days(1),
							Cache.UNLIMITED_IDLE_TIME,
							100000,
							100000,
							ramDiskDir);

			Set<Long> known = new HashSet<Long>();
			final SimulationRunner runner = new SimulationRunner() {

				@Override
				protected void knownTestpostIterationHook(final Cache<Serializable, Serializable> cache)
				{

				}
			};
			known =
					runner.runKnownTest(
							loadTestCache,
							1,
							100000,
							JUnitUtils.PARETO_EIGHTY_PERCENT_UNDER_THOUSAND,
							known);

			loadTestCache.flush();

			Assert.assertEquals(
					loadTestCache.getEntriesMetaData().size(),
					loadTestCache.getIndexFileNumAllocatedBlocks());
			Assert.assertEquals(
					loadTestCache.getEntriesMetaData().size() * 2,
					loadTestCache.getDataFileNumAllocatedBlocks());

			loadTestCache.close();

			final SerializableIndexedDiskCache loadTestCacheNew =
					new SerializableIndexedDiskCache(
							"serializableMetaFileCacheTest",
							Duration.days(1),
							Cache.UNLIMITED_IDLE_TIME,
							100000,
							100000,
							ramDiskDir);

			known =
					runner.runKnownTest(
							loadTestCacheNew,
							1,
							10000,
							JUnitUtils.PARETO_EIGHTY_PERCENT_UNDER_THOUSAND,
							known);

			loadTestCache.flush();

			this.getLogger().info(loadTestCacheNew.getStatistics().toString());

			Assert.assertEquals(
					loadTestCacheNew.getEntriesMetaData().size(),
					loadTestCacheNew.getIndexFileNumAllocatedBlocks());
			Assert.assertEquals(
					loadTestCacheNew.getEntriesMetaData().size() * 2,
					loadTestCacheNew.getDataFileNumAllocatedBlocks());

			loadTestCache.close();

		} finally {
			dataFile.delete();
			metaFile.delete();
		}
	}


	@Test
	public void sameObjectMultipleTimes() throws IOException, CacheException
	{
		SerializableIndexedDiskCache cache = null;
		try {

			cache =
					new SerializableIndexedDiskCache(
							"serializableMetaFileCacheTest",
							Duration.days(1),
							Cache.UNLIMITED_IDLE_TIME,
							1000000,
							1000000,
							this.baseDir);

			Serializable key = this.translateKey(1);
			ExampleObject ex = new ExampleObject(1);
			for (int i = 0; i < 1000; i++) {
				Assert.assertEquals(ex, cache.put(key, ex));
				Assert.assertEquals(ex, cache.put(key, ex));
				Assert.assertEquals(ex, cache.get(key));
				cache.delete(key);
			}

		} finally {

			if (cache != null) {
				cache.close();
			}
		}
	}


	@Override
	protected Serializable translateKey(int key)
	{
		final StringBuffer sb = new StringBuffer();
		for (int i = -1; i < key % 100; i++) {
			sb.append(Long.toString(key));
		}

		return sb.toString();
	}
}
