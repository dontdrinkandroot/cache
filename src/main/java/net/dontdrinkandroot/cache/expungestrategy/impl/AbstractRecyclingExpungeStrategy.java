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
package net.dontdrinkandroot.cache.expungestrategy.impl;

import net.dontdrinkandroot.cache.statistics.CacheStatistics;


/**
 * An {@link AbstractMaxSizeExpungeStrategy} that triggers once the cache size is larger than a max size plus a recycle
 * size or a recycle factor.
 * 
 * @author Philip W. Sorst <philip@sorst.net>
 */
public abstract class AbstractRecyclingExpungeStrategy extends AbstractMaxSizeExpungeStrategy {

	private int recycleSize;


	/**
	 * Constructs a new {@link AbstractRecyclingExpungeStrategy} with the given max size and recycle factor (a value
	 * between 0 and 1 that is the percentage of the max size to be used as the recycle size).
	 */
	public AbstractRecyclingExpungeStrategy(final int maxSize, final float recycleFactor) {

		super(maxSize);
		this.setRecycleFactor(recycleFactor);
	}


	/**
	 * Constructs a new {@link AbstractRecyclingExpungeStrategy} with the given max size and recycle size.
	 */
	public AbstractRecyclingExpungeStrategy(final int maxSize, final int recycleSize) {

		super(maxSize);
		this.recycleSize = recycleSize;
	}


	/**
	 * Set the recycle size.
	 */
	public void setRecycleSize(final int recycleSize) {

		this.recycleSize = recycleSize;
	}


	/**
	 * Get the recycle size.
	 */
	public int getRecycleSize() {

		return this.recycleSize;
	}


	/**
	 * Set the recycle factor (a value between 0 and 1 that is the percentage of the max size to be used as the recycle
	 * size).
	 */
	public void setRecycleFactor(final float recycleFactor) {

		this.recycleSize = (int) (this.maxSize * recycleFactor);
	}


	@Override
	public boolean triggers(final CacheStatistics statistics) {

		return statistics.getCurrentSize() >= this.maxSize + this.recycleSize;
	}
}