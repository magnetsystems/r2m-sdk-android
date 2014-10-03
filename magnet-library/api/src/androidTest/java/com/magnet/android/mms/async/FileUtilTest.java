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

package com.magnet.android.mms.async;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.magnet.android.mms.utils.FileUtil;

public class FileUtilTest extends InstrumentationTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    FileUtil.initCipher(this.getInstrumentation().getContext());
  }

  @SmallTest
  public void testFileUtil() throws Exception {
    String plainText = "Hello What is up?";

    File plainFile = File.createTempFile("plain", "dat");
    System.out.println("plain file="+plainFile.getAbsolutePath());

    OutputStream ous = new FileOutputStream(plainFile);
    ous.write(plainText.getBytes());
    ous.close();

    File cipherFile = File.createTempFile("cipher", "dat");
    System.out.println("cipher file="+cipherFile.getAbsolutePath());

    ous = new FileOutputStream(cipherFile);

    InputStream ins = new FileInputStream(plainFile);
    FileUtil.copy(ins, ous, null, FileUtil.Mode.ENCRYPT);
    ous.close();
    ins.close();

    ins = FileUtil.decrypt(new FileInputStream(cipherFile));
    int n;
    int offset = 0;
    byte[] buffer = new byte[8192];
    int len = buffer.length;
    while ((n = ins.read(buffer, offset, len)) >= 0) {
      len -= n;
      offset += n;
    }
    ins.close();
    String decryptText = new String(buffer, 0, offset);
    System.out.println("decrypt text="+decryptText);
    assertEquals(plainText, decryptText);
  }
}
