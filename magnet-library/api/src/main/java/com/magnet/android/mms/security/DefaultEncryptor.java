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

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Locale;

import android.util.Base64;

import com.magnet.android.mms.exception.MobileRuntimeException;

/**
 * The default encryptor implementation for Android
 */
public class DefaultEncryptor {

  private final SecretKeySpec keySpec;
  // private final MessageDigest digestProvider;

  private final int digestSize; // this depends on the MessageDigest provider

  // Each encrypted data buf is pre-appended with a header
  // DIGEST + IV + CIPHER_LENGTH = header
  // For CipherOutputStream, CIPHER_LENGTH is always -1; also DIGEST is not used
  // (i.e. no data integrity)
  private final int headerSize;

  private static final int LEN_STR_SIZE = 10; // length of cipher in string
                                              // format
  private static final StringBuffer FORMAT_STR = new StringBuffer();
  static {
    // format the length string for header
    FORMAT_STR.append("%0").append(LEN_STR_SIZE).append("d");
  }

  private final StringBuffer transform = new StringBuffer();

  private byte[] doEncrypt(byte[] arg) throws Exception {

    Cipher encoder = Cipher.getInstance(getCipherTranformation());
    initEncryptParams(encoder, buildIvParameter());

    byte[] cipherTxt = encoder.doFinal(arg);
    // prepend the header bytes
    // result = HEADER + CIPHER
    EncHeader header = new EncHeader(encoder.getIV(), cipherTxt);
    ByteArrayOutputStream bos = new ByteArrayOutputStream(headerSize
        + cipherTxt.length);
    try {
      header.write(bos);
      bos.write(cipherTxt);
      return bos.toByteArray();
    } finally {
      bos.close();
    }
  }

  private byte[] doDecrypt(byte[] arg) throws Exception {
    ByteArrayInputStream bis = new ByteArrayInputStream(arg);
    try {
      final byte[] header_buf = new byte[headerSize];
      byte[] cipherText = null;

      // read the header
      if (headerSize != bis.read(header_buf, 0, headerSize)) {
        throw new IOException("data corrupted; invalid header");
      }

      // parse header
      EncHeader header = parseHeader(header_buf);
      // read the cipher text
      cipherText = new byte[header.mCipherLen];
      if (header.mCipherLen != bis.read(cipherText)) {
        throw new IOException("data corrupted; size mismatch");
      }

      // validate data integrity
      MessageDigest digestProvider = MessageDigest.getInstance(encConfig
          .getHashAlgo());
      final byte[] calc_digest = digestProvider.digest(cipherText);
      digestProvider.reset();
      if (!Arrays.equals(calc_digest, header.mDigest)) {
        throw new IOException("data corrupted; invalid data");
      }
      // initialize iv in cipher
      Cipher decoder = Cipher.getInstance(getCipherTranformation());
      initDecryptParams(decoder, header.mIv);
      byte[] cipherTxt = decoder.doFinal(cipherText);
      return cipherTxt;
    } finally {
      bis.close();
    }
  }

  private EncryptorConfig encConfig;


  /**
   * Create an encryptor using specified algorithm and encryption key
   * @param cfg Configuration for the cipher and random number generator, see {@link EncryptorConfig}
   * @param key encryption key
   * @throws InvalidKeyException
   * @throws NoSuchAlgorithmException
   * @throws NoSuchPaddingException
   * @throws InvalidKeySpecException
   * @throws UnsupportedEncodingException
   */
  public DefaultEncryptor(EncryptorConfig cfg, final byte[] key)
      throws NoSuchAlgorithmException, NoSuchPaddingException,
      InvalidKeyException, InvalidKeySpecException,
      UnsupportedEncodingException {

    if (null == key || key.length != EncryptorConfig.DEFAULT_KEY_SIZE) {
      throw new InvalidKeyException();
    }
    encConfig = cfg;
    transform.append(encConfig.getAlgo()).append("/")
        .append(encConfig.getMode()).append("/").append(encConfig.getPadding());

    MessageDigest digestProvider = MessageDigest.getInstance(encConfig
        .getHashAlgo());
    digestSize = digestProvider.getDigestLength();
    headerSize = EncryptorConfig.DEFAULT_IV_LEN + LEN_STR_SIZE + digestSize;
    this.keySpec = new SecretKeySpec(key, encConfig.getAlgo());

    // attempt to get instance to validate impl for specified algorithm exists
    Cipher.getInstance(getCipherTranformation());
  }

