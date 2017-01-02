/*
 * Copyright (C) 2012-2017 Philip Washington Sorst <philip@sorst.net>
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

import net.dontdrinkandroot.cache.Cache;
import net.dontdrinkandroot.cache.metadata.MetaData;

/**
 * @author Philip Washington Sorst <philip@sorst.net>
 */
public class SimpleMetaData implements MetaData
{
    public static final double DECAY_FACTOR = 0.9;

    private final long timeToLive;

    private final long created;

    private final long maxIdleTime;

    private long lastAccess;

    /**
     * How often the corresponding entry has been accessed.
     */
    private int hitCount = 1;

    public SimpleMetaData(final long timeToLive)
    {
        this.created = System.currentTimeMillis();
        this.timeToLive = timeToLive;
        this.lastAccess = System.currentTimeMillis();
        this.maxIdleTime = Cache.UNLIMITED_IDLE_TIME;
    }

    public SimpleMetaData(long created, final long timeToLive)
    {
        this.created = created;
        this.timeToLive = timeToLive;
        this.lastAccess = System.currentTimeMillis();
        this.maxIdleTime = Cache.UNLIMITED_IDLE_TIME;
    }

    public SimpleMetaData(long created, final long timeToLive, long maxIdleTime)
    {
        this.created = created;
        this.timeToLive = timeToLive;
        this.lastAccess = System.currentTimeMillis();
        this.maxIdleTime = maxIdleTime;
    }

    @Override
    public long getTimeToLive()
    {
        return this.timeToLive;
    }

    @Override
    public final long getExpiry()
    {
        return this.created + this.timeToLive;
    }

    @Override
    public final void update()
    {
        this.increaseHitCount();
        this.lastAccess = System.currentTimeMillis();
    }

    @Override
    public final long getLastAccess()
    {
        return this.lastAccess;
    }

    @Override
    public final int getHitCount()
    {
        return this.hitCount;
    }

    @Override
    public boolean isExpired()
    {
        return this.created + this.timeToLive < System.currentTimeMillis();
    }

    @Override
    public long getCreated()
    {
        return this.created;
    }

    @Override
    public boolean isStale()
    {
        if (this.maxIdleTime == Cache.UNLIMITED_IDLE_TIME) {
            return false;
        }

        return this.lastAccess + this.maxIdleTime < System.currentTimeMillis();
    }

    @Override
    public long getMaxIdleTime()
    {
        return this.maxIdleTime;
    }

    @Override
    public void decay()
    {
        this.hitCount = (int) Math.floor(this.hitCount * SimpleMetaData.DECAY_FACTOR);
    }

    public final void increaseHitCount()
    {
        if (this.hitCount < Integer.MAX_VALUE) {
            this.hitCount++;
        }
    }

    @Override
    public String toString()
    {
        StringBuffer sb = new StringBuffer("SimpleMetaData[");
        sb.append("timeToLive=" + this.timeToLive);
        sb.append(",");
        sb.append("created=" + this.timeToLive);
        sb.append(",");
        sb.append("maxIdleTime=" + this.maxIdleTime);
        sb.append(",");
        sb.append("lastAccess=" + this.lastAccess);
        sb.append("]");

        return sb.toString();
    }
}
