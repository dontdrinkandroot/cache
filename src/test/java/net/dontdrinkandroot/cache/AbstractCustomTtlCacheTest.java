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
package net.dontdrinkandroot.cache;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCustomTtlCacheTest<K, V> extends AbstractCacheTest<K, V>
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected Logger getLogger()
    {
        return this.logger;
    }

    protected void testCustomGetPutDelete(CustomTtlCache<K, V> cache) throws Exception
    {
        this.testDefaultPutGetDelete(cache);

        this.put(this.increaseAndGetCurrentId(), cache, 0L);

		/* Wait until expired */
        Thread.sleep(1);
        this.size--;

        this.assertExpired(this.getCurrentId(), cache);
    }

    protected V put(int key, CustomTtlCache<K, V> cache, long ttl) throws Exception
    {
        V object = cache.putWithErrors(this.translateKey(key), this.createInputObject(key), ttl);
        this.putCount++;
        this.size++;
        this.assertStatistics(cache);

        return object;
    }

    protected void assertExpired(int key, Cache<K, V> cache) throws Exception
    {
        Assert.assertNull(cache.getWithErrors(this.translateKey(key)));
        this.getCount++;
        this.expiredCount++;
        this.assertStatistics(cache);
    }
}