  /**
   * Create an encryptor using default algorithms for Android, i.e. "AES/CBC/PKCS5Padding"
   * @param key encryption key
   * @throws InvalidKeyException
   * @throws NoSuchAlgorithmException
   * @throws NoSuchPaddingException
   * @throws InvalidKeySpecException
   * @throws UnsupportedEncodingException
   */
  public DefaultEncryptor(final byte[] key) throws InvalidKeyException,
      NoSuchAlgorithmException, NoSuchPaddingException,
      InvalidKeySpecException, UnsupportedEncodingException {
    this(new EncryptorConfig(), key);
  }

  /**
   * do encrypt and return result as a string
   * @param plain Data to be encrypted. Returns null or empty string if input is null or empty.
   * @return encrypted data in base64 format.
   */
  public String encodeToString(final byte[] plain) {
    if (null == plain) {
      return null;
    }

    if (plain.length == 0) {
      return "";
    }

    try {
      byte[] bResult = doEncrypt(plain);
      String result = Base64.encodeToString(bResult,
          android.util.Base64.DEFAULT);
      return result;
    } catch (Exception e) {
      throw new MobileRuntimeException(e);
    }
  }


  /**
   * do encrypt and return result as encrypted byte array
   * @param plain Plain data to be encrypted. Returns null or empty byte array if input is null or empty.
   * @return encrypted binary data
   */
  public byte[] encode(final byte[] plain) {
    if (null == plain || plain.length == 0) {
      return plain;
    }

    try {
      byte[] bResult = doEncrypt(plain);
      return bResult;

    } catch (Exception e) {
      throw new MobileRuntimeException(e);
    }
  }

  /**
   * Decrypt cipher and return decoded data in byte array
   * @param cipherTxt encrypted text in base64. Returns null or empty string if input is null or empty.
   * @return Decoded byte array
   */
  public byte[] decodeFromString(final CharSequence cipherTxt) {
    if (null == cipherTxt) { 
      return null;
    }
    if (cipherTxt.length() == 0) {
      return cipherTxt.toString().getBytes();
    }
    try {
      final byte[] txt = Base64.decode(cipherTxt.toString(), Base64.DEFAULT);
      byte[] result = doDecrypt(txt);
      return result;
    } catch (Exception e) {
      throw new MobileRuntimeException(e);
    }
  }

  /**
   * Decrypt cipher and return decoded data in byte array
   * @param cipherData encrypted binary byte array. Returns null or empty byte array if input is null or empty.
   * @return Decoded binary byte array
   */
  public byte[] decode(final byte[] cipherData) {
    if (null == cipherData || cipherData.length == 0) {
      return cipherData;
    }
    try {
      byte[] bResult = doDecrypt(cipherData);
      return bResult;
    } catch (Exception e) {
      throw new MobileRuntimeException(e);
    }
  }

