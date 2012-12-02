/**
 * Copyright (C) 2012 Philip W. Sorst <philip@sorst.net> and individual contributors as indicated by the @authors tag.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package net.dontdrinkandroot.cache.impl.disk.indexed;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import net.dontdrinkandroot.cache.CacheException;
import net.dontdrinkandroot.cache.expungestrategy.ExpungeStrategy;
import net.dontdrinkandroot.cache.expungestrategy.impl.ExpiredOnlyExpungeStrategy;
import net.dontdrinkandroot.utils.lang.SerializationUtils;
import net.dontdrinkandroot.utils.lang.time.DateUtils;

import org.apache.commons.lang3.SerializationException;


/**
 * @author Philip W. Sorst <philip@sorst.net>
 */
public class SerializableIndexedDiskCache extends AbstractIndexedDiskCache<Serializable, Serializable> {

	public SerializableIndexedDiskCache(final String name, final long defaultTimeToLive, final File baseDir)
			throws IOException {

		super(name, defaultTimeToLive, new ExpiredOnlyExpungeStrategy(DateUtils.MILLIS_PER_DAY), baseDir);
	}


	public SerializableIndexedDiskCache(
			final String name,
			final long defaultTimeToLive,
			final long defaultMaxIdleTime,
			final File baseDir) throws IOException {

		super(
				name,
				defaultTimeToLive,
				defaultMaxIdleTime,
				new ExpiredOnlyExpungeStrategy(DateUtils.MILLIS_PER_DAY),
				baseDir);
	}


	public SerializableIndexedDiskCache(
			final String name,
			final long defaultTimeToLive,
			final ExpungeStrategy expungeStrategy,
			final File baseDir) throws IOException {

		super(name, defaultTimeToLive, expungeStrategy, baseDir);
	}


	public SerializableIndexedDiskCache(
			final String name,
			final long defaultTimeToLive,
			final long defaultMaxIdleTime,
			final ExpungeStrategy expungeStrategy,
			final File baseDir) throws IOException {

		super(name, defaultTimeToLive, defaultMaxIdleTime, expungeStrategy, baseDir);
	}


	@Override
	public byte[] dataToBytes(final Serializable data) throws CacheException {

		try {

			return SerializationUtils.serialize(data);

		} catch (final SerializationException e) {
			throw new CacheException(e);
		}
	}


	@Override
	public Serializable dataFromBytes(final byte[] dataBytes) throws CacheException {

		try {

			return (Serializable) SerializationUtils.deserialize(dataBytes);

		} catch (final SerializationException e) {
			throw new CacheException(e);
		}
	}


	public int getIndexFileNumAllocatedBlocks() {

		return this.indexFile.getNumAllocated();
	}


	public int getDataFileNumAllocatedBlocks() {

		return this.dataFile.getNumAllocated();
	}

}
