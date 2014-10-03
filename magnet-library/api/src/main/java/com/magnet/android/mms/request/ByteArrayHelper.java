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
package com.magnet.android.mms.request;

import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Base64OutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ByteArrayHelper {

  //use NO_WRAP only in POST of byte[]
  public static final int MAGNET_BASE64_FLAGS = android.util.Base64.NO_WRAP;

  public static String toBase64(byte[] barray) {
    if (barray == null) {
      return "";
    }
    String result;
    result = Base64.encodeToString(barray,MAGNET_BASE64_FLAGS);
    return result;
  }
  public static byte[] fromBase64(String encoded) {
    if (encoded == null) {
      return null;
    }
    byte[] result;
    result = Base64.decode(encoded,MAGNET_BASE64_FLAGS);
    return result;
    
  }

  public static OutputStream getBase64OutputStream(OutputStream oss) {
    return new Base64OutputStream(oss, MAGNET_BASE64_FLAGS);
  }

  public static InputStream getBase64InputStream(InputStream iss) {
    return new Base64InputStream(iss, MAGNET_BASE64_FLAGS);
  }

  public static byte[] toByteArray(InputStream is) throws IOException {
    if (is == null) {
      return null;
    }
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      byte[] buf = new byte[1024];
      int count = 0;
      while ((count = is.read(buf)) != -1) {
        bos.write(buf, 0, count);
      }
    } finally {
      bos.close();
    }
    return bos.toByteArray();
  }

  /**
   * Convenience function to copy from inputstream to outputstream
   * @param is
   */
  public static void toOutputStream(InputStream is, OutputStream os) throws IOException {
    if (is == null) {
      return;
    }
    try {
      byte[] buf = new byte[1024];
      int count = 0;
      while ((count = is.read(buf)) != -1) {
        os.write(buf, 0, count);
      }
    } finally {
      os.close();
    }
  }
  /**
   * Convenience function to copy from inputstream to outputstream
   * @param is
   */
  public static void toBase64OutputStream(InputStream is, OutputStream os) throws IOException {
    if (is == null) {
      return;
    }
    OutputStream osb64 = null;
    try {
      osb64 = getBase64OutputStream(os);
      byte[] buf = new byte[1024];
      int count = 0;
      while ((count = is.read(buf)) != -1) {
        osb64.write(buf, 0, count);
      }
      osb64.flush();
    } finally {
      if (osb64 != null) {
        osb64.close();
      }
      is.close();
    }
  }
  public static byte[] toByteArrayFromByteWrapperArray(Byte[] B) {
    if (B == null) {
      return null;
    }
    byte[] b = new byte[B.length];
    for (int i = 0; i < B.length; i++)
    {
        b[i] = B[i].byteValue();
    }
    return b;
  }
}
