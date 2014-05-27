/**
 * Copyright (C) 2012-2014 Philip W. Sorst <philip@sorst.net>
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
package net.dontdrinkandroot.cache.metadata.impl;

import net.dontdrinkandroot.cache.impl.disk.indexed.storage.IndexData;
import net.dontdrinkandroot.cache.metadata.MetaData;


/**
 * @author Philip W. Sorst <philip@sorst.net>
 */
public class BlockMetaData implements MetaData
{

	private IndexData indexData;

	private final SimpleMetaData metaData;


	public BlockMetaData(IndexData indexData, SimpleMetaData metaData)
	{
		this.indexData = indexData;
		this.metaData = metaData;
	}


	public BlockMetaData(SimpleMetaData metaData)
	{
		this.metaData = metaData;
	}


	@Override
	public boolean isExpired()
	{
		return this.metaData.isExpired();
	}


	@Override
	public boolean isStale()
	{
		return this.metaData.isStale();
	}


	@Override
	public long getExpiry()
	{
		return this.metaData.getExpiry();
	}


	@Override
	public long getMaxIdleTime()
	{
		return this.metaData.getMaxIdleTime();
	}


	@Override
	public void update()
	{
		this.metaData.update();
	}


	@Override
	public int getHitCount()
	{
		return this.metaData.getHitCount();
	}


	@Override
	public long getLastAccess()
	{
		return this.metaData.getLastAccess();
	}


	@Override
	public long getCreated()
	{
		return this.metaData.getCreated();
	}


	@Override
	public void decay()
	{
		this.metaData.decay();
	}


	@Override
	public long getTimeToLive()
	{
		return this.metaData.getTimeToLive();
	}


	public SimpleMetaData getMetaData()
	{
		return this.metaData;
	}


	public IndexData getIndexData()
	{
		return this.indexData;
	}


	public void setIndexData(IndexData indexData)
	{
		this.indexData = indexData;
	}


	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer("BlockMetaData[");
		if (this.indexData != null) {
			sb.append("indexData=" + this.indexData.toString());
			sb.append(",");
		}
		if (this.metaData != null) {
			sb.append("metaData=" + this.metaData.toString());
		}
		sb.append("]");

		return sb.toString();
	}

}
