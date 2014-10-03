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

import android.text.TextUtils;

import com.magnet.android.core.GenericRestConstants;
import com.magnet.android.mms.exception.MarshallingException;
import com.magnet.android.mms.request.marshall.SimpleParamHelper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

public abstract class AbstractResponseParser<T> implements ResponseParser<T> {

  private static final String TAG = AbstractResponseParser.class.getSimpleName();
  protected final Class<?> actualResponseType;
  protected final Class<?> responseType;
  protected final String encodingType;
  protected final String contentType;
  protected final Type[] actualTypes;

  /**
   * Constructs parser using default content type (JSON) and transfer encoding (NONE)
   * @param objectType
   */
  protected AbstractResponseParser(Class<?> objectType) {
    this(objectType, null, GenericRestConstants.CONTENT_TYPE_JSON, GenericRestConstants.MIME_ENCODING_NONE );
  }

  protected AbstractResponseParser(Class<?> objectType, String contentType, String encodingType) {
    this(objectType, null, contentType, encodingType);
  }
  /**
   * Constructs parser using additional parameters for generic types
   * @param objectType The object type the parser should parse to. If generic type, actual parameterized types should be in objectComponentTypes
   * @param objectComponentTypes List of parameterized types for generic objectType; null otherwise
   */
  protected AbstractResponseParser(Class<?> objectType, Type[] objectComponentTypes, String contentType, String encodingType) {
    this.responseType = objectType;
    this.actualTypes = objectComponentTypes;
    this.contentType = TextUtils.isEmpty(contentType) ? GenericRestConstants.CONTENT_TYPE_JSON : contentType;
    this.encodingType = TextUtils.isEmpty(encodingType) ? GenericRestConstants.MIME_ENCODING_NONE : encodingType;

    if (SimpleParamHelper.isCollectionClass(responseType)) {
      if (actualTypes != null && actualTypes.length > 0) {
        // TODO: handle multiple parameterized types; assume only one for now
        actualResponseType = Class.class.cast(actualTypes[0]);
      } else {
        throw new IllegalArgumentException("expected component types for generic result type");
      }
    } else {
      actualResponseType = responseType;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public T parseResponse(final byte[] responseArray) throws MarshallingException {
    if (void.class.equals(responseType) || Void.class.equals(responseType)) {
      return (T) null;
    }
    if (responseType == null || responseArray == null) {
      return (T) null;
    }
    if (responseArray.length == 0) {
      return (T) SimpleParamHelper.formatEmptySimpleObject(responseType);
    }
    try {
      return parseResponse(new ByteArrayInputStream(responseArray));
    } catch (Exception e) {
      MarshallingException error = new MarshallingException(e);
      error.setErrorContent(new String(responseArray));
      error.fillInStackTrace();
      throw error;
    }
  }

  /**
   * Parses input file to a specific object;  will decode content from base64 if transfer encoding is set to BASE64.
   * @param  responseFile Input file containing the data to be parsed
   * @return Parsed type-safe object
   * @throws com.magnet.android.mms.exception.MobileException, IOException
   */
  @SuppressWarnings("unchecked")
  public T parseResponse(final File responseFile) throws MarshallingException, IOException {

    if (void.class.equals(responseType) || Void.class.equals(responseType)) {
      return (T) null;
    }
    if (responseFile == null || responseType == null) {
      return (T) null;
    }
    if (responseFile.length() == 0) {
      return (T) SimpleParamHelper.formatEmptySimpleObject(responseType);
    }
    return parseResponse(new FileInputStream(responseFile));
  }

  @Override
  @SuppressWarnings("unchecked")
  public T parseResponse(final InputStream responseIs) throws MarshallingException {
    if (void.class.equals(responseType) || Void.class.equals(responseType)) {
      return (T) null;
    }
    if (responseIs == null || responseType == null) {
      return (T) null;
    }
    InputStream decodedIs;
    if (doBase64Decode()) {
      decodedIs = ByteArrayHelper.getBase64InputStream(responseIs);
    } else {
      decodedIs = responseIs;
    }

    return parseDecodedResponse(decodedIs);
  }
  @SuppressWarnings("unchecked")
  protected abstract T parseDecodedResponse(final InputStream responseIs) throws MarshallingException;

  private boolean doBase64Decode() {
    boolean result = (GenericRestConstants.MIME_ENCODING_BASE64.equals(encodingType) ||
        ((byte[].class.equals(responseType) || Byte[].class.equals(responseType)) && 
        (TextUtils.isEmpty(contentType) || contentType.startsWith(GenericRestConstants.CONTENT_TYPE_TEXT_PLAIN))));
    return result;
  }
}