  /**
   * Decrypt inputstream.
   * @param is InputStream that feeds encrypted data
   * @return Decoded InputStream that provides decrypted data from is
   */
  public InputStream decodeStream(InputStream is)
      throws IOException {
    if (is == null) {
      return null;
    }
    try {
      final byte[] header_buf = new byte[headerSize];

      // read the header
      if (headerSize != is.read(header_buf, 0, headerSize)) {
        throw new IOException("data corrupted; invalid header");
      }

      // parse header
      EncHeader header = parseHeader(header_buf);

      // can't be pooled
      Cipher decoder = Cipher.getInstance(getCipherTranformation());

      // initialize iv in cipher
      initDecryptParams(decoder, header.mIv);

      return new CipherInputStream(is, decoder);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  /**
   * Encrypt outputstream.
   * @param os OutputStream that writes plain data
   * @return Encrypted OutputStream that output encrypted data when when written to
   */
  public OutputStream encodeStream(OutputStream os)
      throws IOException {
    if (os == null) {
      return null;
    }
    try {
      Cipher encoder = Cipher.getInstance(getCipherTranformation());
      IvParameterSpec iv = buildIvParameter();
      initEncryptParams(encoder, iv);

      // initialize and write header bytes
      EncHeader header = new EncHeader(iv.getIV());
      header.write(os); // write header bytes

      return new CipherOutputStream(os, encoder);
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  private String getCipherTranformation() {
    return transform.toString();
  }

  private EncHeader parseHeader(final byte[] header_buf) {
    if (header_buf.length != headerSize) {
      throw new MobileRuntimeException(new SecurityException(
          "encrypted data header is corrupt"));
    }

    try {
      EncHeader result = new EncHeader();
      ByteArrayInputStream bis = new ByteArrayInputStream(header_buf);
      if (digestSize != bis.read(result.mDigest)) {
        throw new MobileRuntimeException(new SecurityException(
            "encrypted data header is corrupt"));
      }

      if (EncryptorConfig.DEFAULT_IV_LEN != bis.read(result.mIv)) {
        throw new MobileRuntimeException(new SecurityException(
            "encrypted data header is corrupt"));
      }

      // cipher length in string format comes third
      byte[] len_buf = new byte[LEN_STR_SIZE];
      if (LEN_STR_SIZE != bis.read(len_buf)) {
        throw new MobileRuntimeException(new SecurityException(
            "encrypted data header is corrupt"));
      }

      // parse the string containing length
      String len_str = new String(len_buf, "UTF-8");
      result.mCipherLen = Integer.parseInt(len_str);

      return result;
    } catch (Exception e) {
      throw new MobileRuntimeException(e);
    }
  }

  private IvParameterSpec buildIvParameter() {
    // generate a random number for iv
    byte[] iv = new byte[EncryptorConfig.DEFAULT_IV_LEN];
    SecureRandom random;
    try {
      random = SecureRandom.getInstance(EncryptorConfig.DEFAULT_RANDOM_ALGO);
    } catch (NoSuchAlgorithmException e1) {
      // default to whatever is available
      random = new SecureRandom();
    }
    random.nextBytes(iv);
    IvParameterSpec ivParam = new IvParameterSpec(iv);
    return ivParam;
  }

  private void initEncryptParams(Cipher cipher, IvParameterSpec ivParam) {
    try {
      cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParam);
    } catch (Exception e) {
      throw new MobileRuntimeException(e);
    }
  }

  private void initDecryptParams(Cipher cipher, byte[] iv) {
    IvParameterSpec ivParam = new IvParameterSpec(iv);

    try {
      cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParam);
    } catch (Exception e) {
      throw new MobileRuntimeException(e);
    }
  }

  // header to each encrypted block
  private class EncHeader {
    private byte[] mIv;
    private byte[] mDigest;
    private int mCipherLen = 0;

    EncHeader() {
      mDigest = new byte[digestSize];
      mIv = new byte[EncryptorConfig.DEFAULT_IV_LEN];
    }

    EncHeader(byte[] iv, byte[] cipherBuf) {
      mIv = iv;
      mCipherLen = cipherBuf.length;
      // calculate digest
      MessageDigest digestProvider = null;
      try {
        digestProvider = MessageDigest.getInstance(encConfig.getHashAlgo());
      } catch (NoSuchAlgorithmException e) {
        throw new MobileRuntimeException(e);
      }
      mDigest = digestProvider.digest(cipherBuf);
      digestProvider.reset();
    }

    EncHeader(byte[] iv) {
      // this is for stream based ciphers; initialize only IV; other fields are
      // not used
      mIv = iv;
      mCipherLen = 0;
      mDigest = new byte[digestSize];
    }

    private void write(OutputStream os) throws IOException {
      String len_str = String.format(Locale.US, FORMAT_STR.toString(),
          mCipherLen);
      byte[] len_buf = len_str.getBytes("UTF-8");
      os.write(mDigest);
      os.write(mIv);
      os.write(len_buf);
    }
  }

}
