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

package net.dontdrinkandroot.cache.impl.disk.file;

import java.util.Arrays;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;


/**
 * @author Philip W. Sorst <philip@sorst.net>
 */
public class Md5 {

	private final byte[] md5Bytes;


	public Md5(final String s) {

		this.md5Bytes = DigestUtils.md5(s);
	}


	public Md5(final byte[] md5Bytes) {

		this.md5Bytes = md5Bytes;
	}


	public static Md5 fromMd5Hex(final String md5Hex) throws DecoderException {

		final Md5 md5 = new Md5(Hex.decodeHex(md5Hex.toCharArray()));
		return md5;
	}


	public byte[] getBytes() {

		return this.md5Bytes;
	}


	public String getHex() {

		return new String(Hex.encodeHex(this.md5Bytes));
	}


	@Override
	public int hashCode() {

		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(this.md5Bytes);

		return result;
	}


	@Override
	public boolean equals(final Object obj) {

		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (this.getClass() != obj.getClass()) {
			return false;
		}

		final Md5 other = (Md5) obj;
		if (!Arrays.equals(this.md5Bytes, other.md5Bytes)) {
			return false;
		}

		return true;
	}

}
