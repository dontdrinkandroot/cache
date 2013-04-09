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
package net.dontdrinkandroot.cache.metadata;

import java.io.Serializable;


/**
 * @author Philip W. Sorst <philip@sorst.net>
 */
public interface MetaData extends Serializable {

	/**
	 * Check if the entry is expired.
	 */
	boolean isExpired();


	/**
	 * Check if the entry has not been accessed within the idle period.
	 */
	boolean isIdledAway();


	/**
	 * Get the timestamp when the entry expires.
	 */
	long getExpiry();


	/**
	 * Get the maximum time that the entry may idle (not being accessed) before being evicted (in
	 * milliseconds).
	 */
	long getMaxIdleTime();


	/**
	 * Update the entry after a cache hit.
	 */
	void update();


	/**
	 * Get the number of hits.
	 */
	int getHitCount();


	/**
	 * Get the timestamp when the entry was last accessed.
	 */
	long getLastAccess();


	/**
	 * Get the timestamp when the entry was created.
	 */
	long getCreated();


	/**
	 * Decreases the hitcount for use with LRU based expunge strategies.
	 */
	void decay();
}
