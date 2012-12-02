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

package net.dontdrinkandroot.cache.expungestrategy;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;

import net.dontdrinkandroot.cache.metadata.MetaData;
import net.dontdrinkandroot.cache.statistics.CacheStatistics;


/**
 * An ExpungeStrategy is called by caches to select the entries to evict.
 * 
 * @author Philip W. Sorst <philip@sorst.net>
 */
public interface ExpungeStrategy {

	/**
	 * Checks if the strategy triggers based on the given statistics.
	 */
	boolean triggers(CacheStatistics statistics);


	/**
	 * Returns a collection of entries that should be expunged according to this strategy.
	 */
	<K, M extends MetaData> Collection<Entry<K, M>> getToExpungeMetaData(Set<Entry<K, M>> entrySet);

}
