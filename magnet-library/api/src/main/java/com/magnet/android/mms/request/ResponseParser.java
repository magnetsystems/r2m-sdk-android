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

import com.magnet.android.mms.exception.MobileException;
import com.magnet.android.mms.exception.MarshallingException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public interface ResponseParser<T> {

  /**
   * Parses input array to a specific object;  will decode content from base64 if transfer encoding is set to BASE64.
   * @param responseArray Byte array containing data to be parsed
   * @return Parsed type-safe object
   * @throws MarshallingException
   */
  @SuppressWarnings("unchecked")
  T parseResponse(byte[] responseArray) throws MarshallingException;

  /**
   *  Parses input file to a specific object; will decode content from base64 if transfer encoding is set to BASE64
   * @param responseFile File of the data to be parsed
   * @return Parsed type-safe object
   * @throws MobileException
   * @throws IOException Fail to access the file
   */
  public T parseResponse(final File responseFile) throws MarshallingException, IOException;

  /**
   * Parses input stream to a specific object; will decode content from base64 if transfer encoding is set to BASE64
   * @param responseIs InputStream of the data to be parsed
   * @return Parsed type-safe object
   * @throws MarshallingException
   */
  @SuppressWarnings("unchecked")
  T parseResponse(InputStream responseIs) throws MarshallingException;
}
