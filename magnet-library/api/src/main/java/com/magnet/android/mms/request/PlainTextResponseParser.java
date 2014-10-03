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
import com.magnet.android.mms.exception.MobileException;
import com.magnet.android.mms.request.marshall.SimpleParamHelper;
import com.magnet.android.mms.utils.logger.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

public class PlainTextResponseParser<T> extends AbstractResponseParser<T> {

  private static final String TAG = PlainTextResponseParser.class.getSimpleName();

  public PlainTextResponseParser(Class<?> objectType, Type[] objectComponentTypes, String contentType, String encodingType) {
    super(objectType, objectComponentTypes, contentType, encodingType);
  }

  protected T parseDecodedResponse(final InputStream responseIs) throws MarshallingException {
    Class<?> actualResponseType = responseType;

    if (SimpleParamHelper.isCollectionClass(responseType) ) {
      if (actualTypes != null && actualTypes.length > 0) {
        // TODO: handle multiple parameterized types; assume only one for now
        actualResponseType = Class.class.cast(actualTypes[0]);
      }
    }
    if (!SimpleParamHelper.isMarshalledAsPrimitiveType(actualResponseType)) {
      throw new MarshallingException("Failed to parse input as primitive type:" + actualResponseType.getName());
    }

    try {
      if (InputStream.class.equals(actualResponseType)) {
       return (T) responseIs;
      } else if (byte[].class.equals(actualResponseType)) {
        // create byte array buffer
        byte[] result = ByteArrayHelper.toByteArray(responseIs);
        return (T) result;
      }

      Object parsed = null;
      T result;
      // read everything as a string
      String responseString = getStringFromInputStream(responseIs);
      parsed = SimpleParamHelper.getPrimitiveAnyFromString(responseString, actualResponseType);

      // should already be handled in the parsing so simply cast it
      result = (T) parsed;

      return result;
    } catch (Exception e) {
      // cast exception most likely. 
      throw new MarshallingException(e);
    } finally {
      try {
        responseIs.close();
      } catch (IOException e) {
        Log.w(TAG, "unexpected IOException closing inputstream");
      }
    }
  }

  private String getStringFromInputStream(InputStream is) throws MobileException {
    if (is == null) {
      return null;
    }
    StringBuilder result = new StringBuilder();
    try {
      int len = is.available();
      if (len == 0)
        len = 8192;
      byte[] buf = new byte[len];
      int count;
      while ((count=is.read(buf, 0, buf.length)) >= 0) {
        result.append(new String(buf, 0, count));
      }
    } catch (IOException e) {
      e.printStackTrace();
      throw new MobileException("failed to get string from inputstream", e);
    } finally {
      try {
        is.close();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return result.toString();
  }
}
