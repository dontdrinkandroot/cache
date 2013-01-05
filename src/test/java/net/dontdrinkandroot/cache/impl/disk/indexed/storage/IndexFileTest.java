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
package net.dontdrinkandroot.cache.impl.disk.indexed.storage;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import net.dontdrinkandroot.cache.impl.disk.indexed.storage.DataBlock;
import net.dontdrinkandroot.cache.impl.disk.indexed.storage.IndexData;
import net.dontdrinkandroot.cache.impl.disk.indexed.storage.IndexFile;
import net.dontdrinkandroot.cache.metadata.impl.CheckSumNotMatchingException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * Copyright (C) 2012 Philip W. Sorst <philip@sorst.net> and individual contributors as indicated by
 * the @authors tag.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

public class IndexFileTest {

	private File file;

	private IndexFile indexFile;


	@Before
	public void before() throws IOException {

		this.file = File.createTempFile("indexFile", ".index");
		this.indexFile = new IndexFile(this.file);
	}


	@After
	public void after() throws IOException {

		this.indexFile.close();
		this.file.delete();
	}


	@Test
	public void testWriteReadDelete() throws IOException {

		IndexData id1 = new IndexData(new DataBlock(0, 1), new DataBlock(2, 3));
		IndexData id2 = new IndexData(new DataBlock(4, 5), new DataBlock(6, 7));
		IndexData id3 = new IndexData(new DataBlock(8, 9), new DataBlock(10, 11));

		id1 = this.indexFile.write(id1);
		Assert.assertEquals(0, id1.getBlockNum());
		id2 = this.indexFile.write(id2);
		Assert.assertEquals(1, id2.getBlockNum());
		id3 = this.indexFile.write(id3);
		Assert.assertEquals(2, id3.getBlockNum());

		this.indexFile.delete(id2);
		Assert.assertNull(IndexData.read(this.indexFile.getRandomAccessFile(), id2.getBlockNum()));
		id2 = this.indexFile.write(id2);
		Assert.assertEquals(1, id2.getBlockNum());
	}


	@Test
	public void testReReadIndex() throws IOException {

		IndexData id1 = new IndexData(new DataBlock(0, 1), new DataBlock(2, 3));
		IndexData id2 = new IndexData(new DataBlock(4, 5), new DataBlock(6, 7));
		IndexData id3 = new IndexData(new DataBlock(8, 9), new DataBlock(10, 11));

		id1 = this.indexFile.write(id1);
		Assert.assertEquals(0, id1.getBlockNum());
		id2 = this.indexFile.write(id2);
		Assert.assertEquals(1, id2.getBlockNum());
		id3 = this.indexFile.write(id3);
		Assert.assertEquals(2, id3.getBlockNum());

		this.indexFile.delete(id2);
		Assert.assertNull(IndexData.read(this.indexFile.getRandomAccessFile(), id2.getBlockNum()));

		this.indexFile.close();
		this.indexFile = new IndexFile(this.file);
		Collection<IndexData> entries = this.indexFile.initialize();
		Assert.assertEquals(2, entries.size());

		Iterator<IndexData> iterator = entries.iterator();
		IndexData next = iterator.next();
		Assert.assertEquals(id1.getBlockNum(), next.getBlockNum());
		Assert.assertEquals(id1.getKeyMetaBlock().getStartPosition(), next.getKeyMetaBlock().getStartPosition());
		Assert.assertEquals(id1.getKeyMetaBlock().getEndPosition(), next.getKeyMetaBlock().getEndPosition());
		Assert.assertEquals(id1.getValueBlock().getStartPosition(), next.getValueBlock().getStartPosition());
		Assert.assertEquals(id1.getValueBlock().getEndPosition(), next.getValueBlock().getEndPosition());

		next = iterator.next();
		Assert.assertEquals(id3.getBlockNum(), next.getBlockNum());
		Assert.assertEquals(id3.getKeyMetaBlock().getStartPosition(), next.getKeyMetaBlock().getStartPosition());
		Assert.assertEquals(id3.getKeyMetaBlock().getEndPosition(), next.getKeyMetaBlock().getEndPosition());
		Assert.assertEquals(id3.getValueBlock().getStartPosition(), next.getValueBlock().getStartPosition());
		Assert.assertEquals(id3.getValueBlock().getEndPosition(), next.getValueBlock().getEndPosition());
	}


	@Test
	public void testEnlargingAndShrinking() throws CheckSumNotMatchingException, IOException {

		for (int i = 0; i < 10; i++) {
			IndexData indexData = new IndexData(new DataBlock(i * 4, i * 4 + 1), new DataBlock(i * 4 + 2, i * 4 + 3));
			indexData = this.indexFile.write(indexData);
			Assert.assertEquals(i, indexData.getBlockNum());
		}
		Assert.assertEquals(10, this.indexFile.getNumAllocated());
		Assert.assertEquals(10 * IndexData.LENGTH, this.indexFile.length());
		this.indexFile.close();

		this.indexFile = new IndexFile(this.file);
		Collection<IndexData> entries = this.indexFile.initialize();
		for (IndexData entry : entries) {
			this.indexFile.delete(entry);
		}

		// TODO: shrinking not implemented yet
		// Assert.assertEquals(this.indexFile.length(), 0);
	}

