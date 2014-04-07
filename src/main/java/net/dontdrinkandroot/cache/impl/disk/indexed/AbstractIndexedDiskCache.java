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
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import net.dontdrinkandroot.cache.Cache;
import net.dontdrinkandroot.cache.CacheException;
import net.dontdrinkandroot.cache.impl.AbstractMapBackedCustomTtlCache;
import net.dontdrinkandroot.cache.impl.disk.indexed.storage.DataBlock;
import net.dontdrinkandroot.cache.impl.disk.indexed.storage.DataFile;
import net.dontdrinkandroot.cache.impl.disk.indexed.storage.IndexData;
import net.dontdrinkandroot.cache.impl.disk.indexed.storage.IndexFile;
import net.dontdrinkandroot.cache.impl.disk.indexed.storage.KeyedMetaData;
import net.dontdrinkandroot.cache.metadata.impl.BlockMetaData;
import net.dontdrinkandroot.cache.metadata.impl.SimpleMetaData;
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

	private WriterThread writerThread;


	public AbstractIndexedDiskCache(
			final String name,
			final long defaultTimeToLive,
			final int maxSize,
			final int recycleSize,
			final File baseDir) throws IOException {

		this(name, defaultTimeToLive, Cache.UNLIMITED_IDLE_TIME, maxSize, recycleSize, baseDir);
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

		this.writerThread = new WriterThread();
		this.writerThread.start();

		// Runtime.getRuntime().addShutdownHook(new Thread() {
		//
		// @Override
		// public void run() {
		//
		// try {
		// AbstractIndexedDiskCache.this.close();
		// AbstractIndexedDiskCache.this.writerThread.join();
		// } catch (Exception e) {
		// AbstractIndexedDiskCache.this.getLogger().error("Exception when shutting down", e);
		// }
		// }
		// });
	}


	/**
	 * Closes the cache.
	 */
	protected synchronized void close() throws IOException {

		this.flush();

		this.writerThread.requestStop();
		try {
			this.writerThread.join();
		} catch (InterruptedException e) {
		}

		synchronized (this.indexFileLock) {
			synchronized (this.dataFileLock) {
				this.indexFile.close();
				this.dataFile.close();
			}
		}
		if (this.lockFile.exists() && !this.lockFile.delete()) {
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

			if (!this.writerThread.removeFromQueue(key)) {
				IndexData indexData = metaData.getIndexData();
				synchronized (this.indexFileLock) {
					synchronized (this.dataFileLock) {
						this.indexFile.delete(indexData);
						this.dataFile.delete(indexData.getKeyMetaBlock(), true);
						this.dataFile.delete(indexData.getValueBlock(), true);
					}
				}
			}

		} catch (final IOException e) {
			throw new CacheException(e);
		}
	}


	@Override
	protected <T extends V> T doGet(K key, final BlockMetaData metaData) throws CacheException {

		try {

			byte[] data = this.writerThread.findDataBytes(key);

			if (data == null) {
				if (metaData.getIndexData() == null) {
					throw new CacheException("Incositent data");
				}
				synchronized (this.dataFileLock) {
					data = this.dataFile.read(metaData.getIndexData().getValueBlock());
				}
			}

			return this.dataFromBytes(data);

		} catch (final IOException e) {
			throw new CacheException(e);
		}
	}


	@Override
	protected <T extends V> T doPut(final K key, final T data, final long timeToLive, final long maxIdleTime)
			throws CacheException {

		SimpleMetaData simpleMetaData = new SimpleMetaData(System.currentTimeMillis(), timeToLive, maxIdleTime);
		final byte[] dataBytes = this.dataToBytes(data);
		BlockMetaData metaData = new BlockMetaData(simpleMetaData);
		this.writerThread.queue(key, metaData, dataBytes);
		this.putEntry(key, metaData);

		return data;
	}


	/**
	 * Creates the lock file or throws an exception if it already exists.
	 * 
	 * @return The created lock file.
	 * @throws IOException
	 *             Thrown if lock file already exists.
	 */
	private File createLockFile() throws IOException {

		File newLockFile = new File(this.baseDir, this.getName() + ".lock");

		if (newLockFile.exists()) {
			throw new IOException(String.format(
					"Lock file found, this usually means that another cache was already instantiated under "
							+ "the same name or was not shut down correctly. In the latter case you can delete the "
							+ "lock file at %s and reinstantiate the cache.",
					newLockFile.getPath()));
		}
		newLockFile.createNewFile();

		return newLockFile;
	}


	public synchronized void flush() {

		this.writerThread.flush();
	}


	protected abstract <T extends V> T dataFromBytes(final byte[] data) throws CacheException;


	protected abstract <T extends V> byte[] dataToBytes(T data) throws CacheException;


	class WriterThread extends Thread {

		private final LinkedHashMap<K, QueueEntry> queue = new LinkedHashMap<K, QueueEntry>();

		private final Object queueLock = new Object();

		private final Object processingLock = new Object();

		private boolean stopRequested = false;

		private K currentKey = null;

		private QueueEntry currentQueueEntry = null;

		private boolean entryWasProcessed = false;

		private boolean skipWrite = false;


		public WriterThread() {

			super(AbstractIndexedDiskCache.this.getName() + ".writer");
			this.setPriority(Thread.MIN_PRIORITY);
		}


		public void flush() {

			synchronized (this) {
				AbstractIndexedDiskCache.this.getLogger().info("Flushing " + this.queue.size() + " entries");
				Iterator<Entry<K, QueueEntry>> iterator = this.queue.entrySet().iterator();
				while (iterator.hasNext()) {
					Entry<K, QueueEntry> entry = iterator.next();
					try {
						this.write(entry.getKey(), entry.getValue());
					} catch (IOException e) {
						AbstractIndexedDiskCache.this.getLogger().error("Writing " + entry.getKey() + " failed", e);
					}
					iterator.remove();
				}
				AbstractIndexedDiskCache.this.getLogger().info("Flushing done");
			}

		}


		public byte[] findDataBytes(K key) {

			synchronized (this.queueLock) {

				/* Return dataBytes from currently being processed entry */
				if (key.equals(this.currentKey)) {
					return this.currentQueueEntry.dataBytes;
				}

				/* Return dataBytes from queue entry */
				QueueEntry queueEntry = this.queue.get(key);
				if (queueEntry != null) {
					return queueEntry.dataBytes;
				}
			}

			return null;
		}


		public void queue(K key, BlockMetaData metaData, byte[] dataBytes) {

			synchronized (this.queueLock) {
				/*
				 * Add queue entry, put does always delete before so we do not need to check for the
				 * current entry
				 */
				QueueEntry queueEntry = new QueueEntry();
				queueEntry.metaData = metaData;
				queueEntry.dataBytes = dataBytes;
				this.queue.put(key, queueEntry);
			}

			this.interrupt();
		}


		public boolean removeFromQueue(K key) {

			synchronized (this.queueLock) {

				if (key.equals(this.currentKey)) {
					synchronized (this.processingLock) {
						if (this.queue.containsKey(key)) {
							if (!this.entryWasProcessed) {
								/* Entry is still queued, skip */
								this.skipWrite = true;
							}
							this.queue.remove(key);
							return true;
						}

						if (this.entryWasProcessed) {
							/* Entry is already complete, delete on disk */
							return false;
						} else {
							/* Entry is still queued, skip */
							this.skipWrite = true;
							return true;
						}
					}
				}

				if (this.queue.containsKey(key)) {
					this.queue.remove(key);
					return true;
				}
			}

			return false;
		}


		@Override
		public void run() {

			while (!this.stopRequested) {

				this.entryWasProcessed = false;
				this.skipWrite = false;
				synchronized (this) {

					K key = null;
					QueueEntry queueEntry = null;

					synchronized (this.queueLock) {
						Iterator<Entry<K, QueueEntry>> iterator = this.queue.entrySet().iterator();
						if (iterator.hasNext()) {
							Entry<K, QueueEntry> entry = iterator.next();
							key = entry.getKey();
							queueEntry = entry.getValue();
							this.currentKey = key;
							this.currentQueueEntry = queueEntry;
							iterator.remove();
						}
					}

					if (this.currentKey != null) {

						synchronized (this.processingLock) {
							if (!this.skipWrite) {
								try {
									this.write(key, queueEntry);
								} catch (IOException e) {
									AbstractIndexedDiskCache.this.getLogger().error("Writing entry failed", e);
								} finally {
									this.entryWasProcessed = true;
								}
							}
						}

						synchronized (this.queueLock) {
							this.currentKey = null;
							this.currentQueueEntry = null;
						}
					}

				}

				if (!this.entryWasProcessed) {
					try {
						Thread.sleep(10000L);
					} catch (InterruptedException e) {
					}
				}
			}

			AbstractIndexedDiskCache.this.getLogger().info(this.getName() + " stopped");
		}


		private void write(K key, QueueEntry queueEntry) throws IOException {

			KeyedMetaData<K> keyedMetaData = new KeyedMetaData<K>(key, queueEntry.metaData);
			byte[] keyedMetaDataBytes = Serializer.serialize(keyedMetaData);

			synchronized (AbstractIndexedDiskCache.this.indexFileLock) {
				synchronized (AbstractIndexedDiskCache.this.dataFileLock) {
					final DataBlock keyMetaBlock = AbstractIndexedDiskCache.this.dataFile.write(keyedMetaDataBytes);
					final DataBlock valueBlock = AbstractIndexedDiskCache.this.dataFile.write(queueEntry.dataBytes);
					IndexData indexData = new IndexData(keyMetaBlock, valueBlock);
					indexData = AbstractIndexedDiskCache.this.indexFile.write(indexData);
					queueEntry.metaData.setIndexData(indexData);
					// this.currentQueueEntry.metaData.setIndexData(indexData);
				}
			}
		}


		public void requestStop() {

			this.stopRequested = true;
			this.interrupt();
		}


		class QueueEntry {

			BlockMetaData metaData;

			byte[] dataBytes;
		}

	}

}
