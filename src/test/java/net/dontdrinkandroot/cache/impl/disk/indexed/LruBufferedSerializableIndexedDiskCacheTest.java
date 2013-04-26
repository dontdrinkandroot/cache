//package net.dontdrinkandroot.cache.impl.disk.indexed;
//
//import java.io.File;
//import java.io.IOException;
//
//import net.dontdrinkandroot.cache.CacheException;
//import net.dontdrinkandroot.cache.utils.Duration;
//import net.dontdrinkandroot.cache.utils.FileUtils;
//
//import org.junit.After;
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Test;
//
//
//public class LruBufferedSerializableIndexedDiskCacheTest {
//
//	private File baseDir;
//
//	private LruBufferedSerializableIndexedDiskCache cache;
//
//
//	@Before
//	public void before() throws IOException {
//
//		this.baseDir = File.createTempFile("cachetest", null);
//		this.baseDir.delete();
//	}
//
//
//	@After
//	public void after() throws IOException {
//
//		this.cache.close();
//		FileUtils.deleteDirectory(this.baseDir);
//	}
//
//
//	@Test
//	public void testExpunging() throws IOException, CacheException, InterruptedException {
//
//		this.cache =
//				new LruBufferedSerializableIndexedDiskCache("test", Duration.minutes(10), this.baseDir, 4, 1, 2, 1);
//
//		Assert.assertEquals("1", this.cache.put("1", "1")); // 1
//		this.assertCurrentSize(1);
//		this.assertBufferCurrentSize(1);
//		this.assertHits(0);
//		this.assertMisses(0);
//		this.assertBufferHits(0);
//		this.assertBufferMisses(0);
//		this.sleep(1);
//
//		Assert.assertEquals("1", this.cache.get("1"));
//		this.assertCurrentSize(1);
//		this.assertBufferCurrentSize(1);
//		this.assertHits(1);
//		this.assertMisses(0);
//		this.assertBufferHits(1);
//		this.assertBufferMisses(0);
//		this.sleep(1);
//
//		Assert.assertNull(this.cache.get("2"));
//		this.assertCurrentSize(1);
//		this.assertBufferCurrentSize(1);
//		this.assertHits(1);
//		this.assertMisses(1);
//		this.assertBufferHits(1);
//		this.assertBufferMisses(0);
//		this.sleep(1);
//
//		Assert.assertEquals("2", this.cache.put("2", "2")); // 1, 2
//		this.assertCurrentSize(2);
//		this.assertBufferCurrentSize(2);
//		this.sleep(1);
//
//		Assert.assertEquals("3", this.cache.put("3", "3")); // 1, 2, 3
//		this.assertCurrentSize(3);
//		this.assertBufferCurrentSize(3);
//		this.sleep(1);
//
//		Assert.assertEquals("4", this.cache.put("4", "4")); // 3, 4
//		this.assertCurrentSize(4);
//		this.assertBufferCurrentSize(2);
//		this.sleep(1);
//
//		Assert.assertEquals("1", this.cache.get("1")); // 3, 4, 1
//		this.assertCurrentSize(4);
//		this.assertBufferCurrentSize(3);
//		this.assertHits(2);
//		this.assertMisses(1);
//		this.assertBufferHits(1);
//		this.assertBufferMisses(1);
//		this.sleep(1);
//
//		this.cache.delete("3"); // 4, 1
//		this.assertCurrentSize(3);
//		this.assertBufferCurrentSize(2);
//		this.assertHits(2);
//		this.assertMisses(1);
//		this.assertBufferHits(1);
//		this.assertBufferMisses(1);
//		this.sleep(1);
//
//		Assert.assertEquals("2", this.cache.put("2", "2", 1)); // 4, 1, 2
//		this.assertCurrentSize(3);
//		this.assertBufferCurrentSize(3);
//		this.sleep(2);
//
//		Assert.assertNull(this.cache.get("2")); // 4, 1
//		this.assertCurrentSize(2);
//		this.assertBufferCurrentSize(2);
//		this.assertHits(2);
//		this.assertMisses(2);
//		this.assertBufferHits(1);
//		this.assertBufferMisses(1);
//		this.sleep(1);
//	}
//
//
//	private void sleep(int i) throws InterruptedException {
//
//		Thread.sleep(i);
//	}
//
//
//	private void assertBufferMisses(int misses) {
//
//		Assert.assertEquals(misses, this.cache.getBufferStatistics().getCacheMisses());
//
//	}
//
//
//	private void assertBufferHits(int hits) {
//
//		Assert.assertEquals(hits, this.cache.getBufferStatistics().getCacheHits());
//
//	}
//
//
//	private void assertMisses(int misses) {
//
//		Assert.assertEquals(misses, this.cache.getStatistics().getCacheMisses());
//
//	}
//
//
//	private void assertHits(int hits) {
//
//		Assert.assertEquals(hits, this.cache.getStatistics().getCacheHits());
//
//	}
//
//
//	private void assertBufferCurrentSize(int size) {
//
//		Assert.assertEquals(size, this.cache.getBufferStatistics().getCurrentSize());
//	}
//
//
//	private void assertCurrentSize(int size) {
//
//		Assert.assertEquals(size, this.cache.getStatistics().getCurrentSize());
//
//	}
// }
