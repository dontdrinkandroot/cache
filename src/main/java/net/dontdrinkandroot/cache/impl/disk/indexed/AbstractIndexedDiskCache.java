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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.dontdrinkandroot.cache.CacheException;
import net.dontdrinkandroot.cache.expungestrategy.ExpungeStrategy;
import net.dontdrinkandroot.cache.impl.AbstractMapBackedCustomTtlCache;
import net.dontdrinkandroot.cache.impl.disk.indexed.storage.DataBlock;
import net.dontdrinkandroot.cache.impl.disk.indexed.storage.DataFile;
import net.dontdrinkandroot.cache.impl.disk.indexed.storage.IndexData;
import net.dontdrinkandroot.cache.impl.disk.indexed.storage.IndexFile;
import net.dontdrinkandroot.cache.impl.disk.indexed.storage.KeyedMetaData;
import net.dontdrinkandroot.cache.metadata.impl.BlockMetaData;
import net.dontdrinkandroot.utils.lang.SerializationUtils;

import org.apache.commons.lang3.SerializationException;


/**
 * @author Philip W. Sorst <philip@sorst.net>
 */
public abstract class AbstractIndexedDiskCache<K extends Serializable, V extends Serializable>
		extends AbstractMapBackedCustomTtlCache<K, V, BlockMetaData> {

	protected final IndexFile indexFile;

	protected final DataFile dataFile;

	protected final File lockFile;


	public AbstractIndexedDiskCache(
			final String name,
			final long defaultTimeToLive,
			final ExpungeStrategy expungeStrategy,
			final File baseDir) throws IOException {

		super(name, defaultTimeToLive, expungeStrategy);

		baseDir.mkdirs();
		/* Check lock file does not exist */
		this.lockFile = new File(baseDir, name + ".lock");
		if (this.lockFile.exists()) {
			throw new IOException(
					String.format(
							"Lock file found, this usually means that another cache was already instantiated under the same name or was not shut down correctly. In the latter case you can delete the lock file at %s and reinstantiate the cache.",
							this.lockFile.getPath()));
		}
		this.lockFile.createNewFile();

		/* Open block files */
		this.indexFile = new IndexFile(new File(baseDir, name + ".index"));
		this.dataFile = new DataFile(new File(baseDir, name + ".data"));

		/* Read index */
		this.setEntriesMetaDataMap(this.buildIndex());
		this.getStatistics().setCurrentSize(this.getEntriesMetaDataMap().size());
	}


	public AbstractIndexedDiskCache(
			final String name,
			final long defaultTimeToLive,
			final long defaultMaxIdleTime,
			final ExpungeStrategy expungeStrategy,
			final File baseDir) throws IOException {

		super(name, defaultTimeToLive, defaultMaxIdleTime, expungeStrategy);

		baseDir.mkdirs();
		/* Check lock file does not exist */
		this.lockFile = new File(baseDir, name + ".lock");
		if (this.lockFile.exists()) {
			throw new IOException(
					String.format(
							"Lock file found, this usually means that another cache was already instantiated under the same name or was not shut down correctly. In the latter case you can delete the lock file at %s and reinstantiate the cache.",
							this.lockFile.getPath()));
		}
		this.lockFile.createNewFile();

		/* Open block files */
		this.indexFile = new IndexFile(new File(baseDir, name + ".index"));
		this.dataFile = new DataFile(new File(baseDir, name + ".data"));

		/* Read index */
		this.setEntriesMetaDataMap(this.buildIndex());
		this.getStatistics().setCurrentSize(this.getEntriesMetaDataMap().size());
	}


	/**
	 * Closes the cache.
	 */
	public synchronized void close() throws IOException {

		this.indexFile.close();
		this.dataFile.close();
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


	protected Map<K, BlockMetaData> buildIndex() throws IOException {

		this.getLogger().info("{}: Reading index", this.getName());

		long dataLength = 0;
		final Map<K, BlockMetaData> entries = new HashMap<K, BlockMetaData>();
		final Collection<IndexData> indexDataEntries = this.indexFile.initialize();

		for (IndexData indexData : indexDataEntries) {

			DataBlock keyMetaBlock = indexData.getKeyMetaBlock();
			DataBlock valueBlock = indexData.getValueBlock();

			try {

				this.dataFile.allocateSpace(keyMetaBlock);
				byte[] keyMetaBytes = this.dataFile.read(keyMetaBlock);
				@SuppressWarnings("unchecked")
				KeyedMetaData<K> keyedMetaData = (KeyedMetaData<K>) SerializationUtils.deserialize(keyMetaBytes);
				K key = keyedMetaData.getKey();
				BlockMetaData blockMetaData = new BlockMetaData(indexData, keyedMetaData.getMetaData());
				this.dataFile.allocateSpace(valueBlock);
				entries.put(key, blockMetaData);

				dataLength += keyMetaBlock.getLength() + valueBlock.getLength();

			} catch (SerializationException e) {

				/* Reading metadata failed, deallocate indexblocks */
				this.getLogger().warn("Reading {} failed", keyMetaBlock);
				this.indexFile.delete(indexData);
				this.dataFile.delete(keyMetaBlock, false);
			}
		}

		final int dataSpaceUsedPercent = (int) (dataLength * 100 / Math.max(1, this.dataFile.length()));

		if (!this.dataFile.checkConsistency()) {
			throw new IOException("Data File is inconsistent");
		}

		this.getLogger().info(
				"{}: Read index: {} entries, {}% dataSpace utilization",
				new Object[] { this.getName(), entries.size(), dataSpaceUsedPercent });

		return entries;
	}


	@Override
	protected void doDelete(K key, final BlockMetaData metaData) throws CacheException {

		try {

			IndexData indexData = metaData.getIndexData();
			this.indexFile.delete(indexData);
			this.dataFile.delete(indexData.getKeyMetaBlock(), true);
			this.dataFile.delete(indexData.getValueBlock(), true);

		} catch (final IOException e) {
			throw new CacheException(e);
		}
	}


	@Override
	protected V doGet(K key, final BlockMetaData metaData) throws CacheException {

		try {

			final byte[] data = this.dataFile.read(metaData.getIndexData().getValueBlock());
			return this.dataFromBytes(data);

		} catch (final IOException e) {
			throw new CacheException(e);
		}
	}


	@Override
	protected V doPut(final K key, final V data, final long timeToLive, final long maxIdleTime) throws CacheException {

		try {

			final byte[] dataBytes = this.dataToBytes(data);
			final long expiry = System.currentTimeMillis() + timeToLive;

			KeyedMetaData<K> keyedMetaData = new KeyedMetaData<K>(key, expiry, System.currentTimeMillis(), maxIdleTime);
			byte[] keyedMetaDataBytes = SerializationUtils.serialize(keyedMetaData);

			final DataBlock keyMetaBlock = this.dataFile.write(keyedMetaDataBytes);
			final DataBlock valueBlock = this.dataFile.write(dataBytes);

			IndexData indexData = new IndexData(keyMetaBlock, valueBlock);
			indexData = this.indexFile.write(indexData);

			BlockMetaData metaData = new BlockMetaData(indexData, keyedMetaData.getMetaData());

			this.getEntriesMetaDataMap().put(key, metaData);

		} catch (final IOException e) {
			throw new CacheException(e);
		}

		return data;
	}


	protected abstract V dataFromBytes(final byte[] data) throws CacheException;


	protected abstract byte[] dataToBytes(V data) throws CacheException;
}
