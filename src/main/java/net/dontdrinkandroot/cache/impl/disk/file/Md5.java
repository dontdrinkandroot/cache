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
package net.dontdrinkandroot.cache.impl.disk.file;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;


/**
 * @author Philip W. Sorst <philip@sorst.net>
 */
public class Md5 {

	private static final char[] HEX_DIGITS_LOWER = {
			'0',
			'1',
			'2',
			'3',
			'4',
			'5',
			'6',
			'7',
			'8',
			'9',
			'a',
			'b',
			'c',
			'd',
			'e',
			'f' };

	private final byte[] md5Bytes;


	public Md5(final String s) {

		this.md5Bytes = this.md5(s);
	}


	public Md5(final byte[] md5Bytes) {

		this.md5Bytes = md5Bytes;
	}


	public static Md5 fromMd5Hex(final String md5Hex) throws Md5Exception {

		final Md5 md5 = new Md5(Md5.decodeHex(md5Hex.toCharArray()));
		return md5;
	}


	public byte[] getBytes() {

		return this.md5Bytes;
	}


	public String getHex() {

		return new String(Md5.encodeHex(this.md5Bytes));
	}


	private byte[] md5(String data) {

		try {
			return this.getMd5Digest().digest(data.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			/* This really shouldn't happen */
			throw new RuntimeException(e);
		}
	}


	private MessageDigest getMd5Digest() {

		try {
			return MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			/* Fail hard if MD5 Algo is not available */
			throw new RuntimeException(e);
		}
	}


	public static char[] encodeHex(byte[] data) {

		int l = data.length;
		char[] out = new char[l << 1];
		/* two characters form the hex value. */
		for (int i = 0, j = 0; i < l; i++) {
			out[j++] = Md5.HEX_DIGITS_LOWER[(0xF0 & data[i]) >>> 4];
			out[j++] = Md5.HEX_DIGITS_LOWER[0x0F & data[i]];
		}
		return out;
	}


	public static byte[] decodeHex(char[] data) throws Md5Exception {

		int len = data.length;

		if ((len & 0x01) != 0) {
			throw new Md5Exception("Odd number of characters.");
		}

		byte[] out = new byte[len >> 1];

		/* two characters form the hex value. */
		for (int i = 0, j = 0; j < len; i++) {
			int f = Md5.toDigit(data[j], j) << 4;
			j++;
			f = f | Md5.toDigit(data[j], j);
			j++;
			out[i] = (byte) (f & 0xFF);
		}

		return out;
	}


	private static int toDigit(char ch, int index) {

		int digit = Character.digit(ch, 16);
		if (digit == -1) {
			throw new IllegalArgumentException("Illegal hexadecimal character " + ch + " at index " + index);
		}
		return digit;
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
