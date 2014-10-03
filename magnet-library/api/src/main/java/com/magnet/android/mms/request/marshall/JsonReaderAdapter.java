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

package com.magnet.android.mms.request.marshall;

import com.magnet.android.mms.exception.MarshallingException;

/**
 * An interface that can be used to bind a JSON parser
 * implementation.
 */
public interface JsonReaderAdapter {


  public static final int TOKEN_TYPE_FIELD_VALUE = 0;
  public static final int TOKEN_TYPE_START_OBJECT = 1;
  public static final int TOKEN_TYPE_END_OBJECT = 2;
  public static final int TOKEN_TYPE_START_ARRAY = 3;
  public static final int TOKEN_TYPE_END_ARRAY = 4;
  public static final int TOKEN_TYPE_FIELD_NAME = 5;
  public static final int TOKEN_TYPE_NULL = 6;
  public static final int TOKEN_TYPE_END_DOCUMENT = 7;

  int getTokenType() throws MarshallingException;

  void consumeStart() throws MarshallingException;

  void consumeEnd() throws MarshallingException;

  void consumeNull()  throws MarshallingException;

  void consumeStartArray() throws MarshallingException;

  void consumeEndArray() throws MarshallingException;

  void finishDocument() throws MarshallingException;

  String getAttributeContent() throws MarshallingException;

  String getAttributeContent(Class<?> type) throws MarshallingException;

  String getAttributeName() throws MarshallingException;

  boolean isDocumentFinished() throws MarshallingException;
}
