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

import java.util.Collection;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;

import net.dontdrinkandroot.cache.expungestrategy.ExpungeStrategy;
import net.dontdrinkandroot.cache.metadata.MetaData;
import net.dontdrinkandroot.cache.statistics.CacheStatistics;


/**
 * ExpungeStrategy that never triggers and does not return any MetaData to expunge.
 * 
 * @author Philip W. Sorst <philip@sorst.net>
 */
public class NoopExpungeStrategy implements ExpungeStrategy {

	@Override
	public boolean triggers(final CacheStatistics statistics) {

		return false;
	}


	@Override
	public <K, M extends MetaData> Collection<Entry<K, M>> getToExpungeMetaData(Set<Entry<K, M>> entrySet) {

		return Collections.emptyList();
	}

}
