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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import net.dontdrinkandroot.cache.expungestrategy.ExpungeStrategy;
import net.dontdrinkandroot.cache.metadata.MetaData;


/**
 * An {@link ExpungeStrategy} that selects all expired entries and entries according to the
 * implementation so that after those entries have been expunged the cache size is not larger that
 * the given max size.
 * 
 * @author Philip W. Sorst <philip@sorst.net>
 */
public abstract class AbstractMaxSizeExpungeStrategy implements ExpungeStrategy {

	protected int maxSize;


	/**
	 * Constructs a new {@link AbstractMaxSizeExpungeStrategy} with the given max size.
	 */
	public AbstractMaxSizeExpungeStrategy(final int maxSize) {

		this.maxSize = maxSize;
	}


	/**
	 * Get the current max size of this strategy.
	 */
	public int getMaxSize() {

		return this.maxSize;
	}


	@Override
	public <K, M extends MetaData> Collection<Entry<K, M>> getToExpungeMetaData(Set<Entry<K, M>> entrySet) {

		List<Entry<K, M>> toExpunge = new ArrayList<Entry<K, M>>();
		Comparator<Entry<K, M>> comparator = this.getComparator();
		TreeSet<Entry<K, M>> orderedSet = new TreeSet<Entry<K, M>>(comparator);

		/* Select expired and idled away */
		for (final Entry<K, M> entry : entrySet) {

			MetaData metaData = entry.getValue();

			if (metaData.isExpired() || metaData.isIdledAway()) {
				toExpunge.add(entry);
			} else {
				orderedSet.add(entry);
			}
		}

		/* Select from remaining */
		final int numToDelete = entrySet.size() - toExpunge.size() + 1 - this.maxSize;
		final Iterator<Entry<K, M>> iterator = orderedSet.iterator();

		int numDeleted = 0;
		while (iterator.hasNext() && numDeleted < numToDelete) {
			Entry<K, M> entry = iterator.next();
			toExpunge.add(entry);
			numDeleted++;
		}

		return toExpunge;
	}


	/**
	 * Set the max size of this strategy.
	 */
	public void setMaxSize(final int maxSize) {

		this.maxSize = maxSize;
	}


	protected abstract <K, M extends MetaData> Comparator<Entry<K, M>> getComparator();

}