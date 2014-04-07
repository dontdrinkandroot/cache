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

import net.dontdrinkandroot.cache.CacheException;


/**
 * @author Philip W. Sorst <philip@sorst.net>
 */
public class ByteArrayIndexedDiskCache extends AbstractIndexedDiskCache<Serializable, byte[]> {

	public ByteArrayIndexedDiskCache(
			final String name,
			final long defaultTimeToLive,
			final int maxSize,
			final int recycleSize,
			final File baseDir) throws IOException {

		super(name, defaultTimeToLive, maxSize, recycleSize, baseDir);
	}


	public ByteArrayIndexedDiskCache(
			final String name,
			final long defaultTimeToLive,
			final long defaultMaxIdleTime,
			final int maxSize,
			final int recycleSize,
			final File baseDir) throws IOException {

		super(name, defaultTimeToLive, defaultMaxIdleTime, maxSize, recycleSize, baseDir);
	}


	@Override
	public byte[] dataToBytes(final byte[] data) throws CacheException {

		return data;
	}


	@SuppressWarnings("unchecked")
	@Override
	public byte[] dataFromBytes(final byte[] data) throws CacheException {

		return data;
	}


	public int getMetaFileNumAllocatedBlocks() {

		return this.indexFile.getNumAllocated();

	}


	public int getDataFileNumAllocatedBlocks() {

		return this.dataFile.getNumAllocated();

	}


	public synchronized boolean assertAllocatedConsistency() {

		return this.indexFile.getNumAllocated() == this.getEntriesMetaData().size()
				&& this.dataFile.getNumAllocated() == this.getEntriesMetaData().size();
	}
}
