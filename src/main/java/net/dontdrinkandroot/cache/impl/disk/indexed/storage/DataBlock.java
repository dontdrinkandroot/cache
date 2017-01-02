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

/**
 * @author Philip Washington Sorst <philip@sorst.net>
 */
public class DataBlock implements Comparable<DataBlock>
{
    public static final long LENGTH = 8 + 8;

    private final long startPosition;

    private final long endPosition;

    public DataBlock(final long startPosition, final long endPosition)
    {
        if (endPosition < startPosition) {
            throw new IllegalArgumentException("endPosition < startPosition: " + endPosition + "," + startPosition);
        }

        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }

    @Override
    public int compareTo(final DataBlock other)
    {
        final long startResult = this.startPosition - other.startPosition;

        if (startResult < 0) {
            return -1;
        } else if (startResult > 0) {
            return 1;
        }

        final long endResult = this.endPosition - other.endPosition;

        if (endResult < 0) {
            return -1;
        } else if (endResult > 0) {
            return 1;
        }

        return 0;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (this.endPosition ^ this.endPosition >>> 32);
        result = prime * result + (int) (this.startPosition ^ this.startPosition >>> 32);

        return result;
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (this.getClass() != obj.getClass()) {
            return false;
        }

        final DataBlock other = (DataBlock) obj;
        if (this.endPosition != other.endPosition) {
            return false;
        }

        return this.startPosition == other.startPosition;
    }

    @Override
    public String toString()
    {
        return this.startPosition + ":" + this.endPosition + " (" + this.getLength() + ")";
    }

    public boolean overlaps(final DataBlock other)
    {
        final boolean before = this.startPosition <= other.startPosition && other.startPosition <= this.endPosition;
        final boolean after = other.endPosition <= this.startPosition && this.startPosition <= other.endPosition;

        return before || after;
    }

    public long getLength()
    {
        return this.endPosition - this.startPosition + 1;
    }

    public long getStartPosition()
    {
        return this.startPosition;
    }

    public long getEndPosition()
    {
        return this.endPosition;
    }
}
