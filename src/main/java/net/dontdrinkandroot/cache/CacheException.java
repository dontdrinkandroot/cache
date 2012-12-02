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

/**
 * Exception that denotes that something went wrong while accessing the cache (while storing, retrieving or
 * constructing). Although this exception must be caught most applications can simply ignore this exception as caching
 * is not meant to be a reliable storage, still this might lead to significant and undetected performance issues, so at
 * least some kind of logging should be implemented.
 * 
 * @author Philip W. Sorst <philip@sorst.net>
 */
public class CacheException extends Exception {

	private static final long serialVersionUID = -7471725780904149067L;


	public CacheException() {

		super();
	}


	public CacheException(final String msg) {

		super(msg);
	}


	public CacheException(final Throwable t) {

		super(t);
	}


	public CacheException(final String msg, final Throwable t) {

		super(msg, t);
	}

}
