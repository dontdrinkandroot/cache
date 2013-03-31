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
package net.dontdrinkandroot.cache.impl;

import java.io.Serializable;

import net.dontdrinkandroot.cache.AbstractCustomTtlCacheTest;
import net.dontdrinkandroot.cache.Cache;
import net.dontdrinkandroot.cache.ExampleObject;

import org.junit.Assert;


public abstract class AbstractSerializableCustomTtlCacheTest
		extends AbstractCustomTtlCacheTest<Serializable, Serializable> {

	@Override
	protected void doAssertGet(int key, Cache<Serializable, Serializable> cache) throws Exception {

		Serializable ser = cache.getWithErrors(this.translateKey(key));
		Assert.assertNotNull(ser);
		Assert.assertEquals(new ExampleObject(key), ser);
	}


	@Override
	protected Serializable createInputObject(int key) throws Exception {

		return new ExampleObject(key);
	}

}