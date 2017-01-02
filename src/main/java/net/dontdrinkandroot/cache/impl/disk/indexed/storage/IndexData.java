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
package net.dontdrinkandroot.cache.impl.disk.indexed.storage;

import java.io.IOException;
import java.io.RandomAccessFile;

public class IndexData
{
    public static final long LENGTH = 1 + 2 * DataBlock.LENGTH;

    public int blockNum;

    public DataBlock keyMetaBlock;

    public DataBlock valueBlock;

    public IndexData(int blockNum, DataBlock keyMetaBlock, DataBlock valueBlock)
    {
        this.blockNum = blockNum;
        this.keyMetaBlock = keyMetaBlock;
        this.valueBlock = valueBlock;
    }

    public IndexData(DataBlock keyMetaBlock, DataBlock valueBlock)
    {
        this.keyMetaBlock = keyMetaBlock;
        this.valueBlock = valueBlock;
    }

    public int getBlockNum()
    {
        return this.blockNum;
    }

    public void setBlockNum(int blockNum)
    {
        this.blockNum = blockNum;
    }

    public DataBlock getKeyMetaBlock()
    {
        return this.keyMetaBlock;
    }

    public DataBlock getValueBlock()
    {
        return this.valueBlock;
    }

    public static IndexData read(RandomAccessFile randomAccessFile, int blockNum) throws IOException
    {
        final long position = IndexData.LENGTH * blockNum;

        randomAccessFile.seek(position);

        boolean allocated = randomAccessFile.readBoolean();
        if (!allocated) {
            return null;
        }

        long keyMetaStart = randomAccessFile.readLong();
        long keyMetaEnd = randomAccessFile.readLong();
        long valueStart = randomAccessFile.readLong();
        long valueEnd = randomAccessFile.readLong();

        return new IndexData(blockNum, new DataBlock(keyMetaStart, keyMetaEnd), new DataBlock(valueStart, valueEnd));
    }

    public IndexData write(RandomAccessFile randomAccessFile, int blockNum) throws IOException
    {
        this.blockNum = blockNum;

        final long position = IndexData.LENGTH * blockNum;

        randomAccessFile.seek(position);

		/* Allocation marker */
        randomAccessFile.writeBoolean(true);

        randomAccessFile.writeLong(this.keyMetaBlock.getStartPosition());
        randomAccessFile.writeLong(this.keyMetaBlock.getEndPosition());
        randomAccessFile.writeLong(this.valueBlock.getStartPosition());
        randomAccessFile.writeLong(this.valueBlock.getEndPosition());

        return this;
    }

    @Override
    public String toString()
    {
        StringBuffer sb = new StringBuffer("IndexData[");
        sb.append("blockNum=" + this.blockNum);
        sb.append(",");
        sb.append("keyMetaBlock=" + this.keyMetaBlock);
        sb.append(",");
        sb.append("valueBlock=" + this.valueBlock);
        sb.append("]");

        return sb.toString();
    }
}
