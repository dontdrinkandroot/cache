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
import java.util.Collection;

import net.dontdrinkandroot.cache.CacheException;
import net.dontdrinkandroot.cache.impl.AbstractMapBackedCustomTtlCache;
import net.dontdrinkandroot.cache.impl.disk.indexed.storage.DataBlock;
import net.dontdrinkandroot.cache.impl.disk.indexed.storage.DataFile;
import net.dontdrinkandroot.cache.impl.disk.indexed.storage.IndexData;
import net.dontdrinkandroot.cache.impl.disk.indexed.storage.IndexFile;
import net.dontdrinkandroot.cache.impl.disk.indexed.storage.KeyedMetaData;
import net.dontdrinkandroot.cache.metadata.impl.BlockMetaData;
import net.dontdrinkandroot.cache.utils.SerializationException;
import net.dontdrinkandroot.cache.utils.Serializer;


/**
 * @author Philip W. Sorst <philip@sorst.net>
 */
public abstract class AbstractIndexedDiskCache<K extends Serializable, V extends Serializable>
		extends AbstractMapBackedCustomTtlCache<K, V, BlockMetaData> {

	protected Object indexFileLock = new Object();

	protected Object dataFileLock = new Object();

	protected final IndexFile indexFile;

	protected final DataFile dataFile;

	protected final File lockFile;

	private final File baseDir;


	public AbstractIndexedDiskCache(
			final String name,
			final long defaultTimeToLive,
			final int maxSize,
			final int recycleSize,
			final File baseDir) throws IOException {

		super(name, defaultTimeToLive, maxSize, recycleSize);

		this.baseDir = baseDir;
		baseDir.mkdirs();
		this.lockFile = this.createLockFile();

		/* Open block files */
		this.indexFile = new IndexFile(new File(baseDir, name + ".index"));
		this.dataFile = new DataFile(new File(baseDir, name + ".data"));

		/* Read index */
		this.buildIndex();
	}


	public AbstractIndexedDiskCache(
			final String name,
			final long defaultTimeToLive,
			final long defaultMaxIdleTime,
			final int maxSize,
			final int recycleSize,
			final File baseDir) throws IOException {

		super(name, defaultTimeToLive, defaultMaxIdleTime, maxSize, recycleSize);

		this.baseDir = baseDir;
		baseDir.mkdirs();
		this.lockFile = this.createLockFile();

		/* Open block files */
		this.indexFile = new IndexFile(new File(baseDir, name + ".index"));
		this.dataFile = new DataFile(new File(baseDir, name + ".data"));

		/* Read index */
		this.buildIndex();
	}


	/**
	 * Closes the cache.
	 */
	public synchronized void close() throws IOException {

		synchronized (this.indexFileLock) {
			synchronized (this.dataFileLock) {
				this.indexFile.close();
				this.dataFile.close();
			}
		}
		if (!this.lockFile.delete()) {
			throw new IOException(String.format("Could not delete lock file at %s", this.lockFile.getPath()));
		}

		this.getLogger().info("{}: Shutdown complete", this.getName());
	}


	/**
	 * Get the {@link DataFile} of this cache. Only perform altering operations if you know what you
	 * are doing.
	 */
	DataFile getDataFile() {

		return this.dataFile;
	}


	/**
	 * Get the {@link IndexFile} of this cache. Only perform altering operations if you know what
	 * you are doing.
	 */
	IndexFile getIndexFile() {

		return this.indexFile;
	}


	protected void buildIndex() throws IOException {

		this.getLogger().info("{}: Reading index", this.getName());
		long lastTimeLogged = System.currentTimeMillis();

		long dataLength = 0;
		final Collection<IndexData> indexDataEntries = this.indexFile.initialize();

		int numRead = 0;
		int numSuccessfullyRead = 0;
		for (IndexData indexData : indexDataEntries) {

			DataBlock keyMetaBlock = indexData.getKeyMetaBlock();
			DataBlock valueBlock = indexData.getValueBlock();

			try {

				this.dataFile.allocateSpace(keyMetaBlock);
				byte[] keyMetaBytes = this.dataFile.read(keyMetaBlock);
				@SuppressWarnings("unchecked")
				KeyedMetaData<K> keyedMetaData = (KeyedMetaData<K>) Serializer.deserialize(keyMetaBytes);
				K key = keyedMetaData.getKey();
				BlockMetaData blockMetaData = new BlockMetaData(indexData, keyedMetaData.getMetaData());
				this.dataFile.allocateSpace(valueBlock);
				this.putEntry(key, blockMetaData);

				dataLength += keyMetaBlock.getLength() + valueBlock.getLength();
				numSuccessfullyRead++;

			} catch (SerializationException e) {

				/* Reading metadata failed, deallocate indexblocks */
				this.getLogger().warn("Reading {} failed", keyMetaBlock);
				this.indexFile.delete(indexData);
				this.dataFile.delete(keyMetaBlock, false);
			}

			numRead++;

			/* Log every 10 seconds if reading index takes a long time */
			if (System.currentTimeMillis() > lastTimeLogged + 1000L * 10) {
				this.getLogger().info(
						"{}: {}% read",
						new Object[] { this.getName(), numRead * 100 / indexDataEntries.size() });
				lastTimeLogged = System.currentTimeMillis();
			}
		}

		final int dataSpaceUsedPercent = (int) (dataLength * 100 / Math.max(1, this.dataFile.length()));

		if (!this.dataFile.checkConsistency()) {
			throw new IOException("Data File is inconsistent");
		}

		this.getLogger().info(
				"{}: Read index: {} entries, {}% dataSpace utilization",
				new Object[] { this.getName(), numSuccessfullyRead, dataSpaceUsedPercent });
	}


	@Override
	protected void doDelete(K key, final BlockMetaData metaData) throws CacheException {

		try {

			IndexData indexData = metaData.getIndexData();
			synchronized (this.indexFileLock) {
				synchronized (this.dataFileLock) {
					this.indexFile.delete(indexData);
					this.dataFile.delete(indexData.getKeyMetaBlock(), true);
					this.dataFile.delete(indexData.getValueBlock(), true);
				}
			}

		} catch (final IOException e) {
			throw new CacheException(e);
		}
	}


	@Override
	protected <T extends V> T doGet(K key, final BlockMetaData metaData) throws CacheException {

		try {

			synchronized (this.dataFileLock) {
				final byte[] data = this.dataFile.read(metaData.getIndexData().getValueBlock());
				return this.dataFromBytes(data);
			}

		} catch (final IOException e) {
			throw new CacheException(e);
		}
	}


	@Override
	protected <T extends V> T doPut(final K key, final T data, final long timeToLive, final long maxIdleTime)
			throws CacheException {

		try {

			final byte[] dataBytes = this.dataToBytes(data);
			final long expiry = System.currentTimeMillis() + timeToLive;

			KeyedMetaData<K> keyedMetaData = new KeyedMetaData<K>(key, expiry, System.currentTimeMillis(), maxIdleTime);
			byte[] keyedMetaDataBytes = Serializer.serialize(keyedMetaData);

			synchronized (this.indexFileLock) {
				synchronized (this.dataFileLock) {
					final DataBlock keyMetaBlock = this.dataFile.write(keyedMetaDataBytes);
					final DataBlock valueBlock = this.dataFile.write(dataBytes);

					IndexData indexData = new IndexData(keyMetaBlock, valueBlock);
					indexData = this.indexFile.write(indexData);

					BlockMetaData metaData = new BlockMetaData(indexData, keyedMetaData.getMetaData());

					this.putEntry(key, metaData);
				}
			}

		} catch (final IOException e) {
			throw new CacheException(e);
		}

		return data;
	}


	/**
	 * Creates the lock file or throws an exception if it already exists.
	 * 
	 * @return The created lock file.
	 * @throws IOException
	 *             Throwns if lock file already exists.
	 */
	private File createLockFile() throws IOException {

		File newLockFile = new File(this.baseDir, this.getName() + ".lock");

		if (newLockFile.exists()) {
			throw new IOException(
					String.format(
							"Lock file found, this usually means that another cache was already instantiated under the same name or was not shut down correctly. In the latter case you can delete the lock file at %s and reinstantiate the cache.",
							newLockFile.getPath()));
		}
		newLockFile.createNewFile();

		return newLockFile;
	}


	protected abstract <T extends V> T dataFromBytes(final byte[] data) throws CacheException;


	protected abstract <T extends V> byte[] dataToBytes(T data) throws CacheException;
}
