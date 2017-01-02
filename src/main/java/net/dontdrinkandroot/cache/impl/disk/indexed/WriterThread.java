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

import net.dontdrinkandroot.cache.impl.disk.indexed.storage.DataBlock;
import net.dontdrinkandroot.cache.impl.disk.indexed.storage.IndexData;
import net.dontdrinkandroot.cache.impl.disk.indexed.storage.KeyedMetaData;
import net.dontdrinkandroot.cache.metadata.impl.BlockMetaData;
import net.dontdrinkandroot.cache.utils.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

class WriterThread<K extends Serializable, V extends Serializable> extends Thread
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * The cache this thread belongs to.
     */
    private final AbstractIndexedDiskCache<K, V> cache;

    /**
     * Queue of the entries to write to disk.
     */
    private final LinkedHashMap<K, QueueEntry> queue = new LinkedHashMap<K, QueueEntry>();

    /**
     * Locks access on the queue.
     */
    private final Object queueLock = new Object();

    /**
     * Locks the write process.
     */
    private final Object processingLock = new Object();

    /**
     * Indicates if the thread is about to be shut down.
     */
    private boolean stopRequested = false;

    /**
     * The key of the currently processed entry.
     */
    private K currentKey = null;

    /**
     * The currently processed entry.
     */
    private QueueEntry currentQueueEntry = null;

    /**
     * If the processing of the current entry is completed.
     */
    private boolean entryWasProcessed = false;

    /**
     * Skip the processing of the current entry.
     */
    private boolean skipWrite = false;

    public WriterThread(AbstractIndexedDiskCache<K, V> cache)
    {
        super(cache.getName() + ".writer");
        this.cache = cache;
        this.setPriority(Thread.MIN_PRIORITY);
    }

    public int getQueueLength()
    {
        synchronized (this.queueLock) {
            return this.queue.size();
        }
    }

    /**
     * Flush all entries to disk.
     */
    public void flush()
    {
        synchronized (this) {

            this.logger.info(this.getName() + ": Flushing " + this.queue.size() + " entries");

            Iterator<Entry<K, QueueEntry>> iterator = this.queue.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<K, QueueEntry> entry = iterator.next();
                try {
                    this.write(entry.getKey(), entry.getValue());
                } catch (IOException e) {
                    this.logger.error(this.getName() + ": Writing " + entry.getKey() + " failed", e);
                }
                iterator.remove();
            }

            this.logger.info(this.getName() + ": Flushing done");
        }
    }

    /**
     * Checks if the key is part of the writer thread and returns the data if found.
     *
     * @param key The key to search for.
     * @return The data if part of the writer thread or null.
     */
    public byte[] findDataBytes(K key)
    {
        synchronized (this.queueLock) {

			/* Return dataBytes from entry currently being processed */
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

    /**
     * Adds the cache entry to the writer thread.
     *
     * @param key       The key of the entry.
     * @param metaData  The MetaData of the entry.
     * @param dataBytes The Data of the entry.
     */
    public void add(K key, BlockMetaData metaData, byte[] dataBytes)
    {
        synchronized (this.queueLock) {

			/*
             * Add queue entry, put does always delete before so we do not need to check for the current entry, if it
			 * was written to disk it is already deleted, if not it doesn't appear in the queue anymore. In other words:
			 */
            // if (key == currentKey && !this.entryWasProcessed) {
            // throw new RuntimeException("Can not happen");
            // }

            QueueEntry queueEntry = new QueueEntry();
            queueEntry.metaData = metaData;
            queueEntry.dataBytes = dataBytes;
            this.queue.put(key, queueEntry);

            if (this.queue.size() > this.cache.queueSizeWarningLimit) {
                this.logger.warn(this.getName() + ": Write queue is large: " + this.queue.size());
            }
        }

        this.interrupt();
    }

    /**
     * Removes the given key from the writer thread.
     *
     * @param key The key of the entry.
     * @return True if the entry was part of the writer thread and it was removed, false otherwise.
     */
    public boolean remove(K key)
    {
        synchronized (this.queueLock) {

			/* If it is in queue, remove it, it cannot be the current unprocessed entry */
            if (this.queue.containsKey(key)) {
                this.queue.remove(key);
                return true;
            }

			/* Check if entry is about to being processed */
            if (key.equals(this.currentKey)) {

                synchronized (this.processingLock) {

                    if (this.entryWasProcessed) {

						/* Entry was already written, so it is not part of the writer process anymore */
                        return false;
                    } else {

						/* Entry was not written make sure it doen't */
                        this.skipWrite = true;
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public void run()
    {
        try {

            int queueSize = 0;

            while (!this.stopRequested) {

                synchronized (this.processingLock) {
                    this.entryWasProcessed = false;
                    this.skipWrite = false;
                }

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
                            queueSize = this.queue.size();
                        }
                    }

                    if (this.currentKey != null) {

                        synchronized (this.processingLock) {
                            if (!this.skipWrite) {
                                try {
                                    this.logger.debug(this.getName() + ": Writing {}, {} left", key, queueSize);
                                    this.write(key, queueEntry);
                                } catch (IOException e) {
                                    this.logger.error(this.getName() + ": Writing entry failed", e);
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

            this.logger.info(this.getName() + " stopped");
        } catch (RuntimeException e) {
            this.logger.error(this.getName() + ": Something went horribly wrong", e);
            throw e;
        }
    }

    private void write(K key, QueueEntry queueEntry) throws IOException
    {
        KeyedMetaData<K> keyedMetaData = new KeyedMetaData<K>(key, queueEntry.metaData);
        byte[] keyedMetaDataBytes = Serializer.serialize(keyedMetaData);

        synchronized (this.cache.indexFileLock) {
            synchronized (this.cache.dataFileLock) {
                final DataBlock keyMetaBlock = this.cache.dataFile.write(keyedMetaDataBytes);
                final DataBlock valueBlock = this.cache.dataFile.write(queueEntry.dataBytes);
                IndexData indexData = new IndexData(keyMetaBlock, valueBlock);
                indexData = this.cache.indexFile.write(indexData);
                queueEntry.metaData.setIndexData(indexData);
            }
        }
    }

    public void requestStop()
    {
        this.stopRequested = true;
        this.interrupt();
    }

    class QueueEntry
    {

        BlockMetaData metaData;

        byte[] dataBytes;

        @Override
        public String toString()
        {
            return "QueueEntry[" + this.metaData.toString() + "]";
        }
    }
}
