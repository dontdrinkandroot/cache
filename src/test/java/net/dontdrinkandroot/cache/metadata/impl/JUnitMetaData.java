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

public class JUnitMetaData implements MetaData
{
    public static final double DECAY_FACTOR = 0.9;

    private long timeToLive;

    private long lastAccess;

    private long maxIdleTime;

    private long created;

    private int hitCount;

    public JUnitMetaData()
    {
        /* Noop */
    }

    @Override
    public boolean isExpired()
    {
        return this.created + this.timeToLive < System.currentTimeMillis();
    }

    @Override
    public long getExpiry()
    {
        return this.created + this.timeToLive;
    }

    @Override
    public void update()
    {
        if (this.hitCount < Integer.MAX_VALUE) {
            this.hitCount++;
        }
        this.lastAccess = System.currentTimeMillis();
    }

    @Override
    public int getHitCount()
    {
        return this.hitCount;
    }

    @Override
    public long getLastAccess()
    {
        return this.lastAccess;
    }

    @Override
    public long getCreated()
    {
        return this.created;
    }

    @Override
    public long getTimeToLive()
    {
        return this.timeToLive;
    }

    public JUnitMetaData setCreated(long created)
    {
        this.created = created;
        return this;
    }

    public JUnitMetaData setExpiry(long expiry)
    {
        this.timeToLive = expiry - this.created;
        return this;
    }

    public JUnitMetaData setHitCount(int hits)
    {
        this.hitCount = hits;
        return this;
    }

    public JUnitMetaData setLastAccess(long lastAccess)
    {
        this.lastAccess = lastAccess;
        return this;
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
        if (this.hitCount > 0) {
            this.hitCount = (int) Math.floor(this.hitCount * JUnitMetaData.DECAY_FACTOR);
        }
    }
}