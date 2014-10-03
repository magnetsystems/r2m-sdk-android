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
import com.magnet.android.mms.connection.GenericRestConnectionService;
import com.magnet.android.mms.request.marshall.SimpleParamHelper;

import java.lang.reflect.Type;

public class ParserFactory<T> {

  private final Class<T> parsedClazz;
  private final Type[] componentTypes;

  /**
   * Constructs a type-safe ParserFactory for parsing a stream into a POJO
   *
   * @param resultClazz expected resulting class
   */
  public ParserFactory(Class<T> resultClazz) {
    this(resultClazz, null);
  }

  /**
   * Constructs a type-safe ParserFactory for parsing a stream into a POJO of generic type
   *
   * @param resultClazz          expected resulting class
   * @param resultComponentTypes expected component types if the result class is a generic type; null otherwise
   */
  public ParserFactory(Class<T> resultClazz, Type[] resultComponentTypes) {
    parsedClazz = resultClazz;
    componentTypes = resultComponentTypes;
  }

  /**
   * Create instance of ResponseParser
   *
   * @param contentType  Content type; see {@link com.magnet.android.core.GenericRestConstants}
   * @param encodingType optional encoding type such as base64; see {@link com.magnet.android.core.GenericRestConstants}
   * @return Instance of ResponseParser ready for parsing
   */
  public ResponseParser<T> createInstance(String contentType, String encodingType) {
    if (TextUtils.isEmpty(contentType)) {
      if (SimpleParamHelper.isMarshalledAsPrimitiveType(parsedClazz)) {
        contentType = GenericRestConstants.CONTENT_TYPE_TEXT_PLAIN;
      } else {
        // default back to JSON
        contentType = GenericRestConstants.CONTENT_TYPE_JSON;
      }
    }
    if (SimpleParamHelper.isMarshalledAsPrimitiveType(parsedClazz) && contentType.contains(GenericRestConstants.CONTENT_TYPE_TEXT_PLAIN)) {

      return new PlainTextResponseParser(parsedClazz, componentTypes, contentType, encodingType);
    } else {
      return new GenericResponseParser<T>(parsedClazz, componentTypes, contentType, encodingType);
    }
  }
}
