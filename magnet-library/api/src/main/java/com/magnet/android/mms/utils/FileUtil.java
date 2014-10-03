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

import android.content.Context;

import com.magnet.android.mms.exception.MobileRuntimeException;
import com.magnet.android.mms.security.DefaultEncryptor;
import com.magnet.android.mms.security.EncryptorConfig;
import com.magnet.android.mms.utils.logger.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;

/**
 * A utility to streamline multiple file operations.  For example, get a MD5/SHA
 * hash and encrypt/decrypt for a file/input stream.
 */
public class FileUtil {
  private final static String TAG = "FileUtil";  
  public final static byte[] EMPTY_BYTES = new byte[0];
  public static final int CONTENT_LENGTH_THRESHOLD = 40960;
  private final static String NO_DIGEST = "";
  private static DefaultEncryptor sEncryptor;
  private static EncryptorConfig sEncConfig;
  
  public enum Mode {
    NONE,
    ENCRYPT,
  }
  
  public static void initCipher(Context context) {
    if (sEncryptor == null) {
      sEncConfig = new EncryptorConfig();
      byte[] key = new byte[sEncConfig.getKeySize()];
      try {
        long installTime = context.getPackageManager().getPackageInfo(
          context.getPackageName(), 0).firstInstallTime;
        byte[] pkgName = context.getPackageName().getBytes();
        System.arraycopy(pkgName, 0, key, 0, 
            Math.min(key.length, pkgName.length));
        for (int i = 8; --i >= 0; ) {
          key[i] ^= (installTime & 0xff);
          installTime >>>= 8;
        }        
        sEncryptor = new DefaultEncryptor(FileUtil.sEncConfig, key);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
  
  public interface FileOp {
    void preProcess() throws Throwable;
    void process(byte[] buffer, int offset, int count) throws Throwable;
    void postProcess() throws Throwable;
    void onError(Throwable cause);
  }
  
  /**
   * Perform a combo MD5 and SHA on file or buffer with addition parameters.  
   * The file content will be computed first and followed by the optional 
   * parameters.  The collision probability is 2^-288.
   */
  public static class DigestFileParamsOp extends DigestFileOp {
    private String[] mParams;
    
    public DigestFileParamsOp(String...params) {
      super();
      mParams = params;
    }
    
    @Override
    public void postProcess() throws Throwable {
      if (mParams != null) {
        for (String param : mParams) {
          if (param != null) {
            byte[] bytes = param.getBytes();
            mMD5.update(bytes);
            mSHA.update(bytes);
          }
        }
      }
      super.postProcess();
    }
  }
  
  /**
   * Perform a combo MD5 and SHA on file or input stream.  The collision
   * probability is 2^-288.
   */
  public static class DigestFileOp implements FileOp {
    protected MessageDigest mMD5;
    protected MessageDigest mSHA;
    protected byte[] mDigest;
    
    @Override
    public void preProcess() throws Throwable {
      mMD5 = MessageDigest.getInstance("MD5");
      mSHA = MessageDigest.getInstance("SHA");
    }

    @Override
    public void process(byte[] buffer, int offset, int count) throws Throwable {
      mMD5.update(buffer, offset, count);
      mSHA.update(buffer, offset, count);
    }

    @Override
    public void postProcess() throws Throwable {
      byte[] md5 = mMD5.digest();
      byte[] sha = mSHA.digest();
      mDigest = new byte[md5.length+sha.length];
      System.arraycopy(md5, 0, mDigest, 0, md5.length);
      System.arraycopy(sha, 0, mDigest, md5.length, sha.length);
    }
    
    @Override
    public void onError(Throwable cause) {
      if (mMD5 != null) {
        mMD5.reset();
      }
      if (mSHA != null) {
        mSHA.reset();
      }
    }
    
    /**
     * A 36-byte hash code.
     * @return
     */
    public byte[] getDigest() {
      return mDigest;
    }
    
    /**
     * A hash code in hex string.
     * @return
     */
    public String getDigestAsString() {
      return Util.toHexStr(mDigest);
    }
  }
  
  public static class InProgressFileOp implements FileOp {
    
    public static interface ProgressListener {
      /**
       * Report an accumulative I/O byte count.
       * @param count An accumulative byte count.
       */
      public void report(int count);
    }
    
    private int mCount;
    private ProgressListener mListener;
    
    public InProgressFileOp(ProgressListener listener) {
      mListener = listener;
    }
    
    @Override
    public void preProcess() throws Throwable {
      // No op.
    }
    
    @Override
    public void process(byte[] buffer, int offset, int count) throws Throwable {
      if (mListener != null) {
        mCount += count;
        mListener.report(mCount);
      }
    }
    
    @Override
    public void postProcess() throws Throwable {
      // No op.
    }
    
    @Override
    public void onError(Throwable cause) {
      // No Op.
    }
  }
  
  public static class OutputFileOp implements FileOp {
    private final static String TAG = "OutputFileOp";
    private OutputStream mOutput;

    public OutputFileOp(OutputStream output) {
      mOutput = output;
    }

    @Override
    public void preProcess() throws Throwable {
      // No op.
    }

    @Override
    public void process(byte[] buffer, int offset, int count) throws Throwable {
      mOutput.write(buffer, offset, count);
    }

    @Override
    public void postProcess() throws Throwable {
      // No op.
    }

    @Override
    public void onError(Throwable cause) {
      Log.e(TAG, "Unable to write to output", cause);
    }
  }
  
  /**
   * Feed an input stream into multiple processing operations to minimize the 
   * I/O. This is mainly used on the HTTP Response.  When the input stream is 
   * exhausted with its data and it stays at the end of the stream.
   * @param input An input stream.
   * @param fileOps Array of CryptoFileOp, DigestFileOp.
   * @return
   */
  public static boolean tee(InputStream input, FileOp... fileOps) {
    int count;
    byte[] buffer = new byte[8192];
    try {
      for (FileOp fileOp: fileOps) {
        if (fileOp != null) {
          fileOp.preProcess();
        }
      }
      
      while ((count = input.read(buffer)) > 0) {
        for (FileOp fileOp : fileOps) {
          if (fileOp != null) {
            fileOp.process(buffer, 0, count);
          }
        }
      }
      
      for (FileOp fileOp : fileOps) {
        if (fileOp != null) {
          fileOp.postProcess();
        }
      }
      return true;
    } catch (Throwable e) {
      for (FileOp fileOp : fileOps) {
        if (fileOp != null) {
          fileOp.onError(e);
        }
      }
      return false;
    }
  }
    
  /**
   * Feed a file into multiple processing operations to minimize the I/O.
   * @param channel A readable file channel.
   * @param fileOps Array of CryptoFileOp, DigestFileOp.
   * @return
   */
  public static boolean tee(FileChannel channel, FileOp... fileOps) {
    int count;
    ByteBuffer buffer = ByteBuffer.wrap(new byte[8192]);
    try {
      for (FileOp fileOp: fileOps) {
        if (fileOp != null) {
          fileOp.preProcess();
        }
      }
      
      while ((count = channel.read(buffer)) > 0) {
        for (FileOp fileOp : fileOps) {
          if (fileOp != null) {
            fileOp.process(buffer.array(), 0, count);
          }
        }
        buffer.rewind();
      }
      // Position to the beginning of the file.
      channel.position(0L);
      
      for (FileOp fileOp : fileOps) {
        if (fileOp != null) {
          fileOp.postProcess();
        }
      }
      return true;
    } catch (Throwable e) {
      for (FileOp fileOp : fileOps) {
        if (fileOp != null) {
          fileOp.onError(e);
        }
      }
      return false;
    }
  }
  
  /**
   * Feed a buffer into multiple processing operations.
   * @param buffer A ByteBuffer with a position.
   * @param fileOps Array of CryptoFileOp, DigestFileOp.
   * @return
   */
  public static boolean tee(ByteBuffer buffer, FileOp... fileOps) {
    try {
      for (FileOp fileOp: fileOps) {
        if (fileOp != null) {
          fileOp.preProcess();
        }
      }
      
      for (FileOp fileOp : fileOps) {
        if (fileOp != null) {
          if (buffer == null) {
            fileOp.process(EMPTY_BYTES, 0, 0);
          } else {
            fileOp.process(buffer.array(), buffer.position(), buffer.remaining());
          }
        }
      }
      buffer.rewind();
      
      for (FileOp fileOp : fileOps) {
        if (fileOp != null) {
          fileOp.postProcess();
        }
      }
      return true;
    } catch (Throwable e) {
      for (FileOp fileOp : fileOps) {
        if (fileOp != null) {
          fileOp.onError(e);
        }
      }
      return false;
    }
  }
  
  /**
   * Copy a file to another file.
   * @param inf
   * @param outf
   * @param hashOp A DigestFileOp instance or null.
   * @param mode {@link Mode#NONE} or {@link Mode#ENCRYPT}.
   * @return null for error, empty string or a hash string if success
   */
  public static String copy(File inf, File outf, DigestFileOp hashOp,
                              Mode mode) {
    FileInputStream ins = null;
    OutputStream ous = null;
    try {
      ins = new FileInputStream(inf);
      ous = new FileOutputStream(outf);
      if (mode == Mode.ENCRYPT) {
        ous = sEncryptor.encodeStream(ous);
      }
      boolean status = tee(ins.getChannel(), new OutputFileOp(ous), hashOp);
      ous.close();
      ous = null;
      if (!status) {
        outf.delete();
        return null;
      } else if (hashOp == null) {
        return NO_DIGEST;
      } else {
        return hashOp.getDigestAsString();
      }
    } catch (IOException e) {
      Log.e(TAG, "Unable to copy "+inf+" to "+outf);
      outf.delete();
      return null;
    } finally {
      if (ins != null) {
        try {
          ins.close();
        } catch (IOException e) {
          // Ignored.
        }
      }
      if (ous != null) {
        try {
          ous.close();
        } catch (IOException e) {
          // Ignored.
        }
      }
    }
  }

  /**
   * Copy from an inputstream (non-seekable) to outputstream.  The output stream
   * will be closed if mode is {@link Mode#ENCRYPT} and the copy is success.
   * @param ins
   * @param ous
   * @param hashOp A DigestFileParamsOp or null.
   * @param mode {@link Mode#NONE} or {@link Mode#ENCRYPT}.
   * @return null for error, empty string or a hash string if success.
   */
  public static String copy(InputStream ins, OutputStream ous, 
                              DigestFileOp hashOp, Mode mode ) {
    try {
      if (mode == Mode.ENCRYPT) {
        ous = FileUtil.encrypt(ous);
      }
      boolean status = tee(ins, new OutputFileOp(ous), hashOp);
      if (mode == Mode.ENCRYPT) {
        ous.close();
        ous = null;
      }
      if (!status) {
        return null;
      } else if (hashOp == null) {
        return NO_DIGEST;
      } else {
        return hashOp.getDigestAsString();
      }
    } catch (Throwable e) {
      Log.e(TAG, "Unable to copy from "+ins+" to "+ous);
      return null;
    }
  }
  
  /**
   * Obtain a file content as byte array.
   * @param file An input file.
   * @return null if error; otherwise, a byte array.
   */
  public static byte[] fileToByteArray(File file) {
    int n, offset = 0;
    int len = (int) file.length();
    FileInputStream fis = null;
    try {
      byte[] data = new byte[len];
      fis = new FileInputStream(file);
      while ((n = fis.read(data, offset, len)) > 0) {
        offset += n;
        len -= n;
      }
      return data;
    } catch (Throwable e) {
      Log.e(TAG, "fileToByteArray() failed with len="+len, e);
      return null;
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e) {
          // Ignored.
        }
      }
    }
  }
  
  /**
   * Obtain a file content as byte array using memory mapped I/O or heap space.
   * If the file size is greater than the specified threshold, it will use
   * memory mapped I/O.  The return buffer will be read-only.
   * @param file An input file.
   * @param threshold Number of bytes as a threshold.
   * @return null if error; otherwise, a read-only ByteBuffer.
   */
  public static ByteBuffer fileToByteBuffer(File file, int threshold) {
    RandomAccessFile ramFile = null;
    try {
      int len = (int) file.length();
      if (len <= threshold) {
        int n;
        ramFile = new RandomAccessFile(file, "r");
        FileChannel channel = ramFile.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(len);
        while ((len > 0) && ((n = channel.read(buffer)) > 0)) {
          len -= n;
        }
        return (len > 0) ? null : buffer.asReadOnlyBuffer();
      } else {
        ramFile = new RandomAccessFile(file, "r");
        MappedByteBuffer buffer = ramFile.getChannel().map(
            FileChannel.MapMode.READ_ONLY, 0, ramFile.length());
        return buffer.asReadOnlyBuffer();
      }
    } catch (Throwable e) {
      Log.e(TAG, "fileToByteArray() failed", e);
      return null;
    } finally {
      if (ramFile != null) {
        try {
          ramFile.close();
        } catch (IOException e) {
          // Ignored.
        }
      }
    }
  }

  /**
   * Decrypt a cipher text in base64 string to plain text.
   * @param cipherText A cipher text in base64 string.
   * @return A plain text, or null.
   */
  public static String decrypt(String cipherText) {
    try {
      byte[] data = sEncryptor.decodeFromString(cipherText);
      return new String(data);
    } catch (Throwable e) {
      Log.e(TAG, "Unable to decrypt data", e);
      return null;
    }
  }

  public static byte[] decrypt(byte[] cipherData) {
    return sEncryptor.decode(cipherData);
  }
  
  public static InputStream decrypt(InputStream ins) {
    try {
      return sEncryptor.decodeStream(ins);
    } catch (Throwable e) {
      Log.e(TAG, "decrypt InputStream failed", e);
      return null;
    }
  }
  
  /**
   * Encrypt plain text to a cipher text in base64 string.
   * @param plainText
   * @return A cipher text in base64 string, or null.
   */
  public static String encrypt(String plainText) {
    try {
      return sEncryptor.encodeToString(plainText.getBytes());
    } catch (Throwable e) {
      Log.e(TAG, "Unable to encrypt data", e);
      return null;
    }
  }
  
  public static byte[] encrypt(byte[] plain) {
    return sEncryptor.encode(plain);
  }
  
  public static OutputStream encrypt(OutputStream ous) {
    try {
      return sEncryptor.encodeStream(ous);
    } catch (Throwable e) {
      Log.e(TAG, "encrypt OutputStream failed", e);
      return null;
    }
  }
  
  /**
   * Get a MD5/SHA for a byte array with optional parameters.
   * @param bytes A byte array of data.
   * @param params An optional array of parameters.
   * @return null if failed, or an one-way hash value in hex string.
   */
  public static String digest(byte[] bytes, String...params) {
    DigestFileParamsOp digest = new DigestFileParamsOp(params);
    if (!tee(ByteBuffer.wrap(bytes), digest)) 
      return null;
    else
      return digest.getDigestAsString();
  }
  
  /**
   * Get a MD5/SHA for a file with optional parameters.
   * @param file A readable file.
   * @param params An optional array of parameters.
   * @return null if failed, or an one-way hash value in hex string.
   */
  public static String digest(File file, String...params) {
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(file);
      DigestFileParamsOp digest = new DigestFileParamsOp(params); 
      if (!tee(fis.getChannel(), digest)) {
        return null;
      } else {
        return digest.getDigestAsString();
      }
    } catch (IOException e) {
      Log.e(TAG, "MessageDigest failed on file", e);
      return null;      
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e) {
          // Ignored.
        }
      }
    }
  }
  
  /**
   * Serialize an object.
   * @param object
   * @return null if object is null; otherwise, serialized byte array.
   * @exception MobileRuntimeException
   */
  public static byte[] serialize(Serializable object) {
    if (object == null)
      return null;
    
    ObjectOutputStream oos = null;
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(512);
      oos = new ObjectOutputStream(baos);
      oos.writeObject(object);
      return baos.toByteArray();
    } catch (IOException e) {
      Log.e(TAG, "Unable to serialize object: "+object, e);
      throw new MobileRuntimeException("Unable to serialize object: "+object, e);
    } finally {
      if (oos != null) {
        try {
          oos.close();
        } catch (IOException e) {
          // Ignored.
        }
      }
    }
  }
  
  /**
   * Deserialize to an object.
   * @param data
   * @return
   */
  public static Object deserialize(byte[] data) {
    if (data == null)
      return null;
    
    ObjectInputStream ois = null;
    try {
      ByteArrayInputStream bais = new ByteArrayInputStream(data);
      ois = new ObjectInputStream(bais);
      return ois.readObject();
    } catch (Throwable e) {
      Log.e(TAG, "Unable to deserialize to an object", e);
      return null;
    } finally {
      if (ois != null) {
        try {
          ois.close();
        } catch (IOException e) {
          // Ignored.
        }
      }
    }
  }
}