	// @Test
	// public void testWriteReadDelete() throws Exception {
	//
	// File file = File.createTempFile("file", ".meta");
	// file.deleteOnExit();
	// IndexFile metaFile = new IndexFile(file);
	//
	// long startTime = System.currentTimeMillis();
	//
	// final BlockMetaData metaData = new BlockMetaData(new Md5("test"), 23L, 0L, new DataBlock(0,
	// 42));
	// Md5 md5 = metaData.getMd5();
	// long crc32 = metaData.getCrc32();
	// DataBlock dataBlock = metaData.getDataBlock();
	// long expiry = metaData.getExpiry();
	//
	// BlockMetaData written = metaFile.write(metaData);
	//
	// Assert.assertEquals(0, written.getBlockNum());
	//
	// BlockMetaData read = metaFile.read(written.getBlockNum());
	//
	// Assert.assertEquals(md5, written.getMd5());
	// Assert.assertTrue(written.getCreated() >= startTime);
	// Assert.assertTrue(written.getCreated() <= System.currentTimeMillis());
	//
	// Assert.assertEquals(md5, read.getMd5());
	// Assert.assertTrue(read.getCreated() >= startTime);
	// Assert.assertTrue(read.getCreated() <= System.currentTimeMillis());
	//
	// Assert.assertEquals(crc32, written.getCrc32());
	// Assert.assertEquals(crc32, read.getCrc32());
	//
	// Assert.assertEquals(dataBlock, written.getDataBlock());
	// Assert.assertEquals(dataBlock, read.getDataBlock());
	//
	// Assert.assertEquals(expiry, written.getExpiry());
	// Assert.assertEquals(expiry, read.getExpiry());
	//
	// metaFile.delete(0);
	// try {
	// metaFile.read(written.getBlockNum());
	// Assert.fail("Exception expected");
	// } catch (final CheckSumNotMatchingException e) {
	// /* Expected */
	// }
	//
	// written = metaFile.write(metaData);
	//
	// Assert.assertEquals(0, written.getBlockNum());
	//
	// read = metaFile.read(written.getBlockNum());
	//
	// Assert.assertEquals(md5, written.getMd5());
	// Assert.assertEquals(md5, read.getMd5());
	//
	// Assert.assertEquals(crc32, written.getCrc32());
	// Assert.assertEquals(crc32, read.getCrc32());
	//
	// Assert.assertEquals(dataBlock, written.getDataBlock());
	// Assert.assertEquals(dataBlock, read.getDataBlock());
	//
	// Assert.assertEquals(expiry, written.getExpiry());
	// Assert.assertEquals(expiry, read.getExpiry());
	//
	// metaFile.close();
	// }
	//
	//
	// @Test
	// public void testEnlarging() throws CheckSumNotMatchingException, IOException {
	//
	// File file = File.createTempFile("file", ".meta");
	// file.deleteOnExit();
	// IndexFile metaFile = new IndexFile(file);
	//
	// for (int i = 0; i < 10; i++) {
	//
	// final BlockMetaData metaData =
	// new BlockMetaData(new Md5(Integer.toString(i)), i * 1000L, 0L, new DataBlock(i, i + 1));
	// Md5 md5 = metaData.getMd5();
	// long crc32 = metaData.getCrc32();
	// DataBlock dataBlock = metaData.getDataBlock();
	// long expiry = metaData.getExpiry();
	//
	// final BlockMetaData written = metaFile.write(metaData);
	//
	// Assert.assertEquals(i, written.getBlockNum());
	//
	// final BlockMetaData read = metaFile.read(written.getBlockNum());
	//
	// Assert.assertEquals(md5, written.getMd5());
	// Assert.assertEquals(md5, read.getMd5());
	//
	// Assert.assertEquals(crc32, written.getCrc32());
	// Assert.assertEquals(crc32, read.getCrc32());
	//
	// Assert.assertEquals(dataBlock, written.getDataBlock());
	// Assert.assertEquals(dataBlock, read.getDataBlock());
	//
	// Assert.assertEquals(expiry, written.getExpiry());
	// Assert.assertEquals(expiry, read.getExpiry());
	// }
	//
	// metaFile.close();
	//
	// }
	//
	//
	// @Test
	// public void testReadIndex() throws Exception {
	//
	// File file = File.createTempFile("file", ".meta");
	// file.deleteOnExit();
	// IndexFile metaFile = new IndexFile(file);
	//
	// final BlockMetaData valid =
	// new BlockMetaData(new Md5("Valid"), System.currentTimeMillis() + 1000L, 0L, new DataBlock(0,
	// 1));
	//
	// final BlockMetaData expired = new BlockMetaData(new Md5("Expired"), 0L, 0L, new DataBlock(1,
	// 2));
	//
	// final BlockMetaData delete =
	// new BlockMetaData(new Md5("Delete"), System.currentTimeMillis() + 1000L, 0L, new DataBlock(2,
	// 3));
	//
	// final BlockMetaData known =
	// new BlockMetaData(new Md5("Valid"), System.currentTimeMillis() + 1000L, 0L, new DataBlock(2,
	// 3));
	//
	// metaFile.write(expired);
	// metaFile.write(valid);
	// metaFile.write(delete);
	// metaFile.write(known);
	// metaFile.delete(delete);
	//
	// metaFile = new IndexFile(file);
	//
	// final Set<BlockMetaData> metaDataSet = metaFile.initialize();
	//
	// Assert.assertEquals(1, metaDataSet.size());
	//
	// final BlockMetaData read = CollectionUtils.first(metaDataSet);
	//
	// Assert.assertEquals(valid.getMd5(), read.getMd5());
	// Assert.assertEquals(valid.getExpiry(), read.getExpiry());
	// Assert.assertEquals(valid.getCrc32(), read.getCrc32());
	// Assert.assertEquals(valid.getDataBlock(), read.getDataBlock());
	//
	// Assert.assertEquals(0, metaFile.write(expired).getBlockNum());
	// Assert.assertEquals(2, metaFile.write(delete).getBlockNum());
	//
	// try {
	// metaFile.read(4);
	// Assert.fail("Exception expected");
	// } catch (final IOException e) {
	// /* Expected */
	// }
	//
	// metaFile.close();
	// }

}
