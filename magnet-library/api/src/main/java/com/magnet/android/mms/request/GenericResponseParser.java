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

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.magnet.android.core.GenericRestConstants;
import com.magnet.android.mms.exception.MobileException;
import com.magnet.android.mms.request.marshall.GsonStreamReader;
import com.magnet.android.mms.exception.MarshallingException;
import com.magnet.android.mms.request.marshall.SimpleParamHelper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Use this class to parse a general JSON payload originated from direct rest API calls
 */
public class GenericResponseParser<T> extends AbstractResponseParser<T> {

  private static final String TAG = GenericResponseParser.class.getSimpleName();
  private Gson genericGson;

  public final static String PARSER_NAME = "GENERIC_JSON";

  private PlainTextResponseParser textParser = null;

  public GenericResponseParser(Class<?> objectType) {
    super(objectType);
    initGson();
  }

  public GenericResponseParser(Class<?> objectType, String contentType, String encodingType) {
    super(objectType, contentType, encodingType);
    initGson();
  }
  public GenericResponseParser(Class<?> objectType, Type[] objectComponentTypes, String contentType, String encodingType) {
    super(objectType, objectComponentTypes, contentType, encodingType);
    initGson();
  }

  private void initGson() {
    genericGson = new Gson();

  }
  protected T fromJsonToPojo(String strJson, Type objectClass) {
    return genericGson.fromJson(strJson, objectClass);
  }

  private Collection fromJsonToPojoCollection(JsonReader jr, Class<?> bclass) throws IOException {
    List result = new ArrayList();
    // process each element in the array
    jr.beginArray();
    try {
      while (jr.hasNext()) {
        Object obj = genericGson.fromJson(jr, bclass);
        ((ArrayList) result).add(obj);
      }
    } finally {
      jr.endArray();
    }
    return result;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public T parseDecodedResponse(InputStream responseInputStream) throws MarshallingException {
    if (void.class.equals(responseType) || Void.class.equals(responseType)) {
      return (T) null;
    }
    if (responseInputStream == null || responseType == null) {
      return (T) null;
    }
    try {
      GsonStreamReader gr = new GsonStreamReader(responseInputStream);
      T parsed = null;

      if (gr.getReader().peek() == JsonToken.BEGIN_ARRAY) {
        parsed = (T) fromJsonToPojoCollection(gr.getReader(), actualResponseType);
      } else {
        parsed = genericGson.fromJson(gr.getReader(), actualResponseType);
      }
      return parsed;
    } catch (IOException e) {
      throw new MarshallingException(e);
    } catch (Exception e) {
      throw new MarshallingException(e);
    }
  }

}
