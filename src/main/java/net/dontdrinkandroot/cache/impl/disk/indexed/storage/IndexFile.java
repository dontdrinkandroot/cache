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

package net.dontdrinkandroot.cache.impl.disk.indexed.storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// TODO: implement shrinking of index file
/**
 * @author Philip W. Sorst <philip@sorst.net>
 */
public class IndexFile {

	public static float GOLDEN_RATIO = 1.61803399f;

	private final RandomAccessFile randomAccessFile;

	private boolean[] blockMap;

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private int numAllocated = 0;


	public IndexFile(final File file) throws FileNotFoundException {

		this.randomAccessFile = new RandomAccessFile(file, "rw");

		this.blockMap = new boolean[2];
		this.blockMap[0] = false;
		this.blockMap[1] = false;
	}


	/**
	 * Closes the underlying random access file.
	 */
	public synchronized void close() throws IOException {

		this.randomAccessFile.close();
	}


	RandomAccessFile getRandomAccessFile() {

		return this.randomAccessFile;
	}


	/**
	 * Deletes the block of the given metaData.
	 */
	public synchronized void delete(IndexData indexData) throws IOException {

		this.delete(indexData.getBlockNum());
	}


	/**
	 * Deletes the given block.
	 */
	public synchronized void delete(final int blockNum) throws IOException {

		/* Seek to the block, invalidate by writing 0 and deallocate block */
		this.randomAccessFile.seek(blockNum * IndexData.LENGTH);
		this.randomAccessFile.writeBoolean(false);
		this.blockMap[blockNum] = false;
		this.numAllocated--;
		this.logger.debug("Invalidating {}, {} allocated", blockNum, this.numAllocated);
	}


	/**
	 * Gets the number of allocated blocks.
	 */
	public synchronized int getNumAllocated() {

		return this.numAllocated;
	}


	public synchronized Collection<IndexData> initialize() throws IOException {

		List<IndexData> entries = new ArrayList<IndexData>();

		final int numBlocks = this.getNumPossibleBlocks(this.randomAccessFile.length());

		for (int currentBlockNum = 0; currentBlockNum < numBlocks; currentBlockNum++) {

			try {

				IndexData data = IndexData.read(this.randomAccessFile, currentBlockNum);
				if (data != null) {
					this.allocateBlock(currentBlockNum);
					entries.add(data);
				}

			} catch (final AllocationException e) {
				this.logger.error("Allocating " + currentBlockNum + " failed");
			} catch (final IOException e) {
				this.logger.warn("Reading failed at {}: {}", new Object[] { currentBlockNum, e.getMessage() });
			}

		}

		return entries;
	}


	/**
	 * Returns the length of the underlying random access file.
	 */
	public long length() throws IOException {

		return this.randomAccessFile.length();
	}


	public IndexData write(IndexData indexData) throws IOException {

		final int blockNum = this.allocateBlock();
		return indexData.write(this.randomAccessFile, blockNum);
	}


	/**
	 * Find the first free block and allocate or allocate at end.
	 */
	private int allocateBlock() throws AllocationException {

		for (int currentBlockNum = 0; currentBlockNum < this.blockMap.length; currentBlockNum++) {

			if (!this.blockMap[currentBlockNum]) {
				return this.allocateBlock(currentBlockNum);
			}

		}

		return this.allocateBlock(this.blockMap.length);
	}


	private int allocateBlock(final int blockNum) throws AllocationException {

		/* Enlarge available blocks if needed */
		if (blockNum > this.blockMap.length - 1) {
			this.enlargeBlockMap(blockNum);
		}

		/* Block already occupied, fail */
		if (this.blockMap[blockNum]) {
			throw new AllocationException("Block " + blockNum + " already in use");
		}

		/* Allocate block */
		this.blockMap[blockNum] = true;
		this.numAllocated++;
		this.logger.debug("Allocating {}, {} allocated", blockNum, this.numAllocated);

		return blockNum;
	}


	private void enlargeBlockMap(final int neededBlockNum) {

		final int newLength = Math.max(neededBlockNum + 1, (int) (this.blockMap.length * IndexFile.GOLDEN_RATIO));
		this.blockMap = Arrays.copyOf(this.blockMap, newLength);
	}


	private int getNumPossibleBlocks(final long length) {

		int numRequiredBlocks = (int) (length / IndexData.LENGTH);
		if (length % IndexData.LENGTH > 0) {
			numRequiredBlocks++;
		}

		return numRequiredBlocks;
	}

}
