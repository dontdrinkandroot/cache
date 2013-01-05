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

import net.dontdrinkandroot.cache.impl.disk.file.Md5;

import org.apache.commons.codec.DecoderException;
import org.junit.Assert;
import org.junit.Test;


public class Md5Test {

	@Test
	public void testConstruction() throws DecoderException {

		String testString = " asdfjkh akasdkfa sdf a";
		Md5 md5_1 = new Md5(testString);
		Md5 md5_2 = Md5.fromMd5Hex(md5_1.getHex());
		Assert.assertEquals(md5_1, md5_2);
	}
}
