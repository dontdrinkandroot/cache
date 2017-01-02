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
package net.dontdrinkandroot.cache.impl.disk.file;

import net.dontdrinkandroot.cache.AbstractCacheTest;
import net.dontdrinkandroot.cache.Cache;
import net.dontdrinkandroot.cache.utils.Duration;
import net.dontdrinkandroot.cache.utils.FileUtils;
import net.dontdrinkandroot.cache.utils.Md5;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class FileCacheTest extends AbstractCacheTest<Md5, File>
{
    private final static File baseDir = new File(FileUtils.getTempDirectory(), "filefilebackedcachetest");

    @AfterClass
    public static void afterClass() throws IOException
    {
        FileUtils.deleteDirectory(FileCacheTest.baseDir);
    }

    @Before
    public void before() throws IOException
    {
        FileUtils.deleteDirectory(FileCacheTest.baseDir);
    }

    @Test
    public void testDefaultGetPutDelete() throws Exception
    {
        FileCache cache =
                new FileCache(
                        "testCache",
                        Duration.minutes(1),
                        Cache.UNLIMITED_IDLE_TIME,
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE,
                        FileCacheTest.baseDir,
                        2
                );

        super.testDefaultPutGetDelete(cache);
    }

    @Test
    public void testReReadIndex() throws Exception
    {
        FileCache cache =
                new FileCache(
                        "testCache",
                        Duration.minutes(1),
                        Cache.UNLIMITED_IDLE_TIME,
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE,
                        FileCacheTest.baseDir,
                        2
                );

        cache.putWithErrors(this.translateKey(0), this.createInputObject(0));
        this.doAssertGet(0, cache);
        cache.putWithErrors(this.translateKey(1), this.createInputObject(1));
        this.doAssertGet(1, cache);
        cache.putWithErrors(this.translateKey(2), this.createInputObject(2));
        this.doAssertGet(2, cache);

        cache =
                new FileCache(
                        "testCache",
                        Duration.minutes(1),
                        Cache.UNLIMITED_IDLE_TIME,
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE,
                        FileCacheTest.baseDir,
                        2
                );

        this.doAssertGet(0, cache);
        this.doAssertGet(1, cache);
        this.doAssertGet(2, cache);
    }

    @Test
    public void testDefaultExpiry() throws Exception
    {
        final FileCache cache =
                new FileCache(
                        "testCache",
                        0L,
                        Cache.UNLIMITED_IDLE_TIME,
                        Integer.MAX_VALUE,
                        Integer.MAX_VALUE,
                        FileCacheTest.baseDir,
                        2
                );

        super.testDefaultExpiry(cache);
    }

    @Override
    protected void doAssertGet(int key, Cache<Md5, File> cache) throws Exception
    {
        File file = cache.getWithErrors(this.translateKey(key));
        Assert.assertNotNull(file);

        final List<String> lines = this.readLines(file);
        Assert.assertEquals(1, lines.size());
        Assert.assertEquals(this.translateKey(key).getHex(), lines.iterator().next());
    }

    private List<String> readLines(File file) throws IOException
    {
        List<String> lines = new ArrayList<String>();
        BufferedReader br = new BufferedReader(new FileReader(file));

        String line;
        do {
            line = br.readLine();
            if (line != null) {
                lines.add(line);
            }
        } while (line != null);

        br.close();

        return lines;
    }

    private void writeLines(File file, Set<String> lines) throws IOException
    {
        BufferedWriter bw;
        bw = new BufferedWriter(new FileWriter(file));
        for (String line : lines) {
            bw.write(line + "\n");
        }

        bw.close();
    }

    @Override
    protected File createInputObject(int key) throws Exception
    {
        File file = File.createTempFile("filecachetest", ".tmp");
        file.deleteOnExit();
        this.writeLines(file, Collections.singleton(this.translateKey(key).getHex()));

        return file;
    }

    @Override
    protected Md5 translateKey(int key)
    {
        final StringBuffer sb = new StringBuffer();
        for (int i = -1; i < key % 100; i++) {
            sb.append(Long.toString(key));
        }

        return new Md5(sb.toString());
    }
}
