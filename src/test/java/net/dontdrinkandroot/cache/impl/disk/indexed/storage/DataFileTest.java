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
package net.dontdrinkandroot.cache.impl.disk.indexed.storage;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;


public class DataFileTest {

	@Test
	public void testReadWriteDelete() throws IOException {

		File file = File.createTempFile("file", ".data");
		file.deleteOnExit();
		DataFile dataFile = new DataFile(file);

		final String s1 = "asdkfhge aasdf asdf";
		final DataBlock dataBlock1 = dataFile.write(s1.getBytes());
		Assert.assertEquals(s1, new String(dataFile.read(dataBlock1)));

		Assert.assertTrue(dataFile.checkConsistency());

		final String s2 = "dgd  hisefiosuhf";
		final DataBlock dataBlock2 = dataFile.write(s2.getBytes());
		Assert.assertEquals(s1, new String(dataFile.read(dataBlock1)));
		Assert.assertEquals(s2, new String(dataFile.read(dataBlock2)));

		Assert.assertTrue(dataFile.checkConsistency());

		final String s3 = "asdf ijhas dfp f";
		final DataBlock dataBlock3 = dataFile.write(s3.getBytes());
		Assert.assertEquals(s1, new String(dataFile.read(dataBlock1)));
		Assert.assertEquals(s2, new String(dataFile.read(dataBlock2)));
		Assert.assertEquals(s3, new String(dataFile.read(dataBlock3)));

		Assert.assertTrue(dataFile.checkConsistency());

		dataFile.delete(dataBlock1, true);
		try {
			dataFile.read(dataBlock1);
			throw new Exception("Exception expected");
		} catch (final Exception e) {
			/* Expected */
		}

		Assert.assertTrue(dataFile.checkConsistency());

		final String s4 = "short";
		final DataBlock dataBlock4 = dataFile.write(s4.getBytes());
		Assert.assertEquals(s4, new String(dataFile.read(dataBlock4)));
		Assert.assertEquals(s2, new String(dataFile.read(dataBlock2)));
		Assert.assertEquals(s3, new String(dataFile.read(dataBlock3)));
		Assert.assertTrue(dataBlock4.compareTo(dataBlock2) < 0);

		Assert.assertTrue(dataFile.checkConsistency());

		try {
			dataFile.write(new byte[0]);
			throw new Exception("Exception expected");
		} catch (final Exception e) {
			/* Expected */
		}

		dataFile.close();
	}


	@Test
	public void testTruncation() throws IOException {

		File file = File.createTempFile("file", ".data");
		file.deleteOnExit();
		DataFile dataFile = new DataFile(file);

		long fileSize = file.length();

		final String s1 = "asdf";
		final DataBlock db1 = dataFile.write(s1.getBytes());
		Assert.assertEquals(s1, new String(dataFile.read(db1)));
		Assert.assertTrue(file.length() > fileSize);
		fileSize = file.length();

		final String s2 = "asdf sdf sdf";
		final DataBlock db2 = dataFile.write(s2.getBytes());
		Assert.assertEquals(s1, new String(dataFile.read(db1)));
		Assert.assertEquals(s2, new String(dataFile.read(db2)));
		Assert.assertTrue(file.length() > fileSize);
		fileSize = file.length();

		final String s3 = "asdf gfsdf";
		final DataBlock db3 = dataFile.write(s3.getBytes());
		Assert.assertEquals(s1, new String(dataFile.read(db1)));
		Assert.assertEquals(s2, new String(dataFile.read(db2)));
		Assert.assertEquals(s3, new String(dataFile.read(db3)));
		Assert.assertTrue(file.length() > fileSize);
		fileSize = file.length();

		dataFile.delete(db2, true);
		Assert.assertEquals(s1, new String(dataFile.read(db1)));
		Assert.assertEquals(s3, new String(dataFile.read(db3)));
		Assert.assertEquals(file.length(), fileSize);

		dataFile.delete(db3, true);
		Assert.assertEquals(s1, new String(dataFile.read(db1)));
		Assert.assertTrue(file.length() < fileSize);
		fileSize = file.length();

		dataFile.delete(db1, true);
		Assert.assertEquals(0, file.length());

		dataFile.close();
	}


	@Test
	public void testAllocateSpace() throws Exception {

		File file = File.createTempFile("file", ".data");
		file.deleteOnExit();
		DataFile dataFile = new DataFile(file);

		Assert.assertTrue(dataFile.checkConsistency());

		dataFile.allocateSpace(new DataBlock(0, 4));

		Assert.assertTrue(dataFile.checkConsistency());

		try {
			dataFile.allocateSpace(new DataBlock(0, 4));
			throw new Exception("Exception expected");
		} catch (final AllocationException e) {
			/* Expected */
		}

		Assert.assertTrue(dataFile.checkConsistency());

		dataFile.allocateSpace(new DataBlock(5, 9));

		Assert.assertTrue(dataFile.checkConsistency());

		dataFile.allocateSpace(new DataBlock(2, 8));

		Assert.assertFalse(dataFile.checkConsistency());

		dataFile.close();
	}
}
