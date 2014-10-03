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

import android.content.Context;

import com.magnet.android.mms.exception.MobileException;
import com.magnet.android.mms.request.GenericResponseParser;

import java.lang.reflect.Type;
import java.util.concurrent.ExecutionException;


/**
 * A mock CallImpl class for synchronous calls
 */
public class SyncCall<T> extends CallImpl<T> {

  private byte[] response;
  private Class<?> responseType;
  private Type[] actualTypes;
  private Exception responseException;
  private String contentType;
  private String encoding;

  /**
   * 
   * @param context Android app context
   */
  public SyncCall(Context context) {
    super(context, null);
  }

  public void setResponse(byte[] response, Class<?> returnType, Type[] actualTypes, String contentType, String encoding) {
    this.response = response;
    this.responseType = returnType;
    this.actualTypes = actualTypes;
    this.contentType = contentType;
    this.encoding = encoding;
  }
  public void setError(Exception e) {
    this.responseException = e;
  }
  @Override
  public T get() throws InterruptedException, ExecutionException {
    T result = null;
    if (responseException != null) {
      throw new ExecutionException(responseException);
    }
    if (response != null) {
      GenericResponseParser<T> parser = new GenericResponseParser<T>(responseType, actualTypes, contentType, encoding);
      try {
        result = (T) parser.parseResponse(response);
      } catch (MobileException e) {
        throw new ExecutionException(e);
      }
    }
    return result;
  }
}
