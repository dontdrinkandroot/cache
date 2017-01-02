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
package net.dontdrinkandroot.cache.impl.disk.indexed.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * @author Philip Washington Sorst <philip@sorst.net>
 */
public class DataFile
{
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected final RandomAccessFile randomAccessFile;

    /**
     * Ordered set of used blocks
     */
    protected final TreeSet<DataBlock> usedBlocks;

    /**
     * Pointer to the last used block.
     */
    protected DataBlock lastBlock = null;

    protected File file;

    public DataFile(final File file) throws FileNotFoundException
    {
        this.file = file;
        this.randomAccessFile = new RandomAccessFile(file, "rw");
        this.usedBlocks = new TreeSet<DataBlock>();
    }

    public synchronized void allocateSpace(final DataBlock dataBlock) throws AllocationException
    {
        this.checkIfNotExists(dataBlock);
        this.logger.debug("Allocating {}", dataBlock.toString());
        this.addBlock(dataBlock);
    }

    public synchronized boolean checkConsistency()
    {
        if (this.usedBlocks.isEmpty()) {
            return true;
        }

        final Iterator<DataBlock> dataBlockIterator = this.usedBlocks.iterator();
        DataBlock last = dataBlockIterator.next();

        while (dataBlockIterator.hasNext()) {
            final DataBlock current = dataBlockIterator.next();
            if (last.overlaps(current)) {
                return false;
            }
            last = current;
        }

        return true;
    }

    /**
     * Closes the underlying random access file.
     */
    public synchronized void close() throws IOException
    {
        this.randomAccessFile.close();
    }

    public synchronized void delete(final DataBlock dataBlock, boolean truncate)
    {
        this.checkIfExists(dataBlock);
        this.logger.debug("Releasing {}", dataBlock.toString());

        this.usedBlocks.remove(dataBlock);

        if (truncate && this.lastBlock != null && dataBlock.equals(this.lastBlock)) {

            this.lastBlock = this.findLastBlock();

            if (this.lastBlock != null) {

                this.logger.info("Truncating file to " + this.lastBlock.getEndPosition() + 1);
                try {
                    this.randomAccessFile.setLength(this.lastBlock.getEndPosition() + 1);
                } catch (final IOException e) {
                    this.logger.error("Truncating file to " + (this.lastBlock.getEndPosition() + 1) + " failed");
                }
            } else {

                this.logger.info("Truncating file to 0");
                try {
                    this.randomAccessFile.setLength(0);
                } catch (final IOException e) {
                    this.logger.error("Truncating file to 0 failed");
                }
            }
        }
    }

    public synchronized int getNumAllocated()
    {
        return this.usedBlocks.size();
    }

    public String getFileName()
    {
        return this.file.getPath();
    }

    /**
     * Returns the length of the underlying random access file.
     *
     * @throws IOException
     */
    public synchronized long length() throws IOException
    {
        return this.randomAccessFile.length();
    }

    public synchronized byte[] read(final DataBlock dataBlock) throws IOException
    {
        this.checkIfExists(dataBlock);

        this.randomAccessFile.seek(dataBlock.getStartPosition());
        final byte[] data = new byte[(int) dataBlock.getLength()];
        this.randomAccessFile.read(data, 0, (int) dataBlock.getLength());

        return data;
    }

    public synchronized DataBlock write(final byte[] data) throws IOException
    {
        if (data.length <= 0) {
            throw new IllegalArgumentException("Cannot write data with length smaller equals 0 (was "
                    + data.length
                    + ")");
        }

        final DataBlock dataBlock = this.allocateSpace(data.length);
        this.randomAccessFile.seek(dataBlock.getStartPosition());
        this.randomAccessFile.write(data);

        // this.delayWrite();

        return dataBlock;
    }

    /**
     * Delays the write process, only for tests.
     */
    private void delayWrite()
    {
        long targetTime = (long) (System.currentTimeMillis() + Math.random() * 10);
        long sleepTime = targetTime - System.currentTimeMillis();
        while (sleepTime > 0) {
            try {
                Thread.sleep(sleepTime);
                sleepTime = targetTime - System.currentTimeMillis();
            } catch (InterruptedException e) {
            }
        }
    }

    private void addBlock(final DataBlock dataBlock)
    {
        if (this.lastBlock == null) {

            this.lastBlock = dataBlock;
        } else {

            if (dataBlock.compareTo(this.lastBlock) > 0) {
                this.lastBlock = dataBlock;
            }
        }

        this.usedBlocks.add(dataBlock);
    }

    private DataBlock allocateSpace(final long length) throws AllocationException
    {
        /* Remember start and end position for new used space */
        long foundFreeStartPosition = 0;
        long foundFreeEndPosition = 0;

		/* Find start and end position and insertion index */
        final Iterator<DataBlock> usedSpaceIterator = this.usedBlocks.iterator();
        while (usedSpaceIterator.hasNext()) {
            final DataBlock usedSpace = usedSpaceIterator.next();
            if (usedSpace.getStartPosition() - foundFreeStartPosition >= length) {
                /* Possible to insert new space before the one found */
                foundFreeEndPosition = foundFreeStartPosition + length - 1;
                break;
            } else {
                /* Not possible to insert, advance start position */
                foundFreeStartPosition = usedSpace.getEndPosition() + 1;
            }
        }

		/* Prolong if not long enough (can only be the last one) */
        if (foundFreeEndPosition - foundFreeStartPosition < length) {
            foundFreeEndPosition = foundFreeStartPosition + length - 1;
        }

		/* Create new used space */
        final DataBlock dataBlock = new DataBlock(foundFreeStartPosition, foundFreeEndPosition);
        this.logger.debug("Allocating {}", dataBlock.toString());

        this.checkIfNotExists(dataBlock);

		/* Insert at index or at end if none found */
        this.addBlock(dataBlock);

        return dataBlock;
    }

    private void checkIfExists(final DataBlock dataBlock)
    {
        if (!this.usedBlocks.contains(dataBlock)) {
            throw new RuntimeException("Data Block not found : " + dataBlock);
        }
    }

    private void checkIfNotExists(final DataBlock dataBlock) throws AllocationException
    {
        if (this.usedBlocks.contains(dataBlock)) {
            throw new AllocationException("Trying to allocate copy of " + dataBlock);
        }
    }

    private DataBlock findLastBlock()
    {
        final Iterator<DataBlock> iterator = this.usedBlocks.iterator();
        DataBlock last = null;
        while (iterator.hasNext()) {
            last = iterator.next();
        }

        return last;
    }
}
