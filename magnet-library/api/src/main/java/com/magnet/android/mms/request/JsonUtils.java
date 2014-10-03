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

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.magnet.android.mms.exception.MobileRuntimeException;
import com.magnet.android.mms.utils.logger.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

public class JsonUtils {

  private static final String LOG_TAG = JsonUtils.class.getSimpleName();

  public static boolean isStringJson(String strValue) {
    StringReader reader = new StringReader(strValue);
    try {
      return isJson(reader);
    } finally {
      reader.close();
    }
  }

  public static boolean isInputStreamJson(InputStream is, boolean autoclose) {
    InputStreamReader reader = null;

    try {
      reader = new InputStreamReader(is, "UTF-8");
      return isJson(reader);

    } catch (UnsupportedEncodingException e) {
      throw new MobileRuntimeException(e);
    } finally {
      if (reader != null) {
        try {
          if (autoclose) {
            reader.close();
          }
        } catch (IOException e) {
            Log.w(LOG_TAG, "JSON IO exception:" + e.getMessage());
        }
      }
    }
  }

  private static boolean isJson(Reader reader) {
    boolean result = false;
    try {
      JsonReader jr = new JsonReader(reader);
      jr.setLenient(true);
      JsonToken token = jr.peek();
      result = token.equals(JsonToken.BEGIN_OBJECT) || token.equals(JsonToken.BEGIN_ARRAY);
    } catch (Exception e) {
      Log.w(LOG_TAG, "JsonReader exception:" + e.getMessage());
    }
    return result;
  }
}
