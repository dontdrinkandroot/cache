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
package net.dontdrinkandroot.cache.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;


/**
 * Required Methods from Apache Commons Lang SerializationUtils.
 * 
 * @author Apache Commons Lang
 */
public class Serializer {

	public static byte[] serialize(Serializable obj) {

		ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
		Serializer.serialize(obj, baos);
		return baos.toByteArray();
	}


	public static void serialize(Serializable obj, OutputStream outputStream) {

		if (outputStream == null) {
			throw new IllegalArgumentException("The OutputStream must not be null");
		}
		ObjectOutputStream out = null;
		try {
			/* stream closed in the finally */
			out = new ObjectOutputStream(outputStream);
			out.writeObject(obj);

		} catch (IOException ex) {
			throw new SerializationException(ex);
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException ex) { // NOPMD
				/* ignore close exception */
			}
		}
	}


	public static Object deserialize(byte[] objectData) {

		if (objectData == null) {
			throw new IllegalArgumentException("The byte[] must not be null");
		}
		ByteArrayInputStream bais = new ByteArrayInputStream(objectData);
		return Serializer.deserialize(bais);
	}


	public static Object deserialize(InputStream inputStream) {

		if (inputStream == null) {
			throw new IllegalArgumentException("The InputStream must not be null");
		}
		ObjectInputStream in = null;
		try {
			/* stream closed in the finally */
			in = new ObjectInputStream(inputStream);
			return in.readObject();

		} catch (ClassNotFoundException ex) {
			throw new SerializationException(ex);
		} catch (IOException ex) {
			throw new SerializationException(ex);
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) { // NOPMD
				/* ignore close exception */
			}
		}
	}


	/**
	 * Fast cloning as described <a
	 * href="http://javatechniques.com/blog/faster-deep-copies-of-java-objects/">here</a>.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Serializable> T clone(T object) {

		Object obj = null;
		try {

			/* Write the object out to a byte array */
			FastByteArrayOutputStream fbos = new FastByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(fbos);
			out.writeObject(object);
			out.flush();
			out.close();

			/* Retrieve an input stream from the byte array and read a copy of the object back in. */
			ObjectInputStream in = new ObjectInputStream(fbos.getInputStream());
			obj = in.readObject();

		} catch (IOException e) {
			throw new SerializationException(e);
		} catch (ClassNotFoundException cnfe) {
			throw new SerializationException(cnfe);
		}

		return (T) obj;
	}

}

class FastByteArrayOutputStream extends OutputStream {

	/**
	 * Buffer and size
	 */
	protected byte[] buf = null;

	protected int size = 0;


	/**
	 * Constructs a stream with buffer capacity size 5K
	 */
	public FastByteArrayOutputStream() {

		this(5 * 1024);
	}


	/**
	 * Constructs a stream with the given initial size
	 */
	public FastByteArrayOutputStream(int initSize) {

		this.size = 0;
		this.buf = new byte[initSize];
	}


	/**
	 * Ensures that we have a large enough buffer for the given size.
	 */
	private void verifyBufferSize(int sz) {

		if (sz > this.buf.length) {
			byte[] old = this.buf;
			this.buf = new byte[Math.max(sz, 2 * this.buf.length)];
			System.arraycopy(old, 0, this.buf, 0, old.length);
			old = null;
		}
	}


	public int getSize() {

		return this.size;
	}


	/**
	 * Returns the byte array containing the written data. Note that this array will almost always
	 * be larger than the amount of data actually written.
	 */
	public byte[] getByteArray() {

		return this.buf;
	}


	@Override
	public final void write(byte b[]) {

		this.verifyBufferSize(this.size + b.length);
		System.arraycopy(b, 0, this.buf, this.size, b.length);
		this.size += b.length;
	}


	@Override
	public final void write(byte b[], int off, int len) {

		this.verifyBufferSize(this.size + len);
		System.arraycopy(b, off, this.buf, this.size, len);
		this.size += len;
	}


	@Override
	public final void write(int b) {

		this.verifyBufferSize(this.size + 1);
		this.buf[this.size++] = (byte) b;
	}


	public void reset() {

		this.size = 0;
	}


	/**
	 * Returns a ByteArrayInputStream for reading back the written data
	 */
	public InputStream getInputStream() {

		return new FastByteArrayInputStream(this.buf, this.size);
	}


	public class FastByteArrayInputStream extends InputStream {

		/**
		 * Our byte buffer
		 */
		protected byte[] buf = null;

		/**
		 * Number of bytes that we can read from the buffer
		 */
		protected int count = 0;

		/**
		 * Number of bytes that have been read from the buffer
		 */
		protected int pos = 0;


		public FastByteArrayInputStream(byte[] buf, int count) {

			this.buf = buf;
			this.count = count;
		}


		@Override
		public final int available() {

			return this.count - this.pos;
		}


		@Override
		public final int read() {

			return this.pos < this.count ? this.buf[this.pos++] & 0xff : -1;
		}


		@Override
		public final int read(byte[] b, int off, int len) {

			if (this.pos >= this.count) {
				return -1;
			}

			if (this.pos + len > this.count) {
				len = this.count - this.pos;
			}

			System.arraycopy(this.buf, this.pos, b, off, len);
			this.pos += len;
			return len;
		}


		@Override
		public final long skip(long n) {

			if (this.pos + n > this.count) {
				n = this.count - this.pos;
			}
			if (n < 0) {
				return 0;
			}
			this.pos += n;
			return n;
		}

	}
}