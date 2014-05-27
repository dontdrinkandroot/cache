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
package net.dontdrinkandroot.cache.utils;

/**
 * Required Methods From Apache Commons Lang ObjectUtils.
 * 
 * @author Apache Commons Lang
 */
public class ObjectUtils
{

	public static <T extends Comparable<? super T>> int compare(T c1, T c2)
	{
		return ObjectUtils.compare(c1, c2, false);
	}


	public static <T extends Comparable<? super T>> int compare(T c1, T c2, boolean nullGreater)
	{
		if (c1 == c2) {
			return 0;
		} else if (c1 == null) {
			return nullGreater ? 1 : -1;
		} else if (c2 == null) {
			return nullGreater ? -1 : 1;
		}
		return c1.compareTo(c2);
	}
}
