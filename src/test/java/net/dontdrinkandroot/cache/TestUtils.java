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
package net.dontdrinkandroot.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.dontdrinkandroot.utils.lang.time.DateUtils;

import org.junit.Assert;


public class TestUtils {

	public static <T> void assertContainsExactly(Collection<T> coll, T... objects) {

		List<T> expected = new ArrayList<T>(coll);
		for (T object : objects) {
			int idx = expected.indexOf(object);
			if (idx == -1) {
				Assert.fail("Object " + object + " not found");
			} else {
				expected.remove(idx);
			}
		}

		if (!expected.isEmpty()) {
			Assert.fail("Remaining objects: " + coll);
		}
	}


	public static long getFutureExpiry() {

		return System.currentTimeMillis() + DateUtils.MILLIS_PER_DAY;
	}

}
