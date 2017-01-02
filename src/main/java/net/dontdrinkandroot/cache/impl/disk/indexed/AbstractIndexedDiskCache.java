/*
 * Copyright (C) 2012-2017 Philip Washington Sorst <philip@sorst.net>
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

import net.dontdrinkandroot.cache.Cache;
import net.dontdrinkandroot.cache.CacheException;
import net.dontdrinkandroot.cache.impl.AbstractMapBackedCustomTtlCache;
import net.dontdrinkandroot.cache.impl.disk.indexed.storage.*;
import net.dontdrinkandroot.cache.metadata.impl.BlockMetaData;
import net.dontdrinkandroot.cache.metadata.impl.SimpleMetaData;
import net.dontdrinkandroot.cache.utils.SerializationException;
import net.dontdrinkandroot.cache.utils.Serializer;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;

/**
 * @author Philip Washington Sorst <philip@sorst.net>
 */
public abstract class AbstractIndexedDiskCache<K extends Serializable, V extends Serializable>
        extends AbstractMapBackedCustomTtlCache<K, V, BlockMetaData>
{
    private static final int DEFAULT_QUEUE_SIZE_WARNING_LIMIT = 1000;

    protected Object indexFileLock = new Object();

    protected Object dataFileLock = new Object();

    protected final IndexFile indexFile;

    protected final DataFile dataFile;

    protected final File lockFile;

    protected int queueSizeWarningLimit = AbstractIndexedDiskCache.DEFAULT_QUEUE_SIZE_WARNING_LIMIT;

    private final File baseDir;

    private WriterThread<K, V> writerThread;

    public AbstractIndexedDiskCache(
            final String name,
            final long defaultTimeToLive,
            final int maxSize,
            final int recycleSize,
            final File baseDir
    ) throws IOException
    {
        this(name, defaultTimeToLive, Cache.UNLIMITED_IDLE_TIME, maxSize, recycleSize, baseDir);
    }

    public AbstractIndexedDiskCache(
            final String name,
            final long defaultTimeToLive,
            final long defaultMaxIdleTime,
            final int maxSize,
            final int recycleSize,
            final File baseDir
    ) throws IOException
    {
        super(name, defaultTimeToLive, defaultMaxIdleTime, maxSize, recycleSize);

        this.baseDir = baseDir;
        baseDir.mkdirs();
        this.lockFile = this.createLockFile();

		/* Open block files */
        this.indexFile = new IndexFile(new File(baseDir, name + ".index"));
        this.dataFile = new DataFile(new File(baseDir, name + ".data"));

		/* Read index */
        this.buildIndex();

        this.writerThread = new WriterThread<K, V>(this);
        this.writerThread.start();
    }

    /**
     * Set the limit when the writer thread is warning for an excess queue length.
     *
     * @param limit The limit.
     */
    public void setQueueSizeWarningLimit(int limit)
    {
        this.queueSizeWarningLimit = limit;
    }

    /**
     * Get the length of the writer thread queue.
     *
     * @return The length of the writer thread queue.
     */
    public int getWriteQueueLength()
    {
        return this.writerThread.getQueueLength();
    }

    /**
     * Checks if the writer thread is alive.
     *
     * @return If the writer thread is alive.
     */
    public boolean isWriterThreadAlive()
    {
        return this.writerThread.isAlive();
    }

    /**
     * Flushes the writer thread.
     */
    public synchronized void flush()
    {
        this.writerThread.flush();
    }

    /**
     * Closes the cache.
     */
    protected synchronized void close() throws IOException
    {
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
     * Get the {@link DataFile} of this cache. Only perform altering operations if you know what you are doing.
     */
    DataFile getDataFile()
    {
        return this.dataFile;
    }

    /**
     * Get the {@link IndexFile} of this cache. Only perform altering operations if you know what you are doing.
     */
    IndexFile getIndexFile()
    {
        return this.indexFile;
    }

    protected void buildIndex() throws IOException
    {
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
                        new Object[]{this.getName(), numRead * 100 / indexDataEntries.size()}
                );
                lastTimeLogged = System.currentTimeMillis();
            }
        }

        final int dataSpaceUsedPercent = (int) (dataLength * 100 / Math.max(1, this.dataFile.length()));

        if (!this.dataFile.checkConsistency()) {
            throw new IOException("Data File is inconsistent");
        }

        this.getLogger().info(
                "{}: Read index: {} entries, {}% dataSpace utilization",
                this.getName(), numSuccessfullyRead, dataSpaceUsedPercent
        );
    }

    @Override
    protected void doDelete(K key, final BlockMetaData metaData) throws CacheException
    {
        try {

            if (!this.writerThread.remove(key)) {
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
    protected <T extends V> T doGet(K key, final BlockMetaData metaData) throws CacheException
    {
        try {

            byte[] data = this.writerThread.findDataBytes(key);

            if (data == null) {
                if (metaData.getIndexData() == null) {
                    throw new CacheException("Inconsistent data");
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
            throws CacheException
    {
        SimpleMetaData simpleMetaData = new SimpleMetaData(System.currentTimeMillis(), timeToLive, maxIdleTime);
        final byte[] dataBytes = this.dataToBytes(data);
        BlockMetaData metaData = new BlockMetaData(simpleMetaData);
        this.writerThread.add(key, metaData, dataBytes);
        this.putEntry(key, metaData);

        return data;
    }

    /**
     * Creates the lock file or throws an exception if it already exists.
     *
     * @return The created lock file.
     * @throws IOException Thrown if lock file already exists.
     */
    private File createLockFile() throws IOException
    {
        File newLockFile = new File(this.baseDir, this.getName() + ".lock");

        if (newLockFile.exists()) {
            throw new IOException(String.format(
                    "Lock file found, this usually means that another cache was already instantiated under "
                            + "the same name or was not shut down correctly. In the latter case you can delete the "
                            + "lock file at %s and reinstantiate the cache.",
                    newLockFile.getPath()
            ));
        }
        newLockFile.createNewFile();

        return newLockFile;
    }

    protected abstract <T extends V> T dataFromBytes(final byte[] data) throws CacheException;

    protected abstract <T extends V> byte[] dataToBytes(T data) throws CacheException;
}
