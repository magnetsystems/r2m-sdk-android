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
package com.magnet.android.mms.security;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.NoSuchPaddingException;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.SmallTest;

public class DefaultEncryptorTest extends InstrumentationTestCase {

  String whatever23Key = "d2hhdGV2ZXIyMw=="; // "whatever23"

  byte[] key256bit = new byte[256/8];
  String testKey = "dGVzdA!@A*q" + "aVzdAxbA80" + "yunzgRlosxq";
  byte[] shortkey = new byte[7];

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    System.arraycopy(testKey.getBytes(), 0, key256bit, 0, key256bit.length);
    System.arraycopy("f78s".getBytes(), 0, shortkey, 0, "f78s".getBytes().length);
  }

  @SmallTest
  public void testCreate() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, UnsupportedEncodingException {

    // happy path scenario, using proper key length
    DefaultEncryptor enc = new DefaultEncryptor(key256bit);

    // short key - expect exception
    try {
      DefaultEncryptor bad = new DefaultEncryptor(shortkey);
      fail("expected exception with short key");
    } catch (Exception e) {
      assertTrue(true);
    }
    
    // bad algorithm
    EncryptorConfig cfg = new EncryptorConfig();
    cfg.setAlgo("foobar");
    try {
      DefaultEncryptor foo = new DefaultEncryptor(cfg, key256bit);
      fail("expected exception with foobar algorithm");
    } catch (Exception e) {
      assertTrue(true);
    }
  }

  @SmallTest
  public void testBasicencodeDecode() throws Exception {
    
    DefaultEncryptor enc = new DefaultEncryptor(key256bit);

    String expected = "this is an example that contains some text string.";

    String cipher = enc.encodeToString(expected.getBytes());
    assertFalse(expected.equals(cipher));
    byte[] text = enc.decodeFromString(cipher);
    assertEquals(expected, new String(text));

    byte[] eb = new byte[] {0x03, 0x56, 0x39, 0x12, (byte) 0xa0, 0x20, 0x76, 0x34, 0x00, 0x4f, 0x4b,  0x00, 0x00 };

    byte[] cipher2 = enc.encode(eb);
    assertFalse(Arrays.equals(eb,  cipher2));
    byte[] deb = enc.decode(cipher2);
    assertTrue(Arrays.equals(eb,  deb));
  }

  @SmallTest
  public void testEncodeDecodeCorrupt() throws Exception {

    String expected = "this is a test for data integrity";
    DefaultEncryptor enc = new DefaultEncryptor(key256bit);
    byte[] bCipher = enc.encode( expected.getBytes() );
    byte[] original = new byte[bCipher.length];
    System.arraycopy(bCipher,  0,  original, 0,  original.length);

    bCipher[10] = 23;  // set to some random value
    bCipher[11] = 11;  // set to some random value
    bCipher[0] = 9;  // set to some random value
    try {
      byte[] badplain = enc.decode( bCipher);
      fail("expected decode exception with corrupted header");
    } catch (Exception e) {
      assertTrue(true);
    }
  }

  @SmallTest
  public void testMismatchKey() throws Exception {

    String expected = "this is a test for bad keys";
    DefaultEncryptor enc = new DefaultEncryptor(key256bit);
    byte[] bCipher = enc.encode( expected.getBytes() );

    byte[] wrongKey = new byte[key256bit.length];

    // purposely leave out last 3 bytes so keys don't match
    System.arraycopy(key256bit, 0, wrongKey, 0, key256bit.length-3);
    DefaultEncryptor enc2 = new DefaultEncryptor(wrongKey);
    
    try {
      byte[] badplain = enc2.decode( bCipher);
      fail("expected decode exception with incorrect key?");
    } catch (Exception e) {
      assertTrue(true);
    }
  }
  @SmallTest
  public void testManyTimesEncodeDecodeCorrupt() throws Exception {
    for( int i = 0; i < 1000; i++ ) {
      testEncodeDecodeCorrupt();
    }
  }
  @SmallTest
  public void testEncodeDecodeABunchOfTimes() throws Exception {
    for (int i = 0; i < 1000; i++) {
      try {
        testBasicencodeDecode();
      } catch (AssertionError ae) {
        throw ae;
      }
    }
  }
  @SmallTest
  public void testEncodeDecodeUsingDifferentEncryptorsButSameSymmetricKey() throws Exception {
    int iterations = 50;
    
    for (int i = 0; i < iterations; i++) {
      DefaultEncryptor enc1 = new DefaultEncryptor(key256bit);
      DefaultEncryptor enc2 = new DefaultEncryptor(key256bit);
      String expected = "\"this is a test\"" + "iteration #" + i;
      try {

        byte[] cipher = enc1.encode(expected.getBytes());
        assertFalse(Arrays.equals(expected.getBytes(), cipher));
        byte[] plain = enc2.decode(cipher);
        assertTrue(Arrays.equals(expected.getBytes(), plain));
      } catch (RuntimeException e) {
        throw new RuntimeException("failed on iteration " + i + "; with text=" + expected);
      }
    }

  }

  @SmallTest
  public void testStreams() throws Exception {
    DefaultEncryptor enc = new DefaultEncryptor(key256bit);

    File file = File.createTempFile("testStreams", ".tst");
    file.deleteOnExit();

    InputStream is = null;
    OutputStream os = null;

    String expLine = "Featured content represents the best that Wikipedia has to offer.These are the articles, pictures, and other contributions that showcase the polished result of the collaborative efforts that drive Wikipedia. All featured content undergoes a thorough review process to ensure that it meets the highest standards, and can serve as the best example of our end goals. A small bronze star (The featured content star) in the top right corner of a page indicates that the content is featured. This page gives links to all of Wikipedia's featured content.";
    int linelength = expLine.length();
    try {
      os = enc.encodeStream(new FileOutputStream(file));
      for (int i=0; i<1000; i++) {
        os.write(expLine.getBytes());
      }
      os.close();
      os = null;

      is = enc.decodeStream(new FileInputStream(file));
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      String content = reader.readLine();
      int maxcount = content.length();
      int readcount = 0;
      while (readcount < maxcount) {
        assertTrue(content.regionMatches(readcount, expLine,  0, linelength));
        readcount = readcount + expLine.length();
      }
    } catch (IOException e) {
      if (null != is) {
        is.close();
      }

      if (null != os) {
        os.close();
      }
    }
  }

  @SmallTest
  public void testStreamEncodeDecode() throws Exception {
    String plainText = "Hello What is up?";

    File plainFile = File.createTempFile("plain", "dat");
    plainFile.deleteOnExit();
    FileOutputStream fos = new FileOutputStream(plainFile);
    fos.write(plainText.getBytes());
    fos.close();

    File cipherFile = File.createTempFile("cipher", "dat");
    cipherFile.deleteOnExit();
    OutputStream cfos = new FileOutputStream(cipherFile);

    DefaultEncryptor enc = new DefaultEncryptor(key256bit);
    cfos = enc.encodeStream(cfos);

    int n;
    byte[] buffer = new byte[8192];
    InputStream ins = new FileInputStream(plainFile);
    while ((n = ins.read(buffer)) >= 0) {
      cfos.write(buffer, 0, n);
    }
    cfos.close();
    ins.close();

    InputStream cfis = new FileInputStream(cipherFile);
    cfis = enc.decodeStream(cfis);
    int offset = 0;
    int len = buffer.length;
    while ((n = cfis.read(buffer, offset, len)) >= 0) {
      len -= n;
      offset += n;
    }
    cfis.close();
    assertEquals(plainText, new String(buffer, 0, offset));
  }
}
