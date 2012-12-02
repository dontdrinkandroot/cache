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

package net.dontdrinkandroot.cache.statistics;

import java.io.Serializable;


/**
 * @author Philip W. Sorst <philip@sorst.net>
 */
public interface CacheStatistics extends Serializable {

	/**
	 * Get the total number of cache hits.
	 */
	long getCacheHits();


	/**
	 * Get the number of cache misses where the entry was not found.
	 */
	long getCacheMissesNotFound();


	/**
	 * Get the total put count.
	 */
	long getPutCount();


	/**
	 * Get the total get count.
	 */
	long getGetCount();


	/**
	 * Reset the statistics.
	 */
	void reset();


	/**
	 * Get the current hit rate, this is a value [0,1] which is the percentage of hits on all get requests.
	 */
	float getHitRate();


	/**
	 * Get the total number of cache misses.
	 */
	long getCacheMisses();


	/**
	 * Get the current size (the number of entries) in the cache.
	 */
	int getCurrentSize();


	/**
	 * Get the number of cache misses where the entry was expired.
	 */
	long getCacheMissesExpired();
}