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

/**
 * Configuration for the {@link DefaultEncryptor}.
 */
public class EncryptorConfig {

  // default values for Android ciphers
  public static final String DEFAULT_ALGO = "AES";
  public static final String DEFAULT_MODE = "CBC";
  public static final String DEFAULT_PADDING = "PKCS5Padding";
  public static final String DEFAULT_HASH_ALGO = "MD5";
  public static final String DEFAULT_RANDOM_ALGO = "SH1PRNG";

  // 256 bit key size /8 = 32 bytes
  public static final int DEFAULT_KEY_SIZE = 32;

  //for AES; always 16; should not be configurable
  public static final int DEFAULT_IV_LEN = 16; 

  public static final String CIPHER_KEY = "cipherKey";
  public static final String NONCE = "nonce";
  public static final String ALGO = "algo";
  public static final String HASH_ALGO = "hashAlgo";

  public static final String HASH = "hash";

  private int keySize;
  private String mode;
  private String padding;
  private String algo;
  private String randomAlgo;
  private String hashAlgo;

  public EncryptorConfig() {
    // initialize with default values
    keySize = DEFAULT_KEY_SIZE;
    mode = DEFAULT_MODE;
    padding = DEFAULT_PADDING;
    algo = DEFAULT_ALGO;
    randomAlgo = DEFAULT_RANDOM_ALGO;
    hashAlgo = DEFAULT_HASH_ALGO;
  }

  public int getKeySize() {
    return keySize;
  }

  public String getMode() {
    return mode;
  }

  public String getPadding() {
    return padding;
  }

  public String getAlgo() {
    return algo;
  }

  public String getRandomAlgo() {
    return randomAlgo;
  }

  public String getHashAlgo() {
    return hashAlgo;
  }

  public void setKeySize(int keySize) {
    this.keySize = keySize;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public void setPadding(String padding) {
    this.padding = padding;
  }

  public void setAlgo(String algo) {
    this.algo = algo;
  }

  public void setRandomAlgo(String randomAlgo) {
    this.randomAlgo = randomAlgo;
  }

  public void setHashAlgo(String hashAlgo) {
    this.hashAlgo = hashAlgo;
  }
}
