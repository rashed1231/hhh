
package org.unichain.core.capsule.utils;

import static org.unichain.common.utils.ByteUtil.toHexString;

import java.io.Serializable;
import java.util.Arrays;

/**
 */
public class ByteArrayWrapper implements Comparable<ByteArrayWrapper>, Serializable {

  private final byte[] data;
  private int hashCode = 0;

  public ByteArrayWrapper(byte[] data) {
    if (data == null) {
      throw new NullPointerException("Data must not be null");
    }
    this.data = data;
    this.hashCode = Arrays.hashCode(data);
  }

  public boolean equals(Object other) {
    if (!(other instanceof ByteArrayWrapper)) {
      return false;
    }
    byte[] otherData = ((ByteArrayWrapper) other).getData();
    return FastByteComparisons.compareTo(
        data, 0, data.length,
        otherData, 0, otherData.length) == 0;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public int compareTo(ByteArrayWrapper o) {
    return FastByteComparisons.compareTo(
        data, 0, data.length,
        o.getData(), 0, o.getData().length);
  }

  public byte[] getData() {
    return data;
  }

  @Override
  public String toString() {
    return toHexString(data);
  }
}
