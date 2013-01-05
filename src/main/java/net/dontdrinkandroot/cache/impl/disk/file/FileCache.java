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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import net.dontdrinkandroot.cache.CacheException;
import net.dontdrinkandroot.cache.expungestrategy.ExpungeStrategy;
import net.dontdrinkandroot.cache.impl.AbstractMapBackedCache;
import net.dontdrinkandroot.cache.metadata.impl.SimpleMetaData;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.FileUtils;


/**
 * @author Philip W. Sorst <philip@sorst.net>
 */
public class FileCache extends AbstractMapBackedCache<Md5, File, SimpleMetaData> {

	public static Pattern MD5_PATTERN = Pattern.compile("[a-fA-F\\d]{32}");

	private final static int hexLength = 16;

	private final int directoryDepth;

	private final File baseDir;


	public FileCache(
			final String name,
			final long defaultTimeToLive,
			final long defaultMaxIdleTime,
			final ExpungeStrategy expungeStrategy,
			final File baseDir,
			final int directoryDepth) throws IOException, CacheException {

		super(name, defaultTimeToLive, defaultMaxIdleTime, expungeStrategy);

		this.baseDir = baseDir;
		this.directoryDepth = directoryDepth;
		if (directoryDepth < 0) {
			throw new IllegalArgumentException("Directory depth cannot be smaller than 0");
		}
		this.createDirStructure(baseDir, directoryDepth);

		this.setEntriesMetaDataMap(this.readInitialMap());

		this.cleanUp();

		this.getLogger().info("{}: Loaded {} entries", this.getName(), this.getEntriesMetaDataMap().size());
		this.getStatistics().setCurrentSize(this.getEntriesMetaDataMap().size());
	}


	@Override
	protected void doDelete(Md5 key, final SimpleMetaData metaData) throws CacheException {

		final File file = new File(this.getFileName(key));
		if (!file.delete()) {
			throw new CacheException("Couldn't delete file");
		}
	}


	@Override
	protected File doGet(Md5 key, final SimpleMetaData metaData) throws CacheException {

		final File file = new File(this.getFileName(key));
		return file;
	}


	@Override
	protected File doPut(final Md5 md5, final File data) throws CacheException {

		long now = System.currentTimeMillis();
		final SimpleMetaData metaData = new SimpleMetaData(now, now + this.getDefaultTtl());

		final File targetFile = new File(this.getFileName(md5));
		try {
			FileUtils.copyFile(data, targetFile);
		} catch (final IOException e) {
			throw new CacheException("Couldn't copy file", e);
		}

		this.getEntriesMetaDataMap().put(md5, metaData);

		return targetFile;
	}


	public final File getBaseDir() {

		return this.baseDir;
	}


	/**
	 * Recursively creates directories 0x0 - 0xF until the desired depth is reached.
	 * 
	 * @param dir
	 *            The base directory.
	 * @param directoryDepth
	 *            The level of nesting [0,...].
	 */
	protected void createDirStructure(final File dir, final int directoryDepth) throws IOException {

		if (directoryDepth > 0) {
			for (int i = 0; i < FileCache.hexLength; i++) {
				final File newDir = new File(dir.getAbsolutePath() + File.separator + Integer.toHexString(i));
				if (!newDir.exists() && !newDir.mkdirs()) {
					throw new IOException("Creating dir " + newDir.getAbsolutePath() + " failed.");
				}
				this.createDirStructure(newDir, directoryDepth - 1);
			}
		}
	}


	protected String getFileName(final Md5 md5) {

		final String md5Hex = md5.getHex();
		final StringBuffer fileName = new StringBuffer(this.getBaseDir().getAbsolutePath() + File.separator);
		for (int i = 0; i < this.directoryDepth; i++) {
			fileName.append(md5Hex.charAt(i) + File.separator);
		}
		fileName.append(md5Hex);

		return fileName.toString();
	}


	protected Map<Md5, SimpleMetaData> readInitialMap() throws CacheException {

		// TODO Check correct directory structure

		final Map<Md5, SimpleMetaData> itemMap = new HashMap<Md5, SimpleMetaData>();

		final Collection<File> files = FileUtils.listFiles(this.getBaseDir(), null, true);

		for (final File file : files) {
			final String md5Hex = file.getName();
			if (FileCache.MD5_PATTERN.matcher(md5Hex).matches()) {
				Md5 md5;
				try {
					md5 = Md5.fromMd5Hex(md5Hex);
				} catch (final DecoderException e) {
					throw new CacheException("Couldn't add file " + file, e);
				}
				long lastModified = file.lastModified();
				final long expiration = lastModified + this.getDefaultTtl();
				final SimpleMetaData entry = new SimpleMetaData(lastModified, expiration);
				itemMap.put(md5, entry);
			}
		}

		return itemMap;
	}

}
