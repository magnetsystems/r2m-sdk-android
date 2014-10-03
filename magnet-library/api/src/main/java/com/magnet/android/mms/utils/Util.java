/*
 * Copyright (c) 2014 Magnet Systems, Inc.
 * All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you
 *  may not use this file except in compliance with the License. You
 *  may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package com.magnet.android.mms.utils;

import android.os.Bundle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Util {
  private final static char[] HEX_DIGITS = {
    '0', '1', '2', '3', '4', '5', '6', '7',
    '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
  
  /**
   * Covert a byte array to hex string.
   * @param bytes
   * @return null if bytes is null; otherwise, a hex string.
   */
  public static String toHexStr(byte[] bytes) {
    return toHexStr(bytes, 0, bytes.length);
  }
  
  /**
   * Convert a byte array starting at an offset to a hex string.
   * @param bytes
   * @param offset
   * @param count
   * @return
   */
  public static String toHexStr(byte[] bytes, int offset, int count) {
    if (bytes == null) {
      return null;
    }
    StringBuilder result = new StringBuilder(count*2);
    for (int i = offset; --count >= 0; i++) {
      result.append(HEX_DIGITS[(bytes[i] >> 4) & 0xf])
            .append(HEX_DIGITS[bytes[i] & 0xf]);
    }
    return result.toString();
  }
  
  /**
   * Convert a hex string into byte array.
   * @param hexString
   * @return null if hexString is null; otherwise, a byte array.
   */
  public static byte[] toByteArray(String hexString) {
    if (hexString == null) {
      return null;
    }
    int len = hexString.length();
    byte[] bytes = new byte[len/2];
    for (int i = 0; i < len; i += 2) {
      bytes[i/2] = Byte.parseByte(hexString.substring(i, i+2), 16);
    }
    return bytes;
  }
  
  /**
   * Read from an input stream and convert the content into String.  The
   * input stream will be closed.
   * @param input Input stream with UTF-8 bytes.
   * @return A String.
   * @throws IOException
   */
  public static String inputStreamToString(InputStream input) 
                                              throws IOException {
    try {
      int n;
      byte[] buffer = new byte[8192];
      StringBuilder sb = new StringBuilder();
      while ((n = input.read(buffer)) > 0) {
        sb.append(new String(buffer, 0, n));
      }
      return sb.toString();
    } finally {
      input.close();
    }
  }
  
  /**
   * Read from an input stream and return the context as byte array.  The
   * input stream will be closed.
   * @param input Input stream with UTF-8 bytes.
   * @return A byte array.
   * @throws IOException
   */
  public static byte[] inputStreamToByteArray(InputStream input) 
                                                throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      int n;
      byte[] buffer = new byte[8192];
      while ((n = input.read(buffer)) > 0) {
        baos.write(buffer, 0, n);
      }
      return baos.toByteArray();
    } finally {
      input.close();
    }
  }
  
  /**
   * Dump the extras in the intent.
   * @param bundle
   * @return
   */
  public static String dumpExtras(Bundle bundle) {
    StringBuilder sb = new StringBuilder();
    for (String key : bundle.keySet()) {
      Object val = bundle.get(key);
      sb.append('\n').append(key).append('=').append(val.toString());
    }
    return sb.toString();
  }
}
