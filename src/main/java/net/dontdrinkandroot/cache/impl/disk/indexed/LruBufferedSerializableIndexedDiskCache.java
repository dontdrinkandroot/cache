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

import net.dontdrinkandroot.cache.LruBufferedCache;
import net.dontdrinkandroot.cache.expungestrategy.ExpungeStrategy;
import net.dontdrinkandroot.cache.expungestrategy.impl.LruRecyclingExpungeStrategy;


/**
 * A {@link BufferedSerializableIndexedDiskCache} that uses a {@link LruRecyclingExpungeStrategy}
 * for both disk and buffer.
 * 
 * @author Philip W. Sorst <philip@sorst.net>
 * 
 */
public class LruBufferedSerializableIndexedDiskCache extends BufferedSerializableIndexedDiskCache
		implements LruBufferedCache {

	public LruBufferedSerializableIndexedDiskCache(
			String name,
			long defaultTimeToLive,
			File baseDir,
			int size,
			int recycleSize,
			int bufferSize,
			int bufferRecycleSize) throws IOException {

		super(
				name,
				defaultTimeToLive,
				new LruRecyclingExpungeStrategy(size, recycleSize),
				baseDir,
				new LruRecyclingExpungeStrategy(bufferSize, bufferRecycleSize));
	}


	public LruBufferedSerializableIndexedDiskCache(
			String name,
			long defaultTimeToLive,
			long defaultMaxIdleTime,
			File baseDir,
			int size,
			int recycleSize,
			int bufferSize,
			int bufferRecycleSize) throws IOException {

		super(
				name,
				defaultTimeToLive,
				defaultMaxIdleTime,
				new LruRecyclingExpungeStrategy(size, recycleSize),
				baseDir,
				new LruRecyclingExpungeStrategy(bufferSize, bufferRecycleSize));
	}


	@Override
	public void setExpungeStrategy(ExpungeStrategy expungeStrategy) {

		throw new UnsupportedOperationException("Can't change expunge strategy, fixed to LruRecyclingExpungeStrategy");
	}


	@Override
	public int getMaxSize() {

		return ((LruRecyclingExpungeStrategy) this.getExpungeStrategy()).getMaxSize();
	}


	@Override
	public void setMaxSize(int maxSize) {

		((LruRecyclingExpungeStrategy) this.getExpungeStrategy()).setMaxSize(maxSize);
	}


	@Override
	public int getRecycleSize() {

		return ((LruRecyclingExpungeStrategy) this.getExpungeStrategy()).getRecycleSize();
	}


	@Override
	public void setRecycleSize(int recycleSize) {

		((LruRecyclingExpungeStrategy) this.getExpungeStrategy()).setRecycleSize(recycleSize);
	}


	@Override
	public int getBufferMaxSize() {

		return ((LruRecyclingExpungeStrategy) this.bufferExpungeStrategy).getMaxSize();
	}


	@Override
	public void setBufferMaxSize(int maxSize) {

		((LruRecyclingExpungeStrategy) this.bufferExpungeStrategy).setMaxSize(maxSize);
	}


	@Override
	public int getBufferRecycleSize() {

		return ((LruRecyclingExpungeStrategy) this.bufferExpungeStrategy).getRecycleSize();
	}


	@Override
	public void setBufferRecycleSize(int bufferRecycleSize) {

		((LruRecyclingExpungeStrategy) this.bufferExpungeStrategy).setRecycleSize(bufferRecycleSize);
	}

}
