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

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * An interface that can be used to bind a JSON writer
 * implementation.
 */
public interface JsonWriterAdapter {

  void startDocument() throws MarshallingException;

  void startObject() throws MarshallingException;

  void startNamedObject(String objectName) throws MarshallingException;

  void endObject() throws MarshallingException;

  void startArray(String string) throws MarshallingException;

  void beginArray() throws MarshallingException;

  void endArray() throws MarshallingException;

  void finishDocument() throws MarshallingException;

  void writeFieldName(String name) throws MarshallingException ;

  void writeField(String name, Object toWrite)
    throws MarshallingException;

  void writeFieldValue(Object attributeValue)
      throws MarshallingException;

  void writeNullValue() throws MarshallingException;

  void writeValue(boolean value) throws MarshallingException;

  void writeValue(double value) throws MarshallingException;

  void writeValue(long value) throws MarshallingException;

  void writeValue(int value) throws MarshallingException;

  void writeValue(float value) throws MarshallingException;

  void writeValue(BigDecimal value) throws MarshallingException;

  void writeValue(BigInteger value) throws MarshallingException;

  void writeValue(String value) throws MarshallingException;

  void writeValue(byte[] value) throws MarshallingException;


}
