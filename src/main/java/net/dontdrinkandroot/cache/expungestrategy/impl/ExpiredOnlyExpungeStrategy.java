/**
 * Copyright (C) 2012 Philip W. Sorst <philip@sorst.net>
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
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import net.dontdrinkandroot.cache.expungestrategy.ExpungeStrategy;
import net.dontdrinkandroot.cache.metadata.MetaData;
import net.dontdrinkandroot.cache.statistics.CacheStatistics;


/**
 * ExpungeStrategy that selects only entries that are expired.
 * 
 * @author Philip W. Sorst <philip@sorst.net>
 */
public class ExpiredOnlyExpungeStrategy implements ExpungeStrategy {

	private final long cleanupInterval;

	private long lastCleanUp;


	/**
	 * Creates a new {@link ExpiredOnlyExpungeStrategy}.
	 * 
	 * @param cleanUpInterval
	 *            How long to wait in milliseconds before the strategy is triggered again.
	 */
	public ExpiredOnlyExpungeStrategy(long cleanUpInterval) {

		this.lastCleanUp = System.currentTimeMillis();
		this.cleanupInterval = cleanUpInterval;
	}


	@Override
	public boolean triggers(CacheStatistics statistics) {

		return System.currentTimeMillis() - this.lastCleanUp > this.cleanupInterval;
	}


	@Override
	public <K, M extends MetaData> Collection<Entry<K, M>> getToExpungeMetaData(Set<Entry<K, M>> entrySet) {

		this.lastCleanUp = System.currentTimeMillis();
		final List<Entry<K, M>> toExpunge = new ArrayList<Entry<K, M>>();

		for (final Entry<K, M> entry : entrySet) {
			MetaData metaData = entry.getValue();
			if (metaData.isExpired() || metaData.isIdledAway()) {
				toExpunge.add(entry);
			}
		}

		return toExpunge;
	}

}
